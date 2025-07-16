package com.subBike.server.util;

import com.subBike.server.entity.ERole;
import com.subBike.server.entity.User;
import com.subBike.server.mapper.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User(
                    "admin",
                    "admin@subbike.com",
                    passwordEncoder.encode("123456"),
                    ERole.ROLE_ADMIN
            );
            userRepository.save(adminUser);
            System.out.println("Created default admin user.");
        }
    }
} 