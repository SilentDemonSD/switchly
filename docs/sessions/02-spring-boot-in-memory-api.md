# Session 2 — Your first Spring Boot API (in memory)

**Previous:** [Session 1 — What are we building?](01-what-are-we-building.md)

By the end of this session, you'll create an **organization**, a **project** inside it, and a **feature flag** inside that — and turn the flag on and off — all through your own API.

Everything lives in memory for now. No database yet.

| Method | URL                                  | What it does                        |
| ------ | ------------------------------------ | ----------------------------------- |
| `POST` | `/api/v1/orgs`                       | Create an organization              |
| `GET`  | `/api/v1/orgs`                       | List organizations                  |
| `GET`  | `/api/v1/orgs/{orgId}`               | Get one organization                |
| `POST` | `/api/v1/orgs/{orgId}/projects`      | Create a project in an organization |
| `GET`  | `/api/v1/orgs/{orgId}/projects`      | List an organization's projects     |
| `GET`  | `/api/v1/projects/{projectId}`       | Get one project                     |
| `POST` | `/api/v1/projects/{projectId}/flags` | Create a flag in a project          |
| `GET`  | `/api/v1/projects/{projectId}/flags` | List a project's flags              |
| `GET`  | `/api/v1/flags/{flagId}`             | Get one flag                        |
| `PUT`  | `/api/v1/flags/{flagId}/state`       | Turn a flag on or off               |

---

## Step 1 — Create the project

