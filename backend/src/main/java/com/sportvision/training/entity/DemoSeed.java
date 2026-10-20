package com.sportvision.training.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "training_demo_seeds")
public class DemoSeed {
    @Id @Column(length = 80) private String id;
    protected DemoSeed() {}
    public DemoSeed(String id) { this.id = id; }
}
