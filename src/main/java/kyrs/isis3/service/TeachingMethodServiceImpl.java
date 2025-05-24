package kyrs.isis3.service;

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
public class TeachingMethodServiceImpl implements TeachingMethodService {
    private final TeachingMethodRepository teachingMethodRepository;

    @Override
    public void addTeachingMethod(TeachingMethod teachingMethod) {
        teachingMethodRepository.save(teachingMethod);
    }

    @Override
    public List<TeachingMethod> getAllTeachingMethods() {
        return teachingMethodRepository.findAll();
    }

    @Override
    public Optional<TeachingMethod> getTeachingMethodById(Long id) {
        return teachingMethodRepository.findById(id);
    }

    @Override
    public Optional<TeachingMethod> putTeachingMethodById(Long id, TeachingMethod updatedTeachingMethod) {
        Optional<TeachingMethod> existingTeachingMethod = teachingMethodRepository.findById(id);
        if (existingTeachingMethod.isPresent()) {
            TeachingMethod teachingMethodToUpdate = existingTeachingMethod.get();
            if (updatedTeachingMethod.getNameMethod() != null) {
                teachingMethodToUpdate.setNameMethod(updatedTeachingMethod.getNameMethod());
            }
            teachingMethodRepository.save(teachingMethodToUpdate);
        }
        return existingTeachingMethod;
    }

    @Override
    public void deleteTeachingMethodById(Long id) {
        teachingMethodRepository.deleteById(id);
    }
}
