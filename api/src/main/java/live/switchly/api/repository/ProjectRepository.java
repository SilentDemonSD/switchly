package live.switchly.api.repository;

import live.switchly.api.model.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    Project save(Project project);

    Optional<Project> findById(UUID id);

    List<Project> findByOrganizationId(UUID organizationId);
}
