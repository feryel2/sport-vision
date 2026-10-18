package com.sportvision.training.dto;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record ProgramInput(
    @NotBlank(message="Le nom est obligatoire.") @Size(max=120) String name,
    @NotBlank(message="La description est obligatoire.") @Size(max=2000) String description,
    @NotBlank(message="L’objectif est obligatoire.") @Size(max=500) String objective,
    @NotNull @Size(min=1, max=20, message="Entre 1 et 20 séances sont nécessaires.") List<@NotNull @Valid SessionInput> sessions
) {
    public record SessionInput(@NotBlank(message="Le nom de séance est obligatoire.") @Size(max=120) String name,
        @NotNull @Size(min=1,max=30,message="Entre 1 et 30 exercices par séance.") List<@NotNull @Valid PrescriptionInput> prescriptions) {}
    public record PrescriptionInput(
        @Positive Long existingPrescriptionId,
        @NotNull @Positive Long exerciseId,
        @NotNull @Min(1) @Max(100) Integer sets,
        @Min(1) @Max(10000) Integer repetitions,
        @Min(1) @Max(86400) Integer durationSeconds,
        @NotNull @Min(0) @Max(86400) Integer restSeconds,
        @DecimalMin("0") @DecimalMax("10000") @Digits(integer=5,fraction=2) BigDecimal targetLoadKg
    ) {}
    public record Update(@NotNull @Min(1) Integer expectedRevision, @NotNull @Valid ProgramInput program) {}
    public record RevisionRequest(@NotNull @Min(1) Integer expectedRevision) {}
}
