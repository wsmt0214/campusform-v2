package com.campusform.server.recruiting.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SMS 발송 미리보기 응답")
@Getter
@Builder
@AllArgsConstructor // 빌더 패턴은 전체 생성자가 필요합니다.
@NoArgsConstructor  // 기본 생성자도 있으면 좋습니다 (JSON 변환 시 안전)
public class SmsPreviewResponse {

    @Schema(description = "미리보기 메시지 개수", example = "1")
    private int count;
    @Schema(description = "개별 미리보기 메시지 목록")
    private List<PreviewMessage> messages;

    @Schema(description = "개별 SMS 미리보기 정보")
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    // static선언을 해줘야 독립적으로 사용가능하다
    public static class PreviewMessage {
        @Schema(description = "지원자 ID", example = "1")
        private Long applicantId;
        @Schema(description = "수신자 이름", example = "홍길동")
        private String name;
        @Schema(description = "수신자 전화번호", example = "010-1234-5678")
        private String phoneNumber;
        @Schema(description = "지원 정보 (학교, 전공 등)", example = "캠퍼스대학교 컴퓨터공학과")
        private String info;
        @Schema(description = "발송될 SMS 내용", example = "안녕하세요, 홍길동님. 캠퍼스폼 서류 전형에 합격하신 것을 축하드립니다.")
        private String content;
    }
}
