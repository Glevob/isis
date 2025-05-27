package kyrs.isis3.service;

import kyrs.isis3.model.Student;
import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;
import kyrs.isis3.repository.StudentGroupRepository;
import kyrs.isis3.repository.TeachingMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class StudentGroupServiceImpl implements StudentGroupService{
    private final StudentGroupRepository studentGroupRepository;
    private final TeachingMethodRepository teachingMethodRepository;

    @Override
    public void addStudentGroup(StudentGroup studentGroup) {
        TeachingMethod teachingMethod = studentGroup.getTeachingMethod();
        if (teachingMethod != null && teachingMethod.getIdTeachingMethod() != null) {
            teachingMethod = teachingMethodRepository.findById(teachingMethod.getIdTeachingMethod()).orElse(null);
        }
        studentGroup.setTeachingMethod(teachingMethod);

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
            if (updatedStudentGroup.getNameGroup() != null) {
                studentGroupToUpdate.setNameGroup(updatedStudentGroup.getNameGroup());
            }

            if(updatedStudentGroup.getTeachingMethod() != null && updatedStudentGroup.getTeachingMethod().getIdTeachingMethod() != null) {
                TeachingMethod teachingMethod = teachingMethodRepository.findById(updatedStudentGroup.getTeachingMethod().getIdTeachingMethod()).orElse(null);
                studentGroupToUpdate.setTeachingMethod(teachingMethod);
            }
            studentGroupRepository.save(studentGroupToUpdate);
        }
        return existingStudentGroup;
    }

    @Override
    public void deleteStudentGroupById(Long id) {
        studentGroupRepository.deleteById(id);
    }

    public List<StudentGroup> getGroupsByTeachingMethodId(Long methodId) {
        return studentGroupRepository.findByTeachingMethodIdTeachingMethod(methodId);
    }
}
