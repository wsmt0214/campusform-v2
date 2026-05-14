package com.campusform.server.identity.application.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AuthMeResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authMeResponseUsesExpectedBooleanFieldNames() throws Exception {
        AuthMeResponse response = AuthMeResponse.authenticated(
                1L,
                "user@example.com",
                "CampusForm",
                "https://example.com/profile.jpg",
                false);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.has("isAuthenticated")).isTrue();
        assertThat(json.has("isOnboarded")).isTrue();
        assertThat(json.get("isAuthenticated").asBoolean()).isTrue();
        assertThat(json.get("isOnboarded").asBoolean()).isFalse();
    }

    @Test
    void onboardingResponseUsesExpectedBooleanFieldName() throws Exception {
        OnboardingCompletedResponse response = new OnboardingCompletedResponse(true);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.has("isOnboarded")).isTrue();
        assertThat(json.get("isOnboarded").asBoolean()).isTrue();
    }
}
