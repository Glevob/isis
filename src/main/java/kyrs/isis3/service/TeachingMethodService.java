package kyrs.isis3.service;

import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;

import java.util.List;
import java.util.Optional;

public interface TeachingMethodService {
    void addTeachingMethod(TeachingMethod teachingMethod);
    List<TeachingMethod> getAllTeachingMethods();
    Optional<TeachingMethod> getTeachingMethodById(Long id);
    Optional<TeachingMethod> putTeachingMethodById(Long id, TeachingMethod updatedTeachingMethod);
    void deleteTeachingMethodById(Long id);
}
