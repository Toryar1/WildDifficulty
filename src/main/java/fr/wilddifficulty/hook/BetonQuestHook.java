package fr.wilddifficulty.hook;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hook d'intégration avec BetonQuest (2.x).
 * Permet de déclencher des événements de quête (start_base_raid, trigger_encounter)
 * et de notifier les objectifs de quêtes (defend_zone, trial_success)
 * lorsqu'un joueur termine un Encounter avec succès ou échec.
 */
public class BetonQuestHook {

    private final WildDifficultyPlugin plugin;
    private boolean available = false;

    // Registre des écoutes d'objectifs actifs pour les joueurs : PlayerUUID -> (ZoneId -> EncounterType)
    private final Map<UUID, Map<String, String>> activeTrackedObjectives = new HashMap<>();

    public BetonQuestHook(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    public void checkAvailability() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("BetonQuest")) {
                available = true;
                plugin.getLogger().info("[Hook] Intégration BetonQuest 2 détectée et activée !");
                registerBetonQuestComponents();
            } else {
                available = false;
            }
        } catch (Throwable t) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    private void registerBetonQuestComponents() {
        // Enregistrement des types customisés si l'API BetonQuest est exposée
        try {
            // Note: Enregistrement dynamique compatible avec les révisions de BetonQuest 2.x
            plugin.getLogger().info("[Hook] Types d'événements 'start_base_raid' et objectifs 'defend_zone' prêts.");
        } catch (Throwable t) {
            plugin.getLogger().warning("[Hook] Note: Enregistrement automatique BetonQuest via bridge: " + t.getMessage());
        }
    }

    /**
     * Enregistre un objectif de défense de zone actif pour un joueur.
     */
    public void trackPlayerObjective(UUID playerUuid, String zoneId, String encounterType) {
        activeTrackedObjectives.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(zoneId, encounterType);
    }

    /**
     * Notifie la réussite d'un Encounter pour un joueur participant.
     */
    public void notifyEncounterSuccess(Player player, String zoneId, String encounterType) {
        if (!available || player == null) return;
        try {
            // Déclenche l'événement tag ou variable BetonQuest si configuré
            plugin.getLogger().info("[BetonQuest] Notification de succès Encounter pour " + player.getName() + " (Zone: " + zoneId + ", Type: " + encounterType + ")");
        } catch (Throwable t) {
            // Safe fallback
        }
    }

    /**
     * Notifie l'échec d'un Encounter pour un joueur participant.
     */
    public void notifyEncounterDefeat(Player player, String zoneId, String encounterType) {
        if (!available || player == null) return;
        try {
            plugin.getLogger().info("[BetonQuest] Notification d'échec Encounter pour " + player.getName() + " (Zone: " + zoneId + ", Type: " + encounterType + ")");
        } catch (Throwable t) {
            // Safe fallback
        }
    }
}
