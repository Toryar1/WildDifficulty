package fr.wilddifficulty.encounter.mechanic;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.encounter.EncounterConfig;
import fr.wilddifficulty.encounter.EncounterReward;
import fr.wilddifficulty.encounter.EncounterSession;
import fr.wilddifficulty.encounter.EncounterSpawnUtil;
import fr.wilddifficulty.encounter.EncounterStatus;
import fr.wilddifficulty.encounter.EncounterWave;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Mécanique de Trial Bunker (TRIAL_BUNKER).
 * Simule le fonctionnement d'une Trial Chamber :
 * - Détection à ~14 blocs
 * - Vagues échelonnées avec limite de mobs simultanés
 * - Scaling dynamique selon le nombre de joueurs
 * - Récompenses à tous les participants
 * - Cooldown avant réinitialisation
 */
public class TrialBunkerMechanic implements EncounterMechanic {

    private final WildDifficultyPlugin plugin;
    private final Random random = new Random();

    public TrialBunkerMechanic(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(DifficultyZone zone, List<Player> players, EncounterSession session) {
        EncounterConfig config = session.getConfig();

        session.setCurrentWaveIndex(0);
        session.setTrialTotalSpawned(0);
        session.setWaveCountdownSeconds(2);

        String startMsg = plugin.getLangManager().get("encounter.trial_start", Map.of("zone", zone.getId()));
        for (Player p : players) {
            session.addParticipant(p);
            p.sendMessage(startMsg != null ? startMsg : ChatColor.GOLD + "⚔ [Trial] Vous entrez dans le bunker " + zone.getId() + "...");
            p.playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 1.5f, 1.0f);
        }

        session.updateBossBar("&6⚔ Trial : Activation...", 1.0);
    }

