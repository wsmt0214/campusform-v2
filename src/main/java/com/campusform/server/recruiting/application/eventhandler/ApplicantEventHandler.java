package com.campusform.server.recruiting.application.eventhandler;

import com.campusform.server.recruiting.application.component.MessageGenerator;
import com.campusform.server.recruiting.application.port.SmsSender;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.event.ApplicantUpdated;
import com.campusform.server.recruiting.domain.model.message.MessageTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j // (log 사용 가능)
@Component // (스프링 빈 등록)
@RequiredArgsConstructor
public class ApplicantEventHandler {

    private final SmsSender smsSender;
    private final MessageGenerator messageGenerator;

    @Async // 비동기 처리
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 커밋 성공후에만 실행
    public void handleApplicantUpdated(ApplicantUpdated event) {
        log.info("이벤트 수신: 지원자 ID={}, 상태={}, 단계={}", event.applicantId(), event.status(),event.stage());

        try {
            // 메시지 생성 위임 (복잡한 if문 사라짐)
            // DB 템플릿 우선 사용
            String message = messageGenerator.generateMessage(
                    event.projectId(),
                    event.stage(),
                    event.status(),
                    event.applicantName(),
                    event.positionName()
            );

            // 메시지가 생성된 경우에만 전송 (null이면 해당 단계/상태에 맞는 템플릿이 없다는 뜻)
            if (message != null && event.applicantPhone() != null) {
                smsSender.sendSms(event.applicantPhone(), message);
                log.info("문자 발송 완료: 지원자 ID={}", event.applicantId());
            }
        } catch (Exception e) {
            log.error("문자 발송 실패: 지원자 ID={}, 원인={}", event.applicantId(), e.getMessage(), e);
        }
//        // 상태에 따라 다른 문구 생성
//        String message = null;
//        if (event.stage() == RecruitmentStage.DOCUMENT) {
//            if (event.status() == ApplicantStatus.PASS) {
//                // 이름(%s), 포지션(%s) 치환해서 문구 완성
//                message = String.format(DOCUMENT_PASS_TEMPLATE, event.applicantName());
//            }else{
//                message = String.format(DOCUMENT_FAIL_TEMPLATE, event.applicantName());
//            }
//        }
//
//
//        // 보낼 메시지가 있을 때만(합/불 일때만) 전송
//        if (message != null && event.applicantPhone() != null) {
//            try {
//                smsSender.sendSms(event.applicantPhone(), message);
//                log.info("문자 발송 완료: 지원자 ID={}", event.applicantId());
//            } catch (Exception e) {
//                log.error("문자 발송 실패: 지원자 ID={}, 원인={}", event.applicantId(), e.getMessage(), e);
//
//
//            }
//        }
    }
}
