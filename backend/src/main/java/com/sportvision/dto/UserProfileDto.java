package com.sportvision.dto;

import com.sportvision.entity.Role;
import com.sportvision.entity.User;

import java.time.LocalDateTime;

public class UserProfileDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private Role role;
    private LocalDateTime createdAt;

    public static UserProfileDto from(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.fullName = user.getFullName();
        dto.phone = user.getPhone();
        dto.role = user.getRole();
        dto.createdAt = user.getCreatedAt();
        if (user.getFirstName() != null) {
            dto.firstName = user.getFirstName();
            dto.lastName = user.getLastName();
        } else {
            // Comptes créés avant l'ajout prénom/nom : on découpe le nom complet.
            String full = user.getFullName() == null ? "" : user.getFullName().trim();
            int idx = full.indexOf(' ');
            dto.firstName = idx < 0 ? full : full.substring(0, idx);
            dto.lastName = idx < 0 ? "" : full.substring(idx + 1);
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
