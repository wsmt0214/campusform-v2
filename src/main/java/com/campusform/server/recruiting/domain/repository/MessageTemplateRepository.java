package com.campusform.server.recruiting.domain.repository;

import com.campusform.server.recruiting.domain.model.message.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 여기서도 extends JpaRepository를 빼준다.
 * 대신에 persistence쪽에 새로운 MessageTemplateJpaRepository 인터페이스를 만들어준다.
 * */
public interface MessageTemplateRepository{
    // 저장
    MessageTemplate save(MessageTemplate template);

    // 조회
    Optional<MessageTemplate> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);

}
