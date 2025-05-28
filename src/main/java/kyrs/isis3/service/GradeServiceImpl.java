package kyrs.isis3.service;

import kyrs.isis3.model.Grade;
import kyrs.isis3.model.Student;
import kyrs.isis3.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {
    private final GradeRepository gradeRepository;

    @Override
    public Grade saveGrade(Grade grade) {
        return gradeRepository.save(grade);
    }

    @Override
    public List<Grade> getGradesByStudent(Optional<Student> student) {
        return gradeRepository.findByStudent(student);
    }

    @Override
    public List<Grade> getGradesByGroupId(Long groupId) {
        return gradeRepository.findByStudentStudentGroupIdStudentGroup(groupId);
    }

    @Override
    public void deleteGrade(Long id) {
        gradeRepository.deleteById(id);
    }

    @Override
    public Grade getGradeById(Long id) {
        return gradeRepository.findById(id).orElse(null);
    }
}
