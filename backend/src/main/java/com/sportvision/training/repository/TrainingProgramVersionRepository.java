package com.sportvision.training.repository;
import com.sportvision.training.entity.TrainingProgramVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface TrainingProgramVersionRepository extends JpaRepository<TrainingProgramVersion,Long> {
    Optional<TrainingProgramVersion> findByProgramIdAndRevision(long programId,int revision);
    Page<TrainingProgramVersion> findByProgramId(long programId,Pageable pageable);
}
