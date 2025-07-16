package com.subBike.server.controller;

import com.subBike.server.entity.ERole;
import com.subBike.server.entity.User;
import com.subBike.server.entity.dto.UpdateRoleRequest;
import com.subBike.server.entity.dto.UserDto;
import com.subBike.server.payload.response.MessageResponse;
import com.subBike.server.mapper.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(user -> new UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest updateRoleRequest) {
        User userToUpdate = userRepository.findById(id)
                .orElse(null);

        if (userToUpdate == null) {
            return ResponseEntity.notFound().build();
        }

        // Prevent admin from changing their own role
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userToUpdate.getUsername().equals(currentUsername)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Admin cannot change their own role!"));
        }

        try {
            ERole newRole = ERole.valueOf(updateRoleRequest.getRole());
            userToUpdate.setRole(newRole);
            userRepository.save(userToUpdate);
            return ResponseEntity.ok(new MessageResponse("User role updated successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid role specified!"));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User userToDelete = userRepository.findById(id)
                .orElse(null);

        if (userToDelete == null) {
            return ResponseEntity.notFound().build();
        }

        // Prevent admin from deleting themselves
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userToDelete.getUsername().equals(currentUsername)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Admin cannot delete themselves!"));
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully!"));
    }
} 