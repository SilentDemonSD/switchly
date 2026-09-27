package live.switchly.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(@NotBlank String name) {}