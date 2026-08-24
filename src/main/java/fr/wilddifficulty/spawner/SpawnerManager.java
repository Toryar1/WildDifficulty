package fr.wilddifficulty.spawner;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.listener.MobSpawnListener;
import fr.wilddifficulty.variant.MobVariant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnerManager {

    private final WildDifficultyPlugin plugin;
    private final Map<String, CustomSpawner> spawners = new HashMap<>();
    private final Map<String, Long> nextAllowedSpawnTime = new HashMap<>();
    private final Map<UUID, CustomSpawner> spawnerClipboards = new HashMap<>();

    public SpawnerManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, CustomSpawner> getSpawnerClipboards() {
        return spawnerClipboards;
    }

    public Map<String, CustomSpawner> getSpawners() {
        return spawners;
    }

    public void load() {
        spawners.clear();
        nextAllowedSpawnTime.clear();
        File file = new File(plugin.getDataFolder(), "spawners.yml");
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("spawners");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection sSec = section.getConfigurationSection(key);
            if (sSec == null) continue;

            String worldName = sSec.getString("world");
            World world = Bukkit.getWorld(worldName != null ? worldName : "world");
            if (world == null) continue;

            double x = sSec.getDouble("x");
            double y = sSec.getDouble("y");
            double z = sSec.getDouble("z");
            Location loc = new Location(world, x, y, z);

            CustomSpawner spawner = new CustomSpawner(loc);
            spawner.setActive(sSec.getBoolean("active", true));
            spawner.setInterval(sSec.getInt("interval", 10));
            spawner.setRadius(sSec.getInt("radius", 16));
            spawner.setMaxNearby(sSec.getInt("max-nearby", 6));
            spawner.setIntervalRange(sSec.getInt("interval-range", 0));
            spawner.setParticleType(sSec.getString("particle-type", "SMOKE"));
            spawner.setSoundType(sSec.getString("sound-type", "BLOCK_NOTE_BLOCK_PLING"));
            spawner.setSpawnRange(sSec.getInt("spawn-range", 4));

            ConfigurationSection wSec = sSec.getConfigurationSection("weights");
            if (wSec != null) {
                for (String varId : wSec.getKeys(false)) {
                    spawner.getVariantWeights().put(varId, wSec.getInt(varId));
                }
            }
            spawners.put(toKey(loc), spawner);
        }
        plugin.getLogger().info("[Spawners] " + spawners.size() + " spawners chargés.");
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "spawners.yml");
        FileConfiguration cfg = new YamlConfiguration();
        ConfigurationSection root = cfg.createSection("spawners");

        int index = 0;
        for (CustomSpawner spawner : spawners.values()) {
            ConfigurationSection sSec = root.createSection("spawner_" + index++);
            Location loc = spawner.getLocation();
            sSec.set("world", loc.getWorld().getName());
            sSec.set("x", loc.getX());
            sSec.set("y", loc.getY());
            sSec.set("z", loc.getZ());
            sSec.set("active", spawner.isActive());
            sSec.set("interval", spawner.getInterval());
            sSec.set("radius", spawner.getRadius());
            sSec.set("max-nearby", spawner.getMaxNearby());
            sSec.set("interval-range", spawner.getIntervalRange());
            sSec.set("particle-type", spawner.getParticleType());
            sSec.set("sound-type", spawner.getSoundType());
            sSec.set("spawn-range", spawner.getSpawnRange());

            ConfigurationSection wSec = sSec.createSection("weights");
            for (Map.Entry<String, Integer> entry : spawner.getVariantWeights().entrySet()) {
                wSec.set(entry.getKey(), entry.getValue());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Spawners] Impossible de sauvegarder spawners.yml : " + e.getMessage());
        }
    }

    public CustomSpawner getOrCreateSpawner(Location loc) {
        String key = toKey(loc);
        CustomSpawner spawner = spawners.get(key);
        if (spawner == null) {
            spawner = new CustomSpawner(loc);
            spawners.put(key, spawner);
            save();
        }
        return spawner;
    }

    public void removeSpawner(Location loc) {
        if (spawners.remove(toKey(loc)) != null) {
            save();
        }
    }

    public CustomSpawner getSpawner(Location loc) {
        return spawners.get(toKey(loc));
    }

    public Collection<CustomSpawner> getAllSpawners() {
        return spawners.values();
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (CustomSpawner spawner : spawners.values()) {
            if (!spawner.isActive() || spawner.getVariantWeights().isEmpty()) continue;

            Location loc = spawner.getLocation();
            String key = toKey(loc);

            // Cooldown check
            long nextTime = nextAllowedSpawnTime.getOrDefault(key, 0L);
            if (now < nextTime) continue;

            // Player range check
            boolean playerNear = false;
            for (Player player : loc.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(loc) <= spawner.getRadius() * spawner.getRadius()) {
                    playerNear = true;
                    break;
                }
            }
            if (!playerNear) continue;

            // Max nearby check
            int nearbyCount = 0;
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, spawner.getRadius(), spawner.getRadius(), spawner.getRadius())) {
                if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                    String vId = entity.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
                    if (spawner.getVariantWeights().containsKey(vId)) {
                        nearbyCount++;
                    }
                }
            }
            if (nearbyCount >= spawner.getMaxNearby()) continue;

            // Roll variant based on weights
            String rolledVarId = rollVariant(spawner.getVariantWeights());
            if (rolledVarId == null) continue;

            MobVariant variant = plugin.getVariantManager().getVariant(rolledVarId);
            if (variant == null) continue;

            // Spawn entity with spawnRange dispersion
            double rx = 0;
            double rz = 0;
            if (spawner.getSpawnRange() > 0) {
                rx = (new Random().nextDouble() * 2.0 - 1.0) * spawner.getSpawnRange();
                rz = (new Random().nextDouble() * 2.0 - 1.0) * spawner.getSpawnRange();
            }
            Location spawnLoc = loc.clone().add(0.5 + rx, 1.0, 0.5 + rz);
            Location groundLoc = findSolidGroundBelow(spawnLoc);
            
            Class<? extends Entity> eClass = variant.getType().getEntityClass();
            if (variant.getType() == org.bukkit.entity.EntityType.PLAYER) {
                eClass = org.bukkit.entity.Zombie.class;
            }
            if (eClass != null) {
                if (groundLoc.getWorld().getDifficulty() == org.bukkit.Difficulty.PEACEFUL && org.bukkit.entity.Enemy.class.isAssignableFrom(eClass)) {
                    return;
                }
                try {
                    groundLoc.getWorld().spawn(groundLoc, eClass, CreatureSpawnEvent.SpawnReason.CUSTOM, spawned -> {
                        spawned.getPersistentDataContainer().set(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING, variant.getId());
                        spawned.setMetadata("no-stack", new FixedMetadataValue(plugin, true));
                        spawned.setMetadata("rose-no-stack", new FixedMetadataValue(plugin, true));
                        spawned.setMetadata("wildstacker:no-stack", new FixedMetadataValue(plugin, true));
                    });
                } catch (Throwable ignored) {}
            }

            // Particles and sounds
            if (!"none".equalsIgnoreCase(spawner.getParticleType())) {
                try {
                    loc.getWorld().spawnParticle(org.bukkit.Particle.valueOf(spawner.getParticleType().toUpperCase()), loc.clone().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
                } catch (Exception ignored) {}
            }
            if (!"none".equalsIgnoreCase(spawner.getSoundType())) {
                try {
                    loc.getWorld().playSound(loc, org.bukkit.Sound.valueOf(spawner.getSoundType().toUpperCase()), 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }

            // Calculate next cooldown with range variation
            int offset = 0;
            if (spawner.getIntervalRange() > 0) {
                offset = new Random().nextInt(spawner.getIntervalRange() * 2 + 1) - spawner.getIntervalRange();
            }
            int finalSecs = Math.max(1, spawner.getInterval() + offset);
            nextAllowedSpawnTime.put(key, now + finalSecs * 1000L);
        }
    }

    private String rollVariant(Map<String, Integer> weights) {
        int totalWeight = 0;
        for (int w : weights.values()) {
            totalWeight += w;
        }
        if (totalWeight <= 0) return null;

        int random = new Random().nextInt(totalWeight);
        int cursor = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            cursor += entry.getValue();
            if (random < cursor) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Location findSolidGroundBelow(Location start) {
        Location cursor = start.clone();
        if (cursor.getBlock().getType().isSolid()) {
            for (int y = 0; y < 5; y++) {
                cursor.add(0, 1, 0);
                if (!cursor.getBlock().getType().isSolid() && !cursor.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                    return cursor;
                }
            }
            return start;
        }
        for (int y = 0; y < 10; y++) {
            cursor.add(0, -1, 0);
            if (cursor.getBlock().getType().isSolid()) {
                return cursor.add(0, 1, 0);
            }
        }
        return start;
    }

    public static String toKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
}
