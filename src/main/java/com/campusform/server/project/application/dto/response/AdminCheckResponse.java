package com.campusform.server.project.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 확인 응답 DTO
 * 
 * 이메일로 관리자 존재 여부를 확인한 결과를 담는 객체입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCheckResponse {

    private boolean exists;
    private Long userId;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private String errorMessage;

    public static AdminCheckResponse success(Long userId, String nickname, String email, String profileImageUrl) {
        return new AdminCheckResponse(true, userId, nickname, email, profileImageUrl, null);
    }

    public static AdminCheckResponse notFound(String email) {
        return new AdminCheckResponse(false, null, null, null, email,
                "해당 이메일로 가입된 회원이 없습니다: " + email);
    }
}
