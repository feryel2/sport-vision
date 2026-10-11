package com.sportvision.training.repository;

import com.sportvision.training.entity.TrainingAiConfirmation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingAiConfirmationRepository extends JpaRepository<TrainingAiConfirmation,Long> {
    Optional<TrainingAiConfirmation> findByOwnerReferenceAndConfirmationKey(String owner,UUID key);
}
