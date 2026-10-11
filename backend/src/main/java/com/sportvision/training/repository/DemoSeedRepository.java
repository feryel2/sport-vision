package com.sportvision.training.repository;
import com.sportvision.training.entity.DemoSeed;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DemoSeedRepository extends JpaRepository<DemoSeed, String> {}
