package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.request.SetRequiredInterviewerRequest;
import com.campusform.server.recruiting.application.dto.request.UpdateRequiredInterviewersRequest;
import com.campusform.server.recruiting.application.dto.request.UpsertInterviewerAvailabilityRequest;
import com.campusform.server.recruiting.application.dto.response.InterviewerAvailabilityResponse;
import com.campusform.server.recruiting.application.dto.response.InterviewerAvailabilitySummaryResponse;
import com.campusform.server.recruiting.application.dto.response.InterviewerListResponse;
import com.campusform.server.recruiting.application.dto.response.RequiredInterviewersResponse;
import com.campusform.server.recruiting.application.service.InterviewerAvailabilityService;
import com.campusform.server.recruiting.application.service.InterviewerQueryService;
import com.campusform.server.recruiting.application.service.RequiredInterviewerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 설정 - 2. 면접관 시간 등록
 */
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class InterviewerAvailabilityController {

        private final InterviewerAvailabilityService interviewerAvailabilityService;
        private final InterviewerQueryService interviewerQueryService;
        private final RequiredInterviewerService requiredInterviewerService;
        private final AuthService authService;

        /**
         * 면접관 목록 조회
         *
         * <pre>
         * 응답 예시:
         * {
         *   "interviewers": [
         *     {
         *       "userId": 1,
         *       "nickname": "홍길동",
         *       "email": "hong@domain.com",
         *       "profileImageUrl": "https://cdn.domain.com/profile1.png"
         *     },
         *     {
         *       "userId": 2,
         *       "nickname": "김관리자",
         *       "email": "kim@domain.com",
         *       "profileImageUrl": "https://cdn.domain.com/profile2.png"
         *     }
         *   ]
         * }
         * </pre>
         */
        @GetMapping("/{projectId}/interviewers")
        public ResponseEntity<InterviewerListResponse> getInterviewerList(
                        @PathVariable Long projectId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                InterviewerListResponse response = interviewerQueryService.getInterviewerList(projectId, userId);
                return ResponseEntity.ok(response);
        }

        /**
         * 특정 면접관의 가능 시간 조회
         * 
         * <pre>
         * 응답 예시:
         * {
         *   "adminId": 123,
         *   "nickname": "홍길동",
         *   "email": "hong@test.com",
         *   "availabilities": [
         *     {
         *       "date": "2024-07-01",
         *       "timeBlocks": [
         *         {"startTime": "10:00", "endTime": "10:30"},
         *         {"startTime": "14:00", "endTime": "14:30"}
         *       ]
         *     },
         *     {
         *       "date": "2024-07-02",
         *       "timeBlocks": [
         *         {"startTime": "09:00", "endTime": "09:30"}
         *       ]
         *     }
         *   ]
         * }
         * </pre>
         */
        @GetMapping("/{projectId}/interviewers/{adminId}/availability")
        public ResponseEntity<InterviewerAvailabilityResponse> getInterviewerAvailability(
                        @PathVariable Long projectId,
                        @PathVariable Long adminId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                InterviewerAvailabilityResponse response = interviewerAvailabilityService
                                .getInterviewerAvailability(projectId, userId, adminId);
                return ResponseEntity.ok(response);
        }

        /**
         * 특정 면접관의 가능 시간 저장
         * 전체를 Replace 하기 때문에 일부 CRUD 메서드는 추후 개발 필요
         * 
         * <pre>
         * 요청 예시:
         * {
         *   "availabilities": [
         *     { 
         *       "date": "2024-07-01",
         *       "startTimes": ["10:00", "10:30", "14:00"]
         *     },
         *     {
         *       "date": "2024-07-02",
         *       "startTimes": ["09:00", "09:30"]
         *     }
         *   ]
         * }
         * </pre>
         */
        @PutMapping("/{projectId}/interviewers/{adminId}/availability")
        public ResponseEntity<InterviewerAvailabilityResponse> replaceInterviewerAvailability(
                        @PathVariable Long projectId,
                        @PathVariable Long adminId,
                        Authentication authentication,
                        @Valid @RequestBody UpsertInterviewerAvailabilityRequest request) {
                Long userId = authService.extractUserId(authentication);
                InterviewerAvailabilityResponse response = interviewerAvailabilityService
                                .replaceInterviewerAvailability(projectId, userId, adminId, request);
                return ResponseEntity.ok(response);
        }

        /**
         * 시간대별 가능 면접관 수 집계 ("전체" 버튼 시각화용)
         * 
         * <pre>
         * 응답 예시:
         * {
         *   "summaries": [
         *     {
         *       "date": "2024-07-01",
         *       "timeBlocks": [
         *         {
         *           "startTime": "10:00",
         *           "endTime": "10:30",
         *           "availableInterviewerCount": 2
         *         },
         *         {
         *           "startTime": "10:30",
         *           "endTime": "11:00",
         *           "availableInterviewerCount": 1
         *         }
         *       ]
         *     },
         *     {
         *       "date": "2024-07-02",
         *       "timeBlocks": [
         *         {
         *           "startTime": "09:00",
         *           "endTime": "09:30",
         *           "availableInterviewerCount": 3
         *         }
         *       ]
         *     }
         *   ]
         * }
         * </pre>
         */
        @GetMapping("/{projectId}/interviewers/availability-summary")
        public ResponseEntity<InterviewerAvailabilitySummaryResponse> getAvailabilitySummary(
                        @PathVariable Long projectId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                InterviewerAvailabilitySummaryResponse response = interviewerAvailabilityService
                                .getAvailabilitySummary(projectId, userId);
                return ResponseEntity.ok(response);
        }

        /**
         * 필수 면접관 목록 조회
         * 
         * <pre>
         * 응답 예시:
         * {
         *   "adminIds": [1, 2, 3]
         * }
         * </pre>
         */
        @GetMapping("/{projectId}/required-interviewers")
        public ResponseEntity<RequiredInterviewersResponse> getRequiredInterviewers(
                        @PathVariable Long projectId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                RequiredInterviewersResponse response = requiredInterviewerService
                                .getRequiredInterviewers(projectId, userId);
                return ResponseEntity.ok(response);
        }

        /**
         * 필수 면접관 전체 설정
         * 
         * <pre>
         * 요청 예시:
         * {
         *   "adminIds": [1, 2, 3]
         * }
         * </pre>
         */
        @PutMapping("/{projectId}/required-interviewers")
        public ResponseEntity<RequiredInterviewersResponse> replaceRequiredInterviewers(
                        @PathVariable Long projectId,
                        Authentication authentication,
                        @Valid @RequestBody UpdateRequiredInterviewersRequest request) {
                Long userId = authService.extractUserId(authentication);
                RequiredInterviewersResponse response = requiredInterviewerService
                                .replaceAllRequiredInterviewers(projectId, userId, request);
                return ResponseEntity.ok(response);
        }

        /**
         * 필수 면접관 개별 설정
         * 
         * <pre>
         * 요청 예시:
         * {
         *   "required": true  // true: 필수 면접관 추가, false: 필수 면접관 제거
         * }
         * </pre>
         */
        @PutMapping("/{projectId}/required-interviewers/{adminId}")
        public ResponseEntity<RequiredInterviewersResponse> setRequiredInterviewer(
                        @PathVariable Long projectId,
                        @PathVariable Long adminId,
                        Authentication authentication,
                        @Valid @RequestBody SetRequiredInterviewerRequest request) {
                Long userId = authService.extractUserId(authentication);
                RequiredInterviewersResponse response = requiredInterviewerService
                                .updateRequiredInterviewerStatus(projectId, userId, adminId, request);
                return ResponseEntity.ok(response);
        }
}