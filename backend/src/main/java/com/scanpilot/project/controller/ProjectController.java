package com.scanpilot.project.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.project.dto.BranchConfigRequest;
import com.scanpilot.project.dto.MonitoredProjectDto;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.model.MonitoredProject;
import com.scanpilot.project.service.ProjectService;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Selects and onboards a repository for monitoring (DEC-046).
     */
    @PostMapping("/select-repository")
    @RequireAuth
    public ResponseEntity<MonitoredProjectDto> selectRepository(
            @CurrentUser UserSession session,
            @Valid @RequestBody SelectRepositoryRequest request
    ) {
        MonitoredProject project = projectService.selectRepository(session, request);
        return ResponseEntity.ok(MonitoredProjectDto.from(project));
    }

    /**
     * Retrieves the currently active monitored project for the authenticated user.
     */
    @GetMapping("/current")
    @RequireAuth
    public ResponseEntity<MonitoredProjectDto> getCurrentProject(@CurrentUser UserSession session) {
        return projectService.getCurrentProject(session)
                .map(MonitoredProjectDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Retrieves all repositories explicitly monitored by the user.
     */
    @GetMapping("/monitored")
    @RequireAuth
    public ResponseEntity<List<MonitoredProjectDto>> getAllMonitoredProjects(@CurrentUser UserSession session) {
        List<MonitoredProjectDto> dtos = projectService.getAllMonitoredProjects(session).stream()
                .map(MonitoredProjectDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Configures up to 2 secondary branches for monitoring (FR-020, FR-023).
     */
    @PutMapping("/branches")
    @RequireAuth
    public ResponseEntity<MonitoredProjectDto> updateBranchConfiguration(
            @CurrentUser UserSession session,
            @Valid @RequestBody BranchConfigRequest request
    ) {
        MonitoredProject updated = projectService.updateBranchConfiguration(session, request);
        return ResponseEntity.ok(MonitoredProjectDto.from(updated));
    }
}
