package com.campusform.server.identity.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 존재 여부 확인 응답 DTO
 * 
 * 이메일로 사용자 존재 여부를 확인한 결과를 담는 객체입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserExistsResponse {
    private boolean exists;
    private Long userId;
    private String nickname;
    private String email;
    private String profileImageUrl;

    public static UserExistsResponse found(Long userId, String nickname, String email, String profileImageUrl) {
        return new UserExistsResponse(true, userId, nickname, email, profileImageUrl);
    }

    public static UserExistsResponse notFound(String email) {
        return new UserExistsResponse(false, null, null, email, null);
    }
}
