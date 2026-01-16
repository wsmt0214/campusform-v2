package com.campusform.server.identity.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusform.server.identity.domain.model.User;

/**
 * Spring Data JPA를 위한 User Repository
 * 
 * 기본 CRUD 메서드와 커스텀 쿼리 메서드를 제공합니다.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
