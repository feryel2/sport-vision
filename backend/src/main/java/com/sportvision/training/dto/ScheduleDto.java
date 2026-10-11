package com.sportvision.training.dto;
import jakarta.validation.constraints.*;
import java.time.*;
public final class ScheduleDto {
    private ScheduleDto() {}
    public record Create(@NotNull @Positive Long programId,@NotNull @Positive Integer expectedRevision,
        @NotNull @Positive Long sessionId,@NotNull LocalDateTime scheduledLocal) {}
    public record Update(@NotNull @PositiveOrZero Long expectedVersion,@NotNull LocalDateTime scheduledLocal) {}
    public record Action(@NotNull @PositiveOrZero Long expectedVersion) {}
    public record Output(long id,long version,long programId,int programRevision,long sessionId,String programName,
        String sessionName,Instant scheduledAt,LocalDateTime scheduledLocal,String timeZone,String status,Long workoutId) {}
}
