package com.campusform.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정 클래스
 * 
 * HTTP 요청 타임아웃 설정 및 테스트 가능한 빈 제공
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate 빈 생성
     * 
     * 연결 타임아웃: 5초
     * 읽기 타임아웃: 10초
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 연결 타임아웃: 5초
        factory.setReadTimeout(10000); // 읽기 타임아웃: 10초

        return new RestTemplate(factory);
    }
}
