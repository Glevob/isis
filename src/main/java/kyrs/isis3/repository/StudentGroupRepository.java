package kyrs.isis3.repository;

import kyrs.isis3.model.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long>, PagingAndSortingRepository<StudentGroup, Long> {
    List<StudentGroup> findByTeachingMethodIdTeachingMethod(Long idTeachingMethod);

    @Modifying
    @Query("DELETE FROM Student s WHERE s.studentGroup.idStudentGroup = :studentGroupId")
    void deleteStudentsByGroupId(@Param("studentGroupId") Long studentGroupId);

    
}
