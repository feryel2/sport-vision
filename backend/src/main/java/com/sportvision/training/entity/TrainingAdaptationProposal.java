package com.sportvision.training.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="training_adaptation_proposals")
public class TrainingAdaptationProposal {
    @Id private UUID id;
    @Column(nullable=false) private String ownerReference;
    private long programId;
    private int revision;
    @Column(nullable=false,length=64) private String fingerprint;
    @Column(nullable=false,length=4000) private String selection;
    private Instant expiresAt;
    protected TrainingAdaptationProposal() {}
    public TrainingAdaptationProposal(String owner,long program,int revision,String fingerprint,String selection,Instant expiresAt){
        this.id=UUID.randomUUID();this.ownerReference=owner;this.programId=program;this.revision=revision;this.fingerprint=fingerprint;this.selection=selection;this.expiresAt=expiresAt;
    }
    public UUID getId(){return id;} public String getOwnerReference(){return ownerReference;}
    public long getProgramId(){return programId;} public int getRevision(){return revision;}
    public String getFingerprint(){return fingerprint;} public String getSelection(){return selection;}
    public Instant getExpiresAt(){return expiresAt;}
}
