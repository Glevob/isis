package kyrs.isis3.service;

import kyrs.isis3.model.Student;
import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;
import kyrs.isis3.repository.StudentGroupRepository;
import kyrs.isis3.repository.StudentRepository;
import kyrs.isis3.repository.TeachingMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class StudentServiceImpl implements StudentService{
//    private final ExceptionHandler exceptionHandler;
    private final StudentRepository studentRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final TeachingMethodRepository teachingMethodRepository;

    @Override
    public void addStudent(Student student) {
        StudentGroup studentGroup = student.getStudentGroup();
        TeachingMethod teachingMethod = student.getTeachingMethod();

        if (studentGroup != null && studentGroup.getIdStudentGroup() != null) {
            studentGroup = studentGroupRepository.findById(studentGroup.getIdStudentGroup()).orElse(null);
        }
        if (teachingMethod != null && teachingMethod.getIdTeachingMethod() != null) {
            teachingMethod = teachingMethodRepository.findById(teachingMethod.getIdTeachingMethod()).orElse(null);
        }

        student.setStudentGroup(studentGroup);
        student.setTeachingMethod(teachingMethod);

        studentRepository.save(student);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    public Optional<Student> putStudentById(Long id, Student updatedStudent) {
        Optional<Student> existingStudent = studentRepository.findById(id);
        if(existingStudent.isPresent()){

            Student studentToUpdate = existingStudent.get();
            if(updatedStudent.getStudentGroup() != null && updatedStudent.getStudentGroup().getIdStudentGroup() != null) {
                StudentGroup studentGroup = studentGroupRepository.findById(updatedStudent.getStudentGroup().getIdStudentGroup()).orElse(null);
                studentToUpdate.setStudentGroup(studentGroup);
            }
            if(updatedStudent.getTeachingMethod() != null && updatedStudent.getTeachingMethod().getIdTeachingMethod() != null) {
                TeachingMethod teachingMethod = teachingMethodRepository.findById(updatedStudent.getTeachingMethod().getIdTeachingMethod()).orElse(null);
                studentToUpdate.setTeachingMethod(teachingMethod);
            }

            if(updatedStudent.getFullName() != null) {
                studentToUpdate.setFullName(updatedStudent.getFullName());
            }

            studentRepository.save(studentToUpdate);
        }
        return existingStudent;
    }

    @Override
    public void deleteStudentById(Long id) {
        studentRepository.deleteById(id);
    }
}
