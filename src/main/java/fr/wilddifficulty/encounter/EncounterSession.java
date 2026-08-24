package fr.wilddifficulty.encounter;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Session active représentant le déroulement d'un Encounter dans une zone.
 */
public class EncounterSession {

    private final WildDifficultyPlugin plugin;
    private final DifficultyZone zone;
    private final EncounterConfig config;

    private EncounterStatus status = EncounterStatus.ACTIVE;
    private int currentWaveIndex = 0;
    private int waveCountdownSeconds = 0;
    private long startTimeMillis = System.currentTimeMillis();
    private long cooldownEndTimeMillis = 0;
    private int ticksElapsed = 0;

    // Statistiques & mobs vivants
    private final Set<UUID> aliveMobUuids = new HashSet<>();
    private final Set<UUID> participatingPlayerUuids = new HashSet<>();

    // Variables spécifiques pour Trial / Outpost / Raid
    private int trialTotalSpawned = 0;
    private double lingeringSeconds = 0;
    private int badOmenLevel = 0;
    private int totalWaveCount = 0;

    // BossBar
    private BossBar bossBar;

    public EncounterSession(WildDifficultyPlugin plugin, DifficultyZone zone, EncounterConfig config) {
        this.plugin = plugin;
        this.zone = zone;
        this.config = config;

        if (config.isBossBarEnabled()) {
            initBossBar();
        }
    }

    private void initBossBar() {
        try {
            BarColor color = BarColor.valueOf(config.getBossBarColor().toUpperCase());
            BarStyle style = BarStyle.valueOf(config.getBossBarStyle().toUpperCase());
            String title = ChatColor.translateAlternateColorCodes('&', "&6⚔ " + config.getType().getDisplayName() + " &8[&e" + zone.getId() + "&8]");
            this.bossBar = Bukkit.createBossBar(title, color, style);
            this.bossBar.setProgress(1.0);
        } catch (Throwable t) {
            this.bossBar = Bukkit.createBossBar("⚔ " + config.getType().getDisplayName(), BarColor.RED, BarStyle.SOLID);
        }
    }

    public void updateBossBar(String title, double progress) {
        if (bossBar != null) {
            if (title != null) {
                bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
            }
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
    }

    public void addParticipant(Player player) {
        if (player == null) return;
        participatingPlayerUuids.add(player.getUniqueId());
        if (bossBar != null && !bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
    }

    public void removeParticipant(Player player) {
        if (player == null) return;
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    public void syncBossBarPlayers() {
        if (bossBar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (zone.contains(p.getLocation())) {
                addParticipant(p);
            } else {
                bossBar.removePlayer(p);
            }
        }
    }

    public void cleanup() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        aliveMobUuids.clear();
    }

    public DifficultyZone getZone() { return zone; }
    public EncounterConfig getConfig() { return config; }
    public EncounterStatus getStatus() { return status; }
    public void setStatus(EncounterStatus status) { this.status = status; }

    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public void setCurrentWaveIndex(int currentWaveIndex) { this.currentWaveIndex = currentWaveIndex; }

    public int getWaveCountdownSeconds() { return waveCountdownSeconds; }
    public void setWaveCountdownSeconds(int waveCountdownSeconds) { this.waveCountdownSeconds = waveCountdownSeconds; }

    public long getStartTimeMillis() { return startTimeMillis; }
    public long getCooldownEndTimeMillis() { return cooldownEndTimeMillis; }
    public void setCooldownEndTimeMillis(long time) { this.cooldownEndTimeMillis = time; }

    public int getTicksElapsed() { return ticksElapsed; }
    public void incrementTicks() { this.ticksElapsed++; }

    public Set<UUID> getAliveMobUuids() { return aliveMobUuids; }
    public Set<UUID> getParticipatingPlayerUuids() { return participatingPlayerUuids; }

    public int getTrialTotalSpawned() { return trialTotalSpawned; }
    public void setTrialTotalSpawned(int trialTotalSpawned) { this.trialTotalSpawned = trialTotalSpawned; }
    public void incrementTrialSpawned(int amount) { this.trialTotalSpawned += amount; }

    public double getLingeringSeconds() { return lingeringSeconds; }
    public void setLingeringSeconds(double lingeringSeconds) { this.lingeringSeconds = lingeringSeconds; }
    public void addLingeringSeconds(double s) { this.lingeringSeconds += s; }

    public int getBadOmenLevel() { return badOmenLevel; }
    public void setBadOmenLevel(int badOmenLevel) { this.badOmenLevel = badOmenLevel; }

    public int getTotalWaveCount() { return totalWaveCount; }
    public void setTotalWaveCount(int totalWaveCount) { this.totalWaveCount = totalWaveCount; }

    private int secondsOutsideZone = 0;

    public int getSecondsOutsideZone() { return secondsOutsideZone; }
    public void incrementSecondsOutsideZone() { this.secondsOutsideZone++; }
    public void resetSecondsOutsideZone() { this.secondsOutsideZone = 0; }

    public BossBar getBossBar() { return bossBar; }
}
