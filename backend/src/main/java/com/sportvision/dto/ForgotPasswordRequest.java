package com.sportvision.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "Format d'email invalide")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
