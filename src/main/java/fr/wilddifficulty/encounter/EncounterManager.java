package fr.wilddifficulty.encounter;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.encounter.mechanic.*;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Gestionnaire central du moteur d'Encounters de WildDifficulty.
 * Pilote le cycle de vie, les vagues, les timers et les interactions pour chaque zone.
 */
public class EncounterManager {

    private final WildDifficultyPlugin plugin;

    private final Map<EncounterType, EncounterMechanic> mechanics = new HashMap<>();
    private final Map<String, EncounterSession> activeSessions = new HashMap<>();
    private final Map<String, Long> zoneCooldowns = new HashMap<>();

    private BukkitTask tickerTask;

    public EncounterManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        registerMechanics();
        startTicker();
    }

    private void registerMechanics() {
        mechanics.put(EncounterType.BASE_RAID, new BaseRaidMechanic(plugin));
        mechanics.put(EncounterType.TRIAL_BUNKER, new TrialBunkerMechanic(plugin));
        mechanics.put(EncounterType.OUTPOST, new OutpostMechanic(plugin));
        mechanics.put(EncounterType.RUINS, new RuinsMechanic(plugin));
    }

    private void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }

        // Ticker principal exécuté toutes les secondes (20 ticks)
        this.tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void tick() {
        long now = System.currentTimeMillis();

        // 1. Mise à jour des sessions actives
        Iterator<Map.Entry<String, EncounterSession>> it = activeSessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, EncounterSession> entry = it.next();
            EncounterSession session = entry.getValue();
            DifficultyZone zone = session.getZone();

            if (session.getStatus() == EncounterStatus.ACTIVE) {
                EncounterMechanic mechanic = mechanics.get(session.getConfig().getType());
                if (mechanic != null) {
                    mechanic.tick(zone, session);
                }
            } else if (session.getStatus() == EncounterStatus.COOLDOWN) {
                // Enregistrement du cooldown
                zoneCooldowns.put(zone.getId(), session.getCooldownEndTimeMillis());
                it.remove();
            }
        }

        // 2. Nettoyage des cooldowns expirés
        zoneCooldowns.entrySet().removeIf(entry -> now >= entry.getValue());

        // 3. Détection de proximité des joueurs pour les zones d'Encounter inactives
        for (Player player : Bukkit.getOnlinePlayers()) {
            DifficultyZone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
            if (zone != null && zone.getEncounterConfig() != null && zone.getEncounterConfig().getType() != EncounterType.NONE) {
                if (zone.getEncounterConfig().isEnabled() && !isEncounterActive(zone.getId()) && !isOnCooldown(zone.getId())) {
                    // Vérification de déclenchement automatique
                    EncounterType type = zone.getEncounterConfig().getType();
                    if (type == EncounterType.TRIAL_BUNKER || type == EncounterType.OUTPOST || type == EncounterType.RUINS) {
                        startEncounter(zone, Collections.singletonList(player));
                    }
                }
            }
        }
    }

    public boolean isEncounterActive(String zoneId) {
        EncounterSession session = activeSessions.get(zoneId);
        return session != null && session.getStatus() == EncounterStatus.ACTIVE;
    }

    public boolean isOnCooldown(String zoneId) {
        Long cooldownEnd = zoneCooldowns.get(zoneId);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldownSeconds(String zoneId) {
        Long cooldownEnd = zoneCooldowns.get(zoneId);
        if (cooldownEnd == null) return 0;
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    public boolean startEncounter(DifficultyZone zone, List<Player> players) {
        if (zone == null || zone.getEncounterConfig() == null || zone.getEncounterConfig().getType() == EncounterType.NONE) {
            return false;
        }

        if (isEncounterActive(zone.getId()) || isOnCooldown(zone.getId())) {
            return false;
        }

        EncounterConfig config = zone.getEncounterConfig();
        EncounterMechanic mechanic = mechanics.get(config.getType());
        if (mechanic == null) return false;

        EncounterSession session = new EncounterSession(plugin, zone, config);
        activeSessions.put(zone.getId(), session);

        mechanic.start(zone, players, session);
        return true;
    }

    public boolean forceStartEncounter(String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return false;

        // Reset du cooldown s'il existait
        zoneCooldowns.remove(zoneId);

        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (zone.contains(p.getLocation())) {
                nearbyPlayers.add(p);
            }
        }

        if (nearbyPlayers.isEmpty()) {
            // Si aucun joueur n'est dans la zone, on prend les joueurs du même monde
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().getName().equalsIgnoreCase(zone.getWorld())) {
                    nearbyPlayers.add(p);
                    break;
                }
            }
        }

        return startEncounter(zone, nearbyPlayers);
    }

    public boolean forceEndEncounter(String zoneId, boolean success) {
        EncounterSession session = activeSessions.get(zoneId);
        if (session == null) return false;

        EncounterMechanic mechanic = mechanics.get(session.getConfig().getType());
        if (mechanic != null) {
            mechanic.end(session.getZone(), session, success);
        }
        activeSessions.remove(zoneId);
        return true;
    }

    public void onMobDeath(LivingEntity entity, Player killer) {
        for (EncounterSession session : activeSessions.values()) {
            if (session.getAliveMobUuids().contains(entity.getUniqueId())) {
                EncounterMechanic mechanic = mechanics.get(session.getConfig().getType());
                if (mechanic != null) {
                    mechanic.onMobDeath(session.getZone(), session, entity, killer);
                }
                break;
            }
        }
    }

    public void onChestOpen(Player player, DifficultyZone zone) {
        if (zone == null || zone.getEncounterConfig() == null) return;
        EncounterConfig cfg = zone.getEncounterConfig();
        if (cfg.getType() == EncounterType.RUINS && cfg.isRuinsTriggerOnChestOpen()) {
            if (!isEncounterActive(zone.getId()) && !isOnCooldown(zone.getId())) {
                startEncounter(zone, Collections.singletonList(player));
            }
        }
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        for (EncounterSession session : activeSessions.values()) {
            session.cleanup();
        }
        activeSessions.clear();
        zoneCooldowns.clear();
    }

    public EncounterSession getActiveSession(String zoneId) {
        return activeSessions.get(zoneId);
    }

    public Map<String, EncounterSession> getActiveSessions() {
        return activeSessions;
    }
}
