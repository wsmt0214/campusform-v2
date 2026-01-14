# 애그리거트 정리 (JPA 매핑 기준)

- Identity 컨텍스트
  - User (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음

- Notification 컨텍스트
  - Notification (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체(열거형): NotificationType
  - UserNotificationSettings (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음

- Project 컨텍스트
  - Project (애그리거트 루트)
    - 포함 엔티티: ProjectAdmin
    - 값 객체(열거형): ProjectState, SyncStatus
  - GoogleOAuthToken (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음

- Recruiting 컨텍스트
  - Applicant (애그리거트 루트)
    - 포함 엔티티: ApplicantExtraAnswer
    - 값 객체(열거형): ApplicantStatus (documentStatus, interviewStatus)

  - Comment (애그리거트 루트)
    - 포함 엔티티: Comment (self 1:N replies)
    - 값 객체: 없음

  - MessageTemplate (애그리거트 루트로 관리됨)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음

  - InterviewSetting (애그리거트 루트)
    - 포함 엔티티: InterviewDay (1:N), InterviewRequiredInterviewer (1:N), InterviewAvailabilityInvestigationLink (1:1)
    - 값 객체: 없음

  - InterviewerAvailabilityBlock (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음

  - InterviewScheduledSlot (애그리거트 루트)
    - 포함 엔티티: InterviewScheduledSlotApplicant (1:N), InterviewScheduledSlotInterviewer (1:N)
    - 값 객체: 없음
  - IntervieweeAvailabilitySlot (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음
  - InterviewerAvailabilityBlock (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음
  - InterviewScheduleUnassignedApplicant (애그리거트 루트)
    - 포함 엔티티: 없음 (루트 단독)
    - 값 객체: 없음
