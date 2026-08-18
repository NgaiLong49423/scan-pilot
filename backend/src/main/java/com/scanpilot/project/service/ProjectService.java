package com.scanpilot.project.service;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.project.dto.BranchConfigRequest;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.model.MonitoredProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    public static final int MAX_SECONDARY_BRANCHES = 2;

    // Per-user monitored repository store (DEC-046: 1 selected personal repository per user)
    private final Map<Long, MonitoredProject> userProjects = new ConcurrentHashMap<>();

    /**
     * Onboards and monitors a selected repository for the user.
     * Enforces 1 selected personal repository (DEC-046) and derives PRIMARY branch
     * from GitHub default branch (FR-020, FR-022).
     */
    public MonitoredProject selectRepository(UserSession user, SelectRepositoryRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User session is required");
        }
        if (request == null || request.githubRepoId() == null || request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("Valid repository selection details are required");
        }

        String fullName = request.fullName().trim();
        String[] parts = fullName.split("/", 2);
        String owner = (request.owner() != null && !request.owner().isBlank())
                ? request.owner().trim()
                : (parts.length > 0 ? parts[0] : "");
        String name = (request.name() != null && !request.name().isBlank())
                ? request.name().trim()
                : (parts.length > 1 ? parts[1] : fullName);

        String defaultBranch = (request.defaultBranch() != null && !request.defaultBranch().isBlank())
                ? request.defaultBranch().trim()
                : "main";

        boolean isPrivate = Boolean.TRUE.equals(request.isPrivate());

        MonitoredProject project = new MonitoredProject(
                UUID.randomUUID().toString(),
                user.getGithubUserId(),
                request.githubRepoId(),
                owner,
                name,
                fullName,
                defaultBranch,
                defaultBranch, // PRIMARY derived from GitHub default branch (FR-020, FR-022)
                List.of(),     // Secondary branches initially empty
                isPrivate,
                Instant.now(),
                "ACTIVE"
        );

        userProjects.put(user.getGithubUserId(), project);
        return project;
    }

    /**
     * Retrieves the active monitored project for the authenticated user.
     */
    public Optional<MonitoredProject> getCurrentProject(UserSession user) {
        if (user == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userProjects.get(user.getGithubUserId()));
    }

    public Optional<MonitoredProject> getProjectByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userProjects.get(userId));
    }

    /**
     * Configures up to 2 secondary branches for monitoring (FR-020, FR-023).
     */
    public MonitoredProject updateBranchConfiguration(UserSession user, BranchConfigRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User session is required");
        }

        MonitoredProject project = userProjects.get(user.getGithubUserId());
        if (project == null) {
            throw new NoSuchElementException("No active monitored repository found for user");
        }

        List<String> secondaryBranches = request != null && request.secondaryBranches() != null
                ? request.secondaryBranches()
                : List.of();

        // Filter out blank entries and exclude duplicate of primary branch
        List<String> cleaned = secondaryBranches.stream()
                .filter(b -> b != null && !b.isBlank())
                .map(String::trim)
                .filter(b -> !b.equals(project.getPrimaryBranch()))
                .distinct()
                .toList();

        // Validate max 2 secondary branch slots (FR-020, FR-023)
        if (cleaned.size() > MAX_SECONDARY_BRANCHES) {
            throw new IllegalArgumentException("Maximum of " + MAX_SECONDARY_BRANCHES + " secondary branches allowed");
        }

        project.setSecondaryBranches(cleaned);
        return project;
    }

    /**
     * Handles GitHub default branch changes (FR-022, FR-023).
     * Automatically changes primary branch to match new default branch.
     * Retains user-selected secondary branches while removing new default from secondary if present.
     */
    public void handleDefaultBranchSync(MonitoredProject project, String newDefaultBranch) {
        if (project == null || newDefaultBranch == null || newDefaultBranch.isBlank()) {
            return;
        }
        project.updateDefaultBranch(newDefaultBranch.trim());
    }

    public void clearAllProjects() {
        userProjects.clear();
    }
}
