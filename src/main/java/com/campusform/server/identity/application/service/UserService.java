package com.campusform.server.identity.application.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.campusform.server.global.infrastructure.S3Service;
import com.campusform.server.identity.domain.exception.UserNotFoundException;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 정보 수정 서비스
 */
@Profile("!local")
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 프로필 이미지 업데이트
     *
     * @param userId    사용자 ID
     * @param imageFile 새로운 프로필 이미지 파일
     * @return 업데이트된 프로필 이미지 URL
     */
    @Transactional
    public String updateProfileImage(Long userId, MultipartFile imageFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String oldProfileImageUrl = user.getProfileImageUrl();

        // 새 이미지 업로드 (먼저 수행)
        String newProfileImageUrl = s3Service.uploadProfileImage(imageFile, userId);

        // 사용자 정보 업데이트
        user.updateProfileImage(newProfileImageUrl);

        // 트랜잭션 커밋 후 기존 이미지 삭제 (안전하게)
        if (oldProfileImageUrl != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    s3Service.deleteFile(oldProfileImageUrl);
                    log.info("기존 프로필 이미지 삭제 완료: {}", oldProfileImageUrl);
                }
            });
        }

        log.info("프로필 이미지 업데이트 완료: userId={}, newUrl={}", userId, newProfileImageUrl);

        return newProfileImageUrl;
    }

    /**
     * 프로필 이미지 삭제
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String profileImageUrl = user.getProfileImageUrl();

        // 사용자 프로필 이미지 null로 설정 (먼저 수행)
        user.updateProfileImage(null);

        // 트랜잭션 커밋 후 S3에서 이미지 삭제 (안전하게)
        if (profileImageUrl != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    s3Service.deleteFile(profileImageUrl);
                    log.info("S3 프로필 이미지 삭제 완료: {}", profileImageUrl);
                }
            });
        }

        log.info("프로필 이미지 삭제 완료: userId={}", userId);
    }

    /**
     * 닉네임 수정
     *
     * @param userId      사용자 ID
     * @param newNickname 새로운 닉네임
     * @return 수정된 닉네임
     */
    @Transactional
    public String updateNickname(Long userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.updateNickname(newNickname);

        log.info("닉네임 수정 완료: userId={}, newNickname={}", userId, newNickname);

        return user.getNickname();
    }
}
