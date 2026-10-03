package com.sportvision.service;

import com.sportvision.dto.AuthResponse;
import com.sportvision.dto.LoginRequest;
import com.sportvision.dto.RegisterRequest;
import com.sportvision.entity.Role;
import com.sportvision.entity.User;
import com.sportvision.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Identifiants incorrects (email introuvable)"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        String token = "SV_TOKEN_" + UUID.randomUUID() + "_" + user.getId();
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un compte existe déjà avec cette adresse email");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                Role.ROLE_USER
        );

        User saved = userRepository.save(user);
        String token = "SV_TOKEN_" + UUID.randomUUID() + "_" + saved.getId();
        return new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole());
    }

    public User createUserWithRole(String email, String rawPassword, String fullName, String phone, Role role) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).get();
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), fullName, phone, role);
        return userRepository.save(user);
    }
}
