package com.campusform.server.recruiting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;

/**
 * 도메인 계층의 Repository 인터페이스
 * 
 * 특정 기술에 의존하지 않고 도메인 관점에서 인터페이스를 정의합니다.
 * 구현체는 infrastructure 계층에서 제공됩니다.
 */
public interface ApplicantRepository {

    void save(Applicant applicant);

    /**
     * 프로젝트ID, 이름, 전화번호로 지원자 조회
     */
    Optional<Applicant> findByProjectIdAndNameAndPhone(Long projectId, String name, String phone);

    /**
     * 여러 ID로 지원자 목록 조회
     * 슬롯별 지원자 정보 조회에 사용됩니다.
     */
    List<Applicant> findByIds(List<Long> applicantIds);
}
