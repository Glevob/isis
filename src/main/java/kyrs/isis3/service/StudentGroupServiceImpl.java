package kyrs.isis3.service;

import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.repository.StudentGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class StudentGroupServiceImpl implements StudentGroupService{
    private final StudentGroupRepository studentGroupRepository;

    @Override
    public void addStudentGroup(StudentGroup studentGroup) {
        studentGroupRepository.save(studentGroup);
    }

    @Override
    public List<StudentGroup> getAllStudentGroups() {
        return studentGroupRepository.findAll();
    }

    @Override
    public Optional<StudentGroup> getStudentGroupById(Long id) {
        return studentGroupRepository.findById(id);
    }

    @Override
    public Optional<StudentGroup> putStudentGroupById(Long id, StudentGroup updatedStudentGroup) {
        Optional<StudentGroup> existingStudentGroup = studentGroupRepository.findById(id);
        if (existingStudentGroup.isPresent()) {
            StudentGroup studentGroupToUpdate = existingStudentGroup.get();
            if (updatedStudentGroup.getGrade() != null) {
                studentGroupToUpdate.setGrade(updatedStudentGroup.getGrade());
            }
            studentGroupRepository.save(studentGroupToUpdate);
        }
        return existingStudentGroup;
    }

    @Override
    public void deleteStudentGroupById(Long id) {
        studentGroupRepository.deleteById(id);
    }
}
