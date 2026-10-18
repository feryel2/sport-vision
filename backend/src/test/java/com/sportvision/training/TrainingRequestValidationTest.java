package com.sportvision.training;

import com.sportvision.training.controller.*;
import com.sportvision.training.service.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TrainingRequestValidationTest {
    @ParameterizedTest
    @ValueSource(strings={"/api/training/ai/programs/1/adaptation", "/api/training/progress/exercises/1?days=30"})
    void missingRequiredQueryParameterIsAClientError(String url)throws Exception{
        var adaptation=mock(TrainingAdaptationService.class);
        var progress=mock(TrainingProgressService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new TrainingAdaptationController(adaptation),new TrainingProgressController(progress))
            .setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(get(url)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Un paramètre obligatoire est manquant."))
            .andExpect(jsonPath("$.data.errors."+(url.contains("adaptation")?"expectedRevision":"measurementType"))
                .value("Ce paramètre est obligatoire."));
        verifyNoInteractions(adaptation,progress);
    }
}
