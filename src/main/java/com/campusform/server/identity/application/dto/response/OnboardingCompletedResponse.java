package com.campusform.server.identity.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Onboarding completion response")
public record OnboardingCompletedResponse(
        @JsonProperty("isOnboarded")
        @Schema(description = "Onboarding completion status", example = "true") boolean isOnboarded) {
}
