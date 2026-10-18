package com.sportvision.training.repository;
import com.sportvision.training.entity.TrainingAdaptationProposal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TrainingAdaptationProposalRepository extends JpaRepository<TrainingAdaptationProposal,UUID> {}
