package com.campusform.server.project.infrastructure.external.sheet;

import java.io.IOException;
import java.util.Optional;

import com.campusform.server.project.application.service.GoogleOAuthTokenService;
import com.campusform.server.project.domain.exception.TokenNotFoundException;
import com.campusform.server.project.domain.model.sheet.GoogleOAuthToken;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Google Sheets API 서비스 객체를 생성하는 팩토리
 */
@Component
@RequiredArgsConstructor
public class GoogleSheetsServiceFactory {

    private final GoogleOAuthTokenService tokenService;
    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    /**
     * Google Sheets API 서비스 객체를 생성합니다.
     */
    public Sheets createSheetsService(Long ownerId) {
        GoogleOAuthToken token = getValidToken(ownerId);
        HttpRequestInitializer requestInitializer = createRequestInitializer(token.getAccessToken());

        return new Sheets.Builder(httpTransport, jsonFactory, requestInitializer)
                .setApplicationName("CampusForm")
                .build();
    }

    /**
     * 유효한 OAuth 토큰을 조회합니다.
     */
    private GoogleOAuthToken getValidToken(Long ownerId) {
        Optional<GoogleOAuthToken> tokenOpt = tokenService.getValidToken(ownerId);
        if (tokenOpt.isEmpty()) {
            throw new TokenNotFoundException("토큰을 찾을 수 없습니다. ownerId=" + ownerId);
        }
        return tokenOpt.get();
    }

    /**
     * OAuth 토큰을 사용하여 HTTP 요청 초기화자를 생성합니다.
     */
    private HttpRequestInitializer createRequestInitializer(String accessToken) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.getHeaders().setAuthorization("Bearer " + accessToken);
            }
        };
    }
}
