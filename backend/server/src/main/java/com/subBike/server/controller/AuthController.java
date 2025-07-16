package com.subBike.server.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subBike.server.entity.ERole;
import com.subBike.server.entity.User;
import com.subBike.server.payload.request.LoginRequest;
import com.subBike.server.payload.request.SignupRequest;
import com.subBike.server.payload.response.UserInfoResponse;
import com.subBike.server.payload.response.MessageResponse;
import com.subBike.server.mapper.UserRepository;
import com.subBike.server.security.jwt.JwtUtils;
import com.subBike.server.security.services.UserDetailsImpl;
import com.subBike.server.security.services.VerificationCodeService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    VerificationCodeService verificationCodeService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody SignupRequest emailRequest) {
        // Here we reuse SignupRequest just to get the email, a dedicated DTO would be cleaner
        verificationCodeService.generateAndSendCode(emailRequest.getEmail());
        return ResponseEntity.ok(new MessageResponse("Verification code sent successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String jwt = jwtUtils.generateJwtToken(authentication);

        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(new UserInfoResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Validate the verification code
        boolean isCodeValid = verificationCodeService.validateCode(
            signUpRequest.getEmail(),
            signUpRequest.getVerificationCode()
        );

        if (!isCodeValid) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Invalid or expired verification code!"));
        }

        // Create new user's account
        Set<String> strRoles = signUpRequest.getRole();
        ERole role;

        if (strRoles == null || strRoles.isEmpty()) {
            role = ERole.ROLE_USER;
        } else {
            // Assuming only one role is passed from frontend for simplicity during registration
            String roleStr = strRoles.iterator().next();
            switch (roleStr) {
                case "admin":
                    role = ERole.ROLE_ADMIN;
                    break;
                case "manager":
                    role = ERole.ROLE_MANAGER;
                    break;
                case "subway":
                    role = ERole.ROLE_SUBWAY;
                    break;
                default:
                    role = ERole.ROLE_USER;
            }
        }
        
        User user = new User(signUpRequest.getUsername(),
                             signUpRequest.getEmail(),
                             encoder.encode(signUpRequest.getPassword()),
                             role);

        User savedUser = userRepository.save(user);

        // Invalidate the code after successful registration
        verificationCodeService.invalidateCode(signUpRequest.getEmail());

        // Authenticate and generate JWT for auto-login
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signUpRequest.getUsername(), signUpRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ResponseEntity.ok(new UserInfoResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getAuthorities().iterator().next().getAuthority()));
    }
} 