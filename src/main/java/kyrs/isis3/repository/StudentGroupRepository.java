package kyrs.isis3.repository;

import kyrs.isis3.model.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long>, PagingAndSortingRepository<StudentGroup, Long> {
}
