package com.sportvision.repository;

import com.sportvision.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachRepository extends JpaRepository<Coach, Long> {
    List<Coach> findBySportIgnoreCase(String sport);

    @Query("SELECT c FROM Coach c WHERE LOWER(c.sport) LIKE LOWER(CONCAT('%', :sport, '%')) " +
           "OR LOWER(c.specialty) LIKE LOWER(CONCAT('%', :specialty, '%'))")
    List<Coach> findBySportOrSpecialty(@Param("sport") String sport, @Param("specialty") String specialty);
}
