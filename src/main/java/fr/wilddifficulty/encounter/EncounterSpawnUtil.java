package fr.wilddifficulty.encounter;

import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Random;

public class EncounterSpawnUtil {

    private static final Random random = new Random();

    public static Location getSafeSpawnLocation(DifficultyZone zone, Location fallbackCenter, EncounterWave wave, EncounterConfig config) {
        World w = fallbackCenter.getWorld();
        if (w == null) return fallbackCenter;

        // 1. Priorité aux Marqueurs de spawn si configurés
        if (config != null && !config.getSpawnMarkers().isEmpty()) {
            if (wave == null || "MARKERS".equalsIgnoreCase(wave.getSpawnDistribution()) || random.nextDouble() < 0.75) {
                double[] m = config.getSpawnMarkers().get(random.nextInt(config.getSpawnMarkers().size()));
                return new Location(w, m[0], m[1], m[2]);
            }
        }

        // 2. Distribution RANDOM_ZONE
        if (wave != null && "RANDOM_ZONE".equalsIgnoreCase(wave.getSpawnDistribution())) {
            double rx = zone.getMinX() + random.nextDouble() * Math.max(1.0, zone.getMaxX() - zone.getMinX());
            double rz = zone.getMinZ() + random.nextDouble() * Math.max(1.0, zone.getMaxZ() - zone.getMinZ());
            double targetY = (fallbackCenter.getY() >= -64 && fallbackCenter.getY() <= 320) ? fallbackCenter.getY() : zone.getCenterY();
            return findSafeGround(w, rx, targetY, rz, zone);
        }

        // 3. Distribution AROUND_CENTER (autour du centre / joueur)
        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = 3 + random.nextDouble() * 8;
        double x = fallbackCenter.getX() + Math.cos(angle) * dist;
        double z = fallbackCenter.getZ() + Math.sin(angle) * dist;
        double targetY = fallbackCenter.getY();

        return findSafeGround(w, x, targetY, z, zone);
    }

    public static Location findSafeGround(World w, double x, double startY, double z, DifficultyZone zone) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int sy = (int) Math.floor(startY);

        // Scan en partant de startY vers le bas sur 25 blocs et vers le haut sur 6 blocs
        for (int y = Math.min(w.getMaxHeight() - 2, sy + 4); y >= Math.max(w.getMinHeight() + 1, sy - 25); y--) {
            Block ground = w.getBlockAt(bx, y - 1, bz);
            Block feet = w.getBlockAt(bx, y, bz);
            Block head = w.getBlockAt(bx, y + 1, bz);

            if (ground.getType().isSolid() && !ground.isLiquid() && feet.isEmpty() && head.isEmpty()) {
                return new Location(w, bx + 0.5, y, bz + 0.5);
            }
        }

        // Si non trouvé vers le bas, scan vers le haut
        for (int y = sy + 5; y <= Math.min(w.getMaxHeight() - 2, sy + 20); y++) {
            Block ground = w.getBlockAt(bx, y - 1, bz);
            Block feet = w.getBlockAt(bx, y, bz);
            Block head = w.getBlockAt(bx, y + 1, bz);

            if (ground.getType().isSolid() && !ground.isLiquid() && feet.isEmpty() && head.isEmpty()) {
                return new Location(w, bx + 0.5, y, bz + 0.5);
            }
        }

        return new Location(w, bx + 0.5, startY, bz + 0.5);
    }
}
