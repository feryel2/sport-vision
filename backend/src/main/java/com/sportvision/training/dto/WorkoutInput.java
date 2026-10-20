package com.sportvision.training.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class WorkoutInput {
    private WorkoutInput() {}
    public record Start(@NotNull @Positive Long programId,@NotNull @Positive Integer revision,@NotNull @Positive Long sessionId) {}
    public record SetPerformance(
        @Min(value=1,message="Les répétitions doivent être positives.") @Max(value=10000,message="10 000 répétitions maximum.") Integer repetitions,
        @Min(value=1,message="La durée doit être positive.") @Max(value=86400,message="86 400 secondes maximum.") Integer durationSeconds,
        @DecimalMin(value="0",message="La charge doit être positive ou nulle.") @DecimalMax(value="10000",message="10 000 kg maximum.") @Digits(integer=5,fraction=2,message="Deux décimales maximum pour la charge.") BigDecimal loadKg
    ) {}
    public record Complete(boolean confirmSkipPending,@Size(max=2000,message="Le commentaire est limité à 2 000 caractères.") String comment) {}
    public record Abandon(@Size(max=2000,message="Le commentaire est limité à 2 000 caractères.") String comment) {}
}
