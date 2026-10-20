package com.sportvision.dto;

public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$";
    public static final String MESSAGE =
            "Le mot de passe doit contenir 8 à 64 caractères, avec au moins une majuscule, une minuscule, un chiffre et un caractère spécial, sans espace";

    private PasswordPolicy() {}
}
