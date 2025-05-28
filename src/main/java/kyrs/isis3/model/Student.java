package kyrs.isis3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@Entity(name="student")
@Table(name="student")
@Getter
@Setter
public class Student {

    @Id
    @Column(name="id_student")
    @GeneratedValue(generator = "id_student_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "id_student_seq", sequenceName = "id_student_seq", initialValue = 1, allocationSize = 1)
    private Long idStudent;

    @Column(name="full_name")
    private String fullName;

    @ManyToOne
    @JoinColumn(name="id_student_group")
    private StudentGroup studentGroup;

    @JsonIgnore
    @OneToMany(mappedBy = "student")
    private List<Grade> grades;


    public Student(String fullName, StudentGroup studentGroup) {
        this.fullName = fullName;
        this.studentGroup = studentGroup;
    }

    public Student() {}

}
