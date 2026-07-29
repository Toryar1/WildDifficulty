package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.MainConfigManager;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.listener.MobBehaviorListener;
import fr.wilddifficulty.player.PlayerData;
import fr.wilddifficulty.listener.MobSpawnListener;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.zone.DifficultyZone;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MobTickScheduler extends BukkitRunnable {

    private final WildDifficultyPlugin plugin;
    private int tickCount = 0;
    private static final Map<UUID, Map<Location, Integer>> miningProgress = new HashMap<>();
    private static final Map<UUID, Location> playerLastLocations = new HashMap<>();
    private static final Map<UUID, Double> playerThirstExhaustion = new HashMap<>();

    public MobTickScheduler(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tickCount += 10; // Ticks tous les 10 ticks (0.5 sec)
        MainConfigManager mainCfg = plugin.getMainConfigManager();

        if (plugin.getSpawnerManager() != null) {
            plugin.getSpawnerManager().tick();
        }

        // Affichage des contours de zone aux joueurs qui tiennent l'outil
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.getType() == org.bukkit.Material.GOLDEN_HOE && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
                if (plainName.contains("Outil de Zone")) {
                    drawPlayerZoneOutlines(player);
                }
            }
        }

        // Particules au sol à l'approche des zones
        if (plugin.getMainConfigManager().isZoneBorderParticles()) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                drawNearbyZoneBoundaries(player);
            }
        }
        
        if (tickCount % 40 == 0) {
            runZoneBeaconEffects();
        }

        if (tickCount % 100 == 0) {
            runDaytimeSpawning();
        }

        // Blood Moon time check
        if (tickCount % 200 == 0) {
            World world = plugin.getServer().getWorld(mainCfg.getDistanceWorld());
            if (world != null) {
                long time = world.getTime();
                boolean isNight = time >= 13000 && time < 23000;
                boolean hasForced = world.hasMetadata("wd_bloodmoon_forced");
                if (mainCfg.isBloodMoonEnabled() || hasForced) {
                    if (isNight) {
                        if (!mainCfg.isBloodMoonActive() && !world.hasMetadata("wd_bloodmoon_rolled")) {
                            world.setMetadata("wd_bloodmoon_rolled", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                            if (hasForced) {
                                world.removeMetadata("wd_bloodmoon_forced", plugin);
                            }
                            if (hasForced || Math.random() <= mainCfg.getBloodMoonChance()) {
                                mainCfg.setBloodMoonActive(true);
                                plugin.getServer().broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(mainCfg.getBloodMoonStartMessage()));
                                for (Player p : world.getPlayers()) {
                                    try {
                                        p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(mainCfg.getBloodMoonStartSound().toUpperCase()), 1.0f, 1.0f);
                                    } catch (Exception ignored) {}
                                    try {
                                        p.spawnParticle(org.bukkit.Particle.valueOf(mainCfg.getBloodMoonStartParticle().toUpperCase()), p.getLocation().add(0, 1, 0), 40, 0.6, 0.6, 0.6, 0.05);
                                    } catch (Exception ignored) {}
                                    for (String potStr : mainCfg.getBloodMoonStartPotions()) {
                                        try {
                                            String[] parts = potStr.split(":");
                                            org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(parts[0].toUpperCase());
                                            int duration = Integer.parseInt(parts[1]);
                                            int amp = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                                            if (type != null) {
                                                p.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration, amp, false, true));
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }
                            }
                        }
                    } else {
                        if (mainCfg.isBloodMoonActive()) {
                            mainCfg.setBloodMoonActive(false);
                            plugin.getServer().broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(mainCfg.getBloodMoonEndMessage()));
                            for (Player p : world.getPlayers()) {
                                try {
                                    p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(mainCfg.getBloodMoonEndSound().toUpperCase()), 1.0f, 1.0f);
                                } catch (Exception ignored) {}
                                try {
                                    p.spawnParticle(org.bukkit.Particle.valueOf(mainCfg.getBloodMoonEndParticle().toUpperCase()), p.getLocation().add(0, 1, 0), 40, 0.6, 0.6, 0.6, 0.05);
                                } catch (Exception ignored) {}
                                for (String potStr : mainCfg.getBloodMoonEndPotions()) {
                                    try {
                                        String[] parts = potStr.split(":");
                                        org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(parts[0].toUpperCase());
                                        int duration = Integer.parseInt(parts[1]);
                                        int amp = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                                        if (type != null) {
                                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration, amp, false, true));
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                        if (world.hasMetadata("wd_bloodmoon_rolled")) {
                            world.removeMetadata("wd_bloodmoon_rolled", plugin);
                        }
                    }
                }
            }
        }
        if (tickCount % 2 != 0) return;

        long startNanos = System.nanoTime();
        int entityCount = 0;
        int processedCount = 0;

        for (World world : plugin.getServer().getWorlds()) {
            java.util.Collection<LivingEntity> entities;
            try {
                entities = world.getEntitiesByClasses(Mob.class, Player.class).stream()
                    .map(e -> (LivingEntity) e)
                    .collect(java.util.stream.Collectors.toList());
            } catch (Throwable t) {
                entities = world.getLivingEntities();
            }

            for (LivingEntity entity : entities) {
                if (!entity.isValid() || entity.isDead()) {
                    continue;
                }
                entityCount++;

                // Masquer les pseudos flottants pour les monstres/variantes de combat (BossBar prioritaire),
                // tout en préservant visibles les étiquettes appliquées par les joueurs aux animaux domestiques
                if (entity instanceof Monster || entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                    if (!entity.hasMetadata("wd_npc")) {
                        entity.setCustomNameVisible(false);
                    }
                }

                if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                    String varId = entity.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
                    MobVariant var = plugin.getVariantManager().getVariant(varId);
                    if (var != null) {
                        processedCount++;

                        // For Citizens NPC players, handle navigation and attack
                        if (entity.hasMetadata("wd_npc") && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                            Player target = null;
                            if (entity.hasMetadata("wd_npc_target")) {
                                UUID targetUuid = (UUID) entity.getMetadata("wd_npc_target").get(0).value();
                                Player potential = plugin.getServer().getPlayer(targetUuid);
                                if (potential != null && potential.isOnline() && potential.isValid()
                                        && potential.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                                    if (potential.getWorld().equals(entity.getWorld())) {
                                        double distSq = potential.getLocation().distanceSquared(entity.getLocation());
                                        if (distSq <= 2500) {
                                            target = potential;
                                        }
                                    }
                                }
                            }

                            // If target is in safezone, de-aggro! (Checked every 10 ticks)
                            if (target != null && tickCount % 10 == 0) {
                                DifficultyZone targetZone = plugin.getZoneManager().getZoneAt(target.getWorld().getName(), target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ());
                                if (targetZone != null && targetZone.isSafeZone()) {
                                    target = null;
                                    entity.removeMetadata("wd_npc_target", plugin);
                                    fr.wilddifficulty.util.CitizensHook.navigateNPC(entity, null);
                                }
                            }

                            if (target == null && tickCount % 10 == 0) {
                                String mode = var.getAggroMode();
                                if ("AGGRESSIVE".equalsIgnoreCase(mode)) {
                                    target = entity.getWorld().getPlayers().stream()
                                        .filter(p -> p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                                        .filter(p -> !p.hasMetadata("wd_npc"))
                                        .filter(p -> {
                                            DifficultyZone targetZone = plugin.getZoneManager().getZoneAt(p.getWorld().getName(), p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());
                                            return targetZone == null || !targetZone.isSafeZone();
                                        })
                                        .filter(p -> p.getLocation().distanceSquared(entity.getLocation()) <= 2500)
                                        .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(entity.getLocation())))
                                        .orElse(null);
                                } else if ("NEUTRAL_HIT".equalsIgnoreCase(mode) || "NEUTRAL_SQUAD_HIT".equalsIgnoreCase(mode)) {
                                    if (entity.hasMetadata("wd_attacked_by")) {
                                        java.util.Set<UUID> attackers = (java.util.Set<UUID>) entity.getMetadata("wd_attacked_by").get(0).value();
                                        if (attackers != null && !attackers.isEmpty()) {
                                            target = entity.getWorld().getPlayers().stream()
                                                .filter(p -> attackers.contains(p.getUniqueId()))
                                                .filter(p -> p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                                                .filter(p -> {
                                                    DifficultyZone targetZone = plugin.getZoneManager().getZoneAt(p.getWorld().getName(), p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());
                                                    return targetZone == null || !targetZone.isSafeZone();
                                                })
                                                .filter(p -> p.getLocation().distanceSquared(entity.getLocation()) <= 2500)
                                                .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(entity.getLocation())))
                                                .orElse(null);
                                        }
                                    }
                                }
                                if (target != null) {
                                    entity.setMetadata("wd_npc_target", new org.bukkit.metadata.FixedMetadataValue(plugin, target.getUniqueId()));
                                }
                            }

                            if (target != null) {
                                double distSq = target.getLocation().distanceSquared(entity.getLocation());
                                fr.wilddifficulty.util.CitizensHook.navigateNPC(entity, target);
                                if (distSq <= 6.25 && tickCount % 2 == 0) {
                                    if (entity instanceof Player pNpc) {
                                        pNpc.swingMainHand();
                                        double dmg = 4.0;
                                        if (var.getModifiers() != null && var.getModifiers().getDamageValue() > 0) {
                                            dmg = var.getModifiers().getDamageValue();
                                        }
                                        target.damage(dmg, entity);
                                    }
                                }
                            } else {
                                // Wander AI for idling NPCs
                                try {
                                    net.citizensnpcs.api.npc.NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(entity);
                                    if (npc != null) {
                                        if (npc.getEntity() instanceof Player pNpc) {
                                            pNpc.setSprinting(false);
                                        }
                                        if (!npc.getNavigator().isNavigating() && Math.random() < 0.05) {
                                            double rx = entity.getLocation().getX() + (Math.random() - 0.5) * 16.0;
                                            double rz = entity.getLocation().getZ() + (Math.random() - 0.5) * 16.0;
                                            double ry = entity.getWorld().getHighestBlockYAt((int)rx, (int)rz);
                                            Location walkLoc = new Location(entity.getWorld(), rx, ry, rz);
                                            npc.getNavigator().getLocalParameters().speedModifier(1.0f); // Normal speed
                                            npc.getNavigator().setTarget(walkLoc);
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                        tickMob(entity, var);
                    }
                }
            }
        }

        // Cleanup stale BossBar entries (mob died or left the world without triggering onDeath)
        MobBehaviorListener.activeBossBars.entrySet().removeIf(entry -> {
            org.bukkit.entity.Entity ent = plugin.getServer().getEntity(entry.getKey());
            if (ent == null || ent.isDead() || !ent.isValid()) {
                // Hide from all players before removing
                BossBar staleBar = entry.getValue();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.hideBossBar(staleBar);
                }
                return true;
            }
            return false;
        });

        // Centralized BossBar prioritization (max 1 bossbar shown per player)
        for (Player player : plugin.getServer().getOnlinePlayers()) {

            LivingEntity priorityMob = null;
            // Show BossBar ONLY if player attacked this mob within the last 15 seconds!
            UUID targetMobUuid = MobBehaviorListener.playerActiveBossBarMob.get(player.getUniqueId());
            Long lastHit = MobBehaviorListener.playerBossBarLastHit.get(player.getUniqueId());

            if (targetMobUuid != null && lastHit != null && (System.currentTimeMillis() - lastHit <= 15000)) {
                org.bukkit.entity.Entity targetEnt = plugin.getServer().getEntity(targetMobUuid);
                if (targetEnt instanceof LivingEntity targetMob && targetMob.isValid() && !targetMob.isDead()
                        && targetMob.getWorld().equals(player.getWorld())) {
                    double distSq = targetMob.getLocation().distanceSquared(player.getLocation());
                    if (distSq <= 30.0 * 30.0) {
                        priorityMob = targetMob;
                    }
                }
            }

            // Show priority bossbar for attacked mob (custom or vanilla), hide all others
            UUID priorityUuid = (priorityMob != null) ? priorityMob.getUniqueId() : null;
            if (priorityMob != null) {
                // Generate/update BossBar for attacked mob (works for both custom variants and vanilla mobs)
                double health = Math.max(0.0, priorityMob.getHealth());
                double maxHealth = priorityMob.getAttribute(Attribute.MAX_HEALTH).getValue();
                float progress = (float) Math.max(0.0, Math.min(1.0, health / maxHealth));

                String mobName = null;
                if (priorityMob.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                    String varId = priorityMob.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
                    MobVariant var = plugin.getVariantManager().getVariant(varId);
                    if (var != null) {
                        mobName = (var.getDisplayName() != null && !var.getDisplayName().isBlank()) ? var.getDisplayName() : var.getId();
                    }
                }
                if (mobName == null || mobName.isBlank()) {
                    if (priorityMob.getCustomName() != null && !priorityMob.getCustomName().isBlank()) {
                        mobName = priorityMob.getCustomName();
                    } else {
                        String raw = priorityMob.getType().name().toLowerCase().replace("_", " ");
                        mobName = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
                    }
                }

                String formattedTitle = mobName + " §7(" + String.format("%.0f", health) + "/" + String.format("%.0f", maxHealth) + "❤)";
                Component compTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(formattedTitle);

                // Dégradé dynamique : Vert > 60%, Jaune 30-60%, Rouge < 30%
                BossBar.Color color;
                if (progress > 0.60f) {
                    color = BossBar.Color.GREEN;
                } else if (progress >= 0.30f) {
                    color = BossBar.Color.YELLOW;
                } else {
                    color = BossBar.Color.RED;
                }

                BossBar playerBar = MobBehaviorListener.activeBossBars.computeIfAbsent(priorityUuid, k -> {
                    return BossBar.bossBar(compTitle, progress, color, BossBar.Overlay.PROGRESS);
                });
                playerBar.progress(progress);
                playerBar.color(color);
                playerBar.name(compTitle);
                player.showBossBar(playerBar);
            }

            // Hide BossBars for any mob other than the currently attacked priority mob
            for (Map.Entry<UUID, BossBar> entry : MobBehaviorListener.activeBossBars.entrySet()) {
                if (priorityUuid == null || !entry.getKey().equals(priorityUuid)) {
                    player.hideBossBar(entry.getValue());
                }
            }

            // Clean up old hit records
            long now = System.currentTimeMillis();
            MobBehaviorListener.playerBossBarLastHit.entrySet().removeIf(e -> (now - e.getValue()) > 15000);

            if (plugin.getActiveScoreboards().contains(player.getUniqueId())) {
                fr.wilddifficulty.util.ScoreboardUtil.updateScoreboard(plugin, player);
            }
        }

        // Player Thirst & Movement-based Exhaustion Loop
        if (mainCfg.isThirstEnabled()) {
            double globalMult = mainCfg.getThirstDrainMultiplier();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
                if (!pd.isThirstEnabled()) continue;
                if (player.isDead()) {
                    pd.setThirstLevel(20);
                    playerThirstExhaustion.remove(player.getUniqueId());
                    continue;
                }

                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    // Creative/Spectator: keep air at max so the bubble bar is invisible/full
                    if (player.getRemainingAir() != player.getMaximumAir()) {
                        player.setRemainingAir(player.getMaximumAir());
                    }
                    continue;
                }

                Location currentLoc = player.getLocation();
                Location lastLoc = playerLastLocations.get(player.getUniqueId());

                double distMoved = 0.0;
                if (lastLoc != null && lastLoc.getWorld().equals(currentLoc.getWorld())) {
                    distMoved = currentLoc.distance(lastLoc);
                }
                playerLastLocations.put(player.getUniqueId(), currentLoc.clone());

                // Vitesse calquée sur la nourriture/satiété : 0.01/bloc à pied, 0.04/bloc en sprint
                double activityExhaustion = 0.0;
                if (distMoved >= 0.05) {
                    if (player.isSprinting()) {
                        activityExhaustion = distMoved * 0.04 * globalMult;
                    } else {
                        activityExhaustion = distMoved * 0.01 * globalMult;
                    }
                }

                // Déshydratation par source de chaleur proche (Lave, Magma, Feu, Campfire)
                if (mainCfg.isThirstHeatDrainEnabled()) {
                    Location pLoc = player.getLocation();
                    World pWorld = player.getWorld();
                    int px = pLoc.getBlockX();
                    int py = pLoc.getBlockY();
                    int pz = pLoc.getBlockZ();
                    double minHeatDistSq = Double.MAX_VALUE;

                    for (int dx = -4; dx <= 4; dx++) {
                        for (int dy = -2; dy <= 2; dy++) {
                            for (int dz = -4; dz <= 4; dz++) {
                                Material mat = pWorld.getBlockAt(px + dx, py + dy, pz + dz).getType();
                                if (mat == Material.LAVA || mat == Material.MAGMA_BLOCK 
                                        || mat == Material.FIRE || mat == Material.SOUL_FIRE 
                                        || mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE) {
                                    double dSq = dx * dx + dy * dy + dz * dz;
                                    if (dSq < minHeatDistSq) {
                                        minHeatDistSq = dSq;
                                    }
                                }
                            }
                        }
                    }

                    if (minHeatDistSq <= 25.0) {
                        double dist = Math.sqrt(minHeatDistSq);
                        double proximityFactor = Math.max(0.0, (5.0 - dist) / 5.0);
                        double heatExhaustion = 0.008 * proximityFactor * globalMult * mainCfg.getThirstHeatDrainMultiplier();
                        activityExhaustion += heatExhaustion;
                    }
                }

                if (activityExhaustion > 0.0) {
                    double currentExhaustion = playerThirstExhaustion.getOrDefault(player.getUniqueId(), 0.0) + activityExhaustion;
                    while (currentExhaustion >= 4.0) {
                        currentExhaustion -= 4.0;
                        int currentThirst = pd.getThirstLevel();
                        if (currentThirst > 0) {
                            pd.setThirstLevel(currentThirst - 1);
                            plugin.getPlayerSettingsManager().save();
                        } else {
                            // Dehydration damage – no chat message per server charter
                            player.damage(1.0);
                        }
                    }
                    playerThirstExhaustion.put(player.getUniqueId(), currentExhaustion);
                }

                // Sur terre : calcul continu au millième près pour afficher toutes les frames d'éclatement des bulles (300 air ticks)
                boolean underwater = player.getEyeLocation().getBlock().getType() == Material.WATER
                        || player.getLocation().getBlock().getType() == Material.WATER;
                if (!underwater) {
                    double currentExh = playerThirstExhaustion.getOrDefault(player.getUniqueId(), 0.0);
                    double exactThirst = Math.max(0.0, (double) pd.getThirstLevel() - (currentExh / 4.0));
                    double fraction = Math.max(0.0, Math.min(1.0, exactThirst / 20.0));
                    // Maintenir l'air <= 299 ticks pour forcer l'affichage permanent des 10 bulles d'apnée sur terre !
                    int thirstAir = (int) Math.round(fraction * 299.0);

                    if (player.getRemainingAir() != thirstAir) {
                        player.setRemainingAir(thirstAir);
                    }
                }
                // Underwater: vanilla bubble bar manages real air. No ActionBar – per server charter.
            }
        }

        if (mainCfg.isDebug()) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            if (durationMs > 2) {
                plugin.getLogger().info("[WD-DEBUG] Tick loop completed in " + durationMs + "ms. Checked: " + entityCount + " mobs/players, Processed: " + processedCount + " custom variants.");
            }
        }
    }

    private void tickMob(LivingEntity entity, MobVariant var) {
        StatModifiers mods = var.getModifiers();
        if (mods == null) return;

        Location loc = entity.getLocation();

        // 1. BOSSBAR MANAGEMENT (Affichage du nom et PV en haut de l'écran sans empilation)
        UUID uuid = entity.getUniqueId();
        BossBar bar = MobBehaviorListener.activeBossBars.computeIfAbsent(uuid, k -> {
            BossBar.Color color = BossBar.Color.RED;
            BossBar.Overlay style = BossBar.Overlay.PROGRESS;
            Set<BossBar.Flag> flags = new HashSet<>();
            if (mods.getBossBarColor() != null) {
                try { color = BossBar.Color.valueOf(mods.getBossBarColor()); } catch (Exception ignored) {}
            }
            if (mods.getBossBarStyle() != null) {
                try { style = BossBar.Overlay.valueOf(mods.getBossBarStyle()); } catch (Exception ignored) {}
            }
            if (mods.isBossBarDarkenSky()) flags.add(BossBar.Flag.DARKEN_SCREEN);

            return BossBar.bossBar(Component.text("Mob"), 1.0f, color, style, flags);
        });

        // Update title with Name and PV/MaxPV
        double health = Math.max(0.0, entity.getHealth());
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        float progress = (float) Math.max(0.0, Math.min(1.0, health / maxHealth));
        bar.progress(progress);

        String mobName = var.getDisplayName() != null ? var.getDisplayName() : entity.getType().name();
        String formattedTitle = mobName + " §7(" + String.format("%.0f", health) + "/" + String.format("%.0f", maxHealth) + "❤)";
        bar.name(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(formattedTitle));        // 2. VISION A TRAVERS LES MURS (Force target & Cassage de blocs avec progression visuelle)
        if (mods.isGoalWallVision() && entity instanceof Mob mob) {
            if (mob.getTarget() == null && tickCount % 10 == 0) {
                for (Player player : mob.getWorld().getPlayers()) {
                    // Check if player is not in a safezone
                    DifficultyZone z = plugin.getZoneManager().getZoneAt(player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
                    if (z != null && z.isSafeZone()) continue;

                    if (player.getLocation().distanceSquared(loc) <= 20 * 20) {
                        mob.setTarget(player);
                        break;
                    }
                }
            }

            if (mob.getTarget() != null) {
                LivingEntity target = mob.getTarget();
                // Bloquer ciblage si la cible est entrée en safezone (vérifié toutes les 10 ticks)
                boolean targetInSafeZone = false;
                if (tickCount % 10 == 0) {
                    DifficultyZone z = plugin.getZoneManager().getZoneAt(target.getWorld().getName(), target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ());
                    if (z != null && z.isSafeZone()) {
                        targetInSafeZone = true;
                        mob.setMetadata("wd_wall_target_safe", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    } else {
                        mob.removeMetadata("wd_wall_target_safe", plugin);
                    }
                } else {
                    targetInSafeZone = mob.hasMetadata("wd_wall_target_safe");
                }

                if (targetInSafeZone) {
                    mob.setTarget(null);
                } else {
                    Location start = mob.getEyeLocation();
                    org.bukkit.util.Vector dir = target.getEyeLocation().toVector().subtract(start.toVector());
                    double dist = dir.length();
                    if (dist > 0.1 && dist <= 4.0) {
                        dir.normalize();
                        Map<Location, Integer> mobMining = miningProgress.computeIfAbsent(entity.getUniqueId(), k -> new HashMap<>());
                        mobMining.keySet().removeIf(l -> l.distanceSquared(entity.getLocation()) > 6 * 6);

                        for (double d = 1.0; d <= Math.min(dist, 3.0); d += 1.0) {
                            Location blockLoc = start.clone().add(dir.clone().multiply(d));
                            org.bukkit.block.Block block = blockLoc.getBlock();
                            if (block.getType().isSolid() 
                                    && block.getType() != Material.BEDROCK 
                                    && block.getType() != Material.BARRIER 
                                    && block.getType() != Material.OBSIDIAN
                                    && !block.getType().name().contains("PORTAL")) {
                                
                                // Ne pas pouvoir briser les safezones
                                DifficultyZone blockZone = plugin.getZoneManager().getZoneAt(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
                                if (blockZone != null && blockZone.isSafeZone()) {
                                    continue;
                                }

                                Location blockLocBlock = block.getLocation();
                                int currentProg = mobMining.getOrDefault(blockLocBlock, 0);
                                if (currentProg >= 9) {
                                    block.breakNaturally();
                                    mobMining.remove(blockLocBlock);
                                    for (Player p : block.getWorld().getPlayers()) {
                                        if (p.getLocation().distanceSquared(blockLocBlock) < 32 * 32) {
                                            p.sendBlockDamage(blockLocBlock, -1.0f);
                                        }
                                    }
                                } else {
                                    currentProg += 2; // incrémente pour un minage rapide (~2.5 sec)
                                    mobMining.put(blockLocBlock, currentProg);
                                    float visualProgress = Math.min(1.0f, currentProg / 10.0f);
                                    for (Player p : block.getWorld().getPlayers()) {
                                        if (p.getLocation().distanceSquared(blockLocBlock) < 32 * 32) {
                                            p.sendBlockDamage(blockLocBlock, visualProgress);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. RÉGÉNÉRATION PASSIVE (Toutes les 20 ticks / 1 seconde)
        double regenAmount = 0.0;
        if (mods.getPassiveRegen() > 0) regenAmount += mods.getPassiveRegen();
        if (mods.getRegenerationValue() > 0) regenAmount += mods.getRegenerationValue();

        if (regenAmount > 0 && tickCount % 20 == 0) {
            double curMaxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
            entity.setHealth(Math.min(curMaxHealth, entity.getHealth() + regenAmount));
            
            // Actualisation en continu du BossBar lors de la régénération
            BossBar regenBar = MobBehaviorListener.activeBossBars.get(uuid);
            if (regenBar != null) {
                float regenProgress = (float) Math.max(0.0, Math.min(1.0, entity.getHealth() / curMaxHealth));
                regenBar.progress(regenProgress);
            }
        }

        // 4. CAMOUFLAGE (Invisibilité hors combat)
        if (mods.isCamouflage() && entity instanceof Mob mob) {
            if (mob.getTarget() == null) {
                if (!entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
                }
            } else {
                if (entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    entity.removePotionEffect(PotionEffectType.INVISIBILITY);
                }
            }
        }

        // 5. EFfETS VISUELS - AURAS ET TRAILS
        if (!"none".equals(mods.getParticleAuraType())) {
            try {
                Particle p = Particle.valueOf(mods.getParticleAuraType().toUpperCase());
                int freq = Math.max(10, mods.getParticleAuraFreq());
                if (tickCount % freq == 0) {
                    Location pLoc = loc.clone().add(0, 1.0, 0);
                    spawnParticle(p, pLoc, mods.getParticleAuraColor());
                }
            } catch (Exception ignored) {}
        }

        if (!"none".equals(mods.getParticleTrailType()) && entity.getVelocity().lengthSquared() > 0.01) {
            try {
                Particle p = Particle.valueOf(mods.getParticleTrailType().toUpperCase());
                spawnParticle(p, loc.clone().add(0, 0.1, 0), "255,255,255");
            } catch (Exception ignored) {}
        }

        // 5.5. AGGRO MODE SELECTION AND PASSIVE PATHFINDING
        if (entity instanceof Mob mob) {
            String mode = var.getAggroMode();
            if ("AGGRESSIVE".equalsIgnoreCase(mode)) {
                if (mob.getTarget() == null && tickCount % 10 == 0) {
                    Player closest = null;
                    double closestDist = Double.MAX_VALUE;
                    double followRange = mob.getAttribute(Attribute.FOLLOW_RANGE) != null 
                            ? mob.getAttribute(Attribute.FOLLOW_RANGE).getValue() 
                            : 16.0;
                    for (Player p : mob.getWorld().getPlayers()) {
                        DifficultyZone z = plugin.getZoneManager().getZoneAt(p.getWorld().getName(), p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());
                        if (z != null && z.isSafeZone()) continue;
                        
                        double dist = p.getLocation().distance(mob.getLocation());
                        if (dist < followRange && dist < closestDist) {
                            closest = p;
                            closestDist = dist;
                        }
                    }
                    if (closest != null) {
                        mob.setTarget(closest);
                    }
                }
            }

            LivingEntity target = mob.getTarget();
            if (target != null && (!target.isValid() || target.isDead() || target.getWorld() == null || mob.getWorld() == null || !target.getWorld().equals(mob.getWorld()))) {
                mob.setTarget(null);
                target = null;
            }
            if (target != null) {
                boolean targetInSafeZone = false;
                if (tickCount % 10 == 0) {
                    DifficultyZone z = plugin.getZoneManager().getZoneAt(target.getWorld().getName(), target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ());
                    if (z != null && z.isSafeZone()) {
                        targetInSafeZone = true;
                        mob.setMetadata("wd_target_safe", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    } else {
                        mob.removeMetadata("wd_target_safe", plugin);
                    }
                } else {
                    targetInSafeZone = mob.hasMetadata("wd_target_safe");
                }

                if (targetInSafeZone) {
                    mob.setTarget(null);
                } else {
                    mob.getPathfinder().moveTo(target, 1.25);
                    double distSq = mob.getLocation().distanceSquared(target.getLocation());
                    if (distSq <= 2.25) { // 1.5 blocks
                        double dmg = mods.getDamageValue() > 0 ? mods.getDamageValue() : 2.0;
                        target.damage(dmg, mob);
                    }
                }
            }

            // Saut d'obstacles (Jump AI - Checked every 5 ticks)
            if (mods.isJumpAttack() && target != null && tickCount % 5 == 0) {
                Location targetLoc = target.getLocation();
                Location frontLoc = loc.clone().add(mob.getLocation().getDirection().setY(0).normalize().multiply(0.8));
                boolean isBlocked = frontLoc.getBlock().getType().isSolid() || frontLoc.clone().add(0, 1, 0).getBlock().getType().isSolid();
                boolean targetHigher = targetLoc.getY() - loc.getY() >= 0.8 && targetLoc.getY() - loc.getY() <= 2.2 && loc.distanceSquared(targetLoc) <= 9.0;
                
                if ((isBlocked || targetHigher) && entity.isOnGround() && Math.random() < 0.4) {
                    org.bukkit.util.Vector direction = target.getLocation().toVector().subtract(entity.getLocation().toVector());
                    direction.setY(0);
                    if (direction.lengthSquared() > 0) {
                        direction.normalize().multiply(0.25);
                    }
                    org.bukkit.util.Vector vel = entity.getVelocity();
                    vel.setY(0.42);
                    vel.add(direction);
                    entity.setVelocity(vel);
                }
            }

            // IA d'escalade intelligente (Smart Climb - Checked every 5 ticks)
            if (mods.isSmartClimb() && target != null && entity.isOnGround() && tickCount % 5 == 0) {
                Location targetLoc = target.getLocation();
                if (targetLoc.getY() - loc.getY() >= 1.5 && loc.distanceSquared(targetLoc) <= 12 * 12) {
                    org.bukkit.block.Block bestStep = null;
                    double bestDistSq = Double.MAX_VALUE;
                    int currentY = loc.getBlockY();
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            for (int dy = -1; dy <= 2; dy++) {
                                org.bukkit.block.Block block = loc.getWorld().getBlockAt(loc.getBlockX() + dx, currentY + dy, loc.getBlockZ() + dz);
                                if (block.getType().isSolid()) {
                                    org.bukkit.block.Block up1 = block.getRelative(org.bukkit.block.BlockFace.UP);
                                    org.bukkit.block.Block up2 = up1.getRelative(org.bukkit.block.BlockFace.UP);
                                    if (!up1.getType().isSolid() && !up2.getType().isSolid()) {
                                        double stepTopY = block.getY() + 1.0;
                                        if (stepTopY >= loc.getY() - 0.5 && stepTopY <= loc.getY() + 2.2) {
                                            Location locFlat = loc.clone();
                                            locFlat.setY(0);
                                            Location targetFlat = targetLoc.clone();
                                            targetFlat.setY(0);
                                            double currentHorizDistSq = locFlat.distanceSquared(targetFlat);
                                            Location blockFlat = block.getLocation().clone();
                                            blockFlat.setY(0);
                                            double stepHorizDistSq = blockFlat.distanceSquared(targetFlat);
                                            if (stepHorizDistSq < currentHorizDistSq) {
                                                if (stepHorizDistSq < bestDistSq) {
                                                    bestDistSq = stepHorizDistSq;
                                                    bestStep = block;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (bestStep != null) {
                        Location stepLoc = bestStep.getLocation().add(0.5, 1.1, 0.5);
                        mob.getPathfinder().moveTo(stepLoc, 1.25);
                        double horizDist = Math.sqrt(Math.pow(loc.getX() - stepLoc.getX(), 2) + Math.pow(loc.getZ() - stepLoc.getZ(), 2));
                        if (horizDist <= 1.3 && Math.random() < 0.5) {
                            org.bukkit.util.Vector direction = stepLoc.toVector().subtract(entity.getLocation().toVector());
                            direction.setY(0);
                            if (direction.lengthSquared() > 0) {
                                direction.normalize().multiply(0.25);
                            }
                            org.bukkit.util.Vector vel = entity.getVelocity();
                            vel.setY(0.42);
                            vel.add(direction);
                            entity.setVelocity(vel);
                        }
                    }
                }
            }
        }

        // 6. COMPORTEMENTS COMBAT (Seulement si le mob a une cible)
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            LivingEntity target = mob.getTarget();
            double distSq = loc.distanceSquared(target.getLocation());

            // Téléportation vers la cible (Enderman style avec visuel d'enderpearl)
            if (mods.isTeleportToTarget() && distSq > 8 * 8 && distSq < 32 * 32 && Math.random() < 0.05) {
                int maxTp = mods.getTeleportMaxUses();
                int usedTp = 0;
                if (mob.hasMetadata("wd_teleport_uses")) {
                    usedTp = mob.getMetadata("wd_teleport_uses").get(0).asInt();
                }

                if (maxTp == -1 || usedTp < maxTp) {
                    Location tLoc = target.getLocation().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
                    tLoc.setY(mob.getWorld().getHighestBlockYAt(tLoc));
                    
                    // Visuel de projectile d'enderpearl jetée
                    mob.getWorld().playSound(mob.getLocation(), org.bukkit.Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);
                    EnderPearl pearl = mob.launchProjectile(EnderPearl.class, target.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize().multiply(1.5));
                    
                    mob.teleport(tLoc);
                    mob.getWorld().spawnParticle(Particle.PORTAL, mob.getLocation(), 20, 0.5, 1, 0.5);
                    mob.setMetadata("wd_teleport_uses", new org.bukkit.metadata.FixedMetadataValue(plugin, usedTp + 1));
                }
            }

            // Charge / Dash vers la cible
            if (mods.isDashAttack() && distSq < 10 * 10 && distSq > 3 * 3 && Math.random() < 0.1) {
                int maxDash = mods.getDashMaxUses();
                int usedDash = 0;
                if (mob.hasMetadata("wd_dash_uses")) {
                    usedDash = mob.getMetadata("wd_dash_uses").get(0).asInt();
                }

                if (maxDash == -1 || usedDash < maxDash) {
                    org.bukkit.util.Vector dir = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2);
                    mob.setVelocity(dir);
                    mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation(), 10, 0.2, 0.2, 0.2);
                    mob.setMetadata("wd_dash_uses", new org.bukkit.metadata.FixedMetadataValue(plugin, usedDash + 1));
                }
            }

            // Ranged Attack simulation (flag-based)
            if (mods.isRangedAttack() && distSq > 4 * 4 && distSq < 20 * 20 && Math.random() < 0.15) {
                Location eye = mob.getEyeLocation();
                org.bukkit.util.Vector dir = target.getLocation().toVector().subtract(eye.toVector()).normalize();
                String projectileType = mods.getRangedAttackType().toUpperCase();
                
                Class<? extends Projectile> projClass = Arrow.class;
                if ("SNOWBALL".equals(projectileType)) {
                    projClass = Snowball.class;
                } else if ("FIREBALL".equals(projectileType)) {
                    projClass = LargeFireball.class;
                } else if ("SMALL_FIREBALL".equals(projectileType)) {
                    projClass = SmallFireball.class;
                } else if ("WITHER_SKULL".equals(projectileType)) {
                    projClass = WitherSkull.class;
                } else if ("EGG".equals(projectileType)) {
                    projClass = Egg.class;
                } else if ("DRAGON_FIREBALL".equals(projectileType)) {
                    projClass = DragonFireball.class;
                }
                mob.swingMainHand();
                mob.launchProjectile(projClass, dir.multiply(1.3));
            }

            // Item-in-hand ranged attack (auto-detect weapon)
            if (!mods.isRangedAttack() && distSq > 4 * 4 && distSq < 25 * 25 && Math.random() < 0.12) {
                org.bukkit.inventory.ItemStack mainHand = null;
                if (mob.getEquipment() != null) {
                    mainHand = mob.getEquipment().getItemInMainHand();
                }
                if (mainHand != null && mainHand.getType() != Material.AIR) {
                    String matName = mainHand.getType().name();
                    Location eye = mob.getEyeLocation();
                    org.bukkit.util.Vector dir = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
                    if (matName.equals("BOW") || matName.equals("CROSSBOW")) {
                        mob.swingMainHand();
                        Arrow arrow = mob.launchProjectile(Arrow.class, dir.multiply(1.5));
                        arrow.setDamage(mods.getDamageValue() > 0 ? mods.getDamageValue() * 0.5 : 3.0);
                    } else if (matName.equals("WIND_CHARGE")) {
                        try {
                            Class<? extends Projectile> windClass = (Class<? extends Projectile>) Class.forName("org.bukkit.entity.WindCharge");
                            mob.swingMainHand();
                            mob.launchProjectile(windClass, dir.multiply(1.2));
                        } catch (Exception ignored) {
                            mob.swingMainHand();
                            mob.launchProjectile(Snowball.class, dir.multiply(1.2));
                        }
                    } else if (matName.equals("SNOWBALL")) {
                        mob.swingMainHand();
                        mob.launchProjectile(Snowball.class, dir.multiply(1.3));
                    } else if (matName.equals("EGG")) {
                        mob.swingMainHand();
                        mob.launchProjectile(Egg.class, dir.multiply(1.3));
                    } else if (matName.equals("FIRE_CHARGE")) {
                        mob.swingMainHand();
                        mob.launchProjectile(SmallFireball.class, dir.multiply(0.8));
                    } else if (matName.equals("ENDER_PEARL")) {
                        mob.swingMainHand();
                        mob.launchProjectile(EnderPearl.class, dir.multiply(1.5));
                    } else if (matName.equals("TRIDENT")) {
                        mob.swingMainHand();
                        mob.launchProjectile(org.bukkit.entity.Trident.class, dir.multiply(1.4));
                    }
                }
            }

        }

        // 7. FUITE EN CAS DE FAIBLESSE OU SOLO
        if (entity instanceof Mob mob) {
            boolean shouldFlee = false;
            
            // Fuite sous un certain seuil de PV
            double threshold = mods.getFleeUnderHealth();
            if (threshold > 0.0) {
                double maxHp = mob.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (mob.getHealth() < maxHp * threshold) {
                    shouldFlee = true;
                }
            }
            
            // Fuite si solo
            if (mods.isFleeWhenSolo() && !shouldFlee) {
                boolean hasAlly = false;
                for (Entity nearby : mob.getNearbyEntities(15, 15, 15)) {
                    if (nearby.getType() == mob.getType() && !nearby.getUniqueId().equals(mob.getUniqueId()) && nearby instanceof LivingEntity) {
                        hasAlly = true;
                        break;
                    }
                }
                if (!hasAlly) {
                    shouldFlee = true;
                }
            }
            
            if (shouldFlee) {
                Player closestPlayer = null;
                double closestDistSq = Double.MAX_VALUE;
                for (Player p : mob.getWorld().getPlayers()) {
                    double distSq = p.getLocation().distanceSquared(mob.getLocation());
                    if (distSq < 15.0 * 15.0 && distSq < closestDistSq) {
                        closestPlayer = p;
                        closestDistSq = distSq;
                    }
                }
                if (closestPlayer != null) {
                    org.bukkit.util.Vector awayVector = mob.getLocation().toVector().subtract(closestPlayer.getLocation().toVector());
                    if (awayVector.lengthSquared() > 0) {
                        awayVector.normalize().multiply(8);
                    } else {
                        awayVector = new org.bukkit.util.Vector(Math.random() - 0.5, 0, Math.random() - 0.5).normalize().multiply(8);
                    }
                    Location awayLoc = mob.getLocation().add(awayVector);
                    mob.setTarget(null);
                    mob.getPathfinder().moveTo(awayLoc, 1.4);
                }
            }
        }

        // Custom Ambient Sound trigger (since the entity is silent, we play it ourselves)
        if (var.getCustomSounds().containsKey("ambient") && Math.random() < 0.03 && tickCount % 20 == 0) {
            MobVariant.SoundConfig sc = var.getCustomSounds().get("ambient");
            entity.getWorld().playSound(entity.getLocation(), sc.getSoundKey(), sc.getVolume(), sc.getPitch());
        }
    }

    private void spawnParticle(Particle particle, Location loc, String colorRGB) {
        World world = loc.getWorld();
        if (world == null) return;

        if (particle == Particle.DUST) {
            String[] parts = colorRGB.split(",");
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            Particle.DustOptions options = new Particle.DustOptions(Color.fromRGB(r, g, b), 1.0f);
            world.spawnParticle(particle, loc, 3, 0.2, 0.2, 0.2, options);
        } else {
            world.spawnParticle(particle, loc, 3, 0.2, 0.2, 0.2, 0.0);
        }
    }

    private void drawPlayerZoneOutlines(Player player) {
        DifficultyZone pending = plugin.getZoneManager().getPendingZone(player.getUniqueId());
        if (pending != null && pending.getType() == DifficultyZone.ZoneType.POLYGON && !pending.getPoints().isEmpty()) {
            List<double[]> pts = pending.getPoints();
            double y = player.getLocation().getY();
            for (int i = 0; i < pts.size(); i++) {
                double[] pt = pts.get(i);
                player.spawnParticle(Particle.HAPPY_VILLAGER, new Location(player.getWorld(), pt[0], y, pt[1]), 5, 0.1, 0.1, 0.1, 0.0);
                
                if (i < pts.size() - 1) {
                    double[] next = pts.get(i + 1);
                    drawOutlineSegment(player, pt[0], pt[1], next[0], next[1], y, Particle.COMPOSTER);
                }
            }
        }

        for (DifficultyZone zone : plugin.getZoneManager().getAllZones()) {
            if (zone.getType() == DifficultyZone.ZoneType.POLYGON && zone.getWorld().equals(player.getWorld().getName())) {
                List<double[]> pts = zone.getPoints();
                if (pts.size() >= 3) {
                    double y = player.getLocation().getY();
                    for (int i = 0; i < pts.size(); i++) {
                        double[] pt = pts.get(i);
                        double[] next = pts.get((i + 1) % pts.size());
                        drawOutlineSegment(player, pt[0], pt[1], next[0], next[1], y, Particle.HAPPY_VILLAGER);
                    }
                }
            }
        }
    }

    private void drawOutlineSegment(Player player, double x1, double z1, double x2, double z2, double y, Particle particle) {
        double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
        int steps = (int) (dist * 2.0);
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double px = x1 + (x2 - x1) * ratio;
            double pz = z1 + (z2 - z1) * ratio;
            player.spawnParticle(particle, new Location(player.getWorld(), px, y, pz), 1, 0, 0, 0, 0.0);
        }
    }

    private void drawNearbyZoneBoundaries(Player player) {
        double pX = player.getLocation().getX();
        double pY = player.getLocation().getY();
        double pZ = player.getLocation().getZ();

        for (DifficultyZone zone : plugin.getZoneManager().getAllZones()) {
            if (!zone.getWorld().equals(player.getWorld().getName())) continue;
            if (!zone.isParticlesEnabled()) continue;

            boolean playerInside = zone.contains(player.getWorld().getName(), pX, pY, pZ);
            String pName = playerInside ? zone.getParticleInside() : zone.getParticleOutside();
            if (pName == null || pName.equalsIgnoreCase("NONE")) continue;

            Particle particle;
            try {
                particle = Particle.valueOf(pName.toUpperCase());
            } catch (Exception e) {
                particle = Particle.FLAME;
            }

            if (zone.getType() == DifficultyZone.ZoneType.RADIUS) {
                double cx = zone.getCenterX();
                double cz = zone.getCenterZ();
                double radius = zone.getRadius();
                double dx = pX - cx;
                double dz = pZ - cz;
                double distToCenter = Math.sqrt(dx*dx + dz*dz);
                if (Math.abs(distToCenter - radius) < 15.0) {
                    int steps = 72; // 5 degrees per step
                    for (int i = 0; i < steps; i++) {
                        double theta = (i * 2.0 * Math.PI) / steps;
                        double px = cx + radius * Math.cos(theta);
                        double pz = cz + radius * Math.sin(theta);
                        double distToPlayer = Math.sqrt((px - pX)*(px - pX) + (pz - pZ)*(pz - pZ));
                        if (distToPlayer < 12.0) {
                            double pYReal = getGroundYAt(player.getWorld(), px, pz, pY) + zone.getParticleHeightOffset();
                            player.spawnParticle(particle, new Location(player.getWorld(), px, pYReal, pz), 1, 0, 0, 0, 0.0);
                        }
                    }
                }
            } else if (zone.getType() == DifficultyZone.ZoneType.CUBOID) {
                if (pX >= zone.getMinX() - 15.0 && pX <= zone.getMaxX() + 15.0 &&
                    pZ >= zone.getMinZ() - 15.0 && pZ <= zone.getMaxZ() + 15.0) {
                    
                    drawSegmentIfNear(player, zone.getMinX(), zone.getMinZ(), zone.getMaxX(), zone.getMinZ(), pX, pY, pZ, particle, zone);
                    drawSegmentIfNear(player, zone.getMaxX(), zone.getMinZ(), zone.getMaxX(), zone.getMaxZ(), pX, pY, pZ, particle, zone);
                    drawSegmentIfNear(player, zone.getMaxX(), zone.getMaxZ(), zone.getMinX(), zone.getMaxZ(), pX, pY, pZ, particle, zone);
                    drawSegmentIfNear(player, zone.getMinX(), zone.getMaxZ(), zone.getMinX(), zone.getMinZ(), pX, pY, pZ, particle, zone);
                }
            } else if (zone.getType() == DifficultyZone.ZoneType.POLYGON) {
                List<double[]> pts = zone.getPoints();
                if (pts.size() >= 3) {
                    for (int i = 0; i < pts.size(); i++) {
                        double[] pt = pts.get(i);
                        double[] next = pts.get((i + 1) % pts.size());
                        drawSegmentIfNear(player, pt[0], pt[1], next[0], next[1], pX, pY, pZ, particle, zone);
                    }
                }
            }
        }
    }

    private void drawSegmentIfNear(Player player, double x1, double z1, double x2, double z2, double pX, double pY, double pZ, Particle particle, DifficultyZone zone) {
        double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
        int steps = (int) (dist * 2.0);
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double px = x1 + (x2 - x1) * ratio;
            double pz = z1 + (z2 - z1) * ratio;
            double distToPlayer = Math.sqrt((px - pX) * (px - pX) + (pz - pZ) * (pz - pZ));
            if (distToPlayer < 12.0) {
                double pYReal = getGroundYAt(player.getWorld(), px, pz, pY) + zone.getParticleHeightOffset();
                player.spawnParticle(particle, new Location(player.getWorld(), px, pYReal, pz), 1, 0, 0, 0, 0.0);
            }
        }
    }

    private double getGroundYAt(World world, double x, double z, double playerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        for (int y = (int) Math.floor(playerY) + 3; y >= (int) Math.floor(playerY) - 5; y--) {
            if (world.getBlockAt(blockX, y, blockZ).getType().isSolid()) {
                return y + 1.0;
            }
        }
        return playerY;
    }

    private void runDaytimeSpawning() {
        MainConfigManager mainCfg = plugin.getMainConfigManager();
        boolean allowDay = mainCfg.isAllowDaySpawnGlobally();
        List<MobVariant> dayVariants = new ArrayList<>();
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            String st = var.getSpawnTime();
            if (st.equalsIgnoreCase("ANY") || st.equalsIgnoreCase("DAY")) {
                dayVariants.add(var);
            }
        }

        if (!allowDay && dayVariants.isEmpty()) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasMetadata("wd_npc")) continue;
            
            org.bukkit.GameMode gm = player.getGameMode();
            if (gm == org.bukkit.GameMode.CREATIVE || gm == org.bukkit.GameMode.SPECTATOR) continue;
            
            World world = player.getWorld();
            long time = world.getTime();
            boolean isDaytime = (time >= 0 && time <= 12300) || (time >= 23850);
            if (!isDaytime) continue;
            
            DifficultyZone playerZone = plugin.getZoneManager().getZoneAt(world.getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
            if (playerZone != null && playerZone.isSafeZone()) continue;
            
            int cap = mainCfg.getCapVariantesParJoueur();
            int maxDist = mainCfg.getMaxSpawnDistance();
            if (maxDist <= 0) maxDist = 64;
            
            int nearbyVariants = 0;
            for (Entity nearby : player.getNearbyEntities(maxDist, maxDist, maxDist)) {
                if (nearby instanceof LivingEntity && !nearby.hasMetadata("wd_npc")) {
                    if (nearby.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                        nearbyVariants++;
                    }
                }
            }
            if (nearbyVariants >= cap) continue;
            
            for (int attempt = 0; attempt < 3; attempt++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 24.0 + Math.random() * 24.0;
                double px = player.getLocation().getX() + Math.cos(angle) * dist;
                double pz = player.getLocation().getZ() + Math.sin(angle) * dist;
                
                int bx = (int) Math.floor(px);
                int by = player.getLocation().getBlockY();
                
                Location candidateLoc = new Location(world, px, by, pz);
                Location safeLoc = MobSpawnListener.findSafeSpawnLocationAround(candidateLoc);
                if (safeLoc == null) continue;
                
                // Depth matching check
                boolean playerUnderground = MobSpawnListener.isUnderground(player.getLocation());
                boolean spawnUnderground = MobSpawnListener.isUnderground(safeLoc);
                if (playerUnderground != spawnUnderground) continue;
                
                DifficultyZone locZone = plugin.getZoneManager().getZoneAt(world.getName(), safeLoc.getX(), safeLoc.getY(), safeLoc.getZ());
                if (locZone != null && locZone.isSafeZone()) continue;
                
                List<EntityType> triggerTypes = new ArrayList<>();
                for (MobVariant var : dayVariants) {
                    if (!triggerTypes.contains(var.getType())) {
                        triggerTypes.add(var.getType());
                    }
                }
                
                if (triggerTypes.isEmpty()) break;
                
                EntityType chosenType = triggerTypes.get((int) (Math.random() * triggerTypes.size()));
                
                String biomeKey;
                try {
                    org.bukkit.block.Biome b = world.getBiome(safeLoc.getBlockX(), safeLoc.getBlockY(), safeLoc.getBlockZ());
                    org.bukkit.NamespacedKey key = org.bukkit.Registry.BIOME.getKey(b);
                    biomeKey = (key != null) ? key.toString() : b.name();
                } catch (Throwable t) {
                    biomeKey = world.getBiome(safeLoc.getBlockX(), safeLoc.getBlockY(), safeLoc.getBlockZ()).name();
                }
                
                String zoneId = (locZone != null) ? locZone.getId() : null;
                List<String> allowedVars = locZone != null ? locZone.getAllowedVariants() : plugin.getBiomeConfigManager().getAllowedVariants(biomeKey);
                List<String> deniedVars = locZone != null ? locZone.getDeniedVariants() : plugin.getBiomeConfigManager().getDeniedVariants(biomeKey);
                
                MobVariant selectedVar = plugin.getVariantManager().getRandomVariantFor(chosenType, allowedVars, deniedVars, world, biomeKey, safeLoc);
                if (selectedVar != null) {
                    String st = selectedVar.getSpawnTime();
                    if (st.equalsIgnoreCase("ANY") || st.equalsIgnoreCase("DAY")) {
                        Class<? extends Entity> eClass = chosenType.getEntityClass();
                        if (eClass != null) {
                            if (world.getDifficulty() == org.bukkit.Difficulty.PEACEFUL && org.bukkit.entity.Enemy.class.isAssignableFrom(eClass)) {
                                break;
                            }
                            try {
                                world.spawn(safeLoc, eClass, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);
                                break;
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        }
    }

    private void runZoneBeaconEffects() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location loc = player.getLocation();
            DifficultyZone zone = plugin.getZoneManager().getZoneAt(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
            if (zone != null && !zone.getBeaconEffects().isEmpty()) {
                for (java.util.Map.Entry<String, Integer> entry : zone.getBeaconEffects().entrySet()) {
                    try {
                        org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(entry.getKey().toUpperCase());
                        if (type != null) {
                            player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, 100, entry.getValue(), false, true));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
