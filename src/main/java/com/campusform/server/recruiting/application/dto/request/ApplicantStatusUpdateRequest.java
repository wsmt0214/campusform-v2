package com.campusform.server.recruiting.application.dto.request;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplicantStatusUpdateRequest {
    private ApplicantStatus status;
}
