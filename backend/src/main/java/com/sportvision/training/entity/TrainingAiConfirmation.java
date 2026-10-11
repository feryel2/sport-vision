package com.sportvision.training.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="training_ai_confirmations", uniqueConstraints=@UniqueConstraint(
    name="training_ai_confirmation_owner_key", columnNames={"owner_reference","confirmation_key"}))
public class TrainingAiConfirmation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="owner_reference",nullable=false,updatable=false,length=200) private String ownerReference;
    @Column(name="confirmation_key",nullable=false,updatable=false) private UUID confirmationKey;
    @Column(nullable=false,updatable=false,length=64) private String requestHash;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="program_id",nullable=false,updatable=false) private TrainingProgram program;

    protected TrainingAiConfirmation() {}
    public TrainingAiConfirmation(String owner,UUID key,String hash,TrainingProgram program){
        this.ownerReference=owner;this.confirmationKey=key;this.requestHash=hash;this.program=program;
    }
    public String getRequestHash(){return requestHash;}
    public Long getProgramId(){return program.getId();}
}
