package com.subBike.server.controller;

import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.UpdateEmailRequest;
import com.subBike.server.entity.dto.UpdatePasswordRequest;
import com.subBike.server.payload.response.MessageResponse;
import com.subBike.server.mapper.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @PostMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest updatePasswordRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        if (!encoder.matches(updatePasswordRequest.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Incorrect old password!"));
        }

        user.setPassword(encoder.encode(updatePasswordRequest.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Password updated successfully!"));
    }

    @PostMapping("/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateEmail(@Valid @RequestBody UpdateEmailRequest updateEmailRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // Verify password
        if (!encoder.matches(updateEmailRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Incorrect password!"));
        }

        // Check if new email is already taken
        if (userRepository.existsByEmail(updateEmailRequest.getNewEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        user.setEmail(updateEmailRequest.getNewEmail());
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Email updated successfully!"));
    }
} 