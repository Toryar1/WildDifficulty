package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.MainConfigManager;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public final class ScoreboardUtil {

    private ScoreboardUtil() {}

    public static void updateScoreboard(WildDifficultyPlugin plugin, Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("wd_debug", "dummy", "§6§lWildDifficulty");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        DifficultyZone zone = plugin.getZoneManager().getZoneAt(worldName, loc.getX(), loc.getY(), loc.getZ());
        boolean isInside = true;
        if (zone == null) {
            zone = plugin.getZoneManager().getExternalScalingZone(worldName, loc.getX(), loc.getZ());
            isInside = false;
        }

        // Get distance scaling mult
        MainConfigManager mainCfg = plugin.getMainConfigManager();
        double distMult = mainCfg.computeDistanceMultiplier(worldName, loc.getX(), loc.getZ());

        String zoneId = "§7Aucune";
        String zoneType = "§7N/A";
        String distCenterStr = "§7N/A";
        String radiusStr = "§7N/A";
        String extMultStr = "§7N/A";

        if (zone != null) {
            zoneId = (isInside ? "§a" : "§7(Hors) §a") + zone.getId();
            zoneType = zone.isSafeZone() ? "§cSafeZone" : "§eStandard";
            zoneType += " §7(" + zone.getType().name() + ")";

            double dx = loc.getX() - zone.getCenterX();
            double dz = loc.getZ() - zone.getCenterZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            distCenterStr = "§b" + String.format("%.1f", distance) + "m";
            radiusStr = "§e" + String.format("%.1f", zone.getRadius()) + "m";

            double hpMult = 1.0;
            double dmgMult = 1.0;
            double spdMult = 1.0;
            if (distance > zone.getRadius() && zone.hasExtScaling()) {
                hpMult = zone.computeHpExtMult(distance);
                dmgMult = zone.computeDmgExtMult(distance);
                spdMult = zone.computeSpdExtMult(distance);
            }
            extMultStr = "§c❤" + String.format("%.1f", hpMult) + " §6⚔" + String.format("%.1f", dmgMult) + " §b⚡" + String.format("%.1f", spdMult);
        }

        // Set scoreboard lines
        int score = 10;
        objective.getScore("§7-------------------").setScore(score--);
        objective.getScore("§fZone: " + zoneId).setScore(score--);
        objective.getScore("§fType: " + zoneType).setScore(score--);
        objective.getScore("§fDist. Centre: " + distCenterStr).setScore(score--);
        objective.getScore("§fRayon Zone: " + radiusStr).setScore(score--);
        objective.getScore("§fMult. Extérieur: " + extMultStr).setScore(score--);
        objective.getScore("§fMult. Distance: §c" + String.format("%.2f", distMult) + "x").setScore(score--);
        objective.getScore("§fCoord: §7" + loc.getBlockX() + ", " + loc.getBlockZ()).setScore(score--);
        objective.getScore("§a").setScore(score--);
        objective.getScore("§7--------------------").setScore(score--);

        player.setScoreboard(board);
    }

    public static void clearScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
