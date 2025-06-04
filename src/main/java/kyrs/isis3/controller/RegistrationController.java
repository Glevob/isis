package kyrs.isis3.controller;

import kyrs.isis3.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/registration")
public class RegistrationController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Void> registration(
            @RequestParam("login") String login,
            @RequestParam("password") String password,
            @RequestParam("surname") String surname,
            @RequestParam("name") String name,
            @RequestParam("middle_name") String middle_name
    ) {
        userService.registration(login, password, surname, name, middle_name);
        return ResponseEntity.ok().build();
    }
}
