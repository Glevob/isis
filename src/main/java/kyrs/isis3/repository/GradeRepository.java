package kyrs.isis3.repository;

import kyrs.isis3.model.Grade;
import kyrs.isis3.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudent(Optional<Student> student);
    List<Grade> findByStudentStudentGroupIdStudentGroup(Long studentGroupId);
}
