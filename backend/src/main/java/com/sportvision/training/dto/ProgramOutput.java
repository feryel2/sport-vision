package com.sportvision.training.dto;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import com.sportvision.training.entity.*;
import com.sportvision.training.entity.ExerciseOptions.*;
public record ProgramOutput(Long id, String name, String description, String objective, boolean archived,
    int currentRevision, Instant createdAt, Instant updatedAt, Version version) {
    public record Summary(Long id, String name, String objective, boolean archived, int currentRevision, int sessionCount, Instant updatedAt) {
        public static Summary from(TrainingProgram p){return new Summary(p.getId(),p.getName(),p.getObjective(),p.isArchived(),p.getCurrentRevision(),p.getSessionCount(),p.getUpdatedAt());}
    }
    public record RevisionSummary(int revision,String name,boolean archived,Instant createdAt) {
        public static RevisionSummary from(TrainingProgramVersion v){return new RevisionSummary(v.getRevision(),v.getName(),v.isArchived(),v.getCreatedAt());}
    }
    public record Version(Long id,int revision,String name,String description,String objective,boolean archived,Instant createdAt,List<Session> sessions) {
        public static Version from(TrainingProgramVersion v){return new Version(v.getId(),v.getRevision(),v.getName(),v.getDescription(),v.getObjective(),v.isArchived(),v.getCreatedAt(),v.getSessions().stream().map(Session::from).toList());}
    }
    public record Session(Long id,String name,int position,List<Prescription> prescriptions) {
        static Session from(TrainingProgramSession s){return new Session(s.getId(),s.getName(),s.getPosition(),s.getPrescriptions().stream().map(Prescription::from).toList());}
    }
    public record Prescription(Long id,Long exerciseId,String exerciseName,Equipment equipment,MeasurementType measurementType,
        boolean exerciseArchived,int position,int sets,Integer repetitions,Integer durationSeconds,int restSeconds,BigDecimal targetLoadKg) {
        static Prescription from(TrainingPrescription p){return new Prescription(p.getId(),p.getExercise().getId(),p.getExerciseName(),p.getEquipment(),p.getMeasurementType(),p.getExercise().isArchived(),p.getPosition(),p.getSets(),p.getRepetitions(),p.getDurationSeconds(),p.getRestSeconds(),p.getTargetLoadKg());}
    }
    public static ProgramOutput from(TrainingProgram p,TrainingProgramVersion v){return new ProgramOutput(p.getId(),p.getName(),p.getDescription(),p.getObjective(),p.isArchived(),p.getCurrentRevision(),p.getCreatedAt(),p.getUpdatedAt(),Version.from(v));}
}
