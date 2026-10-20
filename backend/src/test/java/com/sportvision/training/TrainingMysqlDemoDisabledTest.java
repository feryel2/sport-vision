package com.sportvision.training;
import com.sportvision.training.bootstrap.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercise profile exclusion with disposable H2, never connect to an actual MySQL server. */
@SpringBootTest(classes=com.sportvision.SportVisionApplication.class, properties={
 "spring.datasource.url=jdbc:h2:mem:mysql_profile_safety;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop",
 "training.dataset.enabled=true","training.demo.enabled=false","sportvision.demo.enabled=false","training.ai.api-key="})
@ActiveProfiles({"test","mysql","local-demo"})
class TrainingMysqlDemoDisabledTest {
 @Autowired ApplicationContext context; @Autowired JdbcTemplate sql;
 @Test void mysqlProfilePreventsBothDemoInitializersEvenIfLocalDemoIsAccidentallySelected() {
  assertThat(context.getBeansOfType(TrainingLocalDatasetInitializer.class)).isEmpty();
  assertThat(context.getBeansOfType(TrainingPersonalDemoInitializer.class)).isEmpty();
  assertThat(sql.queryForObject("select count(*) from training_exercises",Integer.class)).isZero();
  assertThat(sql.queryForObject("select count(*) from training_programs",Integer.class)).isZero();
 }
}
