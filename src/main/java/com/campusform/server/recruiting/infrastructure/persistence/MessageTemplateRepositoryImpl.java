package com.campusform.server.recruiting.infrastructure.persistence;

import com.campusform.server.recruiting.domain.model.message.MessageTemplate;
import com.campusform.server.recruiting.domain.repository.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 도메인 인터페이스와 JPA 인터페이스를 연결해줍니다.
 */
@Repository
@RequiredArgsConstructor
public class MessageTemplateRepositoryImpl implements MessageTemplateRepository {
    private final MessageTemplateJpaRepository jpaRepository;

    @Override
    public MessageTemplate save(MessageTemplate template) {
        return jpaRepository.save(template);
    }

    @Override
    public Optional<MessageTemplate> findByProjectId(Long projectId) {
        return jpaRepository.findById(projectId);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        jpaRepository.findById(projectId).ifPresent(jpaRepository::delete);
    }
}
