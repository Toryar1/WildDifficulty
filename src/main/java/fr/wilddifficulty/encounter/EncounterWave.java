package fr.wilddifficulty.encounter;

import java.util.HashMap;
import java.util.Map;

/**
 * Définition d'une vague d'ennemis pour un Encounter.
 * Peut contenir des variantes individuelles de monstres et/ou des escouades complètes.
 */
public class EncounterWave {

    private int waveNumber;
    private int delaySeconds;
    private String soundEffect = "entity.wither.spawn";
    private String particleEffect = "FLAME";
    private String spawnDistribution = "AROUND_CENTER"; // AROUND_CENTER | RANDOM_ZONE | MARKERS

    // Map VariantId -> Quantité
    private Map<String, Integer> variantSpawns = new HashMap<>();

    // Map SquadId -> Nombre d'escouades
    private Map<String, Integer> squadSpawns = new HashMap<>();

    public EncounterWave() {
        this.waveNumber = 1;
        this.delaySeconds = 5;
    }

    public EncounterWave(int waveNumber, int delaySeconds) {
        this.waveNumber = waveNumber;
        this.delaySeconds = delaySeconds;
    }

    public int getWaveNumber() { return waveNumber; }
    public void setWaveNumber(int waveNumber) { this.waveNumber = waveNumber; }

    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }

    public String getSoundEffect() { return soundEffect; }
    public void setSoundEffect(String soundEffect) { this.soundEffect = soundEffect; }

    public String getParticleEffect() { return particleEffect; }
    public void setParticleEffect(String particleEffect) { this.particleEffect = particleEffect; }

    public String getSpawnDistribution() { return spawnDistribution; }
    public void setSpawnDistribution(String spawnDistribution) { this.spawnDistribution = spawnDistribution; }

    public Map<String, Integer> getVariantSpawns() { return variantSpawns; }
    public void setVariantSpawns(Map<String, Integer> variantSpawns) { this.variantSpawns = variantSpawns; }

    public Map<String, Integer> getSquadSpawns() { return squadSpawns; }
    public void setSquadSpawns(Map<String, Integer> squadSpawns) { this.squadSpawns = squadSpawns; }

    public void addVariant(String variantId, int count) {
        this.variantSpawns.put(variantId, count);
    }

    public void addSquad(String squadId, int count) {
        this.squadSpawns.put(squadId, count);
    }
}
