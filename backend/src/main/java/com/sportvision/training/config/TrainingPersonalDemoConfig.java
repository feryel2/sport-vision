package com.sportvision.training.config;

import com.sportvision.training.bootstrap.TrainingPersonalDemoInitializer;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** No authentication changes: Spring Security has already authenticated these requests. */
@Configuration
@Profile("local-demo & !mysql & !production")
@ConditionalOnProperty(name="training.dataset.enabled",havingValue="true")
public class TrainingPersonalDemoConfig implements WebMvcConfigurer {
    private final TrainingPersonalDemoInitializer demos;
    private final TrainingCurrentUserProvider users;
    public TrainingPersonalDemoConfig(TrainingPersonalDemoInitializer demos, TrainingCurrentUserProvider users) {
        this.demos=demos; this.users=users;
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
                String path=request.getRequestURI().substring(request.getContextPath().length());
                if ("GET".equals(request.getMethod()) && (path.startsWith("/api/training/programs")
                        || path.startsWith("/api/training/workouts") || path.startsWith("/api/training/progress")
                        || path.startsWith("/api/training/schedules") || path.startsWith("/api/training/ai/programs/"))) {
                    demos.prepare(users.ownerReference());
                }
                return true;
            }
        }).addPathPatterns("/api/training/**");
    }
}
