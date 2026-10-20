package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import com.sportvision.training.entity.WorkoutStatuses.*;
@Entity @Table(name="training_workout_sets",uniqueConstraints=@UniqueConstraint(columnNames={"exercise_id","position"}))
public class TrainingWorkoutSet {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="exercise_id",updatable=false) private TrainingWorkoutExercise exercise;
    @Column(nullable=false,updatable=false) private int position;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SetStatus status=SetStatus.PENDING;
    private Integer repetitions;
    private Integer durationSeconds;
    @Column(precision=7,scale=2) private BigDecimal loadKg;
    protected TrainingWorkoutSet() {}
    public TrainingWorkoutSet(TrainingWorkoutExercise exercise,int position){this.exercise=exercise;this.position=position;}
    public void done(Integer repetitions,Integer durationSeconds,BigDecimal loadKg){this.status=SetStatus.DONE;this.repetitions=repetitions;this.durationSeconds=durationSeconds;this.loadKg=loadKg;}
    public void skip(){status=SetStatus.SKIPPED;repetitions=null;durationSeconds=null;loadKg=null;}
    public Long getId(){return id;} public int getPosition(){return position;} public SetStatus getStatus(){return status;}
    public Integer getRepetitions(){return repetitions;} public Integer getDurationSeconds(){return durationSeconds;} public BigDecimal getLoadKg(){return loadKg;}
}
