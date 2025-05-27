package kyrs.isis3.repository;

import kyrs.isis3.model.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long>, PagingAndSortingRepository<StudentGroup, Long> {
    List<StudentGroup> findByTeachingMethodIdTeachingMethod(Long idTeachingMethod);
}
