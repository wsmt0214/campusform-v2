package com.campusform.server.recruiting.loadtest;

import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    @Override
    @Transactional
    public void run(String... args) {
        String email = "loadtest@campusform.local";
        User user = userJpaRepository.findByEmail(email)
                .orElseGet(() -> userJpaRepository.saveAndFlush(User.create(email, "Loadtest", null)));

        Project project = Project.create(
                "Loadtest 프로젝트",
                user.getId(),
                "https://sheet.local/loadtest",
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );
        project.addMapping(new RequiredFieldMapping(1, 2, 3, 4, 5, 6, 7));
        project = projectJpaRepository.saveAndFlush(project);

        Applicant applicant = applicantJpaRepository.saveAndFlush(
                Applicant.create(
                        project.getId(),
                        "부하테스트지원자",
                        "applicant-loadtest@campusform.local",
                        "010-9999-0000",
                        "남",
                        "학교",
                        "전공",
                        "백엔드"
                )
        );

        log.info("[loadtest seed] userId={}, projectId={}, applicantId={}",
                user.getId(), project.getId(), applicant.getId());
        log.info("[loadtest usage] add headers: X-Loadtest-UserId: {}, X-Loadtest-Email: {}",
                user.getId(), user.getEmail());
        log.info("[loadtest usage] PATCH /api/projects/{}/applicants/{}?stage=DOCUMENT body={{\"status\":\"PASS\"}}",
                project.getId(), applicant.getId());
    }
}

