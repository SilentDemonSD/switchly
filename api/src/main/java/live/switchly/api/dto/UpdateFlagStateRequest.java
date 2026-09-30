package live.switchly.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateFlagStateRequest(@NotNull Boolean enabled) {}
