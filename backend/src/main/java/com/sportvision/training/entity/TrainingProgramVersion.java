package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity
@Table(name="training_program_versions", uniqueConstraints=@UniqueConstraint(columnNames={"program_id","revision"}))
public class TrainingProgramVersion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="program_id", updatable=false) private TrainingProgram program;
    @Column(nullable=false, updatable=false) private int revision;
    @Column(nullable=false, updatable=false, length=120) private String name;
    @Column(nullable=false, updatable=false, length=2000) private String description;
    @Column(nullable=false, updatable=false, length=500) private String objective;
    @Column(nullable=false, updatable=false) private boolean archived;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    @OneToMany(mappedBy="version", cascade=CascadeType.PERSIST) @OrderBy("position ASC")
    private List<TrainingProgramSession> sessions=new ArrayList<>();
    protected TrainingProgramVersion() {}
    public TrainingProgramVersion(TrainingProgram program) {
        this.program=program; revision=program.getCurrentRevision(); name=program.getName();
        description=program.getDescription(); objective=program.getObjective(); archived=program.isArchived(); createdAt=program.getUpdatedAt();
    }
    public void addSession(TrainingProgramSession session){sessions.add(session);}
    public Long getId(){return id;} public int getRevision(){return revision;}
    public String getName(){return name;} public String getDescription(){return description;}
    public String getObjective(){return objective;} public boolean isArchived(){return archived;}
    public Instant getCreatedAt(){return createdAt;} public List<TrainingProgramSession> getSessions(){return Collections.unmodifiableList(sessions);}
}
