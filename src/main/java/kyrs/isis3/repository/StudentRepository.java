package kyrs.isis3.repository;

import kyrs.isis3.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long>, PagingAndSortingRepository<Student, Long> {
    List<Student> findByStudentGroupIdStudentGroup(Long idStudentGroup);

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.grades WHERE s.studentGroup.idStudentGroup = :studentGroupId")
    List<Student> findByStudentGroupIdStudentGroupWithGrades(@Param("studentGroupId") Long studentGroupId);

    @Modifying
    @Query("DELETE FROM Student s WHERE s.studentGroup.idStudentGroup = :studentGroupId")
    void deleteByStudentGroupId(@Param("studentGroupId") Long studentGroupId);

    List<Student> findByStudentGroupTeachingMethodIdTeachingMethod(Long methodId);
    List<Student> findByStudentGroupIdStudentGroupAndStudentGroupTeachingMethodIdTeachingMethod(
            Long groupId, Long methodId);
}
