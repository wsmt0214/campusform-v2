package com.campusform.server.project.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusform.server.project.domain.model.sheet.GoogleOAuthToken;

@Repository
public interface GoogleOAuthTokenJpaRepository extends JpaRepository<GoogleOAuthToken, Long> {

    Optional<GoogleOAuthToken> findByOwnerId(Long ownerId);
}
