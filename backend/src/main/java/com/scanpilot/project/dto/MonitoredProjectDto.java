package com.scanpilot.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scanpilot.project.model.MonitoredProject;

import java.time.Instant;
import java.util.List;

public record MonitoredProjectDto(
        String id,
        Long githubRepoId,
        String owner,
        String name,
        String fullName,
        String defaultBranch,
        String primaryBranch,
        List<String> secondaryBranches,
        @JsonProperty("isPrivate")
        boolean isPrivate,
        Instant monitoredAt,
        String status
) {
    public static MonitoredProjectDto from(MonitoredProject project) {
        if (project == null) {
            return null;
        }
        return new MonitoredProjectDto(
                project.getId(),
                project.getGithubRepoId(),
                project.getOwner(),
                project.getName(),
                project.getFullName(),
                project.getDefaultBranch(),
                project.getPrimaryBranch(),
                project.getSecondaryBranches(),
                project.isPrivate(),
                project.getMonitoredAt(),
                project.getStatus()
        );
    }
}
