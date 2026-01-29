package com.campusform.server.recruiting.infrastructure.persistence;

import com.campusform.server.recruiting.domain.model.message.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 실제 JPA기능을 쓴다.
 * 실제 DB와 연결한다.
 */
public interface MessageTemplateJpaRepository extends JpaRepository<MessageTemplate, Long> {
}
