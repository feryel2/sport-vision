package com.sportvision.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Le prénom est requis")
    @Size(max = 60, message = "Le prénom est trop long")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    @Size(max = 60, message = "Le nom est trop long")
    private String lastName;

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'adresse email est trop longue")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;

    @Pattern(regexp = "^$|^[+0-9 ().-]{6,20}$", message = "Numéro de téléphone invalide")
    private String phone;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
