package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;

/**
 * Spring Data JPA용 InterviewSetting Repository
 */
@Repository
public interface InterviewSettingJpaRepository extends JpaRepository<InterviewSetting, Long> {

    Optional<InterviewSetting> findByProjectId(Long projectId);

    /**
     * 지원자 면접 가능 시간 조사 링크 토큰으로 면접 설정 조회
     * 
     * DDD 원칙: InterviewAvailabilityInvestigationLink는 InterviewSetting 애그리거트 루트 안에
     * 포함되므로
     * 루트 애그리거트를 통해 접근합니다.
     * JOIN FETCH를 사용하여 investigationLink를 함께 로드합니다.
     */
    @Query("SELECT s FROM InterviewSetting s " +
            "JOIN FETCH s.investigationLink link " +
            "WHERE link.token = :token")
    Optional<InterviewSetting> findByInvestigationLinkToken(@Param("token") String token);
}
