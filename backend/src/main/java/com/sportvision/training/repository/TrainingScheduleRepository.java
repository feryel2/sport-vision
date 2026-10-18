package com.sportvision.training.repository;
import com.sportvision.training.entity.TrainingSchedule;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface TrainingScheduleRepository extends JpaRepository<TrainingSchedule,Long>,JpaSpecificationExecutor<TrainingSchedule> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TrainingSchedule s where s.id=:id and s.ownerReference=:owner")
    Optional<TrainingSchedule> lockOwned(long id,String owner);
    boolean existsByWorkoutId(long workoutId);
}
