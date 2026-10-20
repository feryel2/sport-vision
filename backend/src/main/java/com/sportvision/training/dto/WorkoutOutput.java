package com.sportvision.training.dto;
import com.sportvision.training.entity.*;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.entity.WorkoutStatuses.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public record WorkoutOutput(Long id,Long programId,Long programVersionId,int programRevision,Long sourceSessionId,
    String programName,String programDescription,String programObjective,String sessionName,int sessionPosition,
    WorkoutStatus status,Instant startedAt,Instant endedAt,long elapsedSeconds,String comment,
    int totalSets,int doneSets,int skippedSets,int pendingSets,List<Exercise> exercises) {
    public record Exercise(Long id,Long sourceExerciseId,Long sourcePrescriptionId,int position,String name,String description,String instructions,
        Equipment equipment,String equipmentLabel,MeasurementType measurementType,String measurementLabel,int plannedSets,
        Integer targetRepetitions,Integer targetDurationSeconds,int restSeconds,BigDecimal targetLoadKg,List<Set> sets){
        static Exercise from(TrainingWorkoutExercise e){return new Exercise(e.getId(),e.getSourceExerciseId(),e.getSourcePrescriptionId(),e.getPosition(),e.getName(),e.getDescription(),e.getInstructions(),e.getEquipment(),e.getEquipmentLabel(),e.getMeasurementType(),e.getMeasurementLabel(),e.getPlannedSets(),e.getTargetRepetitions(),e.getTargetDurationSeconds(),e.getRestSeconds(),e.getTargetLoadKg(),e.getSets().stream().map(Set::from).toList());}
    }
    public record Set(Long id,int position,SetStatus status,Integer repetitions,Integer durationSeconds,BigDecimal loadKg){
        static Set from(TrainingWorkoutSet s){return new Set(s.getId(),s.getPosition(),s.getStatus(),s.getRepetitions(),s.getDurationSeconds(),s.getLoadKg());}
    }
    public record Summary(Long id,String programName,String sessionName,int programRevision,WorkoutStatus status,Instant startedAt,Instant endedAt,long elapsedSeconds,int totalSets,int doneSets,int skippedSets){
        public static Summary from(TrainingWorkout w){return new Summary(w.getId(),w.getProgramName(),w.getSessionName(),w.getProgramRevision(),w.getStatus(),w.getStartedAt(),w.getEndedAt(),elapsed(w),w.getTotalSets(),w.getDoneSets(),w.getSkippedSets());}
    }
    private static long elapsed(TrainingWorkout w){return Math.max(0,Duration.between(w.getStartedAt(),w.getEndedAt()==null?Instant.now():w.getEndedAt()).getSeconds());}
    public static WorkoutOutput from(TrainingWorkout w){return new WorkoutOutput(w.getId(),w.getProgramId(),w.getProgramVersionId(),w.getProgramRevision(),w.getSourceSessionId(),w.getProgramName(),w.getProgramDescription(),w.getProgramObjective(),w.getSessionName(),w.getSessionPosition(),w.getStatus(),w.getStartedAt(),w.getEndedAt(),elapsed(w),w.getComment(),w.getTotalSets(),w.getDoneSets(),w.getSkippedSets(),w.getTotalSets()-w.getDoneSets()-w.getSkippedSets(),w.getExercises().stream().map(Exercise::from).toList());}
}
