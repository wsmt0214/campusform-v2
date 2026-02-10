package com.campusform.server.recruiting.domain.model.message;

import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * 프로젝트 메시지 템플릿 Entity
 * 프로젝트별 문자 템플릿을 관리합니다.
 * 프로젝트 템플릿은 부가정보로 별도 테이블로 관리됩니다.
 */
@Entity
@Table(name = "project_message_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MessageTemplate{
    @Id
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 서류 합격 메시지 템플릿
     */
    @Column(name = "template_document_pass", columnDefinition = "TEXT")
    private String templateDocumentPass;

    /**
     * 서류 불합격 메시지 템플릿
     */
    @Column(name = "template_document_fail", columnDefinition = "TEXT")
    private String templateDocumentFail;

    /**
     * 면접 합격 메시지 템플릿
     */
    @Column(name = "template_interview_pass", columnDefinition = "TEXT")
    private String templateInterviewPass;

    /**
     * 면접 불합격 메시지 템플릿
     */
    @Column(name = "template_interview_fail", columnDefinition = "TEXT")
    private String templateInterviewFail;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // MessageTemplate.java 내부에 추가
    // 영속성 컨텍스트가 없음, 비영속 상태 -> .save()를 꼭 해줘어야함!!!
    public static MessageTemplate createEmpty(Long projectId) {
        MessageTemplate template = new MessageTemplate();
        template.projectId = projectId;
        return template;
    }

    // 업데이트 로직
    public void updateTemplate(RecruitmentStage stage, ScreeningResult status, String content) {
        if (content == null) content = "";

        if (stage == RecruitmentStage.DOCUMENT) {
            if (status == ScreeningResult.PASS) this.templateDocumentPass = content;
            else this.templateDocumentFail = content;
        } else {
            if (status == ScreeningResult.PASS) this.templateInterviewPass = content;
            else this.templateInterviewFail = content;
        }
    }

//    //핵심 도메인 로직 : 실제 발송될 메시지 형식 작성!!
//    /**
//     * 템플릿 변수(@이름, @포지션)를 실제 지원자 정보로 치환하여 반환한다.
//     * @param stage 전형
//     * @param status 합격여부
//     * @param applicantName 지원자 이름
//     * @param positionName 지원 포지션 (없을 경우 "-" 으로 표시)
//     */
//    public String generateMessage(RecruitmentStage stage, ScreeningResult status, String applicantName, String positionName){
//        // 조건에 맞는 메시지를 설정해서 보내야햄 -> 조건에 맞는 메시지를 불러와야함.
//        String rTemplate=getTemplateContent(stage,status);
//
//        if(rTemplate==null || rTemplate.isBlank()) {
//            return " ";
//        }
//        return rTemplate
//                .replace("@이름", applicantName!=null?applicantName:"")
//                .replace("@포지션",positionName!=null?positionName:" - ");
//    }
//
    public String getTemplateContent(RecruitmentStage stage, ScreeningResult status) {
        if(stage==RecruitmentStage.DOCUMENT){
            return status == ScreeningResult.PASS
                    ? templateDocumentPass
                    : templateDocumentFail;
        }else if(stage==RecruitmentStage.INTERVIEW){
            return status == ScreeningResult.PASS
                    ? templateInterviewPass
                    : templateInterviewFail;
        }
        return "";
    }

}
