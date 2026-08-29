package com.scanpilot.github.dto;

import lombok.Builder;

@Builder
public record GitHubWebhookPayloadDto(
    Long githubRepoId,
    Long installationId,
    String branch,
    String defaultBranch,
    String baseBranch,
    String headBranch,
    Boolean isDeleted,
    Boolean isMerged,
    String prAction,
    Integer prNumber,
    String commitSha,
    String baseSha,
    Boolean isFork
) {
    public GitHubWebhookPayloadDto {
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (isMerged == null) {
            isMerged = false;
        }
        if (isFork == null) {
            isFork = false;
        }
    }
}
