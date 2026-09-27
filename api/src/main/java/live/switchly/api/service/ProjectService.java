package live.switchly.api.service;

import live.switchly.api.exception.NotFoundException;
import live.switchly.api.model.Organization;
import live.switchly.api.model.Project;
import live.switchly.api.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final OrganizationService organizationService;
    private final ProjectRepository projectRepository;

    public ProjectService(OrganizationService organizationService, ProjectRepository projectRepository) {
        this.organizationService = organizationService;
        this.projectRepository = projectRepository;
    }

    public Project create(UUID organizationId, String name) {
        Organization organization = organizationService.getById(organizationId);  // 404 if it doesn't exist
        Project project = new Project(UUID.randomUUID(), organization.getId(), name);
        return projectRepository.save(project);
    }

    public Project getById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project " + id + " not found"));
    }

    public List<Project> getAllForOrganization(UUID organizationId) {
        organizationService.getById(organizationId);  // 404 if it doesn't exist
        return projectRepository.findByOrganizationId(organizationId);
    }
}