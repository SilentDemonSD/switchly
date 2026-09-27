package live.switchly.api.service;

import live.switchly.api.exception.NotFoundException;
import live.switchly.api.model.Organization;
import live.switchly.api.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Organization create(String name) {
        Organization organization = new Organization(UUID.randomUUID(), name);
        return organizationRepository.save(organization);
    }

    public Organization getById(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization " + id + " not found"));
    }

    public List<Organization> getAll() {
        return organizationRepository.findAll();
    }
}