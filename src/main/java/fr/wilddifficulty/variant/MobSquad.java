package fr.wilddifficulty.variant;

import java.util.List;
import java.util.Map;

public class MobSquad {

    private final String id;
    private List<String> triggerTypes; 
    private double spawnChance;
    private List<String> allowedBiomes;
    private List<String> allowedZones;
    private Map<String, SquadMemberRange> members;

    private double bonusHealth = 1.0;
    private double bonusDamage = 1.0;
    private double bonusSpeed = 1.0;
    private double bonusRegen = 0.0;

    public MobSquad(String id, List<String> triggerTypes, double spawnChance, List<String> allowedBiomes, List<String> allowedZones, Map<String, SquadMemberRange> members) {
        this.id = id;
        this.triggerTypes = triggerTypes;
        this.spawnChance = spawnChance;
        this.allowedBiomes = allowedBiomes;
        this.allowedZones = allowedZones;
        this.members = members;
    }

    public String getId() { return id; }
    public List<String> getTriggerTypes() { return triggerTypes; }
    public void setTriggerTypes(List<String> triggerTypes) { this.triggerTypes = triggerTypes; }
    public double getSpawnChance() { return spawnChance; }
    public void setSpawnChance(double spawnChance) { this.spawnChance = spawnChance; }
    public List<String> getAllowedBiomes() { return allowedBiomes; }
    public void setAllowedBiomes(List<String> allowedBiomes) { this.allowedBiomes = allowedBiomes; }
    public List<String> getAllowedZones() { return allowedZones; }
    public void setAllowedZones(List<String> allowedZones) { this.allowedZones = allowedZones; }
    public Map<String, SquadMemberRange> getMembers() { return members; }
    public void setMembers(Map<String, SquadMemberRange> members) { this.members = members; }

    public double getBonusHealth() { return bonusHealth; }
    public void setBonusHealth(double v) { this.bonusHealth = v; }
    public double getBonusDamage() { return bonusDamage; }
    public void setBonusDamage(double v) { this.bonusDamage = v; }
    public double getBonusSpeed() { return bonusSpeed; }
    public void setBonusSpeed(double v) { this.bonusSpeed = v; }
    public double getBonusRegen() { return bonusRegen; }
    public void setBonusRegen(double v) { this.bonusRegen = v; }

    public static class SquadMemberRange {
        private int min;
        private int max;

        public SquadMemberRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public int getMin() { return min; }
        public void setMin(int min) { this.min = min; }
        public int getMax() { return max; }
        public void setMax(int max) { this.max = max; }
    }
}
