package com.sportvision.training.dto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public final class TrainingAdaptationDto {
    private TrainingAdaptationDto() {}
    public record Values(int sets,Integer repetitions,Integer durationSeconds,int restSeconds,BigDecimal targetLoadKg) {}
    public record Option(String code,String reason,Values after) {}
    public record Metrics(int workouts,int done,int skipped,long actual,long prescribed,int achieved,int loadSamples,BigDecimal minimumLoadKg,BigDecimal averageLoadKg,long difference) {}
    public record Item(int slot,long prescriptionId,String session,String exercise,String measurementType,Values before,Metrics metrics,List<Option> options) {}
    public record Report(int revision,int completedWorkouts,boolean eligible,String message,List<Item> items) {}
    public record Choice(int slot,String option) {}
    public record Selection(List<Choice> adjustments) {}
    public record Change(int slot,String session,String exercise,String reason,Values before,Values after) {}
    public record Preview(UUID proposalId,Report report,List<Change> changes) {}
}
