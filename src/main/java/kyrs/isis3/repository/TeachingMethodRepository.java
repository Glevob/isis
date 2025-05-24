package kyrs.isis3.repository;

import kyrs.isis3.model.TeachingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TeachingMethodRepository extends JpaRepository<TeachingMethod, Long>, PagingAndSortingRepository<TeachingMethod, Long> {
}
