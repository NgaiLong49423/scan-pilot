package com.scanpilot.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SelectRepositoryRequest(
        @NotNull(message = "GitHub repository ID is required")
        Long githubRepoId,

        @NotBlank(message = "Repository full name is required")
        String fullName,

        String name,
        String owner,
        String defaultBranch,
        Boolean isPrivate
) {}
