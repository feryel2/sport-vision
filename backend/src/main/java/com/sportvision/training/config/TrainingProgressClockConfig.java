package com.sportvision.training.config;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration
public class TrainingProgressClockConfig {
    @Bean public Clock trainingProgressClock(){return Clock.systemUTC();}
}
