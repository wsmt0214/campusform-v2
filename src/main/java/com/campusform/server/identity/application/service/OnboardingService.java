package com.campusform.server.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.application.dto.response.OnboardingCompletedResponse;
import com.campusform.server.identity.domain.exception.UserNotFoundException;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;

    @Transactional
    public OnboardingCompletedResponse completeOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.completeOnboarding();

        return new OnboardingCompletedResponse(user.isOnboarded());
    }
}
