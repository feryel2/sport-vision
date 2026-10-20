package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import com.sportvision.training.entity.ExerciseOptions.*;
@Entity @Table(name="training_workout_exercises",uniqueConstraints=@UniqueConstraint(columnNames={"workout_id","position"}))
public class TrainingWorkoutExercise {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="workout_id",updatable=false) private TrainingWorkout workout;
    @Column(nullable=false,updatable=false) private Long sourceExerciseId;
    @Column(nullable=false,updatable=false) private Long sourcePrescriptionId;
    @Column(nullable=false,updatable=false) private int position;
    @Column(nullable=false,updatable=false,length=120) private String name;
    @Column(nullable=false,updatable=false,length=2000) private String description;
    @Column(nullable=false,updatable=false,length=5000) private String instructions;
    @Enumerated(EnumType.STRING) @Column(nullable=false,updatable=false,length=30) private Equipment equipment;
    @Column(nullable=false,updatable=false,length=80) private String equipmentLabel;
    @Enumerated(EnumType.STRING) @Column(nullable=false,updatable=false,length=30) private MeasurementType measurementType;
    @Column(nullable=false,updatable=false,length=40) private String measurementLabel;
    @Column(nullable=false,updatable=false) private int plannedSets;
    @Column(updatable=false) private Integer targetRepetitions;
    @Column(updatable=false) private Integer targetDurationSeconds;
    @Column(nullable=false,updatable=false) private int restSeconds;
    @Column(updatable=false,precision=7,scale=2) private BigDecimal targetLoadKg;
    @OneToMany(mappedBy="exercise",cascade=CascadeType.PERSIST) @OrderBy("position ASC")
    private List<TrainingWorkoutSet> sets=new ArrayList<>();
    protected TrainingWorkoutExercise() {}
    public TrainingWorkoutExercise(TrainingWorkout workout,TrainingPrescription p){
        this.workout=workout;sourceExerciseId=p.getExercise().getId();sourcePrescriptionId=p.getId();position=p.getPosition();
        name=p.getExerciseName();description=p.getExercise().getDescription();instructions=p.getExercise().getInstructions();
        equipment=p.getEquipment();equipmentLabel=equipment.label();measurementType=p.getMeasurementType();measurementLabel=measurementType.label();
        plannedSets=p.getSets();targetRepetitions=p.getRepetitions();targetDurationSeconds=p.getDurationSeconds();restSeconds=p.getRestSeconds();targetLoadKg=p.getTargetLoadKg();
        for(int i=1;i<=plannedSets;i++)sets.add(new TrainingWorkoutSet(this,i));
    }
    public Long getId(){return id;} public Long getSourceExerciseId(){return sourceExerciseId;} public Long getSourcePrescriptionId(){return sourcePrescriptionId;}
    public int getPosition(){return position;} public String getName(){return name;} public String getDescription(){return description;} public String getInstructions(){return instructions;}
    public Equipment getEquipment(){return equipment;} public String getEquipmentLabel(){return equipmentLabel;}
    public MeasurementType getMeasurementType(){return measurementType;} public String getMeasurementLabel(){return measurementLabel;}
    public int getPlannedSets(){return plannedSets;} public Integer getTargetRepetitions(){return targetRepetitions;}
    public Integer getTargetDurationSeconds(){return targetDurationSeconds;} public int getRestSeconds(){return restSeconds;}
    public BigDecimal getTargetLoadKg(){return targetLoadKg;} public List<TrainingWorkoutSet> getSets(){return Collections.unmodifiableList(sets);}
}
