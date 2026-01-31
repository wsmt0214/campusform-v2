package com.campusform.server.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * UserRepository 구현체
 * 
 * Spring Data JPA에 작업을 위임합니다.
 * 향후 Querydsl이 필요하면 여기에 추가할 수 있습니다.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public void save(User user) {
        userJpaRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public boolean existsById(Long adminId) {
        return userJpaRepository.existsById(adminId);
    }

    @Override
    public List<User> findByIds(List<Long> ids) {
        return userJpaRepository.findAllById(ids);
    }
}
