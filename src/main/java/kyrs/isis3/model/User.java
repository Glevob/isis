package kyrs.isis3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.FetchType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "users")
@Table(name = "users")
@Getter
@Setter
@ToString
public class User implements UserDetails {

    @Id
    @SequenceGenerator(sequenceName = "id_user_seq", name = "id_user_seq", allocationSize = 1)
    @GeneratedValue(generator = "id_user_seq", strategy = GenerationType.SEQUENCE)
    private Long idUser;


    private String login;

    @JsonIgnore
    private String password;

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserRole> userRoles;

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userRoles.stream().map(UserRole::getUserAuthority).collect(Collectors.toList());
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return login;
    }

    @Column(name="surname")
    private String surname;

    @Column(name="name")
    private String name;

    @Column(name="middle_name")
    private String middle_name;

}

