package com.sportvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "Le prénom est requis")
    @Size(max = 60, message = "Le prénom est trop long")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    @Size(max = 60, message = "Le nom est trop long")
    private String lastName;

    @Pattern(regexp = "^$|^[+0-9 ().-]{6,20}$", message = "Numéro de téléphone invalide")
    private String phone;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
