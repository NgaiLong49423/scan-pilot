package com.scanpilot.project.dto;

import java.util.List;

public record BranchConfigRequest(
        List<String> secondaryBranches
) {}
