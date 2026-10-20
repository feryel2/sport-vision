package com.sportvision.training.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvision.training.dto.TrainingAiDto.Goal;
import com.sportvision.training.dto.TrainingAiDto.Preferences;
import com.sportvision.training.entity.ExerciseOptions.Difficulty;
import com.sportvision.training.entity.ExerciseOptions.Equipment;
import com.sportvision.training.entity.ExerciseOptions.MeasurementType;
import com.sportvision.training.entity.ExerciseOptions.MuscleGroup;
import com.sportvision.training.entity.TrainingExercise;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.TrainingExerciseRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingAiServiceTest {
    private static ValidatorFactory validators;

    @Mock private TrainingExerciseRepository exercises;
    @Mock private TrainingProgramService programs;
    @Mock private GeminiClient gemini;
    @Mock private ObjectProvider<TrainingCurrentUserProvider> users;
    @Mock private TrainingCurrentUserProvider owner;
    @Mock private TrainingAiConfirmationService confirmations;

    private TrainingAiService service;
    private final Preferences preferences = new Preferences(
        Goal.GENERAL_FITNESS, Difficulty.BEGINNER, 1, Set.of(Equipment.NONE));

    @BeforeAll
    static void openValidator() {
        validators = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validators.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        when(users.getIfAvailable()).thenReturn(owner);
        when(owner.ownerReference()).thenReturn("training-unit-owner");
        service = new TrainingAiService(exercises, programs, gemini, validators.getValidator(),
            new ObjectMapper(), users, confirmations);
    }

    @Test
    void generatesMixedRepetitionAndDurationPreviewWithoutPersisting() {
        // Arrange: unlike the API's repetition-only example, this session includes duration.
        catalogue(compatibleExercises());
        when(gemini.generate(anyString(), anyString(), any(JsonNode.class))).thenReturn("""
            {
              "name":"Full Body découverte",
              "description":"Une séance au poids du corps avec gainage.",
              "justification":"Exercices autorisés adaptés au niveau débutant.",
              "sessions":[{"name":"Séance complète","exercises":[
                {"exerciseId":101,"measurementType":"REPETITIONS","sets":2,
                 "repetitions":10,"restSeconds":45},
                {"exerciseId":102,"measurementType":"REPETITIONS","sets":2,
                 "repetitions":8,"restSeconds":45},
                {"exerciseId":103,"measurementType":"DURATION","sets":2,
                 "durationSeconds":30,"restSeconds":45}
              ]}]
            }
            """);

        // Act
        var preview = service.generate(preferences);

        // Assert: parsing and real Bean Validation preserve each measurement's meaning.
        assertThat(preview.proposal().name()).isEqualTo("Full Body découverte");
        assertThat(preview.proposal().sessions()).hasSize(1);
        var choices = preview.proposal().sessions().get(0).exercises();
        assertThat(choices).extracting(choice -> choice.exerciseId())
            .containsExactly(101L, 102L, 103L);
        assertThat(choices.get(0).measurementType()).isEqualTo(MeasurementType.REPETITIONS);
        assertThat(choices.get(0).repetitions()).isEqualTo(10);
        assertThat(choices.get(0).durationSeconds()).isNull();
        assertThat(choices.get(2).measurementType()).isEqualTo(MeasurementType.DURATION);
        assertThat(choices.get(2).durationSeconds()).isEqualTo(30);
        assertThat(choices.get(2).repetitions()).isNull();
        assertThat(choices.get(2).sets()).isEqualTo(2);
        assertThat(choices.get(2).restSeconds()).isEqualTo(45);
        assertThat(preview.catalogue()).extracting(exercise -> exercise.id())
            .containsExactly(101L, 102L, 103L);
        verify(gemini).requireConfigured();
        verify(gemini).generate(anyString(), anyString(), any(JsonNode.class));
        verifyNoInteractions(programs, confirmations);
    }

    @Test
    void refusesTwoCompatibleExercisesBeforeCallingGeminiGeneration() {
        // Arrange: two exercises are immediately below the minimum catalogue size of three.
        catalogue(compatibleExercises().subList(0, 2));

        // Act
        var error = assertThrows(TrainingAiException.class, () -> service.generate(preferences));

        // Assert: the early guard prevents both remote generation and persistence.
        assertThat(error.getStatus()).isEqualTo(422);
        assertThat(error.getCode()).isEqualTo("CATALOGUE_INSUFFICIENT");
        assertThat(error.getMessage()).contains("au moins trois exercices actifs compatibles");
        verify(gemini).requireConfigured();
        verify(gemini, never()).generate(anyString(), anyString(), any(JsonNode.class));
        verifyNoInteractions(programs, confirmations);
    }

    @Test
    void translatesUnexpectedClientFailureWithoutLeakingTechnicalDetailsOrSaving() {
        // Arrange: simulate a client failure, without creating any real HTTP connection.
        catalogue(compatibleExercises());
        when(gemini.generate(anyString(), anyString(), any(JsonNode.class)))
            .thenThrow(new IllegalStateException("Détail technique interne simulé"));

        // Act
        var error = assertThrows(TrainingAiException.class, () -> service.generate(preferences));

        // Assert: exercise callGemini's generic catch branch, absent from existing API tests.
        assertThat(error.getStatus()).isEqualTo(503);
        assertThat(error.getCode()).isEqualTo("UNAVAILABLE");
        assertThat(error.getMessage()).isEqualTo("Service IA indisponible.")
            .doesNotContain("Détail technique interne simulé");
        verify(gemini).generate(anyString(), anyString(), any(JsonNode.class));
        verifyNoInteractions(programs, confirmations);
    }

    @Test
    void rejectsNumericMeasurementTypeButAcceptsTextualValue() {
        // Arrange: both responses are identical except for the first measurementType.
        catalogue(compatibleExercises());
        String textualResponse = """
            {
              "name":"Programme de régression",
              "description":"Une séance valide au poids du corps.",
              "justification":"Exercices du catalogue autorisé.",
              "sessions":[{"name":"Séance complète","exercises":[
                {"exerciseId":101,"measurementType":"REPETITIONS","sets":2,
                 "repetitions":10,"restSeconds":45},
                {"exerciseId":102,"measurementType":"REPETITIONS","sets":2,
                 "repetitions":8,"restSeconds":45},
                {"exerciseId":103,"measurementType":"DURATION","sets":2,
                 "durationSeconds":30,"restSeconds":45}
              ]}]
            }
            """;
        String numericResponse = textualResponse.replaceFirst(
            "\"measurementType\":\"REPETITIONS\"", "\"measurementType\":0");
        when(gemini.generate(anyString(), anyString(), any(JsonNode.class)))
            .thenReturn(textualResponse, numericResponse);

        // Act / Assert: the textual control must remain valid.
        var preview = service.generate(preferences);
        assertThat(preview.proposal().sessions().get(0).exercises().get(0).measurementType())
            .isEqualTo(MeasurementType.REPETITIONS);

        // Act / Assert: the numeric response must be rejected, without saving either response.
        try {
            var error = assertThrows(TrainingAiException.class, () -> service.generate(preferences));
            assertThat(error.getStatus()).isEqualTo(502);
            assertThat(error.getCode()).isEqualTo("INVALID_PROPOSAL");
        } finally {
            verify(gemini, org.mockito.Mockito.times(2))
                .generate(anyString(), anyString(), any(JsonNode.class));
            verifyNoInteractions(programs, confirmations);
        }
    }

    private void catalogue(List<TrainingExercise> available) {
        when(exercises.findAll(org.mockito.ArgumentMatchers.<Specification<TrainingExercise>>any(),
            any(Pageable.class))).thenReturn(new PageImpl<>(available));
    }

    private List<TrainingExercise> compatibleExercises() {
        return List.of(
            exercise(101L, "Squat au poids du corps", MuscleGroup.LEGS, MeasurementType.REPETITIONS),
            exercise(102L, "Pompes sur les genoux", MuscleGroup.CHEST, MeasurementType.REPETITIONS),
            exercise(103L, "Gainage ventral", MuscleGroup.CORE, MeasurementType.DURATION));
    }

    private TrainingExercise exercise(long id, String name, MuscleGroup muscle, MeasurementType measure) {
        var exercise = new TrainingExercise();
        exercise.edit(name, "Exercice au poids du corps.", "Exécuter le mouvement avec contrôle.",
            muscle, Equipment.NONE, Difficulty.BEGINNER, measure);
        ReflectionTestUtils.setField(exercise, "id", id);
        return exercise;
    }
}
