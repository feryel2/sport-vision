package com.sportvision.training.repository;
import com.sportvision.training.entity.TrainingProgram;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface TrainingProgramRepository extends JpaRepository<TrainingProgram,Long>, JpaSpecificationExecutor<TrainingProgram> {
    Optional<TrainingProgram> findByIdAndOwnerReference(long id,String ownerReference);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TrainingProgram p where p.id=:id and p.ownerReference=:owner")
    Optional<TrainingProgram> lockOwned(long id,String owner);
}
