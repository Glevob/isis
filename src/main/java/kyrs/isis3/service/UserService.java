package kyrs.isis3.service;

import kyrs.isis3.model.User;

import java.util.Optional;

public interface UserService {

    Optional<User> findByLogin(String login);

    void registration(String login, String password, String surname, String name, String middle_name );

}