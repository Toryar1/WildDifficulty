package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.variant.MobVariant;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobBehaviorListener implements Listener {

    private final WildDifficultyPlugin plugin;
    public static final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> bossBarViewers = new ConcurrentHashMap<>();
    public static final Map<UUID, UUID> playerActiveBossBarMob = new ConcurrentHashMap<>();

    public MobBehaviorListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    // --- 1. RÉSISTANCES ET CONDITIONS DE DÉGÂTS ---
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAttackerHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        if (!(damager instanceof LivingEntity attacker)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (attacker.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            String varId = attacker.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null && var.getModifiers() != null) {
                StatModifiers mods = var.getModifiers();
                String effectName = mods.getOnHitPotionEffect();
                if (effectName != null && !"none".equalsIgnoreCase(effectName) && mods.getOnHitPotionChance() > 0.0) {
                    if (Math.random() <= mods.getOnHitPotionChance()) {
                        try {
                            org.bukkit.potion.PotionEffectType type = getPotionType(effectName);
                            if (type != null) {
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, mods.getOnHitPotionDuration(), mods.getOnHitPotionAmplifier(), false, true));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    public static final Map<UUID, Long> playerBossBarLastHit = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        
        Player player = null;
        if (event.getDamager() instanceof Player p) {
            player = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            player = p;
        }
        
        if (player == null) return;
        
        // Disable floating head text for all attacked mobs
        victim.setCustomNameVisible(false);

        // Record player attack for top BossBar display (custom variants & vanilla mobs)
        playerActiveBossBarMob.put(player.getUniqueId(), victim.getUniqueId());
        playerBossBarLastHit.put(player.getUniqueId(), System.currentTimeMillis());

        if (victim.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            String varId = victim.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null) {
                // Handle attackers list for neutral AI
                java.util.Set<UUID> attackers;
                if (victim.hasMetadata("wd_attacked_by")) {
                    attackers = (java.util.Set<UUID>) victim.getMetadata("wd_attacked_by").get(0).value();
                } else {
                    attackers = new java.util.HashSet<>();
                    victim.setMetadata("wd_attacked_by", new org.bukkit.metadata.FixedMetadataValue(plugin, attackers));
                }
                if (attackers != null) {
                    attackers.add(player.getUniqueId());
                }

                String mode = var.getAggroMode();
                if (victim instanceof Mob mob) {
                    if ("NEUTRAL_HIT".equalsIgnoreCase(mode)) {
                        mob.setTarget(player);
                    } else if ("NEUTRAL_SQUAD_HIT".equalsIgnoreCase(mode)) {
                        mob.setTarget(player);
                        if (mob.getPersistentDataContainer().has(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING)) {
                            String squadId = mob.getPersistentDataContainer().get(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING);
                            for (Entity nearby : mob.getNearbyEntities(25, 25, 25)) {
                                if (nearby instanceof Mob squadMember && squadMember.getPersistentDataContainer().has(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING)) {
                                    String sId = squadMember.getPersistentDataContainer().get(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING);
                                    if (squadId.equalsIgnoreCase(sId)) {
                                        java.util.Set<UUID> squadAttackers;
                                        if (squadMember.hasMetadata("wd_attacked_by")) {
                                            squadAttackers = (java.util.Set<UUID>) squadMember.getMetadata("wd_attacked_by").get(0).value();
                                        } else {
                                            squadAttackers = new java.util.HashSet<>();
                                            squadMember.setMetadata("wd_attacked_by", new org.bukkit.metadata.FixedMetadataValue(plugin, squadAttackers));
                                        }
                                        if (squadAttackers != null) {
                                            squadAttackers.add(player.getUniqueId());
                                        }
                                        squadMember.setTarget(player);
                                    }
                                }
                            }
                        }
                    }
                } else if (victim.hasMetadata("wd_npc")) {
                    // For Citizens Player NPC
                    if ("NEUTRAL_HIT".equalsIgnoreCase(mode) || "NEUTRAL_SQUAD_HIT".equalsIgnoreCase(mode)) {
                        victim.setMetadata("wd_npc_target", new org.bukkit.metadata.FixedMetadataValue(plugin, player.getUniqueId()));
                        // Also alert nearby squad NPCs if NEUTRAL_SQUAD_HIT
                        if ("NEUTRAL_SQUAD_HIT".equalsIgnoreCase(mode) && victim.getPersistentDataContainer().has(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING)) {
                            String squadId = victim.getPersistentDataContainer().get(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING);
                            for (Entity nearby : victim.getNearbyEntities(25, 25, 25)) {
                                if (nearby.hasMetadata("wd_npc") && nearby.getPersistentDataContainer().has(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING)) {
                                    String sId = nearby.getPersistentDataContainer().get(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING);
                                    if (squadId.equalsIgnoreCase(sId)) {
                                        java.util.Set<UUID> squadAttackers;
                                        if (nearby.hasMetadata("wd_attacked_by")) {
                                            squadAttackers = (java.util.Set<UUID>) nearby.getMetadata("wd_attacked_by").get(0).value();
                                        } else {
                                            squadAttackers = new java.util.HashSet<>();
                                            nearby.setMetadata("wd_attacked_by", new org.bukkit.metadata.FixedMetadataValue(plugin, squadAttackers));
                                        }
                                        if (squadAttackers != null) {
                                            squadAttackers.add(player.getUniqueId());
                                        }
                                        nearby.setMetadata("wd_npc_target", new org.bukkit.metadata.FixedMetadataValue(plugin, player.getUniqueId()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Immunité aux dégâts spécifiques
        if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            String varId = entity.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null && var.getModifiers() != null) {
                // Custom Hurt Sound
                if (var.getCustomSounds().containsKey("hurt")) {
                    MobVariant.SoundConfig sc = var.getCustomSounds().get("hurt");
                    entity.getWorld().playSound(entity.getLocation(), sc.getSoundKey(), sc.getVolume(), sc.getPitch());
                }

                StatModifiers mods = var.getModifiers();
                EntityDamageEvent.DamageCause cause = event.getCause();

                // Immunités directes
                if (mods.isImmuneFire() && (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK)) {
                    event.setCancelled(true);
                    return;
                }
                if (mods.isImmuneLava() && cause == EntityDamageEvent.DamageCause.LAVA) {
                    event.setCancelled(true);
                    return;
                }
                if (mods.isImmuneDrowning() && cause == EntityDamageEvent.DamageCause.DROWNING) {
                    event.setCancelled(true);
                    return;
                }
                if (mods.isImmuneFall() && cause == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                    return;
                }

                // Immunités spécifiques aux entités aquatiques hors de l'eau
                if (cause == EntityDamageEvent.DamageCause.DROWNING && "AGGRESSIVE".equalsIgnoreCase(var.getAggroMode())) {
                    EntityType type = entity.getType();
                    if (type == EntityType.DROWNED || type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN 
                            || type == EntityType.SQUID || type == EntityType.GLOW_SQUID || type == EntityType.COD 
                            || type == EntityType.SALMON || type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH 
                            || type == EntityType.DOLPHIN || type == EntityType.AXOLOTL || type == EntityType.TADPOLE) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // Résistances/Vulnérabilités multiplicatives
                double mult = 1.0;
                if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK 
                    || cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                    mult = mods.getResistanceFeu();
                } else if (cause == EntityDamageEvent.DamageCause.MAGIC || cause == EntityDamageEvent.DamageCause.POISON 
                           || cause == EntityDamageEvent.DamageCause.WITHER || cause == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
                    mult = mods.getResistanceMagie();
                } else if (cause == EntityDamageEvent.DamageCause.PROJECTILE) {
                    mult = mods.getResistanceProjectile();
                }

                if (mult != 1.0) {
                    event.setDamage(event.getDamage() * mult);
                }
            }
        }
    }

    // --- 2. UPDATE NAMETAG SUR DÉGÂTS/REGEN ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageMonitor(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            updateNametagAndBossBar(entity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegenMonitor(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            updateNametagAndBossBar(entity);
        }
    }

    private void updateNametagAndBossBar(LivingEntity entity) {
        if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            String varId = entity.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null) {
                // Toujours maintenir masqué au-dessus de la tête en combat (BossBar prioritaire)
                entity.setCustomNameVisible(false);

                // Update BossBar progress
                BossBar bar = activeBossBars.get(entity.getUniqueId());
                if (bar != null) {
                    double maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    float progress = (float) Math.max(0.0, Math.min(1.0, entity.getHealth() / maxHealth));
                    bar.progress(progress);
                }
            }
        }
    }

    // --- 3. AGGRO DE GROUPE ET SONS D'AGGRO ---
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getTarget() instanceof Player player)) return;

        if (mob.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            String varId = mob.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null) {
                // Son d'aggro
                if (var.getCustomSounds().containsKey("aggro")) {
                    MobVariant.SoundConfig sound = var.getCustomSounds().get("aggro");
                    player.playSound(mob.getLocation(), sound.getSoundKey(), sound.getVolume(), sound.getPitch());
                }

                // Group Aggro (comportement-groupe)
                if (var.getModifiers() != null && var.getModifiers().isGroupAggro()) {
                    List<Entity> nearby = mob.getNearbyEntities(15, 15, 15);
                    for (Entity e : nearby) {
                        if (e instanceof Mob nearbyMob && !nearbyMob.getUniqueId().equals(mob.getUniqueId())) {
                            if (nearbyMob.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                                String nVarId = nearbyMob.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
                                if (varId.equals(nVarId) && nearbyMob.getTarget() == null) {
                                    nearbyMob.setTarget(player);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            String varId = entity.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null) {
                String mode = var.getAggroMode();
                if ("PASSIVE".equalsIgnoreCase(mode)) {
                    event.setCancelled(true);
                } else if ("NEUTRAL_HIT".equalsIgnoreCase(mode) || "NEUTRAL_SQUAD_HIT".equalsIgnoreCase(mode)) {
                    boolean hasAttacked = false;
                    if (entity.hasMetadata("wd_attacked_by")) {
                        java.util.Set<UUID> attackers = (java.util.Set<UUID>) entity.getMetadata("wd_attacked_by").get(0).value();
                        if (attackers != null && attackers.contains(player.getUniqueId())) {
                            hasAttacked = true;
                        }
                    }
                    if (!hasAttacked) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcDamage(EntityDamageEvent event) {
        if (event.getEntity().hasMetadata("wd_npc")) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(false);
                double fallDist = event.getEntity().getFallDistance();
                if (fallDist > 3.0 && event.getDamage() <= 0.0) {
                    event.setDamage(fallDist - 3.0);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMobTargetNpc(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (target != null && target.hasMetadata("wd_npc")) {
            boolean wasAttackedByNpc = false;
            if (event.getEntity().hasMetadata("wd_attacked_by")) {
                try {
                    java.util.Set<UUID> attackers = (java.util.Set<UUID>) event.getEntity().getMetadata("wd_attacked_by").get(0).value();
                    if (attackers != null && attackers.contains(target.getUniqueId())) {
                        wasAttackedByNpc = true;
                    }
                } catch (Throwable ignored) {}
            }
            if (!wasAttackedByNpc) {
                if (!(event.getEntity() instanceof Player) || event.getEntity().hasMetadata("wd_npc")) {
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Mob mob) {
                        mob.setTarget(null);
                        try {
                            mob.getPathfinder().stopPathfinding();
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean isNpc = player.hasMetadata("wd_npc");
        if (!isNpc && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            try {
                isNpc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(player);
            } catch (Throwable ignored) {}
        }
        if (isNpc) {
            event.setDeathMessage(null);
            event.deathMessage(null);
        }
    }

    // --- 4. EXPLOSION À LA MORT, DROPS, EXP, ET SONS ---
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        UUID uuid = entity.getUniqueId();

        // Nettoyage BossBar
        BossBar bar = activeBossBars.remove(uuid);
        bossBarViewers.remove(uuid);
        if (bar != null) {
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bar);
            }
        }

        if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            String varId = entity.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null) {
                Location loc = entity.getLocation();

                // Explosion à la mort
                if (var.getModifiers() != null && var.getModifiers().isExplodeOnDeath()) {
                    loc.getWorld().createExplosion(loc, 3.0F, false, false);
                }
                
                // Nettoyer le NPC Citizens si applicable
                if (entity.hasMetadata("wd_npc") && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> fr.wilddifficulty.util.CitizensHook.destroyNPC(entity));
                }

                // Variante spawner à la mort
                if (var.getModifiers() != null) {
                    String dVarId = var.getModifiers().getDeathSpawnVariant();
                    int dAmount = var.getModifiers().getDeathSpawnAmount();
                    if (dVarId != null && !"none".equalsIgnoreCase(dVarId) && dAmount > 0) {
                        MobVariant dVar = plugin.getVariantManager().getVariant(dVarId.toLowerCase());
                        if (dVar != null) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                for (int i = 0; i < dAmount; i++) {
                                    Location spawnLoc = loc.clone().add((Math.random() - 0.5) * 2, 0.1, (Math.random() - 0.5) * 2);
                                    Class<? extends Entity> eClass = dVar.getType().getEntityClass();
                                    if (eClass != null) {
                                        if (loc.getWorld().getDifficulty() == org.bukkit.Difficulty.PEACEFUL && org.bukkit.entity.Enemy.class.isAssignableFrom(eClass)) {
                                            continue;
                                        }
                                        try {
                                            loc.getWorld().spawn(spawnLoc, eClass, CreatureSpawnEvent.SpawnReason.CUSTOM, spawned -> {
                                                spawned.getPersistentDataContainer().set(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING, dVar.getId());
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

                // Son de mort
                if (var.getCustomSounds().containsKey("death")) {
                    MobVariant.SoundConfig sound = var.getCustomSounds().get("death");
                    loc.getWorld().playSound(loc, sound.getSoundKey(), sound.getVolume(), sound.getPitch());
                }

                // XP on death override
                if (var.getXpOnDeath() >= 0) {
                    event.setDroppedExp(var.getXpOnDeath());
                }

                // Déterminer la cause de la mort pour la table de loot conditionnelle
                String condition = "none";
                if (entity.getKiller() != null) {
                    condition = "player";
                } else if (entity.getLastDamageCause() != null) {
                    EntityDamageEvent.DamageCause cause = entity.getLastDamageCause().getCause();
                    if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                        condition = "other";
                    } else {
                        condition = "environment";
                    }
                }

                // Custom Drops
                if (!var.getCustomDrops().isEmpty()) {
                    for (MobVariant.CustomDrop drop : var.getCustomDrops()) {
                        // Check death condition
                        if (!"none".equals(drop.getDeathCondition()) && !drop.getDeathCondition().equalsIgnoreCase(condition)) {
                            continue;
                        }

                        if (Math.random() < drop.getChance()) {
                            Material mat = Material.matchMaterial(drop.getMaterialName());
                            if (mat != null) {
                                int amount = drop.getMinAmount() + (int) (Math.random() * (drop.getMaxAmount() - drop.getMinAmount() + 1));
                                if (amount > 0) {
                                    ItemStack itemStack = new ItemStack(mat, amount);
                                    event.getDrops().add(itemStack);
                                }
                            }
                            if (drop.getXp() > 0) {
                                event.setDroppedExp(event.getDroppedExp() + drop.getXp());
                            }
                        }
                    }
                }

                // Multiplicateur de drops de Lune de Sang (appliqué à tous les loots cumulés)
                if (plugin.getMainConfigManager().isBloodMoonActive()) {
                    double dropMult = plugin.getMainConfigManager().getBloodMoonDropsMultiplier();
                    if (dropMult != 1.0) {
                        for (ItemStack dropStack : event.getDrops()) {
                            if (dropStack != null && dropStack.getType() != Material.AIR) {
                                int newAmt = (int) Math.round(dropStack.getAmount() * dropMult);
                                dropStack.setAmount(Math.max(1, newAmt));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        LivingEntity mother = event.getMother();
        LivingEntity father = event.getFather();

        boolean isMotherCustom = mother.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, org.bukkit.persistence.PersistentDataType.STRING);
        boolean isFatherCustom = father.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, org.bukkit.persistence.PersistentDataType.STRING);

        if (isMotherCustom || isFatherCustom) {
            event.setCancelled(true);
            if (event.getBreeder() instanceof Player player) {
                player.sendMessage("§cVous ne pouvez pas accoupler des variantes de WildDifficulty !");
            }
        }
    }

    public static org.bukkit.potion.PotionEffectType getPotionType(String name) {
        if (name == null) return null;
        String upper = name.toUpperCase().trim();
        return switch (upper) {
            case "POISON" -> org.bukkit.potion.PotionEffectType.POISON;
            case "WITHER" -> org.bukkit.potion.PotionEffectType.WITHER;
            case "BLINDNESS" -> org.bukkit.potion.PotionEffectType.BLINDNESS;
            case "SLOW", "SLOWNESS" -> org.bukkit.potion.PotionEffectType.SLOWNESS;
            case "WEAKNESS" -> org.bukkit.potion.PotionEffectType.WEAKNESS;
            case "CONFUSION", "NAUSEA" -> org.bukkit.potion.PotionEffectType.NAUSEA;
            case "LEVITATION" -> org.bukkit.potion.PotionEffectType.LEVITATION;
            case "SPEED" -> org.bukkit.potion.PotionEffectType.SPEED;
            case "INCREASE_DAMAGE", "STRENGTH" -> org.bukkit.potion.PotionEffectType.STRENGTH;
            case "DAMAGE_RESISTANCE", "RESISTANCE" -> org.bukkit.potion.PotionEffectType.RESISTANCE;
            case "FIRE_RESISTANCE" -> org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE;
            case "REGENERATION" -> org.bukkit.potion.PotionEffectType.REGENERATION;
            case "INVISIBILITY" -> org.bukkit.potion.PotionEffectType.INVISIBILITY;
            case "GLOWING" -> org.bukkit.potion.PotionEffectType.GLOWING;
            default -> {
                try {
                    yield org.bukkit.potion.PotionEffectType.getByName(upper);
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }
}
