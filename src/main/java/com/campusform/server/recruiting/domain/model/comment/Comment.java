package com.campusform.server.recruiting.domain.model.comment;

import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 댓글(면접 코멘트) Entity
 * 지원자별 댓글과 답글을 관리합니다.
 * 댓글에 대한 대댓글을 무제한으로 작성할 수 있습니다.
 */
@Entity
@Table(name = "comments",
       indexes = {
        //    @Index(name = "idx_applicant_id", columnList = "applicant_id"),
           @Index(name = "idx_parent_comment_id", columnList = "parent_comment_id, created_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 지원자 ID (같은 컨텍스트 내부 관계)
     */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /**
     * 작성자 ID (Identity Context 참조이므로 ID만 저장)
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /**
     * 모집 단계 (서류/면접) - DOCUMENT와 INTERVIEW 댓글을 구분하기 위함
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private RecruitmentStage stage;

    /**
     * 부모 댓글 (self-referencing)
     * null이면 이 댓글이 최초 댓글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    /**
     * 댓글 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 답글 목록 (self-referencing 관계)
     * cascade = CascadeType.ALL: 부모 댓글 삭제 시 연산이 대댓글에 전파됨
     * orphanRemoval = true: 부모 삭제 시 대댓글을 모두 삭제 (고아 엔티티 제거)
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // 1. Private 생성자 : 외부에서 new Content() 금지
    private Comment(Long applicantId, Long authorId, RecruitmentStage stage, String content, Comment parent) {
        if(content == null || content.isBlank()){
            throw new IllegalArgumentException("Content cannot be null or blank");
        }
        if(stage == null){
            throw new IllegalArgumentException("Stage cannot be null");
        }
        this.applicantId = applicantId;
        this.authorId = authorId;
        this.stage = stage;
        this.content = content;
        this.parent = parent;
    }

    // 2. 생성 로직

    /**
     * 최초 댓글 생성 : 부모가 없는 루트 댓글 작성
     */
    public static Comment createRoot(Long applicantId, Long authorId, RecruitmentStage stage, String content) {
        return new Comment(applicantId, authorId, stage, content, null);
    }

    /**
     * 답글(대댓글) 작성 : 특정 댓글에 대한 댓글을 생성한다.
     * 깊이 제한 없이 무제한으로 대댓글을 작성할 수 있습니다.
     * 부모 댓글과 같은 stage를 가져야 합니다.
     * 
     * @throws IllegalArgumentException 부모 댓글이 null이거나, 부모 댓글의 applicantId/stage와 일치하지 않는 경우
     */
    public static Comment createReply(Comment parent, Long applicantId, Long authorId, RecruitmentStage stage, String content) {
        if(parent == null){
            throw new IllegalArgumentException("Parent comment cannot be null");
        }
        // 부모 댓글과 applicantId 일치 검증 (데이터 무결성 보장)
        if(!parent.getApplicantId().equals(applicantId)){
            throw new IllegalArgumentException(
                String.format("대댓글은 같은 지원자의 댓글에만 작성 가능합니다. 부모 댓글의 applicantId: %d, 요청한 applicantId: %d", 
                    parent.getApplicantId(), applicantId)
            );
        }
        // 부모 댓글과 stage 일치 검증 (DOCUMENT와 INTERVIEW 댓글 섞이지 않도록)
        if(!parent.getStage().equals(stage)){
            throw new IllegalArgumentException(
                String.format("대댓글은 같은 모집 단계의 댓글에만 작성 가능합니다. 부모 댓글의 stage: %s, 요청한 stage: %s", 
                    parent.getStage(), stage)
            );
        }
        // parent 객체를 직접 설정하여 parent_comment_id가 제대로 저장되도록 함
        Comment reply = new Comment(applicantId, authorId, stage, content, parent);
        // 양방향 관계 설정 (parent의 replies 리스트에 추가)
        parent.addReply(reply);
        return reply;
    }

    // 3. 비즈니스 로직 (수정, 연관관계 등)

    /**
     * 답글 내용 수정
     */
    public void updateContent(String newContent) {
        if(newContent == null || newContent.isBlank()){
            throw new IllegalArgumentException("Content cannot be null or blank");
        }
        this.content = newContent;
    }
    /**
     * 연관관계 편의 메서드 (답글 추가 시 양방향 관계 설정)
     */
    private void addReply(Comment reply) {
        this.replies.add(reply);
    }

    /**
     * 작성자 본인 확인
     */
    public boolean isWrittenBy(Long currentMemberId) {
        return this.authorId.equals(currentMemberId);
    }

}
