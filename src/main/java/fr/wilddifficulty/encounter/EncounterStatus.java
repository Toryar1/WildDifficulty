package fr.wilddifficulty.encounter;

/**
 * Statut du cycle de vie d'un Encounter dans une zone.
 */
public enum EncounterStatus {
    IDLE("En attente"),
    ACTIVE("En cours"),
    COOLDOWN("En recharge"),
    RESETTING("Réinitialisation");

    private final String displayName;

    EncounterStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
