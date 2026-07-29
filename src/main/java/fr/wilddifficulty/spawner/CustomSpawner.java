package fr.wilddifficulty.spawner;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class CustomSpawner {

    private final Location location;
    private boolean active = true;
    private int interval = 10; // secondes
    private int radius = 16;
    private int maxNearby = 6;
    private int intervalRange = 0;
    private String particleType = "SMOKE";
    private String soundType = "BLOCK_NOTE_BLOCK_PLING";
    private int spawnRange = 4;

    private final Map<String, Integer> variantWeights = new HashMap<>();

    public CustomSpawner(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = Math.max(1, interval);
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = Math.max(1, radius);
    }

    public int getMaxNearby() {
        return maxNearby;
    }

    public void setMaxNearby(int maxNearby) {
        this.maxNearby = Math.max(1, maxNearby);
    }

    public Map<String, Integer> getVariantWeights() {
        return variantWeights;
    }

    public int getIntervalRange() {
        return intervalRange;
    }

    public void setIntervalRange(int intervalRange) {
        this.intervalRange = Math.max(0, intervalRange);
    }

    public String getParticleType() {
        return particleType;
    }

    public void setParticleType(String particleType) {
        this.particleType = (particleType == null) ? "SMOKE" : particleType;
    }

    public String getSoundType() {
        return soundType;
    }

    public void setSoundType(String soundType) {
        this.soundType = (soundType == null) ? "BLOCK_NOTE_BLOCK_PLING" : soundType;
    }

    public int getSpawnRange() {
        return spawnRange;
    }

    public void setSpawnRange(int spawnRange) {
        this.spawnRange = Math.max(0, spawnRange);
    }

    public void setVariantWeights(Map<String, Integer> weights) {
        this.variantWeights.clear();
        if (weights != null) {
            this.variantWeights.putAll(weights);
        }
    }

    @Override
    public CustomSpawner clone() {
        CustomSpawner clone = new CustomSpawner(this.location);
        clone.active = this.active;
        clone.interval = this.interval;
        clone.radius = this.radius;
        clone.maxNearby = this.maxNearby;
        clone.intervalRange = this.intervalRange;
        clone.particleType = this.particleType;
        clone.soundType = this.soundType;
        clone.spawnRange = this.spawnRange;
        clone.variantWeights.putAll(this.variantWeights);
        return clone;
    }
}
