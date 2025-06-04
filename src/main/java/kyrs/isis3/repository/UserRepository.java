package kyrs.isis3.repository;

import org.springframework.data.repository.CrudRepository;
import kyrs.isis3.model.User;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByLogin(String login);
}
