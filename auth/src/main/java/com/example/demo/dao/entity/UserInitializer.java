package com.example.demo.dao.entity;

import com.example.demo.dao.repository.UserRepository;
import com.example.demo.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private  AuthenticationService service;

    private final PasswordEncoder passwordEncoder;

    public UserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.findByEmail("admin@example.com").isPresent()) {
            User admin = User.builder()
                    .firstname("Admin")
                    .lastname("User")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("adminpassword"))
                    .enabled(true)
                    .accountLocked(false)
                    .roles(List.of(Role.ADMIN))
                    .tenantid(service.generateTenant())
                    .build();

            userRepository.save(admin);
            System.out.println("Admin user created");
        }
    }
}
