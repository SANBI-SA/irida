package ca.corefacility.bioinformatics.irida.repositories.memory;

import ca.corefacility.bioinformatics.irida.model.User;
import ca.corefacility.bioinformatics.irida.repositories.CRUDRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory implementation of a user repository, for testing purposes only.
 *
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 */
public class UserMemoryRepository implements CRUDRepository<String, User> {

    private static final String BASE_URI = "http://api.irida.ca/User/";
    private static final Map<String, User> store = new HashMap<>();

    @Override
    public User create(User u) throws IllegalArgumentException {
        String id = BASE_URI + u.getUsername();
        u.setId(id);
        store.put(id, u);
        return u;
    }

    @Override
    public User read(String id) throws IllegalArgumentException {
        if (store.containsKey(id)) {
            return store.get(id);
        }
        throw new IllegalArgumentException("No such user exists with id [" + id + ".");
    }

    @Override
    public User update(User u) throws IllegalArgumentException {
        String id = u.getId();
        if (store.containsKey(u.getId())) {

            return store.put(id, u);
        }
        throw new IllegalArgumentException("No such user exists with id [" + id + ".");
    }

    @Override
    public void delete(String id) throws IllegalArgumentException {
        if (!store.containsKey(id)) {
            throw new IllegalArgumentException("No such user exists with id [" + id + ".");
        }
        store.remove(id);
    }

    @Override
    public List<User> list() {
        List<User> users = new ArrayList<>(store.values());
        Collections.sort(users);
        return users;
    }

    @Override
    public Boolean exists(String id) {
        return store.containsKey(id);
    }
}