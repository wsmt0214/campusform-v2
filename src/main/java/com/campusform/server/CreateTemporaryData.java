package com.campusform.server;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 테스트용 데이터를 삽입
 * prod 에서는 비활성화 또는 삭제
 */
@Profile("temporary1")
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTemporaryData {

    private final UserRepository userRepository;

    /**
     * 애플리케이션 초기화 완료 시 실행 콜백 메서드
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("==================== 테스트용 데이터 세팅 시작 ====================");
        putUsersData();
        log.info("==================== 유저 데이터 삽입 완료 ====================");

    }

    private void putUsersData() {
        User user1 = User.create("iht@naver.com", "임형택", "iht.url");
        userRepository.save(user1);
        User user2 = User.create("psg@naver.com", "박성근", "psg.url");
        userRepository.save(user2);
        User user3 = User.create("cjw@naver.com", "최재원", "cjw.url");
        userRepository.save(user3);
        User user4 = User.create("lmk@naver.com", "이민경", "lmk.url");
        userRepository.save(user4);
        User user5 = User.create("hjs@naver.com", "홍지수", "hjs.url");
        userRepository.save(user5);
    }
}
