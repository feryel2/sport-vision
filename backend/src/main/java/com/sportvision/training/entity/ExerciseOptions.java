package com.sportvision.training.entity;

public final class ExerciseOptions {
    private ExerciseOptions() {}
    public interface Labeled { String label(); }
    public enum MuscleGroup implements Labeled {
        CHEST("Pectoraux"), BACK("Dos"), SHOULDERS("Épaules"), ARMS("Bras"),
        LEGS("Jambes"), GLUTES("Fessiers"), CORE("Abdominaux"), FULL_BODY("Corps entier");
        private final String label;
        MuscleGroup(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum Equipment implements Labeled {
        NONE("Sans matériel"), DUMBBELLS("Haltères"), BARBELL("Barre"),
        RESISTANCE_BAND("Élastique"), PULL_UP_BAR("Barre de traction"),
        BENCH("Banc"), KETTLEBELL("Kettlebell"), JUMP_ROPE("Corde à sauter");
        private final String label;
        Equipment(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum Difficulty implements Labeled {
        BEGINNER("Débutant"), INTERMEDIATE("Intermédiaire"), ADVANCED("Avancé");
        private final String label;
        Difficulty(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum MeasurementType implements Labeled {
        REPETITIONS("Répétitions"), DURATION("Durée");
        private final String label;
        MeasurementType(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum ArchiveStatus { ACTIVE, ARCHIVED, ALL }
}
