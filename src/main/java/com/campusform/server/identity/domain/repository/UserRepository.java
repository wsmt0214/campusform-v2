package com.campusform.server.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.identity.domain.model.User;

/**
 * 도메인 계층의 infrastructure 인터페이스
 * 
 * 특정 기술에 의존하지 않고 도메인 관점에서 인터페이스를 서술합니다.
 * 
 * 따라서 Repository를 사용할 때 본 인터페이스를 사용합니다.
 */
public interface UserRepository {

    void save(User user);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    boolean existsById(Long adminId);

    List<User> findByIds(List<Long> ids);

    void delete(User user);
}
