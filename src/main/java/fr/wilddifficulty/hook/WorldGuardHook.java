package fr.wilddifficulty.hook;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Hook d'intégration avec WorldGuard 7.
 * Utilise la réflexion pour une compatibilité totale et sans crash
 * à travers toutes les versions de Minecraft et Paper.
 */
public class WorldGuardHook {

    private final WildDifficultyPlugin plugin;
    private boolean available = false;

    private Object worldGuardInstance;
    private Method getPlatformMethod;
    private Method getRegionContainerMethod;
    private Method adaptWorldMethod;
    private Method getRegionManagerMethod;
    private Method getRegionMethod;
    private Method getRegionsMapMethod;
    private Method containsPointMethod;
    private Method getMinPointMethod;
    private Method getMaxPointMethod;
    private Method getBlockXMethod;
    private Method getBlockYMethod;
    private Method getBlockZMethod;

    public WorldGuardHook(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    public void checkAvailability() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Method getInstanceMethod = wgClass.getMethod("getInstance");
                worldGuardInstance = getInstanceMethod.invoke(null);

                getPlatformMethod = wgClass.getMethod("getPlatform");
                Object platform = getPlatformMethod.invoke(worldGuardInstance);

                Class<?> platformClass = platform.getClass();
                getRegionContainerMethod = platformClass.getMethod("getRegionContainer");
                Object container = getRegionContainerMethod.invoke(platform);

                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                adaptWorldMethod = bukkitAdapterClass.getMethod("adapt", World.class);

                Class<?> containerClass = container.getClass();
                Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");
                getRegionManagerMethod = containerClass.getMethod("get", worldClass);

                Class<?> regionManagerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
                getRegionMethod = regionManagerClass.getMethod("getRegion", String.class);
                getRegionsMapMethod = regionManagerClass.getMethod("getRegions");

                Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
                Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");

                // contains(BlockVector3) or contains(int, int, int)
                try {
                    containsPointMethod = protectedRegionClass.getMethod("contains", int.class, int.class, int.class);
                } catch (NoSuchMethodException e) {
                    containsPointMethod = protectedRegionClass.getMethod("contains", blockVector3Class);
                }

                getMinPointMethod = protectedRegionClass.getMethod("getMinimumPoint");
                getMaxPointMethod = protectedRegionClass.getMethod("getMaximumPoint");

                getBlockXMethod = blockVector3Class.getMethod("getX");
                getBlockYMethod = blockVector3Class.getMethod("getY");
                getBlockZMethod = blockVector3Class.getMethod("getZ");

                available = true;
                plugin.getLogger().info("[Hook] Intégration WorldGuard 7 détectée et activée !");
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

    public boolean isLocationInRegion(Location loc, String regionName) {
        if (!available || loc == null || loc.getWorld() == null || regionName == null || regionName.isEmpty()) {
            return false;
        }
        try {
            Object platform = getPlatformMethod.invoke(worldGuardInstance);
            Object container = getRegionContainerMethod.invoke(platform);
            Object adaptedWorld = adaptWorldMethod.invoke(null, loc.getWorld());
            Object regionManager = getRegionManagerMethod.invoke(container, adaptedWorld);
            if (regionManager == null) return false;

            Object region = getRegionMethod.invoke(regionManager, regionName);
            if (region == null) return false;

            if (containsPointMethod.getParameterCount() == 3) {
                return (boolean) containsPointMethod.invoke(region, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            } else {
                Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                Method atMethod = blockVector3Class.getMethod("at", double.class, double.class, double.class);
                Object bv = atMethod.invoke(null, loc.getX(), loc.getY(), loc.getZ());
                return (boolean) containsPointMethod.invoke(region, bv);
            }
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getRegionsInWorld(World world) {
        if (!available || world == null) return Collections.emptyList();
        try {
            Object platform = getPlatformMethod.invoke(worldGuardInstance);
            Object container = getRegionContainerMethod.invoke(platform);
            Object adaptedWorld = adaptWorldMethod.invoke(null, world);
            Object regionManager = getRegionManagerMethod.invoke(container, adaptedWorld);
            if (regionManager == null) return Collections.emptyList();

            Map<String, ?> map = (Map<String, ?>) getRegionsMapMethod.invoke(regionManager);
            if (map != null) {
                return new ArrayList<>(map.keySet());
            }
        } catch (Throwable t) {
            // Fallback
        }
        return Collections.emptyList();
    }

    public double[] getRegionBounds(World world, String regionName) {
        if (!available || world == null || regionName == null) return null;
        try {
            Object platform = getPlatformMethod.invoke(worldGuardInstance);
            Object container = getRegionContainerMethod.invoke(platform);
            Object adaptedWorld = adaptWorldMethod.invoke(null, world);
            Object regionManager = getRegionManagerMethod.invoke(container, adaptedWorld);
            if (regionManager == null) return null;

            Object region = getRegionMethod.invoke(regionManager, regionName);
            if (region == null) return null;

            Object min = getMinPointMethod.invoke(region);
            Object max = getMaxPointMethod.invoke(region);

            double minX = ((Number) getBlockXMethod.invoke(min)).doubleValue();
            double minY = ((Number) getBlockYMethod.invoke(min)).doubleValue();
            double minZ = ((Number) getBlockZMethod.invoke(min)).doubleValue();
            double maxX = ((Number) getBlockXMethod.invoke(max)).doubleValue();
            double maxY = ((Number) getBlockYMethod.invoke(max)).doubleValue();
            double maxZ = ((Number) getBlockZMethod.invoke(max)).doubleValue();

            return new double[]{minX, minY, minZ, maxX, maxY, maxZ};
        } catch (Throwable t) {
            return null;
        }
    }
}
