package com.sportvision.training.repository;
import com.sportvision.training.entity.*;
import com.sportvision.training.entity.WorkoutStatuses.WorkoutStatus;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
public interface TrainingProgressRepository extends Repository<TrainingWorkout,Long> {
    @Query("""
        select distinct w from TrainingWorkout w left join fetch w.exercises
        where w.ownerReference=:owner and w.status in :statuses
        and w.endedAt>=:from and w.endedAt<:until order by w.endedAt,w.id
        """)
    List<TrainingWorkout> closedWorkouts(String owner,Instant from,Instant until,Set<WorkoutStatus> statuses);
    // Fetch the second collection separately to avoid a Cartesian product of two bags.
    @Query("""
        select distinct e from TrainingWorkoutExercise e left join fetch e.sets
        where e.workout.ownerReference=:owner and e.workout.status=:status
        and e.workout.endedAt>=:from and e.workout.endedAt<:until
        """)
    List<TrainingWorkoutExercise> completedExercises(String owner,Instant from,Instant until,WorkoutStatus status);
}
