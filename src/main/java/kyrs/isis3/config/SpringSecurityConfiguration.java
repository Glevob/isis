package kyrs.isis3.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;

@Slf4j
@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/student/add").hasAuthority("ADMIN")
//                        .requestMatchers("/studentGroup/add").hasAuthority("ADMIN")
//                        .requestMatchers("/teachingMethod/add").hasAuthority("ADMIN")
//                        .requestMatchers("/grade/add").hasAuthority("ADMIN")
//
//                        .requestMatchers("/student/edit").hasAuthority("ADMIN")
//                        .requestMatchers("/studentGroup/edit").hasAuthority("ADMIN")
//                        .requestMatchers("/teachingMethod/edit").hasAuthority("ADMIN")
//                        .requestMatchers("/grade/edit").hasAuthority("ADMIN")
//
//                        .requestMatchers("/student/delete").hasAuthority("ADMIN")
//                        .requestMatchers("/studentGroup/delete").hasAuthority("ADMIN")
//                        .requestMatchers("/teachingMethod/delete").hasAuthority("ADMIN")
//                        .requestMatchers("/grade/delete").hasAuthority("ADMIN")

                                .requestMatchers("/student/*").hasAuthority("ADMIN")
                                .requestMatchers("/studentGroup/*").hasAuthority("ADMIN")
                                .requestMatchers("/teachingMethod/*").hasAuthority("ADMIN")
                                .requestMatchers("/grade/*").hasAuthority("ADMIN")

//                        .requestMatchers("/st").hasAuthority("ADMIN")


//                        .requestMatchers("/lk", "/lk/**").authenticated()
                        .requestMatchers("/login", "/registration", "/home", "/anon").permitAll()
//                        .requestMatchers("/resume/add").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler((request, response, authentication) -> {
                            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                response.sendRedirect("/home");
                            } else {
                                response.sendRedirect("/home");
                            }
                        })
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}