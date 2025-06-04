package kyrs.isis3.service;

import kyrs.isis3.exceptions.LoginAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import kyrs.isis3.model.User;
import kyrs.isis3.model.UserAuthority;
import kyrs.isis3.model.UserRole;
import kyrs.isis3.repository.UserRepository;
import kyrs.isis3.repository.UserRoleRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registration(String login, String password, String surname, String name, String middle_name ) {
        if (userRepository.findByLogin(login).isEmpty()) {
            User user = userRepository.save(
                    new User()
                            .setIdUser(null)
                            .setLogin(login)
                            .setPassword(passwordEncoder.encode(password))
                            .setSurname(surname)
                            .setName(name)
                            .setMiddle_name(middle_name)

            );
            userRoleRepository.save(new UserRole(null, UserAuthority.USER, user));
        } else {
            throw new LoginAlreadyExistsException();
        }
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return userRepository.findByLogin(login).orElseThrow(() -> new UsernameNotFoundException(login));
    }
    @Override
    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }
}