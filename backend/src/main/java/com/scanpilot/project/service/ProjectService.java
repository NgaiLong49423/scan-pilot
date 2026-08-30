package com.scanpilot.project.service;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.service.GitHubAppService;
import com.scanpilot.persistence.entity.MonitoredBranchEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserInstallationEntity;
import com.scanpilot.persistence.repository.MonitoredBranchRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserInstallationRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.project.dto.BranchConfigRequest;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.model.MonitoredProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final UserRepository userRepository;
    private final RepositoryRepository repositoryRepository;
    private final MonitoredBranchRepository monitoredBranchRepository;
    private final UserInstallationRepository userInstallationRepository;
    private final GitHubAppService gitHubAppService;

    // ponytail: in-memory map holds active UI selection context only; PostgreSQL RepositoryEntity is the strict source of truth for repository identity and scan authorization
    private final Map<Long, MonitoredProject> userProjects = new ConcurrentHashMap<>();

    /**
     * Onboards and monitors a selected repository for the user.
     * Enforces PostgreSQL as authoritative source of truth for repositories (Issue #53)
     * and derives PRIMARY branch from GitHub default branch (FR-020, FR-022).
     */
    @Transactional
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

        if (userRepository == null || repositoryRepository == null) {
            throw new IllegalStateException("Database repositories are not available");
        }

        UserEntity userEntity = userRepository.findByGithubUserId(user.getGithubUserId())
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .githubUserId(user.getGithubUserId())
                        .login(user.getLogin())
                        .name(user.getName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .createdAt(Instant.now())
                        .build()));

        // Two-Level Server-Side Authorization for GitHub App Installation Binding
        Long verifiedInstallationId = null;
        com.scanpilot.github.dto.GitHubRepositoryDto verifiedServerRepo = null;

        if (user.getInstallationId() != null && userInstallationRepository != null) {
            // Level 1: Verify user possesses verified association with this installation
            Optional<UserInstallationEntity> userInst = userInstallationRepository.findByUserIdAndInstallationId(userEntity.getId(), user.getInstallationId());
            if (userInst.isPresent() && gitHubAppService != null) {
                try {
                    // Level 2: Verify selected repository is accessible to user under this installation
                    List<com.scanpilot.github.dto.GitHubRepositoryDto> accessibleRepos = gitHubAppService.getUserAccessibleInstallationRepositories(user.getAccessToken(), user.getInstallationId());
                    Optional<com.scanpilot.github.dto.GitHubRepositoryDto> matched = accessibleRepos.stream()
                            .filter(r -> r.id().equals(request.githubRepoId()))
                            .findFirst();
                    if (matched.isPresent()) {
                        verifiedInstallationId = user.getInstallationId();
                        verifiedServerRepo = matched.get();
                    } else {
                        log.warn("Repository is not accessible to user under installation during selection");
                    }
                } catch (Exception e) {
                    log.warn("Failed to verify repository accessibility under installation during selection");
                }
            }
        }
        final Long finalInstallationId = verifiedInstallationId;

        // Persist trusted server-verified metadata when available; fallback safely for unverified flow
        String effectiveOwner;
        String effectiveName;
        String effectiveFullName;
        String effectiveDefaultBranch;
        boolean effectiveIsPrivate;

        if (verifiedServerRepo != null) {
            effectiveOwner = verifiedServerRepo.owner();
            effectiveName = verifiedServerRepo.name();
            effectiveFullName = verifiedServerRepo.fullName();
            effectiveDefaultBranch = verifiedServerRepo.defaultBranch();
            effectiveIsPrivate = verifiedServerRepo.isPrivate();
        } else {
            effectiveOwner = owner;
            effectiveName = name;
            effectiveFullName = fullName;
            effectiveDefaultBranch = defaultBranch;
            effectiveIsPrivate = isPrivate;
        }

        RepositoryEntity repoEntity = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), request.githubRepoId())
                .map(existing -> {
                    // If existing repository was already verified bound, and this selection failed Level-2, do NOT corrupt trusted metadata
                    if (existing.getInstallationId() != null && finalInstallationId == null) {
                        log.warn("Preserving existing verified repository metadata during unverified selection attempt");
                    } else {
                        existing.setOwner(effectiveOwner);
                        existing.setName(effectiveName);
                        existing.setFullName(effectiveFullName);
                        existing.setDefaultBranch(effectiveDefaultBranch);
                        existing.setPrimaryBranch(effectiveDefaultBranch);
                        existing.setIsPrivate(effectiveIsPrivate);
                        existing.setInstallationId(finalInstallationId);
                    }
                    existing.setUpdatedAt(Instant.now());
                    return repositoryRepository.save(existing);
                })
                .orElseGet(() -> repositoryRepository.save(RepositoryEntity.builder()
                        .userId(userEntity.getId())
                        .githubRepoId(request.githubRepoId())
                        .installationId(finalInstallationId)
                        .owner(effectiveOwner)
                        .name(effectiveName)
                        .fullName(effectiveFullName)
                        .defaultBranch(effectiveDefaultBranch)
                        .primaryBranch(effectiveDefaultBranch)
                        .isPrivate(effectiveIsPrivate)
                        .status("ACTIVE")
                        .monitoredAt(Instant.now())
                        .build()));

        UUID repoId = repoEntity.getId();
        Instant monitoredAt = repoEntity.getMonitoredAt() != null ? repoEntity.getMonitoredAt() : Instant.now();

        if (monitoredBranchRepository != null) {
            List<MonitoredBranchEntity> existingBranches = monitoredBranchRepository.findByRepositoryId(repoId);

            // Deactivate any prior PRIMARY branch rows where branchName != defaultBranch
            for (MonitoredBranchEntity b : existingBranches) {
                if ("PRIMARY".equalsIgnoreCase(b.getBranchType()) && !b.getBranchName().equals(defaultBranch)) {
                    b.setIsActive(false);
                    monitoredBranchRepository.save(b);
                }
            }

            // Ensure the new defaultBranch has branchType="PRIMARY" and isActive=true
            Optional<MonitoredBranchEntity> primaryBranchOpt = existingBranches.stream()
                    .filter(b -> b.getBranchName().equals(defaultBranch))
                    .findFirst();
            if (primaryBranchOpt.isPresent()) {
                MonitoredBranchEntity existingPrimary = primaryBranchOpt.get();
                existingPrimary.setBranchType("PRIMARY");
                existingPrimary.setIsActive(true);
                monitoredBranchRepository.save(existingPrimary);
            } else {
                monitoredBranchRepository.save(MonitoredBranchEntity.builder()
                        .repositoryId(repoId)
                        .branchName(defaultBranch)
                        .branchType("PRIMARY")
                        .isActive(true)
                        .createdAt(Instant.now())
                        .build());
            }
        }

        MonitoredProject project = new MonitoredProject(
                repoId.toString(),
                user.getGithubUserId(),
                request.githubRepoId(),
                owner,
                name,
                fullName,
                defaultBranch,
                defaultBranch, // PRIMARY derived from GitHub default branch (FR-020, FR-022)
                List.of(),     // Secondary branches initially empty
                isPrivate,
                monitoredAt,
                "ACTIVE"
        );

        userProjects.put(user.getGithubUserId(), project);
        return project;
    }

    /**
     * Retrieves the active monitored project for the authenticated user.
     * Checks in-memory cache first, then falls back to PostgreSQL (surviving restarts & reloads).
     */
    public Optional<MonitoredProject> getCurrentProject(UserSession user) {
        if (user == null) {
            return Optional.empty();
        }
        MonitoredProject inMemory = userProjects.get(user.getGithubUserId());
        if (inMemory != null) {
            return Optional.of(inMemory);
        }

        List<MonitoredProject> allMonitored = getAllMonitoredProjects(user);
        if (!allMonitored.isEmpty()) {
            MonitoredProject latest = allMonitored.get(0);
            userProjects.put(user.getGithubUserId(), latest);
            return Optional.of(latest);
        }
        return Optional.empty();
    }

    public Optional<MonitoredProject> getProjectByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        MonitoredProject inMemory = userProjects.get(userId);
        if (inMemory != null) {
            return Optional.of(inMemory);
        }

        if (userRepository == null || repositoryRepository == null) {
            return Optional.empty();
        }

        Optional<UserEntity> userEntityOpt = userRepository.findByGithubUserId(userId);
        if (userEntityOpt.isEmpty()) {
            return Optional.empty();
        }

        List<RepositoryEntity> entities = repositoryRepository.findByUserId(userEntityOpt.get().getId());
        if (entities.isEmpty()) {
            return Optional.empty();
        }

        RepositoryEntity e = entities.get(0);
        List<MonitoredBranchEntity> activeBranches = (monitoredBranchRepository != null)
                ? monitoredBranchRepository.findByRepositoryIdAndIsActiveTrue(e.getId())
                : List.of();

        String primary = activeBranches.stream()
                .filter(b -> "PRIMARY".equalsIgnoreCase(b.getBranchType()))
                .map(MonitoredBranchEntity::getBranchName)
                .findFirst()
                .orElse(e.getPrimaryBranch() != null ? e.getPrimaryBranch() : e.getDefaultBranch());

        List<String> secBranches = activeBranches.stream()
                .filter(b -> "SECONDARY".equalsIgnoreCase(b.getBranchType()) || !b.getBranchName().equals(primary))
                .map(MonitoredBranchEntity::getBranchName)
                .filter(name -> !name.equals(primary))
                .distinct()
                .toList();

        MonitoredProject project = new MonitoredProject(
                e.getId().toString(),
                userId,
                e.getGithubRepoId(),
                e.getOwner(),
                e.getName(),
                e.getFullName(),
                e.getDefaultBranch(),
                primary,
                secBranches,
                Boolean.TRUE.equals(e.getIsPrivate()),
                e.getMonitoredAt() != null ? e.getMonitoredAt() : Instant.now(),
                e.getStatus() != null ? e.getStatus() : "ACTIVE"
        );

        userProjects.put(userId, project);
        return Optional.of(project);
    }

    /**
     * Retrieves all repositories explicitly monitored by the authenticated user from PostgreSQL.
     */
    public List<MonitoredProject> getAllMonitoredProjects(UserSession user) {
        if (user == null || userRepository == null || repositoryRepository == null) {
            return List.of();
        }
        Optional<UserEntity> userEntity = userRepository.findByGithubUserId(user.getGithubUserId());
        if (userEntity.isEmpty()) {
            return List.of();
        }
        List<RepositoryEntity> entities = repositoryRepository.findByUserId(userEntity.get().getId());
        return entities.stream()
                .map(e -> {
                    List<MonitoredBranchEntity> activeBranches = (monitoredBranchRepository != null)
                            ? monitoredBranchRepository.findByRepositoryIdAndIsActiveTrue(e.getId())
                            : List.of();

                    String primary = activeBranches.stream()
                            .filter(b -> "PRIMARY".equalsIgnoreCase(b.getBranchType()))
                            .map(MonitoredBranchEntity::getBranchName)
                            .findFirst()
                            .orElse(e.getPrimaryBranch() != null ? e.getPrimaryBranch() : e.getDefaultBranch());

                    List<String> secBranches = activeBranches.stream()
                            .filter(b -> "SECONDARY".equalsIgnoreCase(b.getBranchType()) || !b.getBranchName().equals(primary))
                            .map(MonitoredBranchEntity::getBranchName)
                            .filter(name -> !name.equals(primary))
                            .distinct()
                            .toList();

                    return new MonitoredProject(
                            e.getId().toString(),
                            user.getGithubUserId(),
                            e.getGithubRepoId(),
                            e.getOwner(),
                            e.getName(),
                            e.getFullName(),
                            e.getDefaultBranch(),
                            primary,
                            secBranches,
                            Boolean.TRUE.equals(e.getIsPrivate()),
                            e.getMonitoredAt() != null ? e.getMonitoredAt() : Instant.now(),
                            e.getStatus() != null ? e.getStatus() : "ACTIVE"
                    );
                })
                .toList();
    }

    /**
     * Configures up to 2 secondary branches for monitoring (FR-020, FR-023).
     * Enforces PostgreSQL ownership verification against repositoryId (Issue #53).
     */
    @Transactional
    public MonitoredProject updateBranchConfiguration(UserSession user, BranchConfigRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User session is required");
        }
        if (request == null || request.repositoryId() == null) {
            throw new IllegalArgumentException("Repository ID is required");
        }

        if (userRepository == null || repositoryRepository == null) {
            throw new IllegalStateException("Database repositories are not available");
        }

        UserEntity userEntity = userRepository.findByGithubUserId(user.getGithubUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        RepositoryEntity repo = repositoryRepository.findById(request.repositoryId())
                .orElseThrow(() -> new NoSuchElementException("Repository not found or unauthorized"));

        if (!repo.getUserId().equals(userEntity.getId())) {
            throw new NoSuchElementException("Repository not found or unauthorized");
        }

        String primaryBranch = repo.getPrimaryBranch() != null && !repo.getPrimaryBranch().isBlank()
                ? repo.getPrimaryBranch()
                : (repo.getDefaultBranch() != null ? repo.getDefaultBranch() : "main");

        List<String> secondaryBranches = request.secondaryBranches() != null
                ? request.secondaryBranches()
                : List.of();

        // Filter out blank entries and exclude duplicate of primary branch
        List<String> cleaned = secondaryBranches.stream()
                .filter(b -> b != null && !b.isBlank())
                .map(String::trim)
                .filter(b -> !b.equals(primaryBranch))
                .distinct()
                .toList();

        // Validate max 2 secondary branch slots (FR-020, FR-023)
        if (cleaned.size() > MAX_SECONDARY_BRANCHES) {
            throw new IllegalArgumentException("Maximum of " + MAX_SECONDARY_BRANCHES + " secondary branches allowed");
        }

        if (monitoredBranchRepository != null) {
            List<MonitoredBranchEntity> existingBranches = monitoredBranchRepository.findByRepositoryId(repo.getId());

            // Deactivate existing secondary branches not in cleaned list
            for (MonitoredBranchEntity existing : existingBranches) {
                if ("SECONDARY".equalsIgnoreCase(existing.getBranchType()) && !cleaned.contains(existing.getBranchName())) {
                    existing.setIsActive(false);
                    monitoredBranchRepository.save(existing);
                }
            }

            // Save or update active secondary branches
            for (String branchName : cleaned) {
                Optional<MonitoredBranchEntity> match = existingBranches.stream()
                        .filter(b -> b.getBranchName().equals(branchName))
                        .findFirst();
                if (match.isPresent()) {
                    MonitoredBranchEntity branch = match.get();
                    branch.setBranchType("SECONDARY");
                    branch.setIsActive(true);
                    monitoredBranchRepository.save(branch);
                } else {
                    monitoredBranchRepository.save(MonitoredBranchEntity.builder()
                            .repositoryId(repo.getId())
                            .branchName(branchName)
                            .branchType("SECONDARY")
                            .isActive(true)
                            .createdAt(Instant.now())
                            .build());
                }
            }
        }

        MonitoredProject project = new MonitoredProject(
                repo.getId().toString(),
                user.getGithubUserId(),
                repo.getGithubRepoId(),
                repo.getOwner(),
                repo.getName(),
                repo.getFullName(),
                repo.getDefaultBranch(),
                primaryBranch,
                cleaned,
                Boolean.TRUE.equals(repo.getIsPrivate()),
                repo.getMonitoredAt() != null ? repo.getMonitoredAt() : Instant.now(),
                repo.getStatus() != null ? repo.getStatus() : "ACTIVE"
        );

        userProjects.put(user.getGithubUserId(), project);
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
