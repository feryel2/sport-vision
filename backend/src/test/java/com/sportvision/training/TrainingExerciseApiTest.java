package com.sportvision.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvision.training.repository.TrainingExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = com.sportvision.SportVisionApplication.class, properties = {"spring.datasource.url=jdbc:h2:mem:training_catalog_tests;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "training.demo.enabled=false"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(username="training-test-user", roles="ADMIN")
class TrainingExerciseApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TrainingExerciseRepository repository;
    @BeforeEach void clean() { repository.deleteAll(); }
    private String input(String name) {
        return """
            {"name":"%s","description":"Description","instructions":"Consignes",
            "muscleGroup":"LEGS","equipment":"NONE","difficulty":"BEGINNER","measurementType":"REPETITIONS"}
            """.formatted(name);
    }
    private long create(String name) throws Exception {
        var result = mvc.perform(post("/api/training/exercises").contentType("application/json").content(input(name)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.archived").value(false))
            .andExpect(jsonPath("$.data.createdAt").isNotEmpty()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }
    @Test void rejectsInvalidInputAndEnums() throws Exception {
        mvc.perform(post("/api/training/exercises").contentType("application/json").content(input("   ")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.data.errors.name").exists());
        mvc.perform(post("/api/training/exercises").contentType("application/json").content(input("x".repeat(121))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/training/exercises").contentType("application/json").content(input("Squat").replace("BEGINNER", "EXPERT")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/api/training/exercises").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errors.equipment").exists());
        mvc.perform(get("/api/training/exercises").param("difficulty", "EXPERT")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/training/exercises").contentType("application/json").content(input("Squat").replace("\"BEGINNER\"", "0")))
            .andExpect(status().isBadRequest());
    }
    @Test void returnsNotFoundForMissingId() throws Exception {
        mvc.perform(get("/api/training/exercises/9999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(put("/api/training/exercises/9999999").contentType("application/json").content(input("Squat"))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/training/exercises/9999999/archive")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/training/exercises/9999999/restore")).andExpect(status().isNotFound());
    }
    @Test void filtersAndPaginatesWithStableOrdering() throws Exception {
        long first = create("Squat"); long second = create("Squat"); create("Fentes");
        mvc.perform(get("/api/training/exercises").param("search", "SQU").param("muscleGroup", "LEGS").param("equipment", "NONE")
            .param("difficulty", "BEGINNER").param("measurementType", "REPETITIONS").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2)).andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.items[0].id").value(first));
        mvc.perform(get("/api/training/exercises").param("search", "squ").param("size", "1").param("page", "1"))
            .andExpect(jsonPath("$.data.items[0].id").value(second));
        mvc.perform(get("/api/training/exercises").param("equipment", "BARBELL")).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/training/exercises").param("search", "%")).andExpect(jsonPath("$.data.totalElements").value(0));
        for (String[] pair : new String[][] {{"size","51"},{"size","0"},{"page","-1"},{"sort","description"},{"direction","sideways"}}) {
            mvc.perform(get("/api/training/exercises").param(pair[0],pair[1])).andExpect(status().isBadRequest());
        }
    }
    @Test void archivesRestoresAndKeepsDetailVisible() throws Exception {
        long id = create("Squat");
        mvc.perform(patch("/api/training/exercises/" + id + "/archive")).andExpect(status().isOk()).andExpect(jsonPath("$.data.archived").value(true));
        mvc.perform(get("/api/training/exercises")).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/training/exercises").param("status", "ARCHIVED")).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/training/exercises").param("status", "ALL")).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/training/exercises/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.archived").value(true));
        mvc.perform(patch("/api/training/exercises/" + id + "/restore")).andExpect(status().isOk()).andExpect(jsonPath("$.data.archived").value(false));
        mvc.perform(get("/api/training/exercises")).andExpect(jsonPath("$.data.totalElements").value(1));
    }
    @Test void updatePreservesServerOwnedFieldsAndOptionsAreFrench() throws Exception {
        long id = create("Squat");
        mvc.perform(put("/api/training/exercises/" + id).contentType("application/json").content(input("Squat modifié").replace("\"name\"", "\"archived\":true,\"createdAt\":\"2000-01-01T00:00:00Z\",\"name\"")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Squat modifié"))
            .andExpect(jsonPath("$.data.archived").value(false)).andExpect(jsonPath("$.data.createdAt", not("2000-01-01T00:00:00Z")));
        mvc.perform(get("/api/training/exercises/options")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.equipment[0].value").value("NONE")).andExpect(jsonPath("$.data.equipment[0].label").value("Sans matériel"));
    }
}

