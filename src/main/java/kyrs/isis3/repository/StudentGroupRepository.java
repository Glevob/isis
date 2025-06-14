package kyrs.isis3.repository;

import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long>, PagingAndSortingRepository<StudentGroup, Long> {
    List<StudentGroup> findByTeachingMethodIdTeachingMethod(Long idTeachingMethod);
    Optional<StudentGroup> findByNameGroup(String name);
    @Modifying
    @Query("DELETE FROM Student s WHERE s.studentGroup.idStudentGroup = :studentGroupId")
    void deleteStudentsByGroupId(@Param("studentGroupId") Long studentGroupId);
    List<StudentGroup> findByTeachingMethod(TeachingMethod teachingMethod); // Найти группы по методу обучения

    boolean existsByNameGroup(String nameGroup);
}
