package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="training_schedules",uniqueConstraints=@UniqueConstraint(name="training_schedule_slot",columnNames={"owner_reference","occupied_at"}),
    indexes=@Index(name="training_schedule_dates",columnList="owner_reference,scheduled_at"))
public class TrainingSchedule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @Column(name="owner_reference",nullable=false,updatable=false) private String ownerReference;
    @Column(nullable=false,updatable=false) private long programId;
    @Column(nullable=false,updatable=false) private int programRevision;
    @Column(nullable=false,updatable=false) private long sessionId;
    @Column(nullable=false,updatable=false,length=120) private String programName;
    @Column(nullable=false,updatable=false,length=120) private String sessionName;
    @Column(name="scheduled_at",nullable=false) private Instant scheduledAt;
    // Null libère le créneau après annulation ; la date d'origine reste consultable.
    @Column(name="occupied_at") private Instant occupiedAt;
    @Column(nullable=false) private boolean cancelled;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="workout_id",unique=true) private TrainingWorkout workout;
    protected TrainingSchedule() {}
    public TrainingSchedule(String owner,long program,int revision,long session,String programName,String sessionName,Instant time){
        this.ownerReference=owner;this.programId=program;this.programRevision=revision;this.sessionId=session;
        this.programName=programName;this.sessionName=sessionName;reschedule(time);
    }
    public void reschedule(Instant time){scheduledAt=time;occupiedAt=time;}
    public void cancel(){cancelled=true;occupiedAt=null;}
    public void attach(TrainingWorkout workout){this.workout=workout;}
    public Long getId(){return id;} public long getVersion(){return version;}
    public long getProgramId(){return programId;} public int getProgramRevision(){return programRevision;}
    public long getSessionId(){return sessionId;} public String getProgramName(){return programName;}
    public String getSessionName(){return sessionName;} public Instant getScheduledAt(){return scheduledAt;}
    public boolean isCancelled(){return cancelled;} public TrainingWorkout getWorkout(){return workout;}
}
