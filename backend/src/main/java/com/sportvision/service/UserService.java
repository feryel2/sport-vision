package com.sportvision.service;

import com.sportvision.dto.AuthResponse;
import com.sportvision.dto.ChangePasswordRequest;
import com.sportvision.dto.LoginRequest;
import com.sportvision.dto.RegisterRequest;
import com.sportvision.dto.UpdateProfileRequest;
import com.sportvision.entity.PasswordResetToken;
import com.sportvision.entity.Role;
import com.sportvision.entity.User;
import com.sportvision.repository.PasswordResetTokenRepository;
import com.sportvision.repository.UserRepository;
import com.sportvision.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    // Hash bcrypt factice pour égaliser le temps de réponse quand l'email est inconnu
    private final String dummyHash;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final String frontendUrl;
    private final long resetExpirationMinutes;

    public UserService(UserRepository userRepository,
                       PasswordResetTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       MailService mailService,
                       @Value("${app.frontend-url}") String frontendUrl,
                       @Value("${app.password-reset.expiration-minutes}") long resetExpirationMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
        this.resetExpirationMinutes = resetExpirationMinutes;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
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
        User user = userRepository.findByEmail(normalize(request.getEmail())).orElse(null);

        // Message identique pour email inconnu / mot de passe erroné (anti-énumération)
        if (user == null) {
            passwordEncoder.matches(request.getPassword(), dummyHash);
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }
        if (!user.isActive()) {
            throw new DisabledException("Ce compte a été désactivé");
        }
        return buildAuthResponse(user);
    }

    public void deactivateAccount(String email, String password) {
        User user = getCurrentUser(email);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }
        if (user.getRole() == Role.ROLE_ADMIN && userRepository.countByRoleAndActiveTrue(Role.ROLE_ADMIN) <= 1) {
            throw new IllegalArgumentException("Impossible de désactiver le dernier compte administrateur");
        }
        user.setActive(false);
        userRepository.save(user);
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cette adresse email");
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String phone = request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim();

        User user = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                firstName + " " + lastName,
                phone,
                Role.ROLE_USER
        );
        user.setFirstName(firstName);
        user.setLastName(lastName);

        return buildAuthResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable"));
    }

    public User updateProfile(String email, UpdateProfileRequest request) {
        User user = getCurrentUser(email);
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setFullName(firstName + " " + lastName);
        user.setPhone(request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim());
        return userRepository.save(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getCurrentUser(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /** Génère un jeton à usage unique et envoie le lien par e-mail. Lève une erreur si le compte n'existe pas. */
    public void requestPasswordReset(String rawEmail) {
        User user = userRepository.findByEmail(normalize(rawEmail))
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte n'est associé à cette adresse email"));

        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(new PasswordResetToken(
                hash(token), user, LocalDateTime.now().plusMinutes(resetExpirationMinutes)));

        String link = frontendUrl + "/reset-password?token=" + token;
        String firstName = user.getFirstName() != null ? user.getFirstName() : user.getFullName();
        mailService.sendPasswordReset(user.getEmail(), firstName, link, resetExpirationMinutes);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken stored = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> !t.isUsed() && !t.isExpired())
                .orElseThrow(() -> new IllegalArgumentException("Lien de réinitialisation invalide ou expiré"));

        User user = stored.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        stored.setUsed(true);
        tokenRepository.save(stored);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public User createUserWithRole(String email, String rawPassword, String fullName, String phone, Role role) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).get();
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), fullName, phone, role);
        return userRepository.save(user);
    }
}
