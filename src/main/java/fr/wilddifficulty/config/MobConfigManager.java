package fr.wilddifficulty.config;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de mobs.yml.
 *
 * Cache en mémoire par type de mob :
 * - canSpawnDaytime : autorisé à spawner de jour
 * - ignoreSunlight  : immunité à la combustion solaire
 * - mobModifiers    : surcharge de stats propre à ce type de mob
 */
public class MobConfigManager {

    private final WildDifficultyPlugin plugin;

    // Clé = EntityType.name() en majuscules
    private final Map<String, Boolean> canSpawnDaytime = new HashMap<>();
    private final Map<String, Boolean> ignoreSunlight  = new HashMap<>();
    private final Map<String, StatModifiers> mobModifiers = new HashMap<>();

    public MobConfigManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge ou recharge mobs.yml.
     */
    public void load() {
        canSpawnDaytime.clear();
        ignoreSunlight.clear();
        mobModifiers.clear();

        File file = new File(plugin.getDataFolder(), "mobs.yml");
        if (!file.exists()) {
            plugin.getLogger().warning("[Mobs] mobs.yml introuvable, aucune règle de mob chargée.");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        for (String mobKey : cfg.getKeys(false)) {
            String upperKey = mobKey.toUpperCase();
            ConfigurationSection section = cfg.getConfigurationSection(mobKey);
            if (section == null) continue;

            canSpawnDaytime.put(upperKey, section.getBoolean("peut-spawner-de-jour", false));
            ignoreSunlight.put(upperKey,  section.getBoolean("ignore-soleil", false));

            ConfigurationSection modSection = section.getConfigurationSection("modificateurs");
            if (modSection != null) {
                mobModifiers.put(upperKey, BiomeConfigManager.readModifiers(modSection));
            }

            count++;
        }

        plugin.getLogger().info("[Mobs] " + count + " type(s) de mob configuré(s) depuis mobs.yml.");
    }

    // ── API publique ─────────────────────────────────────────

    /**
     * Ce mob peut-il spawner de jour ?
     * Prend en compte la surcharge globale de config.yml.
     */
    public boolean canSpawnDaytime(String mobKey) {
        if (WildDifficultyPlugin.getInstance().getMainConfigManager().isAllowDaySpawnGlobally()) return true;
        return canSpawnDaytime.getOrDefault(mobKey.toUpperCase(), false);
    }

    /**
     * Ce mob est-il immunisé contre la combustion solaire ?
     */
    public boolean ignoreSunlight(String mobKey) {
        if (WildDifficultyPlugin.getInstance().getMainConfigManager().isDisableBurningGlobally()) return true;
        return ignoreSunlight.getOrDefault(mobKey.toUpperCase(), false);
    }

    /**
     * Retourne le StatModifiers du mob (ou null si non défini).
     */
    public StatModifiers getMobModifiers(String mobKey) {
        return mobModifiers.get(mobKey.toUpperCase());
    }

    /**
     * Indique si ce mob a des règles spécifiques (spawn de jour ou soleil).
     */
    public boolean hasMobRules(String mobKey) {
        String key = mobKey.toUpperCase();
        return canSpawnDaytime.containsKey(key) || ignoreSunlight.containsKey(key);
    }
}
