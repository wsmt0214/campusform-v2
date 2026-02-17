package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 면접 테스트 데이터 자동 생성 API 응답 DTO
 * 요약 + 면접관별 선택 시간 블록 + 지원자별 선택 시간 슬롯 리스트를 담습니다.
 */
@Schema(description = "면접 테스트 데이터 생성 결과")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewTestDataGenerateResponse {

    @Schema(description = "요약: 면접관 수")
    private int interviewerCount;
    @Schema(description = "요약: 생성된 면접관 가용 시간 블록 수 (30분 단위)")
    private int totalInterviewerBlocks;
    @Schema(description = "요약: 대상 지원자 수 (서류 합격자)")
    private int applicantCount;
    @Schema(description = "요약: 생성된 지원자 슬롯 선택 수")
    private int totalApplicantSlots;
    @Schema(description = "요약: 적용한 면접관 날짜 참여 확률 (0.0~1.0)")
    private double dayParticipationRate;
    @Schema(description = "요약: 적용한 지원자 슬롯 선택 확률 (0.0~1.0)")
    private double slotSelectionRate;
    @Schema(description = "요약 메시지")
    private String message;

    @Schema(description = "면접관별로 선택한 시간 블록 목록 (한 눈에 보기)")
    private List<InterviewerEntry> interviewerResults;
    @Schema(description = "지원자별로 선택한 시간 슬롯 목록 (한 눈에 보기)")
    private List<ApplicantEntry> applicantResults;

    // --- 면접관 1명 분: adminId, nickname, 날짜별 시간 블록들
    @Schema(description = "면접관 한 명의 생성 결과")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewerEntry {
        @Schema(description = "면접관 사용자 ID")
        private Long adminId;
        @Schema(description = "면접관 닉네임")
        private String nickname;
        @Schema(description = "날짜별 선택한 30분 블록 목록")
        private List<DayBlocks> availabilities;
    }

    @Schema(description = "특정 날짜의 시간 블록 목록")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayBlocks {
        @Schema(description = "면접 날짜")
        private LocalDate date;
        @Schema(description = "해당 날짜에 선택한 30분 블록들 (시작~종료)")
        private List<TimeBlock> timeBlocks;
    }

    @Schema(description = "30분 단위 시간 블록")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeBlock {
        private LocalTime startTime;
        private LocalTime endTime;
    }

    // --- 지원자 1명 분: applicantId, name, 날짜별 슬롯들
    @Schema(description = "지원자 한 명의 생성 결과")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantEntry {
        @Schema(description = "지원자 ID")
        private Long applicantId;
        @Schema(description = "지원자 이름")
        private String name;
        @Schema(description = "날짜별 선택한 면접 슬롯 목록")
        private List<DaySlots> selections;
    }

    @Schema(description = "특정 날짜의 면접 슬롯 목록")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySlots {
        @Schema(description = "면접 날짜")
        private LocalDate date;
        @Schema(description = "해당 날짜에 선택한 슬롯들 (시작~종료)")
        private List<SlotTime> slots;
    }

    @Schema(description = "면접 슬롯 1개 (설정된 slotDuration 기준)")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotTime {
        private LocalTime startTime;
        private LocalTime endTime;
    }

    /** 요약 + 면접관별/지원자별 상세 리스트로 응답 생성 */
    public static InterviewTestDataGenerateResponse of(
            int interviewerCount,
            int totalInterviewerBlocks,
            int applicantCount,
            int totalApplicantSlots,
            double dayParticipationRate,
            double slotSelectionRate,
            List<InterviewerEntry> interviewerResults,
            List<ApplicantEntry> applicantResults) {
        String message = String.format(
                "테스트 데이터 생성 완료 | 면접관: %d명 (블록 %d개) | 지원자: %d명 (슬롯 %d개) | 날짜참여율: %.0f%% | 슬롯선택율: %.0f%%",
                interviewerCount, totalInterviewerBlocks,
                applicantCount, totalApplicantSlots,
                dayParticipationRate * 100, slotSelectionRate * 100);
        return new InterviewTestDataGenerateResponse(
                interviewerCount, totalInterviewerBlocks,
                applicantCount, totalApplicantSlots,
                dayParticipationRate, slotSelectionRate,
                message,
                interviewerResults != null ? interviewerResults : List.of(),
                applicantResults != null ? applicantResults : List.of());
    }
}
