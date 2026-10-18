package com.sportvision.training.dto;

import java.time.Instant;
import com.sportvision.training.entity.TrainingExercise;
import com.sportvision.training.entity.ExerciseOptions.*;

public record ExerciseOutput(Long id, String name, String description, String instructions,
    MuscleGroup muscleGroup, Equipment equipment, Difficulty difficulty, MeasurementType measurementType,
    boolean archived, Instant createdAt, Instant updatedAt) {
    public static ExerciseOutput from(TrainingExercise e) {
        return new ExerciseOutput(e.getId(), e.getName(), e.getDescription(), e.getInstructions(),
            e.getMuscleGroup(), e.getEquipment(), e.getDifficulty(), e.getMeasurementType(),
            e.isArchived(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
