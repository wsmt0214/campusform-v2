package com.campusform.server.recruiting.domain.model.event;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;

// 변경된 상태와 문자 발송에 필요한 정보(전화번호, 이름 등)
public record ApplicantUpdated(
        Long applicantId,
        Long projectId,
        String applicantName,
        String applicantPhone,
        String positionName,
        ApplicantStatus status,
        StageStatus stage
){
}
