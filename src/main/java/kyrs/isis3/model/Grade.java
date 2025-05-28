package kyrs.isis3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@Entity(name="grade")
@Table(name="grade")
@ToString
public class Grade {
    @Id
    @Column(name="id_grade")
    @GeneratedValue(generator = "id_grade_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "id_grade_seq", sequenceName = "id_grade_seq", initialValue = 1, allocationSize = 1)
    private Long idGrade;

    @Column(name="value_score")
    private String valueScore;

    @ManyToOne
    @JoinColumn(name="id_student")
    private Student student;

    @Column(name="test_date")
    private LocalDate testDate;

    @Column(name="test_name")
    private String testName;

    public Grade(String valueScore, Student student, LocalDate testDate, String testName) {
        this.valueScore = valueScore;
        this.student = student;
        this.testDate = testDate;
        this.testName = testName;
    }

    public Grade() {}
}
