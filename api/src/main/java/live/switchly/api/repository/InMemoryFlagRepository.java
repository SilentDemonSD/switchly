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

    @Override
    public void deleteById(UUID id) {
        store.remove(id);
    }
}