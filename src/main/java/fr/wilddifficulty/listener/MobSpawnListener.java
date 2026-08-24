package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.BiomeConfigManager;
import fr.wilddifficulty.config.MainConfigManager;
import fr.wilddifficulty.config.MobConfigManager;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.util.AttributeUtil;
import fr.wilddifficulty.util.EquipmentUtil;
import fr.wilddifficulty.util.NametagUtil;
import fr.wilddifficulty.util.EntitySubtypeUtil;
import fr.wilddifficulty.variant.MobSquad;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.variant.VariantManager;
import fr.wilddifficulty.zone.DifficultyZone;
import fr.wilddifficulty.zone.ZoneManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class MobSpawnListener implements Listener {

    private final WildDifficultyPlugin plugin;
    public static NamespacedKey KEY_SQUAD_SPAWNED;
    public static NamespacedKey KEY_VARIANT_ID;
    public static NamespacedKey KEY_SQUAD_ID;

    public MobSpawnListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        if (KEY_SQUAD_SPAWNED == null) {
            KEY_SQUAD_SPAWNED = new NamespacedKey(plugin, "wd_squad_spawned");
            KEY_VARIANT_ID = new NamespacedKey(plugin, "wd_variant");
            KEY_SQUAD_ID = new NamespacedKey(plugin, "wd_squad_id");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Skip spawn if too far from players to optimize (only for natural/chunk gen/default spawns)
        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL 
                || reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CHUNK_GEN 
                || reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT) {
            int maxDist = plugin.getMainConfigManager().getMaxSpawnDistance();
            if (maxDist > 0) {
                boolean playerNearby = false;
                double maxDistSq = maxDist * maxDist;
                for (Player p : entity.getWorld().getPlayers()) {
                    if (!p.hasMetadata("wd_npc") && p.getLocation().distanceSquared(entity.getLocation()) <= maxDistSq) {
                        playerNearby = true;
                        break;
                    }
                }
                if (!playerNearby) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        EntityType entityType = entity.getType();
        String mobKey = entityType.name();

        // Do not process Citizens NPCs in normal spawn logic
        boolean isNpc = entity.hasMetadata("wd_npc");
        if (!isNpc && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            try {
                isNpc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(entity);
            } catch (Throwable ignored) {}
        }
        if (isNpc) {
            return;
        }

        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        // Cave vs Surface depth matching logic
        if (world.getEnvironment() == World.Environment.NORMAL 
                && reason != org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.COMMAND 
                && reason != org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM) {
            
            Player nearestPlayer = null;
            double nearestDistSq = Double.MAX_VALUE;
            for (Player p : world.getPlayers()) {
                if (!p.hasMetadata("wd_npc")) {
                    double d = p.getLocation().distanceSquared(loc);
                    if (d < nearestDistSq) {
                        nearestDistSq = d;
                        nearestPlayer = p;
                    }
                }
            }
            if (nearestPlayer != null && nearestDistSq <= 128 * 128) {
                boolean playerUnderground = isUnderground(nearestPlayer.getLocation());
                boolean spawnUnderground = isUnderground(loc);
                if (playerUnderground != spawnUnderground) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        MainConfigManager mainCfg  = plugin.getMainConfigManager();
        BiomeConfigManager biomeCfg = plugin.getBiomeConfigManager();
        MobConfigManager mobCfg    = plugin.getMobConfigManager();
        ZoneManager zoneManager    = plugin.getZoneManager();
        VariantManager varManager  = plugin.getVariantManager();

        String biomeKey;
        try {
            org.bukkit.block.Biome b = world.getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            org.bukkit.NamespacedKey key = org.bukkit.Registry.BIOME.getKey(b);
            biomeKey = (key != null) ? key.toString() : b.name();
        } catch (Throwable t) {
            biomeKey = world.getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).name();
        }

        DifficultyZone zone = zoneManager.getZoneAt(world.getName(), loc.getX(), loc.getY(), loc.getZ());
        String zoneId = (zone != null) ? zone.getId() : null;

        if (zone != null && zone.isSafeZone() && entity instanceof org.bukkit.entity.Enemy) {
            event.setCancelled(true);
            return;
        }

        boolean ignoresBiomeRules = zone != null && zone.isOverrideBiomeRules();
        if (!ignoresBiomeRules && !biomeCfg.isMobAllowedInBiome(biomeKey, mobKey)) {
            event.setCancelled(true);
            return;
        }

        if (zone != null && !zone.isMobAllowed(mobKey)) {
            event.setCancelled(true);
            return;
        }

        // Variantes & Escouades
        MobVariant variant = null;
        boolean isSquadSpawn = entity.getPersistentDataContainer().has(KEY_SQUAD_SPAWNED, PersistentDataType.BYTE);

        if (isSquadSpawn) {
            String vId = entity.getPersistentDataContainer().get(KEY_VARIANT_ID, PersistentDataType.STRING);
            if (vId != null) variant = varManager.getVariant(vId);
        } else {
            // Check for squad trigger
            List<String> allowedSquads = zone != null ? zone.getAllowedSquads() : biomeCfg.getAllowedSquads(biomeKey);
            List<String> deniedSquads = zone != null ? zone.getDeniedSquads() : biomeCfg.getDeniedSquads(biomeKey);
            MobSquad squad = null;
            if (zone == null || !zone.isSafeZone()) {
                squad = varManager.rollSquad(entityType, biomeKey, zoneId, allowedSquads, deniedSquads);
            }
            
            if (squad != null) {
                event.setCancelled(true);
                // Spawn members slightly offset
                final MobSquad finalSquad = squad;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (Map.Entry<String, MobSquad.SquadMemberRange> entry : finalSquad.getMembers().entrySet()) {
                        final MobVariant v = varManager.getVariant(entry.getKey());
                        if (v == null) continue;
                        int count = varManager.getRandomMemberCount(entry.getValue());
                        for (int i = 0; i < count; i++) {
                            Location spawnLoc = findSafeSpawnLocationAround(loc);
                            if (spawnLoc == null) continue;
                            if (v.getType() == org.bukkit.entity.EntityType.PLAYER && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                                final MobVariant fVar = v;
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    double hp = fVar.getModifiers().getHealthValue() > 0 ? fVar.getModifiers().getHealthValue() : 20.0;
                                    double dmg = fVar.getModifiers().getDamageValue() > 0 ? fVar.getModifiers().getDamageValue() : 4.0;
                                    double spd = fVar.getModifiers().getSpeedValue() > 0 ? fVar.getModifiers().getSpeedValue() : 0.25;
                                    
                                    hp *= finalSquad.getBonusHealth();
                                    dmg *= finalSquad.getBonusDamage();
                                    spd *= finalSquad.getBonusSpeed();
                                    
                                    String skin = fVar.getModifiers().getSkullSkin();
                                    String name = fVar.getDisplayName();
                                    if (name == null || name.isEmpty()) name = fVar.getId();
                                    
                                    org.bukkit.entity.Entity spawned = fr.wilddifficulty.util.CitizensHook.spawnNPCPlayer(
                                        fVar.getId(), spawnLoc, name, skin, hp, dmg, spd, plugin
                                    );
                                    if (spawned != null) {
                                        spawned.getPersistentDataContainer().set(KEY_SQUAD_ID, PersistentDataType.STRING, finalSquad.getId());
                                        spawned.getPersistentDataContainer().set(KEY_SQUAD_SPAWNED, PersistentDataType.BYTE, (byte) 1);
                                    }
                                });
                            } else {
                                Class<? extends org.bukkit.entity.Entity> eClass = v.getType().getEntityClass();
                                if (v.getType() == org.bukkit.entity.EntityType.PLAYER) {
                                    eClass = org.bukkit.entity.Zombie.class;
                                }
                                if (world.getDifficulty() == org.bukkit.Difficulty.PEACEFUL && eClass != null && org.bukkit.entity.Enemy.class.isAssignableFrom(eClass)) {
                                    continue;
                                }
                                final MobVariant fVar = v;
                                try {
                                    world.spawn(spawnLoc, eClass, CreatureSpawnEvent.SpawnReason.CUSTOM, member -> {
                                        member.getPersistentDataContainer().set(KEY_SQUAD_SPAWNED, PersistentDataType.BYTE, (byte) 1);
                                        member.getPersistentDataContainer().set(KEY_VARIANT_ID, PersistentDataType.STRING, fVar.getId());
                                        member.getPersistentDataContainer().set(KEY_SQUAD_ID, PersistentDataType.STRING, finalSquad.getId());
                                        member.setMetadata("no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                        member.setMetadata("rose-no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                        member.setMetadata("wildstacker:no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                    });
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                });
                return; // On arrête là pour ce spawn
            } else {
                List<String> allowedVars = zone != null ? zone.getAllowedVariants() : biomeCfg.getAllowedVariants(biomeKey);
                List<String> deniedVars = zone != null ? zone.getDeniedVariants() : biomeCfg.getDeniedVariants(biomeKey);
                variant = varManager.getRandomVariantFor(entityType, allowedVars, deniedVars, world, biomeKey, loc);
                
                // Si aucune variante et que l'entité est bannie du spawn naturel, on annule
                if (variant == null) {
                    boolean isBanned = mainCfg.isNaturalEntityBanned(mobKey);
                    if (isBanned && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.COMMAND && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // Enregistrer l'ID de variante sur le mob pour la persistance
                if (variant != null) {
                    // Safezone aggressive variant block
                    if (zone != null && zone.isSafeZone()) {
                        String mode = variant.getAggroMode();
                        if ("AGGRESSIVE".equalsIgnoreCase(mode) || "NEUTRAL_HIT".equalsIgnoreCase(mode) || "NEUTRAL_SQUAD_HIT".equalsIgnoreCase(mode)) {
                            event.setCancelled(true);
                            return;
                        }
                    }

                    // Limite de 50 variantes max par joueur à proximité (uniquement pour les variantes)
                    if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.COMMAND && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
                        int checkDist = plugin.getMainConfigManager().getMaxSpawnDistance();
                        if (checkDist <= 0) checkDist = 64;
                        double checkDistSq = checkDist * checkDist;

                        java.util.List<Player> nearbyPlayers = new java.util.ArrayList<>();
                        for (Player p : world.getPlayers()) {
                            if (!p.hasMetadata("wd_npc") && p.getLocation().distanceSquared(loc) <= checkDistSq) {
                                nearbyPlayers.add(p);
                            }
                        }

                        if (!nearbyPlayers.isEmpty()) {
                            int customVariantCount = 0;
                            java.util.Set<java.util.UUID> countedMobs = new java.util.HashSet<>();
                            for (Player p : nearbyPlayers) {
                                for (Entity nearby : p.getNearbyEntities(checkDist, checkDist, checkDist)) {
                                    if (nearby instanceof LivingEntity && !nearby.hasMetadata("wd_npc")) {
                                        if (nearby.getPersistentDataContainer().has(KEY_VARIANT_ID, PersistentDataType.STRING)) {
                                            if (countedMobs.add(nearby.getUniqueId())) {
                                                customVariantCount++;
                                            }
                                        }
                                    }
                                }
                            }
                            int cap = plugin.getMainConfigManager().getCapVariantesParJoueur();
                            if (customVariantCount >= cap * nearbyPlayers.size()) {
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }

                    if (handleCitizensSpawn(event, variant, loc, biomeKey, zoneId, zone, mobKey)) {
                        return;
                    }
                    entity.getPersistentDataContainer().set(KEY_VARIANT_ID, PersistentDataType.STRING, variant.getId());
                    entity.setMetadata("no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    entity.setMetadata("rose-no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    entity.setMetadata("wildstacker:no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                }
            }
        }

        // Lune de Sang - Multiplicateur de Spawn
        if (mainCfg.isBloodMoonActive() 
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM 
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.COMMAND
                && !entity.hasMetadata("wd_bloodmoon_duplicated")
                && !isSquadSpawn) {
            double mult = mainCfg.getBloodMoonSpawnMultiplier();
            if (mult > 1.0) {
                double extra = mult - 1.0;
                int count = (int) Math.floor(extra);
                double chance = extra - count;
                if (Math.random() <= chance) {
                    count++;
                }
                if (count > 0) {
                    final int finalCount = count;
                    final MobVariant fVariant = variant;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (int i = 0; i < finalCount; i++) {
                            Location spawnLoc = findSafeSpawnLocationAround(loc);
                            if (spawnLoc == null) continue;
                            Class<? extends Entity> entityClass = entity.getType().getEntityClass();
                            if (entityClass != null) {
                                if (world.getDifficulty() == org.bukkit.Difficulty.PEACEFUL && org.bukkit.entity.Enemy.class.isAssignableFrom(entityClass)) {
                                    continue;
                                }
                                try {
                                    world.spawn(spawnLoc, (Class<Entity>) entityClass, CreatureSpawnEvent.SpawnReason.CUSTOM, spawned -> {
                                        spawned.setMetadata("wd_bloodmoon_duplicated", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                        if (fVariant != null) {
                                            spawned.getPersistentDataContainer().set(KEY_VARIANT_ID, PersistentDataType.STRING, fVariant.getId());
                                        }
                                        spawned.setMetadata("no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                        spawned.setMetadata("rose-no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                        spawned.setMetadata("wildstacker:no-stack", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                    });
                                } catch (Throwable ignored) {}
                            }
                        }
                    });
                }
            }
        }

        StatModifiers finalMod = new StatModifiers();
        // Couches
        if (!ignoresBiomeRules) {
            StatModifiers biomeMod = biomeCfg.getBiomeModifiers(biomeKey);
            if (biomeMod != null) finalMod.stackWith(biomeMod);
            StatModifiers biomeOverride = biomeCfg.getBiomeMobOverride(biomeKey, mobKey);
            if (biomeOverride != null) finalMod.stackWith(biomeOverride);
        }
        if (zone != null) {
            finalMod.stackWith(zone.getModifiers());
            if (zone.isDangerNest() && zone.getNestModifiers() != null) {
                finalMod.stackWith(zone.getNestModifiers());
            }
        }
        StatModifiers mobMod = mobCfg.getMobModifiers(mobKey);
        if (mobMod != null) finalMod.stackWith(mobMod);
        if (variant != null && variant.getModifiers() != null) {
            finalMod.stackWith(variant.getModifiers());
        }

        // Récupérer les valeurs ou utiliser le défaut vanilla de l'entité
        double hp = finalMod.getHealthValue() > 0 ? finalMod.getHealthValue() : getBaseValue(entity, Attribute.MAX_HEALTH);
        double dmg = finalMod.getDamageValue() > 0 ? finalMod.getDamageValue() : getBaseValue(entity, Attribute.ATTACK_DAMAGE);
        double spd = finalMod.getSpeedValue() > 0 ? finalMod.getSpeedValue() : getBaseValue(entity, Attribute.MOVEMENT_SPEED);
        double fr = finalMod.getFollowRangeValue() > 0 ? finalMod.getFollowRangeValue() : getBaseValue(entity, Attribute.FOLLOW_RANGE);
        double kb = finalMod.getKnockbackValue() >= 0 ? finalMod.getKnockbackValue() : getBaseValue(entity, Attribute.KNOCKBACK_RESISTANCE);

        // Distance Scaling (Multiplicateur sur la valeur absolue)
        double distMult = mainCfg.computeDistanceMultiplier(world.getName(), loc.getX(), loc.getZ());
        if (distMult != 1.0) {
            if (mainCfg.isDistanceApplyHealth()) hp *= distMult;
            if (mainCfg.isDistanceApplyDamage()) dmg *= distMult;
            if (mainCfg.isDistanceApplySpeed()) spd *= distMult;
            if (mainCfg.isDistanceApplyFollowRange()) fr *= distMult;
            if (mainCfg.isDistanceApplyKnockback()) kb *= distMult;
        }

        // Zone-specific external scaling — par statistique (PV, Dégâts, Vitesse)
        DifficultyZone extZone = zone;
        if (extZone == null) {
            extZone = zoneManager.getExternalScalingZone(world.getName(), loc.getX(), loc.getZ());
        }
        double hpMult = 1.0;
        double dmgMult = 1.0;
        double spdMult = 1.0;
        if (extZone != null && extZone.hasExtScaling()) {
            double dx = loc.getX() - extZone.getCenterX();
            double dz = loc.getZ() - extZone.getCenterZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > extZone.getRadius()) {
                hpMult  = extZone.computeHpExtMult(distance);
                dmgMult = extZone.computeDmgExtMult(distance);
                spdMult = extZone.computeSpdExtMult(distance);
                if (hpMult  != 1.0) hp  *= hpMult;
                if (dmgMult != 1.0 && dmg > 0) dmg *= dmgMult;
                if (spdMult != 1.0 && spd > 0) spd *= spdMult;
                // Knockback/FollowRange scale with the average of hp+dmg
                double avgMult = (hpMult + dmgMult) / 2.0;
                if (avgMult != 1.0) { fr *= avgMult; kb *= avgMult; }
            }
        }

        // Application des bonus d'escouade
        if (entity.getPersistentDataContainer().has(KEY_SQUAD_ID, PersistentDataType.STRING)) {
            String squadId = entity.getPersistentDataContainer().get(KEY_SQUAD_ID, PersistentDataType.STRING);
            MobSquad sq = varManager.getSquad(squadId);
            if (sq != null) {
                hp *= sq.getBonusHealth();
                if (dmg > 0) dmg *= sq.getBonusDamage();
                if (spd > 0) spd *= sq.getBonusSpeed();
                if (sq.getBonusRegen() > 0) {
                    finalMod.setPassiveRegen(finalMod.getPassiveRegen() + sq.getBonusRegen());
                }
            }
        }

        // Application des multiplicateurs de Lune de Sang
        if (plugin.getMainConfigManager().isBloodMoonActive()) {
            hp *= plugin.getMainConfigManager().getBloodMoonHpMultiplier();
            if (dmg > 0) dmg *= plugin.getMainConfigManager().getBloodMoonDamageMultiplier();
            if (spd > 0) spd *= plugin.getMainConfigManager().getBloodMoonSpeedMultiplier();
        }

        // Application des attributs absolute
        AttributeUtil.applyHealth(entity, hp);
        AttributeUtil.applyDamage(entity, dmg);
        AttributeUtil.applySpeed(entity, spd);
        AttributeUtil.applyFollowRange(entity, fr);
        AttributeUtil.applyKnockbackResistance(entity, kb);

        // Application scale et bébé
        if (variant != null) {
            // Mute vanilla sounds if custom sounds are set
            if (!variant.getCustomSounds().isEmpty()) {
                entity.setSilent(true);
            } else {
                entity.setSilent(false);
            }

            if (variant.isBaby()) {
                if (entity instanceof Ageable ageable) {
                    ageable.setBaby();
                } else if (entity instanceof org.bukkit.entity.Zombie zombie) {
                    zombie.setBaby(true);
                } else if (entity instanceof org.bukkit.entity.PiglinAbstract piglin) {
                    try {
                        piglin.getClass().getMethod("setBaby", boolean.class).invoke(piglin, true);
                    } catch (Exception ignored) {}
                }
            }

            // Custom cosmetic traits (creeper powered, golem cracked, sheep sheared, enderman block)
            StatModifiers vMods = variant.getModifiers();
            if (vMods != null) {
                if (vMods.isCreeperPowered() && entity instanceof org.bukkit.entity.Creeper creeper) {
                    creeper.setPowered(true);
                }
                if (!"none".equalsIgnoreCase(vMods.getGolemCracked()) && entity instanceof org.bukkit.entity.IronGolem golem) {
                    try {
                        String crack = vMods.getGolemCracked().toUpperCase();
                        if ("TRUE".equals(crack)) crack = "HIGH";
                        final String crackFinal = crack;
                        // Try setCrackState via reflection (not all Paper builds expose it directly)
                        try {
                            Class<?> crackClass = Class.forName("org.bukkit.entity.IronGolem$CrackState");
                            Object crackVal = Enum.valueOf((Class<Enum>) crackClass, crackFinal);
                            golem.getClass().getMethod("setCrackState", crackClass).invoke(golem, crackVal);
                        } catch (Exception ex) {
                            // Fallback: simulate with health %
                            double pct = 1.0;
                            if (crack.equals("LOW")) pct = 0.7;
                            else if (crack.equals("MEDIUM")) pct = 0.45;
                            else if (crack.equals("HIGH")) pct = 0.2;
                            double maxHp = getBaseValue(golem, Attribute.MAX_HEALTH);
                            golem.setHealth(Math.min(maxHp * pct, golem.getHealth()));
                        }
                    } catch (Exception ignored) {}
                }
                if (vMods.isSheepSheared() && entity instanceof org.bukkit.entity.Sheep sheep) {
                    sheep.setSheared(true);
                }
                if (!"none".equals(vMods.getEndermanBlock()) && entity instanceof org.bukkit.entity.Enderman enderman) {
                    try {
                        Material mat = Material.matchMaterial(vMods.getEndermanBlock());
                        if (mat != null && mat.isBlock()) {
                            enderman.setCarriedBlock(org.bukkit.Bukkit.createBlockData(mat));
                        }
                    } catch (Exception ignored) {}
                }
            }

            double baseScale = variant.getScale();
            double scaleVar = variant.getScaleVariance();
            if (baseScale != 1.0 || scaleVar > 0) {
                double rolledScale = baseScale;
                if (scaleVar > 0) {
                    rolledScale = baseScale + (Math.random() * 2.0 - 1.0) * scaleVar;
                    rolledScale = Math.max(0.1, rolledScale); // safety limit
                }
                AttributeUtil.applyScale(entity, rolledScale);
            }

            // Spawn particle effect
            if (vMods != null && !"none".equals(vMods.getParticleSpawnType())) {
                try {
                    Particle p = Particle.valueOf(vMods.getParticleSpawnType().toUpperCase());
                    world.spawnParticle(p, loc.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.1);
                } catch (Exception ignored) {}
            }
        }

        // Sun immunity
        if (variant != null && (variant.isIgnoreSunlight() || "DAY".equalsIgnoreCase(variant.getSpawnTime()) || "ANY".equalsIgnoreCase(variant.getSpawnTime()))) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
            entity.setMetadata("wd_no_burn", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }

        // Résistance au feu globale (si applicable)
        if (finalMod.getResistanceFeu() == 0.0) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        }

        // Status effects / Potion effects
        if (!finalMod.getPotionEffects().isEmpty()) {
            for (String effectName : finalMod.getPotionEffects()) {
                try {
                    PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                    if (type != null) {
                        entity.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, 0, false, false));
                    }
                } catch (Exception ignored) {}
            }
        }

        EquipmentUtil.applyEquipment(entity, finalMod, variant != null ? variant.getCustomModelData() : 0);
        EntitySubtypeUtil.applySubtype(entity, finalMod.getEntitySubtype());

        // Forcer l'application 1 tick après le spawn pour outrepasser l'écrasement de skin par le biome vanilla !
        if (finalMod.getEntitySubtype() != null && !finalMod.getEntitySubtype().isBlank() && !"none".equalsIgnoreCase(finalMod.getEntitySubtype())) {
            String st = finalMod.getEntitySubtype();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    EntitySubtypeUtil.applySubtype(entity, st);
                }
            }, 1L);
        }

        if (entity instanceof Mob mob) {
            boolean isBoss = variant != null && variant.getModifiers() != null && variant.getModifiers().isBossBarEnabled();
            if (!isBoss) {
                mob.setRemoveWhenFarAway(true);
            } else {
                mob.setRemoveWhenFarAway(false);
            }

            // Remove tempt goals so custom variants (especially aggressive ones) don't follow players holding food
            if (variant != null) {
                try {
                    var mobGoals = org.bukkit.Bukkit.getMobGoals();
                    for (var goal : mobGoals.getAllGoals(mob)) {
                        String goalName = goal.getClass().getSimpleName().toLowerCase();
                        String keyName = goal.getKey().getNamespacedKey().getKey().toLowerCase();
                        if (goalName.contains("tempt") || keyName.contains("tempt")) {
                            mobGoals.removeGoal(mob, goal);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (mainCfg.isNametagsEnabled()) {
            NametagUtil.applyNametag(entity, mainCfg.getNametagFormat(), 0, variant);
        }

        if (mainCfg.isDebug()) {
            plugin.getLogger().info("[WD-DEBUG] Spawned Custom Mob: " + entityType.name() + " (" + (variant != null ? variant.getId() : "base") + ")"
                + " at " + world.getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                + " | HP=" + String.format("%.1f", hp) + " dmg=" + String.format("%.1f", dmg)
                + " speed=" + String.format("%.3f", spd) + " distMult=" + String.format("%.2f", distMult)
                + " extMult=[HP:" + String.format("%.2f", hpMult) + ",DMG:" + String.format("%.2f", dmgMult) + ",SPD:" + String.format("%.2f", spdMult) + "]");
        }
    }

    private double getBaseValue(LivingEntity entity, Attribute attr) {
        AttributeInstance inst = entity.getAttribute(attr);
        return inst != null ? inst.getBaseValue() : 0.0;
    }

    private boolean handleCitizensSpawn(CreatureSpawnEvent event, MobVariant variant, Location loc, String biomeKey, String zoneId, DifficultyZone zone, String mobKey) {
        if (variant == null || variant.getType() != org.bukkit.entity.EntityType.PLAYER) {
            return false;
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            return false;
        }
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            fr.wilddifficulty.config.BiomeConfigManager biomeCfg2 = plugin.getBiomeConfigManager();
            fr.wilddifficulty.config.MobConfigManager mobCfg2 = plugin.getMobConfigManager();
            fr.wilddifficulty.config.MainConfigManager mainCfg2 = plugin.getMainConfigManager();
            StatModifiers finalMod = new StatModifiers();
            StatModifiers biomeMod = biomeCfg2.getBiomeModifiers(biomeKey);
            if (biomeMod != null) finalMod.stackWith(biomeMod);
            StatModifiers biomeOverride = biomeCfg2.getBiomeMobOverride(biomeKey, mobKey);
            if (biomeOverride != null) finalMod.stackWith(biomeOverride);
            if (zone != null) {
                finalMod.stackWith(zone.getModifiers());
                if (zone.isDangerNest() && zone.getNestModifiers() != null) {
                    finalMod.stackWith(zone.getNestModifiers());
                }
            }
            StatModifiers mobMod = mobCfg2.getMobModifiers(mobKey);
            if (mobMod != null) finalMod.stackWith(mobMod);
            if (variant.getModifiers() != null) {
                finalMod.stackWith(variant.getModifiers());
            }

            double hp = finalMod.getHealthValue() > 0 ? finalMod.getHealthValue() : 20.0;
            double dmg = finalMod.getDamageValue() > 0 ? finalMod.getDamageValue() : 4.0;
            double spd = finalMod.getSpeedValue() > 0 ? finalMod.getSpeedValue() : 0.25;

            double distMult = mainCfg2.computeDistanceMultiplier(loc.getWorld().getName(), loc.getX(), loc.getZ());
            if (distMult != 1.0) {
                if (mainCfg2.isDistanceApplyHealth()) hp *= distMult;
                if (mainCfg2.isDistanceApplyDamage()) dmg *= distMult;
                if (mainCfg2.isDistanceApplySpeed()) spd *= distMult;
            }

            DifficultyZone extZone = zone;
            if (extZone == null) {
                extZone = plugin.getZoneManager().getExternalScalingZone(loc.getWorld().getName(), loc.getX(), loc.getZ());
            }
            double extMult = 1.0;
            if (extZone != null && extZone.getExtStep() > 0.0) {
                double dx = loc.getX() - extZone.getCenterX();
                double dz = loc.getZ() - extZone.getCenterZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > extZone.getRadius()) {
                    double excess = distance - extZone.getRadius();
                    long steps = (long) Math.floor(excess / extZone.getExtStep());
                    extMult = 1.0 + steps * extZone.getExtMultPerStep();
                    extMult = Math.min(extZone.getExtMaxMult(), extMult);
                    if (extMult != 1.0) {
                        hp *= extMult;
                        dmg *= extMult;
                        spd *= extMult;
                    }
                }
            }

            if (plugin.getMainConfigManager().isBloodMoonActive()) {
                hp *= plugin.getMainConfigManager().getBloodMoonHpMultiplier();
                dmg *= plugin.getMainConfigManager().getBloodMoonDamageMultiplier();
                spd *= plugin.getMainConfigManager().getBloodMoonSpeedMultiplier();
            }

            String skin = finalMod.getSkullSkin();
            String name = variant.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = variant.getId();
            }
            org.bukkit.entity.Entity spawned = fr.wilddifficulty.util.CitizensHook.spawnNPCPlayer(
                variant.getId(), loc, name, skin, hp, dmg, spd, plugin
            );
            
            if (spawned != null) {
                spawned.getPersistentDataContainer().set(KEY_VARIANT_ID, PersistentDataType.STRING, variant.getId());
                if (event.getEntity().getPersistentDataContainer().has(KEY_SQUAD_ID, PersistentDataType.STRING)) {
                    String squadId = event.getEntity().getPersistentDataContainer().get(KEY_SQUAD_ID, PersistentDataType.STRING);
                    spawned.getPersistentDataContainer().set(KEY_SQUAD_ID, PersistentDataType.STRING, squadId);
                    spawned.getPersistentDataContainer().set(KEY_SQUAD_SPAWNED, PersistentDataType.BYTE, (byte) 1);
                }
            }

            if (mainCfg2.isDebug()) {
                plugin.getLogger().info("[WD-DEBUG] Spawned Citizens PLAYER NPC: " + name + " (" + variant.getId() + ")"
                    + " at " + loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " | HP=" + String.format("%.1f", hp) + " dmg=" + String.format("%.1f", dmg)
                    + " speed=" + String.format("%.3f", spd) + " distMult=" + String.format("%.2f", distMult)
                    + " extMult=" + String.format("%.2f", extMult));
            }
        });
        return true;
    }

    public static boolean isUnderground(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        if (world.getEnvironment() != World.Environment.NORMAL) return false;
        
        int highestY = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        if (loc.getY() >= highestY - 8) {
            return false;
        }
        
        for (int y = loc.getBlockY() + 1; y < highestY; y++) {
            if (world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSafeFloorBlock(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (!type.isSolid()) return false;
        if (block.isLiquid() || type == Material.WATER || type == Material.LAVA || type == Material.MAGMA_BLOCK 
                || type == Material.FIRE || type == Material.SOUL_FIRE || type == Material.CACTUS 
                || type == Material.POWDER_SNOW || type == Material.BARRIER || type == Material.SEAGRASS 
                || type == Material.TALL_SEAGRASS || type == Material.KELP || type == Material.KELP_PLANT
                || type.name().contains("LEAVES") || type.name().contains("FENCE") || type.name().contains("WALL") 
                || type.name().contains("GLASS") || type.name().contains("SLAB") || type.name().contains("STAIR")
                || type.name().contains("TRAPDOOR") || type.name().contains("GATE")) {
            return false;
        }
        return true;
    }

    public static boolean isSafeAirBlock(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (type.isSolid() || block.isLiquid() || type == Material.WATER || type == Material.LAVA 
                || type == Material.POWDER_SNOW || type == Material.FIRE || type == Material.SOUL_FIRE
                || type == Material.SEAGRASS || type == Material.TALL_SEAGRASS || type == Material.KELP || type == Material.KELP_PLANT
                || type.name().contains("LEAVES") || type.name().contains("FENCE") || type.name().contains("WALL")
                || type.name().contains("TRAPDOOR") || type.name().contains("DOOR") || type.name().contains("GATE") || type.name().contains("WATER")) {
            return false;
        }
        return true;
    }

    public static Location ensureSafeLocation(Location center) {
        if (center == null || center.getWorld() == null) return null;
        World world = center.getWorld();
        int bx = center.getBlockX();
        int bz = center.getBlockZ();
        int playerY = center.getBlockY();

        for (int dy = 0; dy <= 5; dy++) {
            for (int sign : new int[]{0, 1, -1}) {
                if (dy == 0 && sign != 0) continue;
                int y = playerY + (dy * sign);
                if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) continue;
                
                Block floor = world.getBlockAt(bx, y - 1, bz);
                Block legs = world.getBlockAt(bx, y, bz);
                Block head = world.getBlockAt(bx, y + 1, bz);

                if (isSafeFloorBlock(floor) && isSafeAirBlock(legs) && isSafeAirBlock(head)) {
                    return new Location(world, bx + 0.5, y, bz + 0.5, center.getYaw(), center.getPitch());
                }
            }
        }

        for (int y = playerY - 6; y >= Math.max(world.getMinHeight() + 1, playerY - 20); y--) {
            Block floor = world.getBlockAt(bx, y - 1, bz);
            Block legs = world.getBlockAt(bx, y, bz);
            Block head = world.getBlockAt(bx, y + 1, bz);
            if (isSafeFloorBlock(floor) && isSafeAirBlock(legs) && isSafeAirBlock(head)) {
                return new Location(world, bx + 0.5, y, bz + 0.5, center.getYaw(), center.getPitch());
            }
        }

        Block floor = world.getBlockAt(bx, playerY - 1, bz);
        Block legs = world.getBlockAt(bx, playerY, bz);
        Block head = world.getBlockAt(bx, playerY + 1, bz);
        if (isSafeFloorBlock(floor) && isSafeAirBlock(legs) && isSafeAirBlock(head)) {
            return new Location(world, bx + 0.5, playerY, bz + 0.5, center.getYaw(), center.getPitch());
        }

        return null;
    }

    public static Location findSafeSpawnLocationAround(Location center) {
        if (center == null || center.getWorld() == null) return null;
        World world = center.getWorld();
        for (int attempt = 0; attempt < 10; attempt++) {
            double dx = (Math.random() - 0.5) * 6;
            double dz = (Math.random() - 0.5) * 6;
            double dy = (Math.random() - 0.5) * 2;
            Location loc = center.clone().add(dx, dy, dz);
            int bx = loc.getBlockX();
            int bz = loc.getBlockZ();
            int by = loc.getBlockY();
            
            for (int y = by + 2; y >= by - 2; y--) {
                Block floor = world.getBlockAt(bx, y - 1, bz);
                Block legs = world.getBlockAt(bx, y, bz);
                Block head = world.getBlockAt(bx, y + 1, bz);
                if (isSafeFloorBlock(floor) && isSafeAirBlock(legs) && isSafeAirBlock(head)) {
                    return new Location(world, bx + 0.5, y, bz + 0.5, center.getYaw(), center.getPitch());
                }
            }
        }
        return ensureSafeLocation(center);
    }
}
