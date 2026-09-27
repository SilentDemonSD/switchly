package live.switchly.api.controller;

import live.switchly.api.dto.CreateOrganizationRequest;
import live.switchly.api.model.Organization;
import live.switchly.api.service.OrganizationService;
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
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/orgs")
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.create(request.name());
    }

    @GetMapping("/orgs")
    public List<Organization> getAll() {
        return organizationService.getAll();
    }

    @GetMapping("/orgs/{orgId}")
    public Organization getById(@PathVariable UUID orgId) {
        return organizationService.getById(orgId);
    }
}