package kyrs.isis3.repository;

import kyrs.isis3.model.TeachingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface TeachingMethodRepository extends JpaRepository<TeachingMethod, Long>, PagingAndSortingRepository<TeachingMethod, Long> {
    Optional<TeachingMethod> findByNameMethod(String nameMethod);

    boolean existsByNameMethod(String nameMethod);
}

//public interface TeachingMethodRepository extends JpaRepository<TeachingMethod, Long> {
//    Optional<TeachingMethod> findByNameMethod(String name);
//}
//
//public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
//    Optional<StudentGroup> findByNameGroup(String name);
//}
//
//public interface StudentRepository extends JpaRepository<Student, Long> {
//    Optional<Student> findByFullNameAndStudentGroup(String fullName, StudentGroup group);
//}