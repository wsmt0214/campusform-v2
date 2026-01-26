package com.campusform.server.identity.application.dto.response;

/**
 * 프로필 이미지 삭제 응답 DTO
 */
public record DeleteProfileImageResponse(
        String message
) {
    public static DeleteProfileImageResponse success() {
        return new DeleteProfileImageResponse("프로필 이미지가 삭제되었습니다.");
    }
}
