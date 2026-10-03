package com.sportvision.dto;

public class CoachMatchingRequest {
    private String sport; // Musculation, Running, Fitness, Cyclisme, Crossfit
    private String goal; // Prise de masse, Perte de poids, Force, Marathon, Remise en forme
    private String level; // Débutant, Intermédiaire, Avancé, Tous niveaux
    private String availability; // Matin, Soir, Weekend, Flexible

    public CoachMatchingRequest() {}

    public CoachMatchingRequest(String sport, String goal, String level, String availability) {
        this.sport = sport;
        this.goal = goal;
        this.level = level;
        this.availability = availability;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }
}
