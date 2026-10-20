package com.sportvision.training.dto;
import com.sportvision.training.entity.ExerciseOptions.MeasurementType;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public final class ProgressOutput {
    private ProgressOutput() {}
    public record Period(int days,String timeZone,LocalDate firstDay,LocalDate lastDay,Instant fromInclusive,Instant toExclusive) {}
    public record Indicators(long completedWorkouts,long abandonedWorkouts,long doneSets,long elapsedSeconds) {}
    public record Day(LocalDate date,long completedWorkouts) {}
    public record Point(long workoutId,Instant endedAt,LocalDate date,String sessionName,List<String> historicalNames,
                        long doneSets,Long totalRepetitions,Long effortSeconds,BigDecimal maxLoadKg) {}
    public record Exercise(long exerciseId,MeasurementType measurementType,String name,List<Point> points) {}
    public record Dashboard(Period period,Indicators indicators,List<Day> days,List<Exercise> exercises) {}
}
