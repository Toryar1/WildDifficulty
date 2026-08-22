package fr.wilddifficulty.encounter;

/**
 * Types d'Encounters gérés par le moteur d'événements par zones de WildDifficulty.
 */
public enum EncounterType {
    NONE("Aucun"),
    BASE_RAID("Invasion de Base / Raid"),
    TRIAL_BUNKER("Bunker Trial"),
    OUTPOST("Avant-Poste Illager"),
    RUINS("Ruines & Vestiges Anciens");

    private final String displayName;

    EncounterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EncounterType fromString(String name) {
        if (name == null) return NONE;
        try {
            return EncounterType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
