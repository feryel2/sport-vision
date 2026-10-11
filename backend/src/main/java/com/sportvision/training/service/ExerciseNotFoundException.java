package com.sportvision.training.service;

public class ExerciseNotFoundException extends RuntimeException {
    public ExerciseNotFoundException() { super("Exercice introuvable."); }
}
