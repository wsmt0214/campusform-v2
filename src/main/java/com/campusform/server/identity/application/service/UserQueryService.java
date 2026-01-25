package com.campusform.server.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.application.dto.response.UserExistsResponse;
import com.campusform.server.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 오직 데이터를 읽어서 반환하는 역할만 수행
 * 
 * 데이터 변경없이 읽기 전용 쿼리만 담당
 */
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    /**
     * 이메일로 사용자 조회
     */
    @Transactional(readOnly = true)
    public UserExistsResponse findByEmail(String email) {
        // 존재 시 map으로 변환 <-> 미존재 시 orElse 처리
        return userRepository.findByEmail(email)
                .map(user -> UserExistsResponse.found(user.getId(), user.getNickname(), user.getEmail(),
                        user.getProfileImageUrl()))
                .orElse(UserExistsResponse.notFound(email));
    }
}
