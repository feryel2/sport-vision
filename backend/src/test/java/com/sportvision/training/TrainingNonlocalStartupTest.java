package com.sportvision.training;
import com.sportvision.SportVisionApplication;
import com.sportvision.training.bootstrap.TrainingLocalDatasetInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(classes=SportVisionApplication.class,properties={"spring.datasource.url=jdbc:h2:mem:nonlocal_startup;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.ai.api-key="})
@ActiveProfiles("production") @AutoConfigureMockMvc
class TrainingNonlocalStartupTest {
 @Autowired ApplicationContext context; @Autowired JdbcTemplate sql; @Autowired MockMvc mvc;
 @Test void explicitNonlocalProfileDoesNotSeedOrExposeDemoApis()throws Exception{
  assertThat(context.getBeansOfType(TrainingLocalDatasetInitializer.class)).isEmpty();
  assertThat(context.getBeansOfType(com.sportvision.training.bootstrap.TrainingPersonalDemoInitializer.class)).isEmpty();
  assertThat(sql.queryForObject("select count(*) from training_exercises",Integer.class)).isZero();
  mvc.perform(get("/api/training/ai/options")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
 }
}
