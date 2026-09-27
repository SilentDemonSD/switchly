package live.switchly.api.controller;

import live.switchly.api.dto.CreateProjectRequest;
import live.switchly.api.model.Project;
import live.switchly.api.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/orgs/{orgId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@PathVariable UUID orgId, @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(orgId, request.name());
    }

    @GetMapping("/orgs/{orgId}/projects")
    public List<Project> getAllForOrganization(@PathVariable UUID orgId) {
        return projectService.getAllForOrganization(orgId);
    }

    @GetMapping("/projects/{projectId}")
    public Project getById(@PathVariable UUID projectId) {
        return projectService.getById(projectId);
    }
}