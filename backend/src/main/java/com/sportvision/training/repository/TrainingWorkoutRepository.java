package com.sportvision.training.repository;
import com.sportvision.training.entity.TrainingWorkout;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface TrainingWorkoutRepository extends JpaRepository<TrainingWorkout,Long>,JpaSpecificationExecutor<TrainingWorkout> {
    Optional<TrainingWorkout> findByIdAndOwnerReference(long id,String ownerReference);
    Optional<TrainingWorkout> findByActiveOwner(String activeOwner);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from TrainingWorkout w where w.id=:id and w.ownerReference=:owner")
    Optional<TrainingWorkout> lockOwned(long id,String owner);
}
