package com.sportvision.dto;

import com.sportvision.entity.Coach;
import java.util.List;

public class CoachMatchingResult {
    private Coach coach;
    private int matchScore; // 0 à 100%
    private String compatibilityBadge; // "Match Parfait (98%)", etc.
    private List<String> matchReasons;

    public CoachMatchingResult() {}

    public CoachMatchingResult(Coach coach, int matchScore, String compatibilityBadge, List<String> matchReasons) {
        this.coach = coach;
        this.matchScore = matchScore;
        this.compatibilityBadge = compatibilityBadge;
        this.matchReasons = matchReasons;
    }

    public Coach getCoach() {
        return coach;
    }

    public void setCoach(Coach coach) {
        this.coach = coach;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getCompatibilityBadge() {
        return compatibilityBadge;
    }

    public void setCompatibilityBadge(String compatibilityBadge) {
        this.compatibilityBadge = compatibilityBadge;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons;
    }
}
