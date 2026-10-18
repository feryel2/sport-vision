package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import com.sportvision.training.entity.WorkoutStatuses.*;
@Entity
@Table(name="training_workouts", uniqueConstraints=@UniqueConstraint(name="training_one_active_workout_per_owner",columnNames="active_owner"), indexes=@Index(name="training_workout_owner_idx",columnList="owner_reference,started_at"))
public class TrainingWorkout {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="owner_reference",nullable=false,updatable=false,length=200) private String ownerReference;
    // NULL for closed workouts: uniqueness is enforced by the database, across concurrent requests.
    @Column(name="active_owner",length=200) private String activeOwner;
    @Column(nullable=false,updatable=false) private Long programId;
    @Column(nullable=false,updatable=false) private Long programVersionId;
    @Column(nullable=false,updatable=false) private int programRevision;
    @Column(nullable=false,updatable=false) private Long sourceSessionId;
    @Column(nullable=false,updatable=false,length=120) private String programName;
    @Column(nullable=false,updatable=false,length=2000) private String programDescription;
    @Column(nullable=false,updatable=false,length=500) private String programObjective;
    @Column(nullable=false,updatable=false,length=120) private String sessionName;
    @Column(nullable=false,updatable=false) private int sessionPosition;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private WorkoutStatus status=WorkoutStatus.IN_PROGRESS;
    @Column(name="started_at",nullable=false,updatable=false) private Instant startedAt;
    private Instant endedAt;
    @Column(length=2000) private String comment;
    @Column(nullable=false) private int totalSets;
    @Column(nullable=false) private int doneSets;
    @Column(nullable=false) private int skippedSets;
    @OneToMany(mappedBy="workout",cascade=CascadeType.PERSIST) @OrderBy("position ASC")
    private List<TrainingWorkoutExercise> exercises=new ArrayList<>();
    protected TrainingWorkout() {}
    public TrainingWorkout(String owner,TrainingProgram program,TrainingProgramVersion version,TrainingProgramSession session){
        ownerReference=owner;activeOwner=owner;programId=program.getId();programVersionId=version.getId();programRevision=version.getRevision();
        sourceSessionId=session.getId();programName=version.getName();programDescription=version.getDescription();programObjective=version.getObjective();
        sessionName=session.getName();sessionPosition=session.getPosition();startedAt=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public void addExercise(TrainingWorkoutExercise exercise){exercises.add(exercise);}
    public void refreshProgress(){
        totalSets=0;doneSets=0;skippedSets=0;
        for(var exercise:exercises)for(var set:exercise.getSets()){
            totalSets++;if(set.getStatus()==SetStatus.DONE)doneSets++;if(set.getStatus()==SetStatus.SKIPPED)skippedSets++;
        }
    }
    public void close(WorkoutStatus status,String comment){this.status=status;this.comment=comment==null?null:comment.strip();endedAt=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);activeOwner=null;}
    public Long getId(){return id;} public Long getProgramId(){return programId;} public Long getProgramVersionId(){return programVersionId;}
    public int getProgramRevision(){return programRevision;} public Long getSourceSessionId(){return sourceSessionId;}
    public String getProgramName(){return programName;} public String getProgramDescription(){return programDescription;}
    public String getProgramObjective(){return programObjective;} public String getSessionName(){return sessionName;} public int getSessionPosition(){return sessionPosition;}
    public WorkoutStatus getStatus(){return status;} public Instant getStartedAt(){return startedAt;} public Instant getEndedAt(){return endedAt;}
    public String getComment(){return comment;} public int getTotalSets(){return totalSets;} public int getDoneSets(){return doneSets;} public int getSkippedSets(){return skippedSets;}
    public List<TrainingWorkoutExercise> getExercises(){return Collections.unmodifiableList(exercises);}
}

