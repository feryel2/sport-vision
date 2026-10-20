package com.sportvision.training;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes = com.sportvision.SportVisionApplication.class, properties={"spring.datasource.url=jdbc:h2:mem:training_no_identity_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("test") @AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(username="training-test-user", roles="ADMIN")
class TrainingIdentityUnavailableTest {
    @Autowired MockMvc mvc;
    @Test void refusesPrivateAccessOutsideLocalWithoutProvider() throws Exception {
        mvc.perform(get("/api/training/programs")).andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/training/programs/1")).andExpect(status().isForbidden());
        mvc.perform(post("/api/training/programs/1/duplicate")).andExpect(status().isForbidden());
    }
}
