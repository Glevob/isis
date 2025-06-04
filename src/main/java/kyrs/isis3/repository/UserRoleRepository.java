package kyrs.isis3.repository;

import org.springframework.data.repository.CrudRepository;
import kyrs.isis3.model.UserRole;

public interface UserRoleRepository extends CrudRepository<UserRole, Long> {
}