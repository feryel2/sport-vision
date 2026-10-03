package com.sportvision.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String category; // ex: Protéines, Glucides complexes, Lipides sains, Compléments

    private Double calories; // kcal pour 100g

    private Double proteins; // grammes

    private Double carbs; // glucides

    private Double fats; // lipides

    @Column(name = "serving_portion")
    private String portion; // ex: 100g, 1 shaker (30g), 1 banane

    @Column(length = 1000)
    private String benefits; // Bienfaits sportifs

    public Food() {}

    public Food(String name, String category, Double calories, Double proteins, Double carbs, Double fats, String portion, String benefits) {
        this.name = name;
        this.category = category;
        this.calories = calories;
        this.proteins = proteins;
        this.carbs = carbs;
        this.fats = fats;
        this.portion = portion;
        this.benefits = benefits;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getCalories() {
        return calories;
    }

    public void setCalories(Double calories) {
        this.calories = calories;
    }

    public Double getProteins() {
        return proteins;
    }

    public void setProteins(Double proteins) {
        this.proteins = proteins;
    }

    public Double getCarbs() {
        return carbs;
    }

    public void setCarbs(Double carbs) {
        this.carbs = carbs;
    }

    public Double getFats() {
        return fats;
    }

    public void setFats(Double fats) {
        this.fats = fats;
    }

    public String getPortion() {
        return portion;
    }

    public void setPortion(String portion) {
        this.portion = portion;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }
}
