package com.scanpilot.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryDto(
        Long id,
        String name,
        String fullName,
        String owner,
        String defaultBranch,
        @JsonProperty("isPrivate")
        boolean isPrivate,
        String htmlUrl,
        String description,
        @JsonProperty("isSelected")
        boolean isSelected
) {}
