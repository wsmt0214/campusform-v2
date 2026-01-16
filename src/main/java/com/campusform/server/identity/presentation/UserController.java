package com.campusform.server.identity.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.dto.response.UserExistsResponse;
import com.campusform.server.identity.application.service.UserQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;

    /**
     * 이메일로 회원 존재 여부 확인
     */
    @GetMapping("/exists")
    public ResponseEntity<UserExistsResponse> existsByEmail(@RequestParam String email) {
        UserExistsResponse response = userQueryService.findByEmail(email);
        return ResponseEntity.ok(response);
    }
}
