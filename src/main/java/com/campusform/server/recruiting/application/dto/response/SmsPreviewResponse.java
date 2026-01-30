package com.campusform.server.recruiting.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@Builder
@AllArgsConstructor // 빌더 패턴은 전체 생성자가 필요합니다.
@NoArgsConstructor  // 기본 생성자도 있으면 좋습니다 (JSON 변환 시 안전)
public class SmsPreviewResponse {

    private int count;
    private List<PreviewMessage> messages;
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    // static선언을 해줘야 독립적으로 사용가능하다
    public static class PreviewMessage {
        private Long applicantId;
        private String name;
        private String phoneNumber;
        private String info;
        private String content;
    }
}
