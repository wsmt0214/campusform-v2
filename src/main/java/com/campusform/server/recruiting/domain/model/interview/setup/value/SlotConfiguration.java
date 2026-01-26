package com.campusform.server.recruiting.domain.model.interview.setup.value;

import lombok.Getter;

/**
 * 슬롯 설정 값 객체
 * 도메인 규칙에 따라 주요 파라미터들의 유효성을 검증하고 불변성을 보장
 */
@Getter
public class SlotConfiguration {

    private final Integer slotDurationMin;
    private final Integer slotBreakMin;
    private final Integer maxApplicantsPerSlot;
    private final Integer minInterviewersPerSlot;
    private final Integer maxInterviewersPerSlot;

    private SlotConfiguration(
            Integer slotDurationMin,
            Integer slotBreakMin,
            Integer maxApplicantsPerSlot,
            Integer minInterviewersPerSlot,
            Integer maxInterviewersPerSlot) {
        validate(slotDurationMin, slotBreakMin, maxApplicantsPerSlot, minInterviewersPerSlot,
                maxInterviewersPerSlot);
        this.slotDurationMin = slotDurationMin;
        this.slotBreakMin = slotBreakMin;
        this.maxApplicantsPerSlot = maxApplicantsPerSlot;
        this.minInterviewersPerSlot = minInterviewersPerSlot;
        this.maxInterviewersPerSlot = maxInterviewersPerSlot;
    }

    /* 슬롯 설정 생성 팩토리 메서드 */
    public static SlotConfiguration of(
            Integer slotDurationMin,
            Integer slotBreakMin,
            Integer maxApplicantsPerSlot,
            Integer minInterviewersPerSlot,
            Integer maxInterviewersPerSlot) {
        return new SlotConfiguration(slotDurationMin, slotBreakMin, maxApplicantsPerSlot,
                minInterviewersPerSlot, maxInterviewersPerSlot);
    }

    /* 슬롯 설정 검증 */
    private void validate(
            Integer slotDurationMin,
            Integer slotBreakMin,
            Integer maxApplicantsPerSlot,
            Integer minInterviewersPerSlot,
            Integer maxInterviewersPerSlot) {

        if (slotDurationMin == null || slotDurationMin <= 0) {
            throw new IllegalArgumentException("면접 예상 소요 시간(slotDurationMin)은 1분 이상이어야 합니다.");
        }
        // 요구사항: 5분 단위만 허용
        if (slotDurationMin % 5 != 0) {
            throw new IllegalArgumentException("면접 예상 소요 시간(slotDurationMin)은 5분 단위만 허용됩니다.");
        }

        if (slotBreakMin == null || slotBreakMin < 0) {
            throw new IllegalArgumentException("면접 휴식 시간(slotBreakMin)은 0분 이상이어야 합니다.");
        }

        if (maxApplicantsPerSlot == null || maxApplicantsPerSlot < 1) {
            throw new IllegalArgumentException("타임 당 최대 지원자 수(maxApplicantsPerSlot)는 1 이상이어야 합니다.");
        }

        if (minInterviewersPerSlot == null || maxInterviewersPerSlot == null) {
            throw new IllegalArgumentException("타임 당 면접관 수(min/maxInterviewersPerSlot)가 필요합니다.");
        }
        if (minInterviewersPerSlot < 1) {
            throw new IllegalArgumentException("타임 당 최소 면접관 수(minInterviewersPerSlot)는 1 이상이어야 합니다.");
        }
        if (minInterviewersPerSlot > maxInterviewersPerSlot) {
            throw new IllegalArgumentException("minInterviewersPerSlot은 maxInterviewersPerSlot 이하이어야 합니다.");
        }
    }

    /* 슬롯 길이 계산 (duration + break) */
    public int getSlotLengthMin() {
        return slotDurationMin + slotBreakMin;
    }
}
