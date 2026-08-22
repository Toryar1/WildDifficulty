package fr.wilddifficulty.hook;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

/**
 * Hook d'intégration avec Iris World Generator (VolmitSoftware).
 * Permet de détecter les mondes, biomes et structures générés par Iris
 * pour faciliter la création automatique de zones d'Encounter.
 */
public class IrisHook {

    private final WildDifficultyPlugin plugin;
    private boolean available = false;

    public IrisHook(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    public void checkAvailability() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Iris")) {
                available = true;
                plugin.getLogger().info("[Hook] Intégration Iris World Generator détectée et activée !");
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

    /**
     * Vérifie si un monde donné est généré par Iris.
     */
    public boolean isIrisWorld(World world) {
        if (!available || world == null) return false;
        try {
            // Vérification par generator
            if (world.getGenerator() != null && world.getGenerator().getClass().getName().toLowerCase().contains("iris")) {
                return true;
            }
            // Vérification par nom de dimension Iris ou pack
            return world.getName().toLowerCase().contains("iris");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Tente de déterminer le nom du biome ou de la structure Iris à la position donnée.
     */
    public String getIrisBiomeOrStructure(Location loc) {
        if (loc == null || loc.getWorld() == null) return "UNKNOWN";
        try {
            Biome biome = loc.getBlock().getBiome();
            return biome.name();
        } catch (Throwable t) {
            return "UNKNOWN";
        }
    }
}
