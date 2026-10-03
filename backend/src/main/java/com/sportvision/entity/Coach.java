package com.sportvision.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "coaches")
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialty; // ex: Musculation, Prise de masse, Perte de poids, Marathon, Trail, Préparation physique

    @Column(nullable = false)
    private String sport; // Musculation, Running, Natation, Cyclisme, Crossfit

    private String targetLevel; // Débutant, Intermédiaire, Avancé, Tous niveaux

    private String availability; // Matin, Soir, Weekend, Flexible

    private Double rating = 4.9;

    private Double hourlyRate; // €/heure

    @Column(length = 2000)
    private String bio;

    private String imageUrl;

    private String email;

    private String phone;

    public Coach() {}

    public Coach(String fullName, String specialty, String sport, String targetLevel, String availability, Double rating, Double hourlyRate, String bio, String imageUrl, String email, String phone) {
        this.fullName = fullName;
        this.specialty = specialty;
        this.sport = sport;
        this.targetLevel = targetLevel;
        this.availability = availability;
        this.rating = rating;
        this.hourlyRate = hourlyRate;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.email = email;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
