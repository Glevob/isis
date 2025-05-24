package kyrs.isis3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Entity(name="teaching_method")
@Table(name="teaching_method")
@Getter
@Setter
public class TeachingMethod {

    @Id
    @Column(name="id_teaching_method")
    @GeneratedValue(generator = "id_teaching_method_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name="id_teaching_method_seq", sequenceName = "id_teaching_method_seq", initialValue = 1, allocationSize = 1)
    private Long idTeachingMethod;

    @Column(name="name_method")
    private String nameMethod;

    @JsonIgnore
    @OneToMany(mappedBy = "teachingMethod")
    private List<Student> students;

    public TeachingMethod(String nameMethod) {
        this.nameMethod = nameMethod;
    }

    public TeachingMethod() {}
}
