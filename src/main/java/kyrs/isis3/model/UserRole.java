package kyrs.isis3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table(name = "user_roles")
@Entity(name = "user_roles")
@AllArgsConstructor
@NoArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(generator = "id_user_role_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "id_user_role_seq", sequenceName = "id_user_role_seq", allocationSize = 1)
    private Long idUserRole;

    @Enumerated
    private UserAuthority userAuthority;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_user")
    private User user;
}

