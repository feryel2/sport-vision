package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import com.sportvision.training.entity.ExerciseOptions.*;
@Entity
@Table(name="training_prescriptions", uniqueConstraints=@UniqueConstraint(columnNames={"session_id","position"}))
public class TrainingPrescription {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="session_id", updatable=false) private TrainingProgramSession session;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="exercise_id", updatable=false) private TrainingExercise exercise;
    @Column(nullable=false, updatable=false, length=120) private String exerciseName;
    @Enumerated(EnumType.STRING) @Column(nullable=false, updatable=false, length=30) private Equipment equipment;
    @Enumerated(EnumType.STRING) @Column(nullable=false, updatable=false, length=30) private MeasurementType measurementType;
    @Column(nullable=false, updatable=false) private int position;
    @Column(nullable=false, updatable=false) private int sets;
    @Column(updatable=false) private Integer repetitions;
    @Column(updatable=false) private Integer durationSeconds;
    @Column(nullable=false, updatable=false) private int restSeconds;
    @Column(updatable=false, precision=7, scale=2) private BigDecimal targetLoadKg;
    protected TrainingPrescription() {}
    public TrainingPrescription(TrainingProgramSession session, TrainingExercise exercise, String exerciseName, Equipment equipment,
        MeasurementType measurementType, int position, int sets, Integer repetitions, Integer durationSeconds, int restSeconds, BigDecimal targetLoadKg) {
        this.session=session;this.exercise=exercise;this.exerciseName=exerciseName;this.equipment=equipment;this.measurementType=measurementType;
        this.position=position;this.sets=sets;this.repetitions=repetitions;this.durationSeconds=durationSeconds;this.restSeconds=restSeconds;this.targetLoadKg=targetLoadKg;
    }
    public Long getId(){return id;} public TrainingExercise getExercise(){return exercise;} public String getExerciseName(){return exerciseName;}
    public Equipment getEquipment(){return equipment;} public MeasurementType getMeasurementType(){return measurementType;}
    public int getPosition(){return position;} public int getSets(){return sets;} public Integer getRepetitions(){return repetitions;}
    public Integer getDurationSeconds(){return durationSeconds;} public int getRestSeconds(){return restSeconds;} public BigDecimal getTargetLoadKg(){return targetLoadKg;}
}
