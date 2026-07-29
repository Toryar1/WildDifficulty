package fr.wilddifficulty.player;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSettingsManager {

    private final WildDifficultyPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, PlayerData> players = new HashMap<>();

    public PlayerSettingsManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    public void load() {
        players.clear();
        if (!file.exists()) {
            try {
                plugin.saveResource("players.yml", false);
            } catch (Exception ignored) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().warning("Impossible de créer le fichier players.yml: " + e.getMessage());
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.isConfigurationSection("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = config.getString("players." + key + ".name", "Unknown");
                    PlayerData pd = new PlayerData(uuid, name);
                    pd.setDifficultyLevel(config.getString("players." + key + ".difficulty", "NORMAL"));
                    pd.setThirstLevel(config.getInt("players." + key + ".thirst", 20));
                    pd.setThirstEnabled(config.getBoolean("players." + key + ".thirst-enabled", true));
                    pd.setHardcoreEnabled(config.getBoolean("players." + key + ".hardcore-enabled", false));
                    pd.setHardcoreNoRegen(config.getBoolean("players." + key + ".hardcore-no-regen", true));
                    pd.setHardcoreAllowGoldenApples(config.getBoolean("players." + key + ".hardcore-allow-gapple", true));
                    pd.setHardcoreAllowPotions(config.getBoolean("players." + key + ".hardcore-allow-potions", true));
                    pd.setHardcoreAllowSafezoneRegen(config.getBoolean("players." + key + ".hardcore-allow-safezone-regen", true));
                    pd.setHardcoreInstantDeathDespawn(config.getBoolean("players." + key + ".hardcore-instant-death-despawn", false));
                    players.put(uuid, pd);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        if (config == null) return;
        config.set("players", null);
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerData pd = entry.getValue();
            config.set(path + ".name", pd.getPlayerName());
            config.set(path + ".difficulty", pd.getDifficultyLevel());
            config.set(path + ".thirst", pd.getThirstLevel());
            config.set(path + ".thirst-enabled", pd.isThirstEnabled());
            config.set(path + ".hardcore-enabled", pd.isHardcoreEnabled());
            config.set(path + ".hardcore-no-regen", pd.isHardcoreNoRegen());
            config.set(path + ".hardcore-allow-gapple", pd.isHardcoreAllowGoldenApples());
            config.set(path + ".hardcore-allow-potions", pd.isHardcoreAllowPotions());
            config.set(path + ".hardcore-allow-safezone-regen", pd.isHardcoreAllowSafezoneRegen());
            config.set(path + ".hardcore-instant-death-despawn", pd.isHardcoreInstantDeathDespawn());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder players.yml: " + e.getMessage());
        }
    }

    public PlayerData getPlayerData(UUID uuid, String name) {
        return players.computeIfAbsent(uuid, k -> new PlayerData(k, name));
    }
}
