package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.recruiting.application.dto.request.SubmitSlotsRequest;
import com.campusform.server.recruiting.application.dto.response.InterviewSlotListResponse;
import com.campusform.server.recruiting.application.dto.response.SubmitSlotsResponse;
import com.campusform.server.recruiting.application.service.IntervieweeAvailabilityService;

import lombok.RequiredArgsConstructor;

/**
 * 지원자 면접 가능 시간 조사 공개 API
 * 
 * 토큰 기반으로 지원자가 면접 가능 시간 슬롯을 조회하고 제출합니다.
 * 인증 없이 접근 가능한 공개 API입니다.
 */
@RestController
@RequestMapping("/api/public/interview")
@RequiredArgsConstructor
public class PublicInterviewController {

    private final IntervieweeAvailabilityService intervieweeAvailabilityService;

    /**
     * 면접 슬롯 목록 조회
     * 
     * <pre>
     * 응답 예시:
     * {
     *   "summaries": [
     *     {
     *       "date": "2024-07-01",
     *       "slots": [
     *         {
     *           "startTime": "10:00",
     *           "endTime": "10:20",
     *           "availableInterviewerCount": 2
     *         },
     *         {
     *           "startTime": "10:25",
     *           "endTime": "10:45",
     *           "availableInterviewerCount": 1
     *         }
     *       ]
     *     }
     *   ]
     * }
     * </pre>
     */
    @GetMapping("/slots")
    public ResponseEntity<InterviewSlotListResponse> getSlots(@RequestParam String token) {
        InterviewSlotListResponse response = intervieweeAvailabilityService.getSlotsByToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * 지원자 면접 가능 슬롯 제출
     * 
     * <pre>
     * 요청 예시:
     * {
     *   "name": "홍길동",
     *   "phone": "010-1234-5678",
     *   "selections": [
     *     { "date": "2024-06-25", "startTimes": ["10:00", "14:00"] },
     *     { "date": "2024-06-26", "startTimes": ["11:00"] }
     *   ]
     * }
     * 
     * 응답 예시:
     * {
     *   "message": "면접 가능 시간이 성공적으로 제출되었습니다."
     * }
     * </pre>
     */
    @PostMapping("/submit")
    public ResponseEntity<SubmitSlotsResponse> submitSlots(
            @RequestParam String token,
            @RequestBody SubmitSlotsRequest request) {
        intervieweeAvailabilityService.submitSlots(token, request);
        return ResponseEntity.ok(SubmitSlotsResponse.success());
    }
}
