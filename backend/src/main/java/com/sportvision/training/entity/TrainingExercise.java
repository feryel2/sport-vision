package com.sportvision.training.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.sportvision.training.entity.ExerciseOptions.*;

@Entity
@Table(name = "training_exercises")
public class TrainingExercise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 2000) private String description;
    @Column(nullable = false, length = 5000) private String instructions;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private MuscleGroup muscleGroup;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Equipment equipment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Difficulty difficulty;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private MeasurementType measurementType;
    @Column(nullable = false) private boolean archived = false;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getInstructions() { return instructions; }
    public MuscleGroup getMuscleGroup() { return muscleGroup; }
    public Equipment getEquipment() { return equipment; }
    public Difficulty getDifficulty() { return difficulty; }
    public MeasurementType getMeasurementType() { return measurementType; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void edit(String name, String description, String instructions, MuscleGroup muscleGroup,
                     Equipment equipment, Difficulty difficulty, MeasurementType measurementType) {
        this.name = name.strip(); this.description = description.strip(); this.instructions = instructions.strip();
        this.muscleGroup = muscleGroup; this.equipment = equipment;
        this.difficulty = difficulty; this.measurementType = measurementType;
    }
}
