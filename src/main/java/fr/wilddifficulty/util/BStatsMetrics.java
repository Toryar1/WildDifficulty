package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

/**
 * Intègre bStats pour les métriques de WildDifficulty.
 * Integrates bStats metrics for WildDifficulty.
 *
 * Page bStats (à configurer après publication) :
 * https://bstats.org/plugin/bukkit/WildDifficulty/<PLUGIN_ID>
 *
 * Les métriques sont anonymes et respectueuses de la vie privée.
 * Les administrateurs peuvent les désactiver dans plugins/bStats/config.yml.
 */
public class BStatsMetrics {

    // Remplacer par l'ID bStats réel après l'enregistrement du plugin
    // Replace with the actual bStats plugin ID after registration
    private static final int BSTATS_PLUGIN_ID = 1; // TODO: set after registration at bstats.org

    public static void register(WildDifficultyPlugin plugin) {
        try {
            Metrics metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

            // Langue configurée
            metrics.addCustomChart(new SimplePie("language", () ->
                plugin.getConfig().getString("plugin.language", "fr")
            ));

            // Blood Moon activé ?
            metrics.addCustomChart(new SimplePie("bloodmoon_enabled", () ->
                plugin.getConfig().getBoolean("bloodmoon.actif", false) ? "Activé" : "Désactivé"
            ));

            // Scaling de distance activé ?
            metrics.addCustomChart(new SimplePie("distance_scaling", () ->
                plugin.getConfig().getBoolean("scaling-distance.actif", true) ? "Activé" : "Désactivé"
            ));

            // Nombre de variantes chargées
            metrics.addCustomChart(new SingleLineChart("loaded_variants", () ->
                plugin.getVariantManager() != null ? plugin.getVariantManager().getAllVariants().size() : 0
            ));

            // Nombre de zones chargées
            metrics.addCustomChart(new SingleLineChart("loaded_zones", () ->
                plugin.getZoneManager() != null ? plugin.getZoneManager().getAllZones().size() : 0
            ));

        } catch (Exception e) {
            plugin.getLogger().warning("[bStats] Impossible d'initialiser les métriques : " + e.getMessage());
        }
    }
}
