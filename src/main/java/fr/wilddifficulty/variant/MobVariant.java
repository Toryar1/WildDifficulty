package fr.wilddifficulty.variant;

import fr.wilddifficulty.config.StatModifiers;
import org.bukkit.entity.EntityType;

import java.util.*;

public class MobVariant {

    private final String id;
    private EntityType type;
    private String displayName;
    private double weight;
    private boolean ignoreSunlight;
    private boolean baby = false;
    private double scale = 1.0;
    private double scaleVariance = 0.0;

    // Conditions de spawn
    private List<String> allowedBiomes = new ArrayList<>();
    private String spawnWeather = "ANY"; // ANY | RAINY | CLEAR
    private String spawnTime = "ANY";    // ANY | DAY | NIGHT
    private String caveSpawn = "ANY";    // ANY | ONLY_CAVES | NO_CAVES
    
    private String aggroMode = "PASSIVE"; // PASSIVE | AGGRESSIVE | NEUTRAL_HIT | NEUTRAL_SQUAD_HIT
    private int customModelData = 0;
    private int xpOnDeath = -1;
    
    private StatModifiers modifiers;
    
    private List<ConditionalName> conditionalNames = new ArrayList<>();
    private List<PossibleItem> possibleHandItems = new ArrayList<>();
    private List<CustomDrop> customDrops = new ArrayList<>();
    private Map<String, SoundConfig> customSounds = new HashMap<>(); // ambient, aggro, death

    public MobVariant(String id, EntityType type, String displayName, double weight, boolean ignoreSunlight, StatModifiers modifiers) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.weight = weight;
        this.ignoreSunlight = ignoreSunlight;
        this.modifiers = modifiers;
    }

    public String getId() { return id; }
    public EntityType getType() { return type; }
    public void setType(EntityType type) { this.type = type; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public boolean isIgnoreSunlight() { return ignoreSunlight; }
    public void setIgnoreSunlight(boolean ignoreSunlight) { this.ignoreSunlight = ignoreSunlight; }

    public boolean isBaby() { return baby; }
    public void setBaby(boolean baby) { this.baby = baby; }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }
    public double getScaleVariance() { return scaleVariance; }
    public void setScaleVariance(double sv) { this.scaleVariance = sv; }

    public StatModifiers getModifiers() { return modifiers; }
    public void setModifiers(StatModifiers modifiers) { this.modifiers = modifiers; }

    public List<String> getAllowedBiomes() { return allowedBiomes; }
    public void setAllowedBiomes(List<String> b) { this.allowedBiomes = b; }

    public String getSpawnWeather() { return spawnWeather; }
    public void setSpawnWeather(String w) { this.spawnWeather = w; }

    public String getSpawnTime() { return spawnTime; }
    public void setSpawnTime(String t) { this.spawnTime = t; }

    public String getCaveSpawn() { return caveSpawn; }
    public void setCaveSpawn(String c) { this.caveSpawn = c; }

    public String getAggroMode() { return aggroMode; }
    public void setAggroMode(String a) { this.aggroMode = a; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int val) { this.customModelData = val; }

    public int getXpOnDeath() { return xpOnDeath; }
    public void setXpOnDeath(int xp) { this.xpOnDeath = xp; }

    public List<ConditionalName> getConditionalNames() { return conditionalNames; }
    public void setConditionalNames(List<ConditionalName> conditionalNames) { this.conditionalNames = conditionalNames; }

    public List<PossibleItem> getPossibleHandItems() { return possibleHandItems; }
    public void setPossibleHandItems(List<PossibleItem> possibleHandItems) { this.possibleHandItems = possibleHandItems; }

    public List<CustomDrop> getCustomDrops() { return customDrops; }
    public void setCustomDrops(List<CustomDrop> customDrops) { this.customDrops = customDrops; }

    public Map<String, SoundConfig> getCustomSounds() { return customSounds; }
    public void setCustomSounds(Map<String, SoundConfig> customSounds) { this.customSounds = customSounds; }

    // Classes internes pour les structures complexes

    public static class ConditionalName {
        private final double threshold; // fraction de vie (ex: 0.5)
        private final String name;

        public ConditionalName(double threshold, String name) {
            this.threshold = threshold;
            this.name = name;
        }

        public double getThreshold() { return threshold; }
        public String getName() { return name; }
    }

    public static class PossibleItem {
        private final String materialName;
        private final double chance;

        public PossibleItem(String materialName, double chance) {
            this.materialName = materialName;
            this.chance = chance;
        }

        public String getMaterialName() { return materialName; }
        public double getChance() { return chance; }
    }

    public static class CustomDrop {
        private final String materialName;
        private final double chance;
        private final int minAmount;
        private final int maxAmount;
        private final int xp;
        private final String deathCondition; // player | environment | other | none

        public CustomDrop(String materialName, double chance, int minAmount, int maxAmount, int xp, String deathCondition) {
            this.materialName = materialName;
            this.chance = chance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.xp = xp;
            this.deathCondition = deathCondition;
        }

        public String getMaterialName() { return materialName; }
        public double getChance() { return chance; }
        public int getMinAmount() { return minAmount; }
        public int getMaxAmount() { return maxAmount; }
        public int getXp() { return xp; }
        public String getDeathCondition() { return deathCondition; }
    }

    public static class SoundConfig {
        private final String soundKey;
        private final float volume;
        private final float pitch;

        public SoundConfig(String soundKey, float volume, float pitch) {
            this.soundKey = soundKey;
            this.volume = volume;
            this.pitch = pitch;
        }

        public String getSoundKey() { return soundKey; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
    }
}
