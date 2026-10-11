package com.sportvision.training.dto;
import com.sportvision.training.entity.ExerciseOptions.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Set;

public final class TrainingAlternativeDto {
    private TrainingAlternativeDto() {}
    public enum Motive { EQUIPMENT_UNAVAILABLE, PERSONAL_PREFERENCE }
    public record Request(@NotNull @Min(1) Integer expectedRevision,@NotNull @Positive Long prescriptionId,
        @NotNull Motive motive,@NotNull Difficulty level,@NotNull @Size(min=1,max=8) Set<@NotNull Equipment> equipment) {}
    public record Selection(@NotNull @Positive Long exerciseId,@NotBlank @Size(max=600) String reason) {}
    public record Preview(Request request,ProgramOutput.Prescription current,TrainingAiDto.CatalogExercise alternative,String reason) {}
    public record Confirmation(@NotNull @Valid Request request,@NotNull @Positive Long exerciseId) {}
}
