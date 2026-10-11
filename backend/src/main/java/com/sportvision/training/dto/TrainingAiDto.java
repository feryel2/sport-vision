package com.sportvision.training.dto;
import com.sportvision.training.entity.ExerciseOptions.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
public final class TrainingAiDto {
    private TrainingAiDto() {}
    // Le métier stocke un objectif textuel ; ces choix structurés fournissent ce texte.
    public enum Goal {
        GENERAL_FITNESS("Forme générale"), STRENGTH("Renforcement musculaire"), ENDURANCE("Endurance"), MOBILITY("Mobilité");
        private final String label;
        Goal(String label){this.label=label;}
        public String label(){return label;}
    }
    public record Preferences(@NotNull Goal objective,@NotNull Difficulty level,
        @NotNull @Min(1) @Max(5) Integer sessionCount,
        @NotNull @Size(min=1,max=8) Set<@NotNull Equipment> equipment) {}
    public record Choice(@NotNull @Positive Long exerciseId,@NotNull MeasurementType measurementType,
        @NotNull @Min(1) @Max(100) Integer sets,
        @Min(1) @Max(10000) Integer repetitions,@Min(1) @Max(86400) Integer durationSeconds,
        @NotNull @Min(0) @Max(86400) Integer restSeconds) {}
    public record Session(@NotBlank @Size(max=120) String name,
        @NotNull @Size(min=3,max=6) List<@NotNull @Valid Choice> exercises) {}
    public record Proposal(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=2000) String description,
        @NotBlank @Size(max=1000) String justification,
        @NotNull @Size(min=1,max=5) List<@NotNull @Valid Session> sessions) {}
    public record CatalogExercise(long id,String name,MuscleGroup muscleGroup,Equipment equipment,MeasurementType measurementType) {}
    public record Preview(Proposal proposal,List<CatalogExercise> catalogue) {}
    public record Save(@NotNull UUID idempotencyKey,@NotNull @Valid Preferences preferences,@NotNull @Valid Proposal proposal) {}
    public record Option(String value,String label) {}
    public record Options(List<Option> objectives,List<Option> levels,List<Option> equipment) {}
}
