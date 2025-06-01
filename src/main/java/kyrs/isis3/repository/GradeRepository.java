package kyrs.isis3.repository;

import kyrs.isis3.model.Grade;
import kyrs.isis3.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudent(Optional<Student> student);
    List<Grade> findByStudentStudentGroupIdStudentGroup(Long studentGroupId);

    @Modifying
    @Query("DELETE FROM Grade g WHERE g.student.studentGroup.idStudentGroup = :studentGroupId")
    void deleteByStudentStudentGroupId(@Param("studentGroupId") Long studentGroupId);

    @Modifying
    @Query("DELETE FROM Grade g WHERE g.student.studentGroup.idStudentGroup = :studentGroupId")
    void deleteByStudentGroupId(@Param("studentGroupId") Long studentGroupId);

    @Query("SELECT g FROM Grade g WHERE g.student.studentGroup.teachingMethod.idTeachingMethod = :teachingMethodId")
    List<Grade> findByTeachingMethodId(@Param("teachingMethodId") Long teachingMethodId);
}
