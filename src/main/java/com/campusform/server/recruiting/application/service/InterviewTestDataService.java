package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.response.InterviewTestDataGenerateResponse;
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
import lombok.extern.slf4j.Slf4j;

/**
 * 면접 테스트 데이터 자동 생성 서비스
 * 면접관 가용 시간(30분 블록) + 지원자 슬롯 선택을 확률로 랜덤 생성하고, 생성 결과를 응답으로 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewTestDataService {

    private final InterviewContextLoader contextLoader;
    private final UserRepository userRepository;
    private final ApplicantRepository applicantRepository;
    private final InterviewerAvailabilityBlockRepository blockRepository;
    private final IntervieweeAvailabilitySlotRepository slotRepository;

    private final InterviewSlotGenerator slotGenerator = new InterviewSlotGenerator();

    /**
     * 테스트 데이터 일괄 생성:
     * (1) 면접관 가용 블록 생성 → (2) 블록 기반 슬롯 1회 생성 → (3) 지원자가 동일 슬롯 풀에서 선택 → (4)(5) 응답 조립
     */
    @Transactional
    public InterviewTestDataGenerateResponse generateTestData(Long projectId, double dayParticipationRate,
            double slotSelectionRate) {
        // 프로젝트·면접 설정 로드 및 날짜/블록 매핑 준비
        InterviewContext ctx = contextLoader.loadContext(projectId);
        Project project = ctx.project();
        InterviewSetting setting = ctx.setting();
        Random random = new Random();
        List<Long> adminIds = project.getAdminIds();
        List<InterviewDay> days = setting.getDays();
        if (adminIds.isEmpty()) {
            throw new IllegalStateException("프로젝트에 면접관(관리자)이 없습니다. 프로젝트 소유자 또는 관리자를 먼저 등록해 주세요.");
        }
        if (days.isEmpty()) {
            throw new IllegalStateException("면접 일정(날짜)이 없습니다. 면접 정보 설정에서 면접 날짜를 먼저 등록해 주세요.");
        }
        List<LocalTime> allBlocks = setting.getTimeRange().generateBlocks(); // 설정 시간 범위 내 30분 단위 블록 전체
        int totalBlockCount = allBlocks.size();
        if (totalBlockCount == 0) {
            throw new IllegalStateException("면접 시간 범위로 생성 가능한 30분 블록이 없습니다. 시작/종료 시간을 확인해 주세요.");
        }
        List<Long> dayIds = days.stream().map(InterviewDay::getId).toList();
        Map<Long, LocalDate> dayIdToDate = days.stream()
                .collect(Collectors.toMap(InterviewDay::getId, InterviewDay::getInterviewDate));
        int totalInterviewerBlocks = 0;

        // (1) 면접관별: 기존 블록 삭제 후, 날짜마다 참여 확률로 연속 블록 1구간 랜덤 생성·저장
        for (Long adminId : adminIds) {
            List<InterviewerAvailabilityBlock> existing = blockRepository.findByAdminIdAndInterviewDayIdIn(adminId, dayIds);
            if (!existing.isEmpty()) {
                blockRepository.deleteAll(existing);
                blockRepository.flush();
            }
            List<InterviewerAvailabilityBlock> blocksToSave = new ArrayList<>();
            // 면접관당 최소 1일은 참여 보장(확률만으로 전부 스킵되는 것 방지)
            int guaranteedDayIdx = random.nextInt(days.size());
            for (int d = 0; d < days.size(); d++) {
                InterviewDay day = days.get(d);
                boolean participate = (d == guaranteedDayIdx) || (random.nextDouble() <= dayParticipationRate);
                if (!participate) continue;
                int startIdx = random.nextInt(totalBlockCount); // 연속 구간 시작 인덱스
                int maxLen = totalBlockCount - startIdx;
                int len = 1 + random.nextInt(maxLen); // 구간 길이(블록 개수) 1~maxLen
                int endIdx = startIdx + len;
                for (int i = startIdx; i < endIdx; i++) {
                    LocalTime blockStart = allBlocks.get(i);
                    blocksToSave.add(InterviewerAvailabilityBlock.create(adminId, day.getId(), blockStart));
                }
            }
            if (!blocksToSave.isEmpty()) {
                blockRepository.saveAll(blocksToSave);
                totalInterviewerBlocks += blocksToSave.size();
            }
        }
        blockRepository.flush(); // 슬롯 생성 전 최신 블록 반영

        // (2) 면접관 블록 → 날짜별 슬롯 1회 생성 (모든 지원자가 동일한 슬롯 풀에서 선택해야 하므로)
        Map<Long, List<InterviewSlotGenerator.SlotInfo>> slotsByDay = new HashMap<>();
        for (InterviewDay day : days) {
            List<InterviewerAvailabilityBlock> dayBlocks = blockRepository.findByInterviewDayId(day.getId());
            List<InterviewSlotGenerator.SlotInfo> generatedSlots = slotGenerator.generateSlots(setting, dayBlocks);
            slotsByDay.put(day.getId(), generatedSlots);
            log.info("[테스트 데이터] 날짜={} → 생성된 슬롯 {}개: {}", day.getInterviewDate(),
                    generatedSlots.size(),
                    generatedSlots.stream().map(s -> s.startTime() + "~" + s.endTime()).toList());
        }

        // (3) 지원자별: 서류 합격자만, 미리 생성된 슬롯 풀에서 slotSelectionRate 확률로 선택
        List<Applicant> passApplicants = applicantRepository
                .findByProjectIdAndDocumentStatus(projectId, ScreeningResult.PASS);
        int totalApplicantSlots = 0;
        for (Applicant applicant : passApplicants) {
            slotRepository.deleteByApplicantId(applicant.getId());
            List<IntervieweeAvailabilitySlot> slotsToSave = new ArrayList<>();
            for (InterviewDay day : days) {
                List<InterviewSlotGenerator.SlotInfo> availableSlots = slotsByDay.get(day.getId());
                for (InterviewSlotGenerator.SlotInfo slot : availableSlots) {
                    if (random.nextDouble() < slotSelectionRate) {
                        slotsToSave.add(IntervieweeAvailabilitySlot.create(
                                applicant.getId(), day.getId(), slot.startTime()));
                    }
                }
            }
            if (!slotsToSave.isEmpty()) {
                slotRepository.saveAll(slotsToSave);
                totalApplicantSlots += slotsToSave.size();
            }
        }

        // (4) 응답: 면접관별 저장된 블록을 날짜→TimeBlock 리스트로 묶어 InterviewerEntry 구성
        List<InterviewTestDataGenerateResponse.InterviewerEntry> interviewerResults = new ArrayList<>();
        for (Long adminId : adminIds) {
            List<InterviewerAvailabilityBlock> blocks = blockRepository.findByAdminIdAndInterviewDayIdIn(adminId, dayIds);
            if (blocks.isEmpty()) continue;
            String nickname = userRepository.findById(adminId).map(User::getNickname).orElse("");
            Map<Long, List<InterviewerAvailabilityBlock>> byDay = blocks.stream()
                    .collect(Collectors.groupingBy(InterviewerAvailabilityBlock::getInterviewDayId)); // dayId 기준 그룹
            List<InterviewTestDataGenerateResponse.DayBlocks> availabilities = byDay.entrySet().stream()
                    .map(e -> {
                        LocalDate date = dayIdToDate.get(e.getKey());
                        List<InterviewTestDataGenerateResponse.TimeBlock> timeBlocks = e.getValue().stream()
                                .sorted(Comparator.comparing(InterviewerAvailabilityBlock::getStartTime))
                                .map(b -> new InterviewTestDataGenerateResponse.TimeBlock(
                                        b.getStartTime(), b.getStartTime().plusMinutes(30))) // 30분 블록 start~end
                                .toList();
                        return new InterviewTestDataGenerateResponse.DayBlocks(date, timeBlocks);
                    })
                    .sorted(Comparator.comparing(InterviewTestDataGenerateResponse.DayBlocks::getDate))
                    .toList();
            interviewerResults.add(new InterviewTestDataGenerateResponse.InterviewerEntry(adminId, nickname, availabilities));
        }

        // (5) 응답: 지원자별 저장된 슬롯을 날짜→SlotTime 리스트로 묶어 ApplicantEntry 구성
        int slotDurationMin = setting.getSlotDurationMin();
        List<IntervieweeAvailabilitySlot> allSlots = slotRepository.findByInterviewDayIdIn(dayIds);
        Map<Long, List<IntervieweeAvailabilitySlot>> slotsByApplicant = allSlots.stream()
                .collect(Collectors.groupingBy(IntervieweeAvailabilitySlot::getApplicantId));
        List<InterviewTestDataGenerateResponse.ApplicantEntry> applicantResults = new ArrayList<>();
        for (Long applicantId : slotsByApplicant.keySet()) {
            Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
            String name = applicant != null ? applicant.getName() : "";
            List<IntervieweeAvailabilitySlot> slots = slotsByApplicant.get(applicantId);
            Map<Long, List<IntervieweeAvailabilitySlot>> byDay = slots.stream()
                    .collect(Collectors.groupingBy(IntervieweeAvailabilitySlot::getInterviewDayId)); // dayId 기준 그룹
            List<InterviewTestDataGenerateResponse.DaySlots> selections = byDay.entrySet().stream()
                    .map(e -> {
                        LocalDate date = dayIdToDate.get(e.getKey());
                        List<InterviewTestDataGenerateResponse.SlotTime> slotTimes = e.getValue().stream()
                                .sorted(Comparator.comparing(IntervieweeAvailabilitySlot::getStartTime))
                                .map(s -> new InterviewTestDataGenerateResponse.SlotTime(
                                        s.getStartTime(), s.getStartTime().plusMinutes(slotDurationMin))) // 설정 슬롯 길이
                                .toList();
                        return new InterviewTestDataGenerateResponse.DaySlots(date, slotTimes);
                    })
                    .sorted(Comparator.comparing(InterviewTestDataGenerateResponse.DaySlots::getDate))
                    .toList();
            applicantResults.add(new InterviewTestDataGenerateResponse.ApplicantEntry(applicantId, name, selections));
        }
        applicantResults.sort(Comparator.comparing(InterviewTestDataGenerateResponse.ApplicantEntry::getApplicantId));

        // 요약 수치 + 면접관/지원자 상세 리스트로 DTO 생성 후 반환
        return InterviewTestDataGenerateResponse.of(
                adminIds.size(), totalInterviewerBlocks,
                passApplicants.size(), totalApplicantSlots,
                dayParticipationRate, slotSelectionRate,
                interviewerResults, applicantResults);
    }
}
