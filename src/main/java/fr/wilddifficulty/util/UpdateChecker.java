package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Vérifie les mises à jour du plugin depuis SpigotMC.
 * Checks for plugin updates from SpigotMC.
 */
public class UpdateChecker {

    // Remplacer par l'ID Spigot du plugin une fois publié
    // Replace with the actual Spigot resource ID once published
    private static final int SPIGOT_RESOURCE_ID = 0; // TODO: set after publication

    private final WildDifficultyPlugin plugin;
    private String latestVersion = null;
    private boolean updateAvailable = false;

    public UpdateChecker(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Lance la vérification de mise à jour de manière asynchrone.
     */
    public void checkAsync() {
        if (SPIGOT_RESOURCE_ID == 0) return; // Resource ID not set yet
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + SPIGOT_RESOURCE_ID);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    latestVersion = reader.readLine().trim();
                }

                String currentVersion = plugin.getDescription().getVersion();
                updateAvailable = !currentVersion.equalsIgnoreCase(latestVersion);

                if (updateAvailable) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().warning("╔══════════════════════════════════════════╗");
                        plugin.getLogger().warning("║  WildDifficulty - Mise à jour disponible ║");
                        plugin.getLogger().warning("║  Version actuelle : " + currentVersion + "                 ║");
                        plugin.getLogger().warning("║  Nouvelle version : " + latestVersion + "                 ║");
                        plugin.getLogger().warning("║  https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID + "  ║");
                        plugin.getLogger().warning("╚══════════════════════════════════════════╝");
                    });
                }
            } catch (Exception e) {
                if (plugin.getMainConfigManager().isDebug()) {
                    plugin.getLogger().warning("[UpdateChecker] Impossible de vérifier les mises à jour : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Notifie un joueur si une mise à jour est disponible (appelé onJoin pour les admins).
     */
    public void notifyPlayer(org.bukkit.entity.Player player) {
        if (!updateAvailable || latestVersion == null) return;
        if (!player.hasPermission("wilddifficulty.admin")) return;
        player.sendMessage("§8[§6WildDifficulty§8] §eUne mise à jour est disponible : §a" + latestVersion
                + " §7(actuelle : §c" + plugin.getDescription().getVersion() + "§7)");
        player.sendMessage("§8[§6WildDifficulty§8] §7Téléchargez-la sur §bhttps://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID);
    }

    public boolean isUpdateAvailable() { return updateAvailable; }
    public String getLatestVersion() { return latestVersion; }
}