    @Override
    public void tick(DifficultyZone zone, EncounterSession session) {
        EncounterConfig config = session.getConfig();
        session.incrementTicks();
        session.syncBossBarPlayers();

        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        // Vérification des conditions de défaite
        boolean anyPlayerInZone = false;
        boolean allPlayersDead = true;
        int activePlayersCount = 0;

        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                if (!p.isDead()) allPlayersDead = false;
                if (zone.contains(p.getLocation())) {
                    anyPlayerInZone = true;
                    activePlayersCount++;
                }
            }
        }

        if (allPlayersDead) {
            end(zone, session, false);
            return;
        }

        if (!anyPlayerInZone) {
            session.incrementSecondsOutsideZone();
            int grace = config.getPlayerLeaveGracePeriodSeconds();
            int remaining = grace - session.getSecondsOutsideZone();
            if ("TIMEOUT_OUTSIDE_ZONE".equalsIgnoreCase(config.getDefeatCondition())) {
                if (remaining <= 0) {
                    end(zone, session, false);
                    return;
                } else if (remaining % 5 == 0 || remaining <= 5) {
                    for (UUID uuid : session.getParticipatingPlayerUuids()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            String warn = plugin.getLangManager().get("encounter.outside_zone_warning", Map.of("seconds", String.valueOf(remaining)));
                            p.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(warn));
                        }
                    }
                }
            }
        } else {
            session.resetSecondsOutsideZone();
        }

        // Nettoyage des mobs morts
        session.getAliveMobUuids().removeIf(mobUuid -> {
            Entity ent = Bukkit.getEntity(mobUuid);
            return ent == null || ent.isDead() || !ent.isValid();
        });

        // Application des objectifs IA
        if (session.getTicksElapsed() % 2 == 0) {
            applyMobObjectives(zone, session);
        }

        // Calcul du total de mobs à éliminer avec scaling par joueur
        int baseTotal = config.getTrialTotalMobs();
        int scaledTotal = (int) Math.round(baseTotal * (1.0 + (Math.max(1, activePlayersCount) - 1) * config.getTrialScalingPerPlayer()));

        // Spawn régulier jusqu'au plafond de la session
        if (session.getTrialTotalSpawned() < scaledTotal) {
            if (session.getAliveMobUuids().size() < config.getTrialMobCap()) {
                spawnTrialMob(zone, session);
            }
        }

        int aliveCount = session.getAliveMobUuids().size();
        int defeated = session.getTrialTotalSpawned() - aliveCount;
        double progress = scaledTotal > 0 ? (double) defeated / (double) scaledTotal : 1.0;

        session.updateBossBar("&6⚔ Trial Bunker : " + defeated + "/" + scaledTotal + " vaincus &7(" + aliveCount + " actifs)", progress);

        // Vérification de la victoire (tous les monstres prévus ont été spawnés et éliminés)
        if (session.getTrialTotalSpawned() >= scaledTotal && aliveCount == 0) {
            end(zone, session, true);
        }
    }

    private void applyMobObjectives(DifficultyZone zone, EncounterSession session) {
        String objective = session.getConfig().getMobObjective();
        Location center = new Location(Bukkit.getWorld(zone.getWorld()), zone.getCenterX(), zone.getCenterY(), zone.getCenterZ());

        for (UUID mobUuid : session.getAliveMobUuids()) {
            Entity ent = Bukkit.getEntity(mobUuid);
            if (ent instanceof org.bukkit.entity.Mob mob && mob.isValid()) {
                if ("ATTACK_CENTER_POINT".equalsIgnoreCase(objective)) {
                    if (mob.getLocation().distanceSquared(center) > 4) {
                        mob.getPathfinder().moveTo(center, 1.25);
                    }
                } else if ("TARGET_PLAYERS".equalsIgnoreCase(objective) || "KILL_VILLAGERS_AND_NPCS".equalsIgnoreCase(objective)) {
                    if (mob.getTarget() == null) {
                        Player closest = null;
                        double closestDist = Double.MAX_VALUE;
                        for (UUID uuid : session.getParticipatingPlayerUuids()) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null && p.isOnline() && !p.isDead()) {
                                double d = p.getLocation().distanceSquared(mob.getLocation());
                                if (d < closestDist) {
                                    closestDist = d;
                                    closest = p;
                                }
                            }
                        }
                        if (closest != null) mob.setTarget(closest);
                    }
                }
            }
        }
    }

    private void spawnTrialMob(DifficultyZone zone, EncounterSession session) {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        // Position de spawn basée sur un joueur présent ou le centre de la zone
        Location refLoc = new Location(world, zone.getCenterX(), zone.getCenterY(), zone.getCenterZ());
        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && zone.contains(p.getLocation())) {
                refLoc = p.getLocation();
                break;
            }
        }

        EncounterConfig config = session.getConfig();
        List<EncounterWave> waves = config.getWaves();
        EncounterWave wave = waves.isEmpty() ? null : waves.get(random.nextInt(waves.size()));
        Location loc = EncounterSpawnUtil.getSafeSpawnLocation(zone, refLoc, wave, config);

        if (wave != null && !wave.getVariantSpawns().isEmpty()) {
            String varId = wave.getVariantSpawns().keySet().iterator().next();
            MobVariant variant = plugin.getVariantManager().getVariant(varId);
            if (variant != null) {
                LivingEntity mob = plugin.getVariantManager().spawnVariantMob(variant, loc);
                if (mob != null) {
                    session.getAliveMobUuids().add(mob.getUniqueId());
                    session.incrementTrialSpawned(1);
                    world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.2f, 1.0f);
                    return;
                }
            }
        }

        // Spawn par défaut (Zombie / Squelette avec effets Trial)
        LivingEntity mob = (LivingEntity) world.spawnEntity(loc, random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON);
        session.getAliveMobUuids().add(mob.getUniqueId());
        session.incrementTrialSpawned(1);
        world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.2f, 1.0f);
    }

    @Override
    public void onMobDeath(DifficultyZone zone, EncounterSession session, LivingEntity entity, Player killer) {
        session.getAliveMobUuids().remove(entity.getUniqueId());
    }

    @Override
    public void end(DifficultyZone zone, EncounterSession session, boolean success) {
        session.setStatus(EncounterStatus.COOLDOWN);
        session.setCooldownEndTimeMillis(System.currentTimeMillis() + (session.getConfig().getCooldownSeconds() * 1000L));

        String successMsg = plugin.getLangManager().get("encounter.trial_success", Map.of("zone", zone.getId()));
        String failMsg = plugin.getLangManager().get("encounter.trial_fail", Map.of("zone", zone.getId()));

        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                if (success) {
                    p.sendMessage(successMsg != null ? successMsg : ChatColor.GREEN + "★ [Trial Réussi] Vous avez conquis le bunker " + zone.getId() + " !");
                    p.playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.5f, 1.0f);

                    distributeRewards(p, session.getConfig().getRewards());
                    plugin.getBetonQuestHook().notifyEncounterSuccess(p, zone.getId(), "TRIAL_BUNKER");
                } else {
                    p.sendMessage(failMsg != null ? failMsg : ChatColor.RED + "☠ [Trial Échoué] Le bunker s'est refermé...");
                    p.playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, 1.0f, 0.8f);
                    plugin.getBetonQuestHook().notifyEncounterDefeat(p, zone.getId(), "TRIAL_BUNKER");
                }
            }
        }

        session.cleanup();
    }

    private void distributeRewards(Player player, EncounterReward reward) {
        if (reward == null || player == null) return;
        if (reward.getXpAmount() > 0) player.giveExp(reward.getXpAmount());

        for (EncounterReward.RewardItem ri : reward.getItems()) {
            if (random.nextDouble() <= ri.getChance()) {
                Material mat = Material.matchMaterial(ri.getMaterialName());
                if (mat != null) {
                    ItemStack is = new ItemStack(mat, ri.getAmount());
                    player.getInventory().addItem(is);
                }
            }
        }

        for (String cmd : reward.getConsoleCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }
    }
}
