package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.application.dto.request.SubmitSlotsRequest;
import com.campusform.server.recruiting.application.dto.response.InterviewSlotListResponse;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.IntervieweeAvailabilitySlotRepository;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;
import com.campusform.server.recruiting.domain.service.InterviewSlotGenerator;

import lombok.RequiredArgsConstructor;

/**
 * 지원자 면접 가능 시간 조사 서비스 (공개 API용)
 * 
 * 토큰 기반으로 지원자가 면접 가능 시간 슬롯을 조회하고 제출합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntervieweeAvailabilityService {

    private final InterviewContextLoader contextLoader;
    private final InterviewerAvailabilityBlockRepository availabilityBlockRepository;
    private final ApplicantRepository applicantRepository;
    private final IntervieweeAvailabilitySlotRepository slotRepository;
    private final InterviewSlotGenerator slotGenerator = new InterviewSlotGenerator(); // 도메인 서비스

    /**
     * 토큰으로 면접 슬롯 목록 조회
     * 공개 API에서 사용되며, 인증 없이 토큰만으로 접근 가능합니다.
     */
    public InterviewSlotListResponse getSlotsByToken(String token) {
        // 토큰으로 프로젝트와 면접 설정 조회
        InterviewContext ctx = contextLoader.loadContextByToken(token);
        InterviewSetting setting = ctx.setting();

        // 면접 정보 설정 기반으로 날짜별 슬롯 생성
        List<InterviewSlotListResponse.DaySlotSummary> summaries = setting.getDays().stream()
                .sorted(Comparator.comparing(InterviewDay::getInterviewDate))
                .map(day -> {
                    LocalDate date = day.getInterviewDate();
                    Long dayId = day.getId();

                    // 해당 날짜의 모든 면접관 가능 시간 블록 조회
                    List<InterviewerAvailabilityBlock> dayBlocks = availabilityBlockRepository
                            .findByInterviewDayId(dayId);

                    // 도메인 서비스를 사용하여 슬롯 생성
                    List<InterviewSlotGenerator.SlotInfo> domainSlots = slotGenerator.generateSlots(
                            setting,
                            dayBlocks);

                    // 도메인 DTO를 응답 DTO로 변환 (availableInterviewerCount가 0보다 큰 슬롯만 포함)
                    // InterviewSlotGenerator에서 이미 0인 슬롯은 생성하지 않으므로 필터링은 방어적 프로그래밍 차원에서 유지
                    List<InterviewSlotListResponse.SlotInfo> slots = domainSlots.stream()
                            .filter(slot -> slot.availableInterviewerCount() > 0) // 면접관이 없는
                                                                                  // 슬롯은 제외
                            .map(slot -> InterviewSlotListResponse.SlotInfo.of(
                                    slot.startTime(), slot.endTime(),
                                    slot.availableInterviewerCount()))
                            .toList();

                    return InterviewSlotListResponse.DaySlotSummary.of(date, slots);
                })
                .toList();

        return InterviewSlotListResponse.of(summaries);
    }

    /**
     * 지원자 면접 가능 슬롯 제출
     * 기존 제출 내역은 삭제하고 새로 제출한 슬롯으로 덮어쓰기합니다.
     */
    @Transactional
    public void submitSlots(String token, SubmitSlotsRequest request) {
        // 토큰으로 프로젝트와 면접 설정 조회
        InterviewContext ctx = contextLoader.loadContextByToken(token);

        // 지원자 면접 가능 시간 제출은 면접 단계(INTERVIEW)에서만 가능
        ctx.project().validateInterviewStage();

        Long projectId = ctx.project().getId();
        InterviewSetting setting = ctx.setting();

        // 이름+전화번호로 지원자 확인
        Applicant applicant = applicantRepository.findByProjectIdAndNameAndPhone(projectId, request.getName(),
                request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원자 정보를 찾을 수 없습니다. 이름과 전화번호를 확인해주세요."));

        // 서류 합격자만 면접 가능 시간 제출 가능 (서류 불합격자는 면접 대상이 아님)
        if (applicant.getDocumentStatus() != ScreeningResult.PASS) {
            throw new IllegalStateException("서류 합격자만 면접 가능 시간을 제출할 수 있습니다.");
        }

        // 기존 제출 내역 삭제 <- 처음 제출이라면 삭제X
        slotRepository.deleteByApplicantId(applicant.getId());

        /**
         * 지원자가 제출한 요청(request)을 통해 IntervieweeAvailabilitySlot 객체 리스트 생성
         * 
         * <pre>
         * 예시:
         * {
         *   "name": "홍길동",
         *   "phone": "010-1234-5678",
         *   "selections": [
         *     { "date": "2024-06-25", "startTimes": ["10:00", "14:00"] },
         *     { "date": "2024-06-26", "startTimes": ["11:00"] }
         *   ]
         * }
         * </pre>
         */

        // 모든 날짜에 대한 실제 생성 가능한 슬롯 목록을 한 번에 생성
        // 날짜별로 (InterviewDay, 생성 가능한 시작 시간 Set)을 저장하는 Map
        Map<LocalDate, DaySlotInfo> daySlotInfoMap = new HashMap<>();
        for (InterviewDay day : setting.getDays()) {
            LocalDate date = day.getInterviewDate();
            Long dayId = day.getId();

            // 해당 날짜의 모든 면접관 가능 시간 블록 조회
            List<InterviewerAvailabilityBlock> dayBlocks = availabilityBlockRepository
                    .findByInterviewDayId(dayId);

            // 실제 생성 가능한 슬롯 목록 생성 (도메인 서비스 사용)
            List<InterviewSlotGenerator.SlotInfo> availableSlots = slotGenerator.generateSlots(setting,
                    dayBlocks);

            // 생성 가능한 슬롯의 시작 시간만 추출 (검증용)
            Set<LocalTime> availableStartTimes = availableSlots.stream()
                    .map(InterviewSlotGenerator.SlotInfo::startTime)
                    .collect(Collectors.toSet());

            daySlotInfoMap.put(date, new DaySlotInfo(day, availableStartTimes));
        }

        // 제출된 슬롯 검증 및 생성
        List<IntervieweeAvailabilitySlot> newSlots = request.getSelections().stream()
                .flatMap(selection -> {
                    LocalDate date = selection.getDate();
                    List<LocalTime> startTimes = selection.getStartTimes();

                    // 같은 시간대를 중복 선택한 경우를 대비해, 한 번만 처리하도록 정규화
                    List<LocalTime> distinctStartTimes = startTimes.stream()
                            .distinct()
                            .toList();

                    // 날짜별 슬롯 정보 조회
                    DaySlotInfo daySlotInfo = daySlotInfoMap.get(date);
                    if (daySlotInfo == null) {
                        throw new IllegalArgumentException("유효하지 않은 면접 날짜입니다. date=" + date);
                    }

                    InterviewDay interviewDay = daySlotInfo.interviewDay();
                    Set<LocalTime> availableStartTimes = daySlotInfo.availableStartTimes();

                    // 제출된 각 시작 시간이 실제 생성 가능한 슬롯인지 검증
                    for (LocalTime startTime : distinctStartTimes) {
                        if (!availableStartTimes.contains(startTime)) {
                            throw new IllegalArgumentException(
                                    String.format("유효하지 않은 슬롯입니다. date=%s, startTime=%s",
                                            date, startTime));
                        }
                    }

                    // 각 시작 시간에 대해 슬롯 생성
                    return distinctStartTimes.stream()
                            .map(startTime -> IntervieweeAvailabilitySlot.create(
                                    applicant.getId(), interviewDay.getId(),
                                    startTime));
                })
                .collect(Collectors.toList());

        // 슬롯 저장
        slotRepository.saveAll(newSlots);
    }

    /**
     * 날짜별 슬롯 정보를 담는 내부 클래스
     */
    private record DaySlotInfo(InterviewDay interviewDay, Set<LocalTime> availableStartTimes) {
    }
}
