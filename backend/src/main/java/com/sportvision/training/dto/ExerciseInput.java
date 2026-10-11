package com.sportvision.training.dto;

import jakarta.validation.constraints.*;
import com.sportvision.training.entity.ExerciseOptions.*;

public record ExerciseInput(
    @NotBlank(message = "Le nom est obligatoire.") @Size(max = 120, message = "120 caractères maximum.") String name,
    @NotBlank(message = "La description est obligatoire.") @Size(max = 2000, message = "2000 caractères maximum.") String description,
    @NotBlank(message = "Les consignes sont obligatoires.") @Size(max = 5000, message = "5000 caractères maximum.") String instructions,
    @NotNull(message = "Le groupe musculaire est obligatoire.") MuscleGroup muscleGroup,
    @NotNull(message = "Le matériel est obligatoire.") Equipment equipment,
    @NotNull(message = "La difficulté est obligatoire.") Difficulty difficulty,
    @NotNull(message = "Le type de mesure est obligatoire.") MeasurementType measurementType
) {}
