package com.campusform.server.project.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.project.domain.model.sheet.GoogleOAuthToken;
import com.campusform.server.project.domain.repository.GoogleOAuthTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Google OAuth Token Repository 구현체
 * 
 * JPA를 사용하여 데이터 영속성을 처리합니다.
 */
@Repository
@RequiredArgsConstructor
public class GoogleOAuthTokenRepositoryImpl implements GoogleOAuthTokenRepository {

    private final GoogleOAuthTokenJpaRepository jpaRepository;

    @Override
    public GoogleOAuthToken save(GoogleOAuthToken token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<GoogleOAuthToken> findByOwnerId(Long ownerId) {
        return jpaRepository.findByOwnerId(ownerId);
    }

    @Override
    public void delete(GoogleOAuthToken token) {
        jpaRepository.delete(token);
    }
}