Go to [start.spring.io](https://start.spring.io) and fill in:

| Setting       | Value                                                                     |
| ------------- | ------------------------------------------------------------------------- |
| Project       | **Maven**                                                                 |
| Language      | **Java**                                                                  |
| Spring Boot   | The default version selected (avoid anything ending in SNAPSHOT, M or RC) |
| Group         | `live.switchly`                                                           |
| Artifact      | `api`                                                                     |
| Package name  | `live.switchly.api` (filled in automatically)                             |
| Packaging     | **Jar**                                                                   |
| Configuration | **YAML**                                                                  |
| Java          | **21**                                                                    |
| Dependencies  | **Spring Web**, **Validation**                                            |

Click **Generate**, unzip it, and put the `api` folder inside your `switchly` repository:

```
switchly/
├── api/        ← here
└── docs/
```

Open the `api` folder in IntelliJ IDEA or VS Code.

**Rename the main class.** start.spring.io calls it `ApiApplication`. Rename it to `SwitchlyApiApplication`:

- **IntelliJ:** right-click the file → **Refactor → Rename**
- **VS Code:** open the file, click on the class name, press **F2**

This renames the file and every reference to it. Do the same for `ApiApplicationTests` in `src/test` → `SwitchlyApiApplicationTests`.

---

## Step 2 — Run it

From inside the `api` folder:

```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```

The first run downloads dependencies, so it takes a minute. Wait for:

```
Tomcat started on port 8080
```

Open [http://localhost:8080](http://localhost:8080). You'll see a **Whitelabel Error Page** — that's success. The server is running; it just has no pages yet.

Stop it with `Ctrl + C`.

---

## Step 3 — The layers

Every request travels through the same layers, in the same order:

```
HTTP request
    │
    ▼
Controller   → speaks HTTP: reads the request, returns a response. No business logic.
    │
    ▼
Service      → the business rules: "a project needs an organization that exists".
    │
    ▼
Repository   → stores and finds data. Today: a Map in memory. Later: a database.
```

And alongside them:

| Package                                 | Holds                                                             |
| --------------------------------------- | ----------------------------------------------------------------- |
| `model`                                 | The things we store: `Organization`, `Project`, `Flag`            |
| `dto`                                   | The shape of request bodies — what the client sends us            |
| `exception`                             | Our errors, and the one place that turns them into HTTP responses |
| `controller` · `service` · `repository` | The three layers above                                            |

Configuration lives in `src/main/resources/application.yaml` for now (it may be named `application.yml` — same thing).

**The rule:** a controller only talks to a service, and a service only talks to repositories and other services. Nothing skips a layer.

---

## Step 4 — Create the packages

Under `src/main/java/live/switchly/api/`, create these folders:

```
live/switchly/api/
├── SwitchlyApiApplication.java    (already there)
├── controller/
├── dto/
├── exception/
├── model/
├── repository/
└── service/
```

Every class must live **inside** `live.switchly.api` (or a folder under it). Spring only finds classes there.

---

## Step 5 — Organization

We build it bottom-up: model → repository → service → controller.

### 5.1 Model — `model/Organization.java`

```java
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
```

### 5.2 Repository — `repository/OrganizationRepository.java`

An **interface**: it says _what_ the repository can do, not _how_.

```java
package live.switchly.api.repository;

import live.switchly.api.model.Organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {

    Organization save(Organization organization);

    Optional<Organization> findById(UUID id);

    List<Organization> findAll();
}
```

### 5.3 In-memory repository — `repository/InMemoryOrganizationRepository.java`

The _how_: a `Map` in memory. `@Repository` tells Spring to create one and hand it to whoever needs it.

```java
package live.switchly.api.repository;

import live.switchly.api.model.Organization;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOrganizationRepository implements OrganizationRepository {

    private final Map<UUID, Organization> store = new ConcurrentHashMap<>();

    @Override
    public Organization save(Organization organization) {
        store.put(organization.getId(), organization);
        return organization;
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Organization> findAll() {
        return new ArrayList<>(store.values());
    }
}
```

> **Why an interface?** Later, a database version replaces this class. Because the service only knows about `OrganizationRepository`, the service and controller won't change at all.

### 5.4 Service — `service/OrganizationService.java`

`NotFoundException` will show red for now — we create it in Step 6.

```java
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
```

> **Where's the `new`?** We never write `new OrganizationService(...)`. Spring sees the constructor, finds an `OrganizationRepository`, and passes it in. This is called **dependency injection**.

### 5.5 Request body — `dto/CreateOrganizationRequest.java`

```java
package live.switchly.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(@NotBlank String name) {}
```

### 5.6 Controller — `controller/OrganizationController.java`

```java
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
@RequestMapping("/api/v1/orgs")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.create(request.name());
    }

    @GetMapping
    public List<Organization> getAll() {
        return organizationService.getAll();
    }

    @GetMapping("/{orgId}")
    public Organization getById(@PathVariable UUID orgId) {
        return organizationService.getById(orgId);
    }
}
```

- `@RequestBody` turns the JSON body into a `CreateOrganizationRequest`.
- `@Valid` checks the `@NotBlank` rule before your code runs.
- `@ResponseStatus(HttpStatus.CREATED)` returns **201** instead of 200.

---

## Step 6 — Errors, handled properly

### 6.1 `exception/NotFoundException.java`

```java
package live.switchly.api.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
```

### 6.2 `exception/ErrorResponse.java` — the shape of every error we return

```java
package live.switchly.api.exception;

public record ErrorResponse(ErrorDetail error) {

    public record ErrorDetail(String code, String message) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetail(code, message));
    }
}
```

### 6.3 `exception/GlobalExceptionHandler.java`

One place that turns exceptions into HTTP responses, for every controller.

```java
package live.switchly.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException e) {
        return ErrorResponse.of("NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ErrorResponse.of("VALIDATION_FAILED", message);
    }
}
```

### 6.4 Try it

Run the app again, and in a second terminal:

```bash
curl -i -X POST http://localhost:8080/api/v1/orgs \
  -H "Content-Type: application/json" \
  -d '{"name": "Zomato"}'
```

```
HTTP/1.1 201
{"id":"6f1c…","name":"Zomato"}
```

Copy the `id`. Now try these:

```bash
curl http://localhost:8080/api/v1/orgs                     # the list
curl http://localhost:8080/api/v1/orgs/PASTE_ORG_ID        # just Zomato
curl -i http://localhost:8080/api/v1/orgs/00000000-0000-0000-0000-000000000000
```

The last one returns:

```
HTTP/1.1 404
{"error":{"code":"NOT_FOUND","message":"Organization 00000000-… not found"}}
```

And an empty name:

```bash
curl -i -X POST http://localhost:8080/api/v1/orgs \
  -H "Content-Type: application/json" -d '{"name": ""}'
```

```
HTTP/1.1 400
{"error":{"code":"VALIDATION_FAILED","message":"name: must not be blank"}}
```

---

## Step 7 — Project

Same pattern, faster. A project always belongs to an organization.

### `model/Project.java`

```java
package live.switchly.api.model;

import java.util.UUID;

public class Project {

    private final UUID id;
    private final UUID organizationId;
    private final String name;

    public Project(UUID id, UUID organizationId, String name) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
}
```

### `repository/ProjectRepository.java`

```java
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
```

### `repository/InMemoryProjectRepository.java`

```java
package live.switchly.api.repository;

import live.switchly.api.model.Project;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProjectRepository implements ProjectRepository {

    private final Map<UUID, Project> store = new ConcurrentHashMap<>();

    @Override
    public Project save(Project project) {
        store.put(project.getId(), project);
        return project;
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Project> findByOrganizationId(UUID organizationId) {
        return store.values().stream()
                .filter(project -> project.getOrganizationId().equals(organizationId))
                .toList();
    }
}
```

### `service/ProjectService.java`

Notice it asks `OrganizationService` — not the organization repository — whether the organization exists. Services talk to services.

```java
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
```

### `dto/CreateProjectRequest.java`

```java
package live.switchly.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(@NotBlank String name) {}
```

### `controller/ProjectController.java`

```java
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
```

### Try it

```bash
curl -i -X POST http://localhost:8080/api/v1/orgs/PASTE_ORG_ID/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Consumer App"}'
```

Copy the project's `id`. Then try the same request with an organization ID that doesn't exist — you should get a 404.

---

## Step 8 — Flag, with an on/off switch

A flag belongs to a project. It has a `key` — the name your customers' code will use, like `new-checkout` — and it starts **off**.

### `model/Flag.java`

```java
package live.switchly.api.model;

import java.util.UUID;

public class Flag {

    private final UUID id;
    private final UUID organizationId;
    private final UUID projectId;
    private final String key;
    private final String name;
    private boolean enabled;

    public Flag(UUID id, UUID organizationId, UUID projectId, String key, String name, boolean enabled) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.key = key;
        this.name = name;
        this.enabled = enabled;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getProjectId() { return projectId; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

### `repository/FlagRepository.java`

```java
package live.switchly.api.repository;

import live.switchly.api.model.Flag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlagRepository {

    Flag save(Flag flag);

    Optional<Flag> findById(UUID id);

    List<Flag> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndKey(UUID projectId, String key);
}
```

### `repository/InMemoryFlagRepository.java`

```java
package live.switchly.api.repository;

import live.switchly.api.model.Flag;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryFlagRepository implements FlagRepository {

    private final Map<UUID, Flag> store = new ConcurrentHashMap<>();

    @Override
    public Flag save(Flag flag) {
        store.put(flag.getId(), flag);
        return flag;
    }

    @Override
    public Optional<Flag> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Flag> findByProjectId(UUID projectId) {
        return store.values().stream()
                .filter(flag -> flag.getProjectId().equals(projectId))
                .toList();
    }

    @Override
    public boolean existsByProjectIdAndKey(UUID projectId, String key) {
        return store.values().stream()
                .anyMatch(flag -> flag.getProjectId().equals(projectId) && flag.getKey().equals(key));
    }
}
```

### `exception/ConflictException.java`

Two flags in the same project can't share a key.

```java
package live.switchly.api.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
```

Add this method to `GlobalExceptionHandler`:

```java
@ExceptionHandler(ConflictException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ErrorResponse handleConflict(ConflictException e) {
    return ErrorResponse.of("CONFLICT", e.getMessage());
}
```

### `service/FlagService.java`

```java
package live.switchly.api.service;

import live.switchly.api.exception.ConflictException;
import live.switchly.api.exception.NotFoundException;
import live.switchly.api.model.Flag;
import live.switchly.api.model.Project;
import live.switchly.api.repository.FlagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FlagService {

    private final ProjectService projectService;
    private final FlagRepository flagRepository;

    public FlagService(ProjectService projectService, FlagRepository flagRepository) {
        this.projectService = projectService;
        this.flagRepository = flagRepository;
    }

    public Flag create(UUID projectId, String key, String name) {
        Project project = projectService.getById(projectId);  // 404 if it doesn't exist

        if (flagRepository.existsByProjectIdAndKey(projectId, key)) {
            throw new ConflictException("A flag with key '" + key + "' already exists in this project");
        }

        Flag flag = new Flag(UUID.randomUUID(), project.getOrganizationId(), project.getId(), key, name, false);
        return flagRepository.save(flag);
    }

    public Flag getById(UUID id) {
        return flagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flag " + id + " not found"));
    }

    public List<Flag> getAllForProject(UUID projectId) {
        projectService.getById(projectId);  // 404 if it doesn't exist
        return flagRepository.findByProjectId(projectId);
    }

    public Flag setEnabled(UUID flagId, boolean enabled) {
        Flag flag = getById(flagId);
        flag.setEnabled(enabled);
        return flagRepository.save(flag);
    }
}
```

### `dto/CreateFlagRequest.java` and `dto/UpdateFlagStateRequest.java`

```java
package live.switchly.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateFlagRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9-]+", message = "use only lowercase letters, numbers and hyphens")
        String key,

        @NotBlank
        String name
) {}
```

```java
package live.switchly.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateFlagStateRequest(@NotNull Boolean enabled) {}
```

### `controller/FlagController.java`

```java
package live.switchly.api.controller;

import live.switchly.api.dto.CreateFlagRequest;
import live.switchly.api.dto.UpdateFlagStateRequest;
import live.switchly.api.model.Flag;
import live.switchly.api.service.FlagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FlagController {

    private final FlagService flagService;

    public FlagController(FlagService flagService) {
        this.flagService = flagService;
    }

    @PostMapping("/projects/{projectId}/flags")
    @ResponseStatus(HttpStatus.CREATED)
    public Flag create(@PathVariable UUID projectId, @Valid @RequestBody CreateFlagRequest request) {
        return flagService.create(projectId, request.key(), request.name());
    }

    @GetMapping("/projects/{projectId}/flags")
    public List<Flag> getAllForProject(@PathVariable UUID projectId) {
        return flagService.getAllForProject(projectId);
    }

    @GetMapping("/flags/{flagId}")
    public Flag getById(@PathVariable UUID flagId) {
        return flagService.getById(flagId);
    }

    @PutMapping("/flags/{flagId}/state")
    public Flag setState(@PathVariable UUID flagId, @Valid @RequestBody UpdateFlagStateRequest request) {
        return flagService.setEnabled(flagId, request.enabled());
    }
}
```

> **Why `PUT .../state` with `{"enabled": true}`, and not a `/toggle` endpoint?** "Set it to on" gives the same result no matter how many times it's sent. "Flip it" doesn't — if a flip request times out and you retry, you don't know whether the flag ends up on or off.

---

## Step 9 — The whole flow

Restart the app, then run these in order. Replace the `PASTE_…` values with the IDs from the previous response.

```bash
# 1. Create an organization
curl -X POST http://localhost:8080/api/v1/orgs \
  -H "Content-Type: application/json" -d '{"name": "Zomato"}'

# 2. Create a project in it
curl -X POST http://localhost:8080/api/v1/orgs/PASTE_ORG_ID/projects \
  -H "Content-Type: application/json" -d '{"name": "Consumer App"}'

# 3. Create a flag in the project — it starts OFF
curl -X POST http://localhost:8080/api/v1/projects/PASTE_PROJECT_ID/flags \
  -H "Content-Type: application/json" -d '{"key": "new-checkout", "name": "New Checkout"}'

# 4. Turn it ON
curl -X PUT http://localhost:8080/api/v1/flags/PASTE_FLAG_ID/state \
  -H "Content-Type: application/json" -d '{"enabled": true}'

# 5. Check it
curl http://localhost:8080/api/v1/flags/PASTE_FLAG_ID
```

Step 5 should show `"enabled":true`.

Now try to break it:

- Create the same flag key twice in one project → **409**
- Create a flag with the key `New Checkout` → **400**
- Turn on a flag ID that doesn't exist → **404**

### One last thing: restart the app

Stop it, start it, and run `curl http://localhost:8080/api/v1/orgs`.

```
[]
```

Everything is gone. Memory is wiped when the program stops. That's the problem a database solves — and because of the repository interfaces, adding one won't touch your services or controllers.

---

## Your final structure

```
api/src/main/java/live/switchly/api/
├── SwitchlyApiApplication.java
├── controller/
│   ├── OrganizationController.java
│   ├── ProjectController.java
│   └── FlagController.java
├── dto/
│   ├── CreateOrganizationRequest.java
│   ├── CreateProjectRequest.java
│   ├── CreateFlagRequest.java
│   └── UpdateFlagStateRequest.java
├── exception/
│   ├── NotFoundException.java
│   ├── ConflictException.java
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
├── model/
│   ├── Organization.java
│   ├── Project.java
│   └── Flag.java
├── repository/
│   ├── OrganizationRepository.java     InMemoryOrganizationRepository.java
│   ├── ProjectRepository.java          InMemoryProjectRepository.java
│   └── FlagRepository.java             InMemoryFlagRepository.java
└── service/
    ├── OrganizationService.java
    ├── ProjectService.java
    └── FlagService.java
```

---

## If something goes wrong

| You see                                        | Fix                                                                                            |
| ---------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `Port 8080 was already in use`                 | Another copy is still running. Stop it, or set `server.port` to `8081` in `application.yaml`. |
| **Every** URL returns 404, even `/api/v1/orgs` | Your controller is outside the `live.switchly.api` package. Move it in.                        |
| `415 Unsupported Media Type`                   | You forgot `-H "Content-Type: application/json"`.                                              |
| An empty name is accepted                      | You forgot `@Valid` before `@RequestBody`.                                                     |
| `No qualifying bean of type …Repository`       | You forgot `@Repository` on the in-memory class (or `@Service` on a service).                  |
| `Name for argument of type UUID not specified` | Run with `./mvnw spring-boot:run` instead of your IDE's Run button.                            |
| `curl` behaves strangely on Windows            | Use Git Bash, or use Postman: **Import → Raw text** accepts these `curl` commands.             |

---

## Homework

1. **Add a `description` field to `Flag`.** Optional when creating. Which files did you have to touch — and which didn't you?
2. **Add `DELETE /api/v1/flags/{flagId}`.** Return **204 No Content**. Deleting a flag that doesn't exist should return 404.
3. **Think, don't code:** a customer wants `new-checkout` **on** in their test environment but **off** for real users. What in your current design would have to change?

---

## Checkpoint

- ✅ Your app runs, and you can explain what each layer does.
- ✅ You can create an organization → project → flag, and turn the flag on and off.
- ✅ Unknown IDs return **404**, duplicate keys **409**, and bad input **400** — each with a clear message.
