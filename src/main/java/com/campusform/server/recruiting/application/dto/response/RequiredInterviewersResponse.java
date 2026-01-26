package com.campusform.server.recruiting.application.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 필수 면접관 목록 조회 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class RequiredInterviewersResponse {

    private final List<Long> adminIds;

    public static RequiredInterviewersResponse of(List<Long> adminIds) {
        return new RequiredInterviewersResponse(adminIds);
    }
}
