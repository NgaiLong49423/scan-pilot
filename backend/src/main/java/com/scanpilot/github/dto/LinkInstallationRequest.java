package com.scanpilot.github.dto;

import jakarta.validation.constraints.NotNull;

public record LinkInstallationRequest(
        @NotNull(message = "Installation ID is required")
        Long installationId
) {}
