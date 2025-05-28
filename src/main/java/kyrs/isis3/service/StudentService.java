package kyrs.isis3.service;

import kyrs.isis3.model.Student;
import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;

import java.util.List;
import java.util.Optional;

public interface StudentService {
    void addStudent(Student student);

    List<Student> getAllStudents();

    Optional<Student> getStudentById(Long id);

    Optional<Student> putStudentById(Long id, Student updatedStudent);

    void deleteStudentById(Long id);
    List<Student> getStudentsByGroupId(Long groupId);
    List<Student> filterStudents(Long groupId, Long methodId);
    List<StudentGroup> getAllGroups();
    List<TeachingMethod> getAllMethods();
}
