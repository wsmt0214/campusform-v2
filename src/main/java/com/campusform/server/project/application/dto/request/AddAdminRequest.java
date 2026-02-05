package com.campusform.server.project.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 추가 요청 DTO
 */
@Schema(description = "관리자 추가 요청")
@Getter
@NoArgsConstructor
public class AddAdminRequest {

    @Schema(description = "추가할 관리자의 이메일", example = "admin@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;
}
