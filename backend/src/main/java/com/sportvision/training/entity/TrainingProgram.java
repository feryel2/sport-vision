package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="training_programs", indexes=@Index(name="training_program_owner_idx", columnList="owner_reference"))
public class TrainingProgram {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="owner_reference", nullable=false, updatable=false, length=200) private String ownerReference;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, length=2000) private String description;
    @Column(nullable=false, length=500) private String objective;
    @Column(nullable=false) private boolean archived;
    @Column(nullable=false) private int currentRevision;
    @Column(nullable=false) private int sessionCount;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected TrainingProgram() {}
    public TrainingProgram(String ownerReference) { this.ownerReference=ownerReference; this.createdAt=Instant.now(); }
    public void advance(String name, String description, String objective, boolean archived, int sessionCount) {
        this.name=name; this.description=description; this.objective=objective; this.archived=archived;
        this.sessionCount=sessionCount; this.currentRevision++; this.updatedAt=Instant.now();
    }
    public Long getId(){return id;} public String getOwnerReference(){return ownerReference;}
    public String getName(){return name;} public String getDescription(){return description;}
    public String getObjective(){return objective;} public boolean isArchived(){return archived;}
    public int getCurrentRevision(){return currentRevision;} public int getSessionCount(){return sessionCount;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
