package com.sportvision.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMs;
    // jti -> date d'expiration des jetons révoqués (déconnexion)
    private final Map<String, Long> revoked = new ConcurrentHashMap<>();

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] bytes;
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.warn("app.jwt.secret absent ou trop court : un secret aléatoire est généré (les sessions sont perdues au redémarrage).");
            bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Retourne les claims si le jeton est valide, signé, non expiré et non révoqué. */
    public Optional<Claims> validate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (revoked.containsKey(claims.getId())) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void revoke(Claims claims) {
        revoked.put(claims.getId(), claims.getExpiration().getTime());
    }

    @Scheduled(fixedRate = 600_000)
    public void purgeExpiredRevocations() {
        long now = System.currentTimeMillis();
        revoked.values().removeIf(exp -> exp < now);
    }
}
