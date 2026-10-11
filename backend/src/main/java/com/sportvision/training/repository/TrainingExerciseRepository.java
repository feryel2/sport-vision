package com.sportvision.training.repository;

import com.sportvision.training.entity.TrainingExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainingExerciseRepository extends JpaRepository<TrainingExercise, Long>, JpaSpecificationExecutor<TrainingExercise> {
    boolean existsByNameIgnoreCase(String name);
}
