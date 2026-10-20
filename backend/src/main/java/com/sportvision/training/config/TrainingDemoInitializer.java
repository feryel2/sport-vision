package com.sportvision.training.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.sportvision.training.dto.ExerciseInput;
import com.sportvision.training.entity.DemoSeed;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.repository.*;
import com.sportvision.training.service.TrainingExerciseService;

@Component
@Profile({"local", "integration"})
@ConditionalOnProperty(name = "training.demo.enabled", havingValue = "true")
public class TrainingDemoInitializer implements ApplicationRunner {
    private final DemoSeedRepository seeds;
    private final TrainingExerciseRepository exercises;
    private final TrainingExerciseService service;
    public TrainingDemoInitializer(DemoSeedRepository seeds, TrainingExerciseRepository exercises, TrainingExerciseService service) {
        this.seeds = seeds; this.exercises = exercises; this.service = service;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (seeds.existsById("exercise-catalog-v1")) return;
        add("Pompes", "Renforcement des pectoraux et des triceps au poids du corps.", "Placez les mains sous les épaules.\nGardez le corps aligné en fléchissant les coudes.\nRepoussez le sol en contrôlant le mouvement.", MuscleGroup.CHEST, Equipment.NONE, Difficulty.BEGINNER, MeasurementType.REPETITIONS);
        add("Squats", "Mouvement global pour renforcer les jambes.", "Écartez les pieds à la largeur des épaules.\nFléchissez les hanches et les genoux en gardant les talons au sol.\nRemontez de façon contrôlée.", MuscleGroup.LEGS, Equipment.NONE, Difficulty.BEGINNER, MeasurementType.REPETITIONS);
        add("Gainage ventral", "Maintien statique pour travailler la sangle abdominale.", "Placez les avant-bras au sol, coudes sous les épaules.\nAlignez le bassin avec le tronc.\nRespirez régulièrement pendant le maintien.", MuscleGroup.CORE, Equipment.NONE, Difficulty.BEGINNER, MeasurementType.DURATION);
        add("Tractions", "Renforcement du dos à la barre de traction.", "Saisissez la barre avec une prise stable.\nTirez les coudes vers le bas sans vous balancer.\nRedescendez lentement.", MuscleGroup.BACK, Equipment.PULL_UP_BAR, Difficulty.ADVANCED, MeasurementType.REPETITIONS);
        add("Curl avec haltères", "Flexion des coudes pour solliciter les biceps.", "Tenez les haltères le long du corps.\nFléchissez les coudes sans avancer les épaules.\nRedescendez avec contrôle.", MuscleGroup.ARMS, Equipment.DUMBBELLS, Difficulty.BEGINNER, MeasurementType.REPETITIONS);
        add("Élévations latérales", "Mouvement ciblé pour les épaules.", "Tenez une charge légère dans chaque main.\nLevez les bras sur les côtés sans dépasser les épaules.\nAbaissez lentement les bras.", MuscleGroup.SHOULDERS, Equipment.DUMBBELLS, Difficulty.INTERMEDIATE, MeasurementType.REPETITIONS);
        add("Pont fessier", "Extension des hanches au sol pour les fessiers.", "Allongez-vous sur le dos, pieds à plat et genoux fléchis.\nSoulevez le bassin sans cambrer le dos.\nRevenez doucement au sol.", MuscleGroup.GLUTES, Equipment.NONE, Difficulty.BEGINNER, MeasurementType.REPETITIONS);
        add("Tirage à l’élastique", "Travail du dos avec résistance progressive.", "Fixez solidement l’élastique devant vous.\nTirez les coudes vers l’arrière sans hausser les épaules.\nRelâchez progressivement la tension.", MuscleGroup.BACK, Equipment.RESISTANCE_BAND, Difficulty.BEGINNER, MeasurementType.REPETITIONS);
        add("Soulevé de terre", "Mouvement de force sollicitant la chaîne postérieure.", "Placez la barre près des tibias.\nReculez les hanches en gardant le dos stable.\nRedressez-vous avec la barre proche du corps, puis reposez-la avec contrôle.", MuscleGroup.LEGS, Equipment.BARBELL, Difficulty.ADVANCED, MeasurementType.REPETITIONS);
        add("Montées sur banc", "Travail unilatéral des jambes sur un support stable.", "Posez un pied entièrement sur le banc.\nPoussez sur ce pied pour monter.\nRedescendez lentement et alternez les jambes.", MuscleGroup.LEGS, Equipment.BENCH, Difficulty.INTERMEDIATE, MeasurementType.REPETITIONS);
        add("Marche avec kettlebell", "Portage pour travailler le maintien du tronc.", "Tenez la kettlebell d’un côté, bras le long du corps.\nMarchez lentement en gardant le buste droit.\nChangez de côté à mi-parcours.", MuscleGroup.CORE, Equipment.KETTLEBELL, Difficulty.INTERMEDIATE, MeasurementType.DURATION);
        add("Corde à sauter", "Exercice rythmé sollicitant tout le corps.", "Ajustez la corde à votre taille.\nEffectuez de petits sauts souples en tournant les poignets.\nGardez une cadence confortable.", MuscleGroup.FULL_BODY, Equipment.JUMP_ROPE, Difficulty.INTERMEDIATE, MeasurementType.DURATION);
        seeds.save(new DemoSeed("exercise-catalog-v1"));
    }
    private void add(String name, String description, String instructions, MuscleGroup muscleGroup, Equipment equipment, Difficulty difficulty, MeasurementType measurementType) {
        if (!exercises.existsByNameIgnoreCase(name)) service.create(new ExerciseInput(name, description, instructions, muscleGroup, equipment, difficulty, measurementType));
    }
}
