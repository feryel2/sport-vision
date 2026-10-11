package com.sportvision.training.config;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
@Configuration
public class TrainingIdentityConfig {
    @Bean @Profile("local") @ConditionalOnMissingBean(TrainingCurrentUserProvider.class)
    TrainingCurrentUserProvider localTrainingUser() { return () -> "training-local-demo"; }
}
