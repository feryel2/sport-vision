package com.sportvision.dto;

import jakarta.validation.constraints.NotBlank;

public class DeactivateAccountRequest {

    @NotBlank(message = "Le mot de passe est requis pour désactiver le compte")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
