package com.campusform.server.recruiting.loadtest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import com.campusform.server.global.config.LoadtestSecurityConfig;
import com.campusform.server.global.loadtest.SheetSyncLoadtestDataset;
import com.campusform.server.global.loadtest.SheetSyncLoadtestDataset.Row;
import com.campusform.server.global.loadtest.SheetSyncLoadtestProperties;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.infrastructure.persistence.UserJpaRepository;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.value.RequiredFieldMapping;
import com.campusform.server.project.infrastructure.persistence.ProjectJpaRepository;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.infrastructure.persistence.ApplicantJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * loadtest 환경에서 재현 가능한 seed 데이터를 준비하는 책임
 *
 * CommandLineRunner가 실행되지 않는 환경에서도 seed가 누락되지 않도록
 * ApplicationReadyEvent에서도 동일 로직을 한 번 더 보장하는 구조
 */
@Slf4j
@Service
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadtestSeedService {

    private static final String LOADTEST_EMAIL = "loadtest@campusform.local";

    private final UserJpaRepository userJpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final ApplicantJpaRepository applicantJpaRepository;
    private final SheetSyncLoadtestDataset dataset;
    private final SheetSyncLoadtestProperties props;
    private final PlatformTransactionManager transactionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnReady() {
        seedIfNeeded();
    }

    public void seedIfNeeded() {
        log.info("[loadtest seed] start rows={}, extraColumns={}, scenario={}, randomSeed={}, ratio(new/unchanged/changed)={}/{}/{}",
                props.getRows(), props.getExtraColumns(), props.getScenario(), props.getRandomSeed(),
                props.getNewRatio(), props.getUnchangedRatio(), props.getChangedRatio());

        // ddl-auto=create 환경에서는 스키마 생성과 seed 트리거 타이밍이 겹치며 테이블 미존재 예외가 발생할 수 있음
        // 운영 로직과 분리된 loadtest 전용 seed이므로 스키마가 준비될 때까지 짧게 재시도하는 방식을 허용함
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                if (userJpaRepository.existsByEmail(LOADTEST_EMAIL)
                        && projectJpaRepository.findBySheetUrl(SheetSyncLoadtestDataset.LOADTEST_SHEET_URL).isPresent()) {
                    log.info("[loadtest seed] skip (already seeded) email={}, sheetUrl={}",
                            LOADTEST_EMAIL, SheetSyncLoadtestDataset.LOADTEST_SHEET_URL);
                    return;
                }
                break;
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean looksLikeMissingTable = msg.contains("doesn't exist") || msg.contains("does not exist");
                if (!looksLikeMissingTable || attempts >= 10) {
                    log.error("[loadtest seed] precheck failed attempts={}", attempts, e);
                    throw e;
                }
                log.warn("[loadtest seed] schema not ready attempts={} retrying", attempts);
                sleepQuietly(500);
            }
        }

        User user = userJpaRepository.findByEmail(LOADTEST_EMAIL)
                .orElseGet(() -> userJpaRepository.saveAndFlush(User.create(LOADTEST_EMAIL, "Loadtest", null)));

        Project project = projectJpaRepository.findBySheetUrl(SheetSyncLoadtestDataset.LOADTEST_SHEET_URL)
                .orElseGet(() -> {
                    Project p = Project.create(
                            "Loadtest 프로젝트",
                            user.getId(),
                            SheetSyncLoadtestDataset.LOADTEST_SHEET_URL,
                            LocalDate.now(),
                            LocalDate.now().plusDays(7)
                    );
                    // RequiredFieldMapping 파라미터 순서: name, school, major, gender, phone, email, position.
                    // loadtest 시트 컬럼 인덱스: name=1, email=2, phone=3, gender=4, school=5, major=6, position=7.
                    p.addMapping(new RequiredFieldMapping(1, 5, 6, 4, 3, 2, 7));
                    return projectJpaRepository.saveAndFlush(p);
                });

        seedApplicantsForSync(project.getId());

        log.info("[loadtest seed] userId={}, projectId={}, applicants={}",
                user.getId(), project.getId(), applicantJpaRepository.countByProjectId(project.getId()));
        log.info("[loadtest usage] headers {}={}, {}={}",
                LoadtestSecurityConfig.USER_ID_HEADER, user.getId(),
                LoadtestSecurityConfig.EMAIL_HEADER, user.getEmail());
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void seedApplicantsForSync(Long projectId) {
        String scenario = props.getScenario() != null ? props.getScenario().trim().toLowerCase() : "mixed";
        if ("all_new".equals(scenario)) {
            log.info("[loadtest seed] scenario=all_new skip seeding existing applicants");
            return;
        }

        SheetSyncLoadtestDataset.DatasetRows rows = dataset.generateRows();
        log.info("[loadtest seed] generated rows total={}, unchanged={}, changed={}, new={}",
                rows.all().size(), rows.unchanged().size(), rows.changed().size(), rows.newly().size());

        List<Applicant> applicants = new ArrayList<>();
        for (Row row : rows.unchanged()) {
            Applicant a = Applicant.create(projectId, row.name(), row.email(), row.phone(), row.gender(),
                    row.school(), row.major(), row.position());
            addExtraAnswers(a, row);
            applicants.add(a);
        }

        for (Row row : rows.changed()) {
            if ("all_changed_base_only".equals(scenario)) {
                String differentPhone = row.phone() + "-old";
                Applicant a = Applicant.create(projectId, row.name(), row.email(), differentPhone, row.gender(),
                        row.school(), row.major(), row.position());
                addExtraAnswers(a, row);
                applicants.add(a);
                continue;
            }

            String differentPhone = row.phone() + "-old";
            Applicant a = Applicant.create(projectId, row.name(), row.email(), differentPhone, row.gender(),
                    row.school(), row.major(), row.position());
            addExtraAnswersWithDifferentValues(a, row);
            applicants.add(a);
        }

        // 단일 대량 트랜잭션은 InnoDB redo log를 빠르게 소모해 정체가 발생할 수 있음
        // loadtest seed는 실험 재현성 목적이므로 배치 단위로 커밋해서 안정적으로 데이터를 적재하는 방식 선택
        int batchSize = 500;
        int total = applicants.size();
        for (int from = 0; from < total; from += batchSize) {
            int to = Math.min(total, from + batchSize);
            List<Applicant> batch = applicants.subList(from, to);
            TransactionTemplate tx = newRequiresNewTransactionTemplate();
            tx.executeWithoutResult(status -> applicantJpaRepository.saveAllAndFlush(batch));
            log.info("[loadtest seed] applicants batch committed {}/{}", to, total);
        }

        log.info("[loadtest seed] applicants total={}, unchanged={}, changed={}, new={}",
                rows.all().size(), rows.unchanged().size(), rows.changed().size(), rows.newly().size());
    }

    private TransactionTemplate newRequiresNewTransactionTemplate() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }

    private void addExtraAnswers(Applicant applicant, Row row) {
        int startIdx = SheetSyncLoadtestDataset.BASE_COLUMNS;
        for (int i = 0; i < row.extraAnswers().size(); i++) {
            String q = "extra_q_" + (i + 1);
            String ans = row.extraAnswers().get(i);
            applicant.addExtraAnswer(q, ans, startIdx + i);
        }
    }

    private void addExtraAnswersWithDifferentValues(Applicant applicant, Row row) {
        int startIdx = SheetSyncLoadtestDataset.BASE_COLUMNS;
        for (int i = 0; i < row.extraAnswers().size(); i++) {
            String q = "extra_q_" + (i + 1);
            String ans = row.extraAnswers().get(i) + "_old";
            applicant.addExtraAnswer(q, ans, startIdx + i);
        }
    }
}

