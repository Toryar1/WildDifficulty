package fr.wilddifficulty.hook;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.player.PlayerData;
import fr.wilddifficulty.zone.DifficultyZone;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Intégration PlaceholderAPI pour WildDifficulty.
 * Provides PlaceholderAPI support for WildDifficulty.
 *
 * Placeholders disponibles / Available placeholders:
 *   %wilddifficulty_thirst%         → niveau de soif (0-20)
 *   %wilddifficulty_thirst_pct%     → pourcentage de soif (0-100)
 *   %wilddifficulty_zone%           → identifiant de la zone actuelle ou "none"
 *   %wilddifficulty_zone_id%        → identifiant de la zone ou "none"
 *   %wilddifficulty_bloodmoon%      → "true" / "false"
 *   %wilddifficulty_difficulty_mult%→ multiplicateur de distance actuel
 *   %wilddifficulty_hardcore%       → "true" / "false"
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final WildDifficultyPlugin plugin;

    public PlaceholderAPIHook(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wilddifficulty";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Toryar1";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep registered through reloads
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        switch (identifier) {
            case "thirst" -> {
                PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
                return String.valueOf(pd != null ? pd.getThirstLevel() : 20);
            }
            case "thirst_pct" -> {
                PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
                int thirst = pd != null ? pd.getThirstLevel() : 20;
                return String.valueOf((int) Math.round(thirst / 20.0 * 100));
            }
            case "zone", "zone_id" -> {
                DifficultyZone zone = plugin.getZoneManager().getZoneAt(
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ()
                );
                return zone != null ? zone.getId() : "none";
            }
            case "bloodmoon" -> {
                boolean active = plugin.getMainConfigManager().isBloodMoonActive();
                return active ? "true" : "false";
            }
            case "difficulty_mult" -> {
                double mult = plugin.getMainConfigManager().computeDistanceMultiplier(
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getZ()
                );
                return String.format("%.2f", mult);
            }
            case "hardcore" -> {
                PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
                return (pd != null && pd.isHardcoreEnabled()) ? "true" : "false";
            }
            default -> { return null; }
        }
    }
}
