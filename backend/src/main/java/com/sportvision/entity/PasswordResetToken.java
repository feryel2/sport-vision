package com.sportvision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Seul le hash SHA-256 du jeton est stocke ; le jeton en clair n'existe que dans l'e-mail.
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean used = false;

    public PasswordResetToken() {}

    public PasswordResetToken(String tokenHash, User user, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }

    public String getTokenHash() { return tokenHash; }

    public User getUser() { return user; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public boolean isUsed() { return used; }

    public void setUsed(boolean used) { this.used = used; }

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
}
