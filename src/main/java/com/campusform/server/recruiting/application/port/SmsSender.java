package com.campusform.server.recruiting.application.port;

public interface SmsSender {
    void sendSms(String phoneNumber, String content);
}
