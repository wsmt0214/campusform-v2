package com.campusform.server.recruiting.domain.model.interview.setup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.recruiting.domain.model.interview.setup.value.DateRange;
import com.campusform.server.recruiting.domain.model.interview.setup.value.SlotConfiguration;
import com.campusform.server.recruiting.domain.model.interview.setup.value.TimeRange;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 면접 설정 Entity
 * 면접 관련 규칙 및 정책을 관리합니다.
 */
@Entity
@Table(name = "interview_settings",
       indexes = @Index(name = "idx_project_id", columnList = "project_id", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewSetting {

    @Id
    @GeneratedValue
    private Long id;

    // 다른 어그리거트 -> 참조 아닌 연관으로 관계 설정
    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    /**
     * 면접 시작 시간
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * 면접 종료 시간
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * 슬롯 길이 (분 단위)
     * 슬롯 = duration + break
     */
    @Column(name = "slot_duration_min", nullable = false)
    private Integer slotDurationMin;

    /**
     * 슬롯 간 휴식 시간 (분 단위)
     */
    @Column(name = "slot_break_min", nullable = false)
    private Integer slotBreakMin = 0;

    /**
     * 슬롯당 최대 지원자 수
     */
    @Column(name = "max_applicants_per_slot", nullable = false)
    private Integer maxApplicantsPerSlot;

    /**
     * 슬롯당 최소 면접관 수
     */
    @Column(name = "min_interviewers_per_slot", nullable = false)
    private Integer minInterviewersPerSlot;

    /**
     * 슬롯당 최대 면접관 수
     */
    @Column(name = "max_interviewers_per_slot", nullable = false)
    private Integer maxInterviewersPerSlot;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // InterviewSetting (루트)
    @OneToMany(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewDay> days = new ArrayList<>();

    @OneToMany(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewRequiredInterviewer> requiredInterviewers = new ArrayList<>();

    @OneToOne(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private InterviewAvailabilityInvestigationLink investigationLink;

    /**
     * InterviewSetting 생성 팩토리 메서드
     * 값 객체를 사용하여 도메인 규칙 검증
     * 생성 시점에 검증하여 불완전한 상황 방지
     */
    public static InterviewSetting create(
        Long projectId,
        TimeRange timeRange,
        SlotConfiguration slotConfig,
        DateRange dateRange) {
        InterviewSetting setting = new InterviewSetting();
        setting.projectId = projectId;
        setting.startTime = timeRange.getStartTime();
        setting.endTime = timeRange.getEndTime();
        setting.slotDurationMin = slotConfig.getSlotDurationMin();
        setting.slotBreakMin = slotConfig.getSlotBreakMin();
        setting.maxApplicantsPerSlot = slotConfig.getMaxApplicantsPerSlot();
        setting.minInterviewersPerSlot = slotConfig.getMinInterviewersPerSlot();
        setting.maxInterviewersPerSlot = slotConfig.getMaxInterviewersPerSlot();

        setting.replaceDays(dateRange.expandToDates());

        // 지원자 면접 가능 시간 조사 링크 생성 (최초 1번만 생성)
        setting.investigationLink = InterviewAvailabilityInvestigationLink.create(setting);

        return setting;
    }

    /**
     * 면접 설정 수정 (값 객체 사용)
     * 값 객체를 사용하여 도메인 규칙 검증
     * 생성 시점에 검증하여 불완전한 상황 방지
     */
    public void update(TimeRange timeRange, SlotConfiguration slotConfig, DateRange dateRange) {
        this.startTime = timeRange.getStartTime();
        this.endTime = timeRange.getEndTime();
        this.slotDurationMin = slotConfig.getSlotDurationMin();
        this.slotBreakMin = slotConfig.getSlotBreakMin();
        this.maxApplicantsPerSlot = slotConfig.getMaxApplicantsPerSlot();
        this.minInterviewersPerSlot = slotConfig.getMinInterviewersPerSlot();
        this.maxInterviewersPerSlot = slotConfig.getMaxInterviewersPerSlot();
        replaceDays(dateRange.expandToDates());
    }

    /**
     * 시간 범위를 값 객체로 반환
     */
    public TimeRange getTimeRange() {
        return TimeRange.of(startTime, endTime);
    }

    /**
     * 슬롯 설정을 값 객체로 반환
     */
    public SlotConfiguration getSlotConfiguration() {
        return SlotConfiguration.of(
            slotDurationMin, slotBreakMin, maxApplicantsPerSlot,
            minInterviewersPerSlot, maxInterviewersPerSlot);
    }

    /**
     * 검증용 - 특정 시간이 이 설정의 시간 범위 내에 포함되는지 확인
     */
    public boolean containsTime(LocalTime time) {
        return getTimeRange().contains(time);
    }

    /**
     * 검증용 - 특정 시간 블록(30분)이 이 설정의 시간 범위 내에 완전히 포함되는지 확인
     */
    public boolean containsTimeBlockStartWith(LocalTime blockStartTime) {
        return getTimeRange().containsBlock(blockStartTime);
    }

    /**
     * 검증용 - 특정 시간 블록(30분)이 이 설정의 시간 범위와 겹치는지 확인
     * 블록의 일부라도 시간 범위와 겹치면 true 반환
     */
    public boolean overlapsWithTimeBlock(LocalTime blockStartTime) {
        return getTimeRange().overlapsWithBlock(blockStartTime);
    }

    /**
     * 블록 시작 시간 검증
     *
     * @param blockStartTime 검증할 블록 시작 시간
     * @throws IllegalArgumentException 블록 시작 시간이 유효하지 않은 경우 (xx:00 또는 xx:30이 아닌 경우)
     */
    public void validateBlockStartTime(LocalTime blockStartTime) {
        if (!TimeRange.isValidBlockStartTime(blockStartTime)) {
            throw new IllegalArgumentException("블록의 시작 시간은 xx:00 또는 xx:30 이어야 합니다. startTime=" + blockStartTime);
        }
    }

    /**
     * 블록이 면접 설정의 시간 범위와 겹치는지 검증
     * 블록의 일부라도 시간 범위와 겹치면 유효한 것으로 판단
     *
     * @param blockStartTime 검증할 블록 시작 시간
     * @throws IllegalArgumentException 블록이 면접 시간 범위를 벗어난 경우
     */
    public void validateBlockWithinTimeRange(LocalTime blockStartTime) {
        if (!overlapsWithTimeBlock(blockStartTime)) {
            throw new IllegalArgumentException(
                "면접 정보 시간 범위를 벗어났습니다. startTime=" + blockStartTime +
                    ", 범위=" + getStartTime() + "~" + getEndTime());
        }
    }

    /**
     * 면접 날짜 목록 교체
     * clear() 후 바로 add()하면 영속성 컨텍스트와 DB 동기화 문제가 발생할 수 있어서
     * diff 기반으로 추가/삭제만 수행
     */
    private void replaceDays(List<LocalDate> interviewDates) {
        if (interviewDates == null || interviewDates.isEmpty()) {
            this.days.clear();
            return;
        }

        // 중복 제거
        Set<LocalDate> newDates = interviewDates.stream()
            .distinct()
            .collect(Collectors.toSet());

        // 기존 날짜와 새 날짜 비교
        Set<LocalDate> existingDates = this.days.stream()
            .map(InterviewDay::getInterviewDate)
            .collect(Collectors.toSet());

        // 새 목록에 없는 기존 날짜 삭제
        List<InterviewDay> toRemove = this.days.stream()
            .filter(day -> !newDates.contains(day.getInterviewDate()))
            .collect(Collectors.toList());
        this.days.removeAll(toRemove);

        // 새 목록에 있는 날짜 추가
        Set<LocalDate> toAdd = newDates.stream()
            .filter(date -> !existingDates.contains(date))
            .collect(Collectors.toSet());
        toAdd.forEach(date -> this.days.add(InterviewDay.create(this, date)));
    }

    /**
     * 필수 면접관 목록 교체
     */
    public void replaceRequiredInterviewers(List<Long> adminIds) {
        if (adminIds == null) {
            throw new IllegalArgumentException("adminIds는 null일 수 없습니다. 빈 리스트로 전체 해제를 표현하세요.");
        }

        // 중복 제거
        Set<Long> newAdminIds = adminIds.stream()
            .distinct()
            .collect(Collectors.toSet());

        // 기존 면접관 ID와 새 면접관 ID 비교
        Set<Long> existingAdminIds = this.requiredInterviewers.stream()
            .map(InterviewRequiredInterviewer::getAdminId)
            .collect(Collectors.toSet());

        // 새 목록에 없는 기존 면접관 삭제
        List<InterviewRequiredInterviewer> toRemove = this.requiredInterviewers.stream()
            .filter(required -> !newAdminIds.contains(required.getAdminId()))
            .collect(Collectors.toList());
        this.requiredInterviewers.removeAll(toRemove);

        // 새 목록에 있는 면접관 추가
        Set<Long> toAdd = newAdminIds.stream()
            .filter(adminId -> !existingAdminIds.contains(adminId))
            .collect(Collectors.toSet());
        toAdd.forEach(adminId -> this.requiredInterviewers.add(InterviewRequiredInterviewer.create(this, adminId)));
    }

    /**
     * 필수 면접관 ID 목록 조회
     */
    public List<Long> getRequiredInterviewerIds() {
        return this.requiredInterviewers.stream()
            .map(InterviewRequiredInterviewer::getAdminId)
            .toList();
    }

    /**
     * 필수 면접관 개별 설정
     */
    public void setRequiredInterviewer(Long adminId, boolean required) {
        if (required) {
            boolean alreadyExists = this.requiredInterviewers.stream()
                .anyMatch(requiredInterviewer -> requiredInterviewer.getAdminId().equals(adminId));
            if (!alreadyExists) {
                this.requiredInterviewers.add(InterviewRequiredInterviewer.create(this, adminId));
            }
        } else {
            this.requiredInterviewers.removeIf(requiredInterviewer -> requiredInterviewer.getAdminId().equals(adminId));
        }
    }
}
