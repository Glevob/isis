package kyrs.isis3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Entity(name="student_group")
@Table(name="student_group")
@Getter
@Setter
public class StudentGroup {

    @Id
    @Column(name="id_student_group")
    @GeneratedValue(generator = "id_student_group_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name="id_student_group_seq", sequenceName = "id_student_group_seq", initialValue = 1, allocationSize = 1)
    private Long idStudentGroup;

    @Column(name="grade")
    private String grade;

    @JsonIgnore
    @OneToMany(mappedBy = "studentGroup")
    private List<Student> students;

    public StudentGroup(String grade) {
        this.grade = grade;
    }

    public StudentGroup() {}
}
