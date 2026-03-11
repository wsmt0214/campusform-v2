package com.campusform.server.identity.domain.event;

/**
 * 사용자 회원가입 완료 이벤트
 *
 * Identity Context에서 신규 사용자가 회원가입을 완료했을 때 발행됩니다.
 * Project Context에서 이 이벤트를 수신하여 Google Sheets 권한 요청 플로우를 처리
 */
public record UserRegisteredEvent(
        Long userId, // 사용자 ID
        String email // 사용자 이메일
) {}
