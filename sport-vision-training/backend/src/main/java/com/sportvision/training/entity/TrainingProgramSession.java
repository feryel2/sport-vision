package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.util.*;
@Entity
@Table(name="training_program_sessions", uniqueConstraints=@UniqueConstraint(columnNames={"version_id","position"}))
public class TrainingProgramSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="version_id", updatable=false) private TrainingProgramVersion version;
    @Column(nullable=false, updatable=false, length=120) private String name;
    @Column(nullable=false, updatable=false) private int position;
    @OneToMany(mappedBy="session", cascade=CascadeType.PERSIST) @OrderBy("position ASC")
    private List<TrainingPrescription> prescriptions=new ArrayList<>();
    protected TrainingProgramSession() {}
    public TrainingProgramSession(TrainingProgramVersion version, String name, int position){this.version=version; this.name=name; this.position=position;}
    public void addPrescription(TrainingPrescription prescription){prescriptions.add(prescription);}
    public Long getId(){return id;} public String getName(){return name;} public int getPosition(){return position;}
    public List<TrainingPrescription> getPrescriptions(){return Collections.unmodifiableList(prescriptions);}
}
