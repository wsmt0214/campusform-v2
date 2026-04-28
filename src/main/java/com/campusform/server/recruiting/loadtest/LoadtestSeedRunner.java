package com.campusform.server.recruiting.loadtest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadtestSeedRunner implements CommandLineRunner {

    private final UserJpaRepository userJpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final ApplicantJpaRepository applicantJpaRepository;
    private final SheetSyncLoadtestDataset dataset;
    private final SheetSyncLoadtestProperties props;

    @Override
    @Transactional
    public void run(String... args) {
        String email = "loadtest@campusform.local";
        User user = userJpaRepository.findByEmail(email)
                .orElseGet(() -> userJpaRepository.saveAndFlush(User.create(email, "Loadtest", null)));

        Project project = Project.create(
                "Loadtest 프로젝트",
                user.getId(),
                SheetSyncLoadtestDataset.LOADTEST_SHEET_URL,
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );
        // RequiredFieldMapping 파라미터 순서: name, school, major, gender, phone, email, position
        // loadtest 시트 컬럼 인덱스: name=1, email=2, phone=3, gender=4, school=5, major=6, position=7
        project.addMapping(new RequiredFieldMapping(1, 5, 6, 4, 3, 2, 7));
        project = projectJpaRepository.saveAndFlush(project);

        seedApplicantsForSync(project.getId());

        log.info("[loadtest seed] userId={}, projectId={}, applicantId={}",
                user.getId(), project.getId(), null);
        log.info("[loadtest usage] add headers: X-Loadtest-UserId: {}, X-Loadtest-Email: {}",
                user.getId(), user.getEmail());
        log.info("[loadtest usage] PATCH /api/projects/{}/applicants/{}?stage=DOCUMENT body={{\"status\":\"PASS\"}}",
                project.getId(), "applicantId");
    }

    private void seedApplicantsForSync(Long projectId) {
        String scenario = props.getScenario() != null ? props.getScenario().trim().toLowerCase() : "mixed";
        if ("all_new".equals(scenario)) {
            log.info("[loadtest seed] scenario=all_new skip seeding existing applicants");
            return;
        }

        SheetSyncLoadtestDataset.DatasetRows rows = dataset.generateRows();

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

            // mixed 또는 all_changed_extra
            String differentPhone = row.phone() + "-old";
            Applicant a = Applicant.create(projectId, row.name(), row.email(), differentPhone, row.gender(),
                    row.school(), row.major(), row.position());
            addExtraAnswersWithDifferentValues(a, row);
            applicants.add(a);
        }

        applicantJpaRepository.saveAllAndFlush(applicants);

        log.info("[loadtest seed] applicants total={}, unchanged={}, changed={}, new={}",
                rows.all().size(), rows.unchanged().size(), rows.changed().size(), rows.newly().size());
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

