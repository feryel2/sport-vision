package com.sportvision.security;

import com.sportvision.entity.User;
import com.sportvision.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Volontairement pas un @Component : il est ajouté uniquement dans la chaîne Spring Security.
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String CLAIMS_ATTRIBUTE = "jwtClaims";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.validate(header.substring(7)).ifPresent(claims -> authenticate(request, claims));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Claims claims) {
        // Le compte doit toujours exister et être actif ; le rôle est relu en base
        User user = userRepository.findByEmail(claims.getSubject()).filter(User::isActive).orElse(null);
        if (user == null) {
            return;
        }
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority(user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.setAttribute(CLAIMS_ATTRIBUTE, claims);
    }
}
