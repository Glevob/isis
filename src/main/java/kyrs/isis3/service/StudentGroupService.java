package kyrs.isis3.service;

import jakarta.transaction.Transactional;
import kyrs.isis3.model.StudentGroup;

import java.util.List;
import java.util.Optional;

public interface StudentGroupService {
    void addStudentGroup(StudentGroup studentGroup);
    List<StudentGroup> getAllStudentGroups();
    Optional<StudentGroup> getStudentGroupById(Long id);
    Optional<StudentGroup> putStudentGroupById(Long id, StudentGroup updatedStudentGroup);
    void deleteStudentGroupById(Long id);
    List<StudentGroup> getGroupsByTeachingMethodId(Long methodId);
    @Transactional
    void deleteGroupWithStudents(Long id);

}
