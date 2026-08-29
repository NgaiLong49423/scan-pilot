package com.scanpilot.github.dto;

public record UserAccessibleInstallationDto(
        Long id,
        Long accountId,
        String accountLogin,
        String accountType
) {}
