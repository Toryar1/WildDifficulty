package fr.wilddifficulty.player;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String playerName;
    private String difficultyLevel; // FACILE, NORMAL, DIFFICILE, EXTREME
    private int thirstLevel;        // 0 to 20
    private boolean thirstEnabled;
    private boolean hardcoreEnabled;
    private boolean hardcoreNoRegen;
    private boolean hardcoreAllowGoldenApples;
    private boolean hardcoreAllowPotions;
    private boolean hardcoreAllowSafezoneRegen;
    private boolean hardcoreInstantDeathDespawn;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName != null ? playerName : "Unknown";
        this.difficultyLevel = "NORMAL";
        this.thirstLevel = 20;
        this.thirstEnabled = true;
        this.hardcoreEnabled = false;
        this.hardcoreNoRegen = true;
        this.hardcoreAllowGoldenApples = true;
        this.hardcoreAllowPotions = true;
        this.hardcoreAllowSafezoneRegen = true;
        this.hardcoreInstantDeathDespawn = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public double getDamageMultiplier() {
        return switch (difficultyLevel.toUpperCase()) {
            case "FACILE" -> 0.75;
            case "DIFFICILE" -> 1.35;
            case "EXTREME" -> 1.80;
            default -> 1.0;
        };
    }

    public int getThirstLevel() {
        return thirstLevel;
    }

    public void setThirstLevel(int thirstLevel) {
        this.thirstLevel = Math.max(0, Math.min(20, thirstLevel));
    }

    public boolean isThirstEnabled() {
        return thirstEnabled;
    }

    public void setThirstEnabled(boolean thirstEnabled) {
        this.thirstEnabled = thirstEnabled;
    }

    public boolean isHardcoreEnabled() {
        return hardcoreEnabled;
    }

    public void setHardcoreEnabled(boolean hardcoreEnabled) {
        this.hardcoreEnabled = hardcoreEnabled;
    }

    public boolean isHardcoreNoRegen() { return hardcoreNoRegen; }
    public void setHardcoreNoRegen(boolean v) { this.hardcoreNoRegen = v; }

    public boolean isHardcoreAllowGoldenApples() { return hardcoreAllowGoldenApples; }
    public void setHardcoreAllowGoldenApples(boolean v) { this.hardcoreAllowGoldenApples = v; }

    public boolean isHardcoreAllowPotions() { return hardcoreAllowPotions; }
    public void setHardcoreAllowPotions(boolean v) { this.hardcoreAllowPotions = v; }

    public boolean isHardcoreAllowSafezoneRegen() { return hardcoreAllowSafezoneRegen; }
    public void setHardcoreAllowSafezoneRegen(boolean v) { this.hardcoreAllowSafezoneRegen = v; }

    public boolean isHardcoreInstantDeathDespawn() { return hardcoreInstantDeathDespawn; }
    public void setHardcoreInstantDeathDespawn(boolean v) { this.hardcoreInstantDeathDespawn = v; }
}
