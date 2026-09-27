package live.switchly.api.model;

import java.util.UUID;

public class Organization {

    private final UUID id;
    private final String name;

    public Organization(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
}