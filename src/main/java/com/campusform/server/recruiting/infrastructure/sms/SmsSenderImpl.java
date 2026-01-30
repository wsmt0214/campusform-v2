package com.campusform.server.recruiting.infrastructure.sms;

import com.campusform.server.recruiting.application.port.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class  SmsSenderImpl implements SmsSender {
    // @Value("${coolsms.api.key}") ... API 키 주입

    @Override
    public void sendSms(String phoneNumber, String content) {
        // 1. 실제 외부 API 연동 코드
        /*
        Message coolsms = new Message(apiKey, apiSecret);
        HashMap<String, String> params = new HashMap<>();
        params.put("to", phoneNumber);
        params.put("from", "01012345678"); // 발신번호
        params.put("text", content);
        coolsms.send(params);
        */

        log.info("[SMS 전송] 수신번호: {}, 내용: {}", phoneNumber, content);
    }
}
