package kyrs.isis3.service;

import kyrs.isis3.model.Grade;
import kyrs.isis3.model.Student;

import java.util.List;
import java.util.Optional;

public interface GradeService {
    Grade saveGrade(Grade grade);
    List<Grade> getGradesByStudent(Optional<Student> student);
    List<Grade> getGradesByGroupId(Long studentGroupId);
    void deleteGrade(Long id);
    Grade getGradeById(Long id);
}
