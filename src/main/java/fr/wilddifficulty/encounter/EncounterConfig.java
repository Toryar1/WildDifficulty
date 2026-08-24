package fr.wilddifficulty.encounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration complète des paramètres d'un Encounter pour une zone.
 */
public class EncounterConfig {

    private EncounterType type = EncounterType.NONE;
    private boolean enabled = true;
    private int cooldownSeconds = 1800; // 30 minutes par défaut
    private int minPlayers = 1;
    private int maxPlayers = 10;

    // BASE_RAID options
    private String raidMode = "CUSTOM_RAID_MODE"; // VANILLA_RAID_MODE | CUSTOM_RAID_MODE
    private int raidWaveCount = 5;
    private boolean protectNpc = false;
    private String protectNpcName = "Village Elder";

    // TRIAL_BUNKER options
    private String trialMode = "INTERNAL_SIMULATION"; // VANILLA_TRIAL_SPAWNERS | INTERNAL_SIMULATION
    private double trialActivationRadius = 14.0;
    private int trialMobCap = 8;
    private int trialTotalMobs = 20;
    private double trialScalingPerPlayer = 0.5; // +50% de mobs par joueur additionnel

    // OUTPOST options
    private double outpostPatrolIntervalSeconds = 60.0;
    private double outpostLingeringInvasionTimeSeconds = 120.0;
    private double outpostCaptainChance = 0.50;

    // RUINS options
    private double ruinsTriggerRadius = 12.0;
    private boolean ruinsTriggerOnChestOpen = true;

    // FX & BossBar
    private boolean bossBarEnabled = true;
    private String bossBarColor = "RED"; // BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
    private String bossBarStyle = "NOTCHED_10"; // PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20

    // Vagues & Récompenses & Marqueurs
    private List<EncounterWave> waves = new ArrayList<>();
    private EncounterReward rewards = new EncounterReward();
    private List<double[]> spawnMarkers = new ArrayList<>();

    // Conditions de Défaite & Objectif IA
    private String defeatCondition = "TIMEOUT_OUTSIDE_ZONE"; // TIMEOUT_OUTSIDE_ZONE | ALL_PLAYERS_DEAD | TIME_LIMIT
    private int playerLeaveGracePeriodSeconds = 30;
    private String mobObjective = "TARGET_PLAYERS"; // TARGET_PLAYERS | ATTACK_CENTER_POINT | KILL_VILLAGERS_AND_NPCS | DEFEND_ZONE

    public EncounterConfig() {}

    public String getDefeatCondition() { return defeatCondition; }
    public void setDefeatCondition(String defeatCondition) { this.defeatCondition = defeatCondition; }

    public int getPlayerLeaveGracePeriodSeconds() { return playerLeaveGracePeriodSeconds; }
    public void setPlayerLeaveGracePeriodSeconds(int s) { this.playerLeaveGracePeriodSeconds = Math.max(0, s); }

    public String getMobObjective() { return mobObjective; }
    public void setMobObjective(String mobObjective) { this.mobObjective = mobObjective; }

    public List<double[]> getSpawnMarkers() { return spawnMarkers; }
    public void setSpawnMarkers(List<double[]> spawnMarkers) { this.spawnMarkers = spawnMarkers; }
    public void addSpawnMarker(double x, double y, double z) { this.spawnMarkers.add(new double[]{x, y, z}); }
    public void removeSpawnMarker(int index) {
        if (index >= 0 && index < spawnMarkers.size()) {
            spawnMarkers.remove(index);
        }
    }
    public void clearSpawnMarkers() { this.spawnMarkers.clear(); }

    public EncounterConfig(EncounterType type) {
        this.type = type;
    }

    public EncounterType getType() { return type; }
    public void setType(EncounterType type) { this.type = type; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public String getRaidMode() { return raidMode; }
    public void setRaidMode(String raidMode) { this.raidMode = raidMode; }

    public int getRaidWaveCount() { return raidWaveCount; }
    public void setRaidWaveCount(int raidWaveCount) { this.raidWaveCount = raidWaveCount; }

    public boolean isProtectNpc() { return protectNpc; }
    public void setProtectNpc(boolean protectNpc) { this.protectNpc = protectNpc; }

    public String getProtectNpcName() { return protectNpcName; }
    public void setProtectNpcName(String protectNpcName) { this.protectNpcName = protectNpcName; }

    public String getTrialMode() { return trialMode; }
    public void setTrialMode(String trialMode) { this.trialMode = trialMode; }

    public double getTrialActivationRadius() { return trialActivationRadius; }
    public void setTrialActivationRadius(double trialActivationRadius) { this.trialActivationRadius = trialActivationRadius; }

    public int getTrialMobCap() { return trialMobCap; }
    public void setTrialMobCap(int trialMobCap) { this.trialMobCap = trialMobCap; }

    public int getTrialTotalMobs() { return trialTotalMobs; }
    public void setTrialTotalMobs(int trialTotalMobs) { this.trialTotalMobs = trialTotalMobs; }

    public double getTrialScalingPerPlayer() { return trialScalingPerPlayer; }
    public void setTrialScalingPerPlayer(double trialScalingPerPlayer) { this.trialScalingPerPlayer = trialScalingPerPlayer; }

    public double getOutpostPatrolIntervalSeconds() { return outpostPatrolIntervalSeconds; }
    public void setOutpostPatrolIntervalSeconds(double s) { this.outpostPatrolIntervalSeconds = s; }

    public double getOutpostLingeringInvasionTimeSeconds() { return outpostLingeringInvasionTimeSeconds; }
    public void setOutpostLingeringInvasionTimeSeconds(double s) { this.outpostLingeringInvasionTimeSeconds = s; }

    public double getOutpostCaptainChance() { return outpostCaptainChance; }
    public void setOutpostCaptainChance(double c) { this.outpostCaptainChance = c; }

    public double getRuinsTriggerRadius() { return ruinsTriggerRadius; }
    public void setRuinsTriggerRadius(double r) { this.ruinsTriggerRadius = r; }

    public boolean isRuinsTriggerOnChestOpen() { return ruinsTriggerOnChestOpen; }
    public void setRuinsTriggerOnChestOpen(boolean b) { this.ruinsTriggerOnChestOpen = b; }

    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public void setBossBarEnabled(boolean b) { this.bossBarEnabled = b; }

    public String getBossBarColor() { return bossBarColor; }
    public void setBossBarColor(String c) { this.bossBarColor = c; }

    public String getBossBarStyle() { return bossBarStyle; }
    public void setBossBarStyle(String s) { this.bossBarStyle = s; }

    public List<EncounterWave> getWaves() { return waves; }
    public void setWaves(List<EncounterWave> waves) { this.waves = waves; }

    public EncounterReward getRewards() { return rewards; }
    public void setRewards(EncounterReward rewards) { this.rewards = rewards; }
}
