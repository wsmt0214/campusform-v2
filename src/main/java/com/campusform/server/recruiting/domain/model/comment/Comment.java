package com.campusform.server.recruiting.domain.model.comment;

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
 * 최초 댓글에 대한 답글만 허용하며, 답글에 대한 대댓글은 지원하지 않습니다.
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
     * OneToMany 이므로 DB에 테이블 생성 안됨
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
    private Comment(Long applicantId, Long authorId, String content, Comment parent) {
        if(content == null || content.isBlank()){
            throw new IllegalArgumentException("Content cannot be null or blank");
        }
        this.applicantId = applicantId;
        this.authorId = authorId;
        this.content = content;
        this.parent = parent;
    }

    // 2. 생성 로직

    /**
     * 최초 댓글 생성 : 부모가 없는 루트 댓글 작성
     */
    public static Comment createRoot(Long applicantId, Long authorId, String content) {
        return new Comment(applicantId, authorId, content, null);
    }

    /**
     * 답글(대댓글) 작성 : 특정 댓글에 대한 댓글을 생성한다.
     */
    public static Comment createReply(Comment parent, Long applicantId, Long authorId, String content) {
        if(parent == null){
            throw new IllegalArgumentException("Parent comment cannot be null");
        }
        Comment reply = new Comment(applicantId, authorId, content, parent);
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
