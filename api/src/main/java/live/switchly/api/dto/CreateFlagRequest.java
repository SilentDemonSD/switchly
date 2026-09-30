package live.switchly.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateFlagRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9-]+", message = "use only lowercase letters, numbers and hyphens")
        String key,

        @NotBlank
        String name,

        String description
) {}
