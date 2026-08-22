package com.scanpilot.project.dto;

import java.util.List;
import java.util.UUID;

public record BranchConfigRequest(
        UUID repositoryId,
        List<String> secondaryBranches
) {
    public BranchConfigRequest(List<String> secondaryBranches) {
        this(null, secondaryBranches);
    }
}
