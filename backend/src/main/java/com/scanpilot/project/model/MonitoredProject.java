package com.scanpilot.project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory thread-safe model for storing monitored repository state per user.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MonitoredProject {

    private String id;
    private Long userId;
    private Long githubRepoId;
    private String owner;
    private String name;
    private String fullName;
    private volatile String defaultBranch;
    private volatile String primaryBranch;
    @Builder.Default
    private List<String> secondaryBranches = new CopyOnWriteArrayList<>();
    private boolean isPrivate;
    private Instant monitoredAt;
    private volatile String status;

    public List<String> getSecondaryBranches() {
        return secondaryBranches != null ? Collections.unmodifiableList(secondaryBranches) : List.of();
    }

    public synchronized void setSecondaryBranches(List<String> branches) {
        if (this.secondaryBranches == null || !(this.secondaryBranches instanceof CopyOnWriteArrayList)) {
            this.secondaryBranches = new CopyOnWriteArrayList<>();
        } else {
            this.secondaryBranches.clear();
        }
        if (branches != null) {
            for (String b : branches) {
                if (b != null && !b.isBlank() && !this.secondaryBranches.contains(b.trim())) {
                    this.secondaryBranches.add(b.trim());
                }
            }
        }
    }

    public synchronized void updateDefaultBranch(String newDefaultBranch) {
        if (newDefaultBranch == null || newDefaultBranch.isBlank()) {
            return;
        }
        String cleanBranch = newDefaultBranch.trim();
        this.defaultBranch = cleanBranch;
        this.primaryBranch = cleanBranch;
        // If the new primary branch was previously a secondary branch, remove it from secondary slots (FR-022, FR-023)
        if (this.secondaryBranches != null) {
            try {
                this.secondaryBranches.remove(cleanBranch);
            } catch (UnsupportedOperationException e) {
                this.secondaryBranches = new CopyOnWriteArrayList<>(this.secondaryBranches);
                this.secondaryBranches.remove(cleanBranch);
            }
        }
    }
}
