package fr.wilddifficulty.encounter.mechanic;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.encounter.EncounterConfig;
import fr.wilddifficulty.encounter.EncounterReward;
import fr.wilddifficulty.encounter.EncounterSession;
import fr.wilddifficulty.encounter.EncounterSpawnUtil;
import fr.wilddifficulty.encounter.EncounterStatus;
import fr.wilddifficulty.encounter.EncounterWave;
import fr.wilddifficulty.util.CompatUtil;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Mécanique d'invasion de base / Raid (BASE_RAID).
 * Supporte le mode vanilla (Bad Omen / POI) et le mode custom par vagues successives.
 */
public class BaseRaidMechanic implements EncounterMechanic {

    private final WildDifficultyPlugin plugin;
    private final Random random = new Random();

    public BaseRaidMechanic(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(DifficultyZone zone, List<Player> players, EncounterSession session) {
        EncounterConfig config = session.getConfig();

        // Mode Raid Custom ou Vanilla : calcul du nombre de vagues
        int baseWaves = config.getWaves().isEmpty() ? config.getRaidWaveCount() : config.getWaves().size();
        int badOmenLevel = session.getBadOmenLevel();
        int extraWaves = 0;
        if (badOmenLevel > 0) {
            for (int i = 0; i < badOmenLevel; i++) {
                if (random.nextDouble() < (0.35 + (badOmenLevel * 0.12))) {
                    extraWaves++;
                }
            }
        }
        int totalWaves = Math.max(1, baseWaves + extraWaves);
        session.setTotalWaveCount(totalWaves);

        session.setCurrentWaveIndex(0);
        session.setWaveCountdownSeconds(3);
        session.resetSecondsOutsideZone();

        String startMsg = badOmenLevel > 0
                ? plugin.getLangManager().get("encounter.bad_omen_raid_start", Map.of("zone", zone.getId(), "level", String.valueOf(badOmenLevel), "waves", String.valueOf(totalWaves)))
                : plugin.getLangManager().get("encounter.raid_start", Map.of("zone", zone.getId()));

        for (Player p : players) {
            session.addParticipant(p);
            p.sendMessage(startMsg != null ? startMsg : ChatColor.RED + "⚔ [Alerte Raid] L'invasion commence dans la zone " + zone.getId() + " (" + totalWaves + " vagues) !");
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.5f, 1.0f);
        }

        session.updateBossBar("&c⚔ Raid : Préparation...", 1.0);
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
        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                if (!p.isDead()) allPlayersDead = false;
                if (zone.contains(p.getLocation())) {
                    anyPlayerInZone = true;
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

        // Gestion du compte à rebours de vague
        if (session.getWaveCountdownSeconds() > 0) {
            session.setWaveCountdownSeconds(session.getWaveCountdownSeconds() - 1);
            int waveNum = session.getCurrentWaveIndex() + 1;
            session.updateBossBar("&e⚔ Raid : Vague " + waveNum + " dans " + session.getWaveCountdownSeconds() + "s", 1.0);

            if (session.getWaveCountdownSeconds() == 0) {
                spawnWave(zone, session);
            }
            return;
        }

        // Nettoyage des mobs morts ou despawnés
        session.getAliveMobUuids().removeIf(mobUuid -> {
            Entity ent = Bukkit.getEntity(mobUuid);
            return ent == null || ent.isDead() || !ent.isValid();
        });

        // Application de l'objectif IA aux mobs vivants
        if (session.getTicksElapsed() % 2 == 0) {
            applyMobObjectives(zone, session);
        }

        // Mise à jour de la BossBar avec le pourcentage d'ennemis restants
        int remaining = session.getAliveMobUuids().size();
        int waveNum = session.getCurrentWaveIndex() + 1;
        int totalWaves = session.getTotalWaveCount() > 0 ? session.getTotalWaveCount() : (config.getWaves().isEmpty() ? config.getRaidWaveCount() : config.getWaves().size());

        if (remaining > 0) {
            double progress = Math.min(1.0, (double) remaining / 10.0);
            session.updateBossBar("&c⚔ Raid : Vague " + waveNum + "/" + totalWaves + " &7(" + remaining + " restants)", progress);
        } else {
            // Tous les monstres de la vague sont vaincus !
            if (session.getCurrentWaveIndex() + 1 < totalWaves) {
                session.setCurrentWaveIndex(session.getCurrentWaveIndex() + 1);
                session.setWaveCountdownSeconds(5); // 5s de pause entre les vagues
                for (UUID uuid : session.getParticipatingPlayerUuids()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.sendMessage(ChatColor.GREEN + "✔ Vague " + waveNum + " repoussée ! Préparez-vous...");
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    }
                }
            } else {
                // Victoire finale !
                end(zone, session, true);
            }
        }
    }

    private void applyMobObjectives(DifficultyZone zone, EncounterSession session) {
        String objective = session.getConfig().getMobObjective();
        Location center = new Location(Bukkit.getWorld(zone.getWorld()), zone.getCenterX(), zone.getCenterY(), zone.getCenterZ());

        for (UUID mobUuid : session.getAliveMobUuids()) {
            Entity ent = Bukkit.getEntity(mobUuid);
            if (ent instanceof org.bukkit.entity.Mob mob && mob.isValid()) {
                if ("ATTACK_CENTER_POINT".equalsIgnoreCase(objective)) {
                    if (mob.getLocation().distanceSquared(center) > 9) {
                        mob.getPathfinder().moveTo(center, 1.25);
                    }
                } else if ("KILL_VILLAGERS_AND_NPCS".equalsIgnoreCase(objective)) {
                    if (mob.getTarget() == null || mob.getTarget() instanceof Player) {
                        for (Entity nearby : mob.getNearbyEntities(20, 10, 20)) {
                            if (nearby.getType() == EntityType.VILLAGER || nearby.getType() == EntityType.IRON_GOLEM || nearby.hasMetadata("wd_npc")) {
                                if (nearby instanceof LivingEntity targetLe && !targetLe.isDead()) {
                                    mob.setTarget(targetLe);
                                    break;
                                }
                            }
                        }
                    }
                } else if ("TARGET_PLAYERS".equalsIgnoreCase(objective)) {
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

    private void spawnWave(DifficultyZone zone, EncounterSession session) {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        EncounterConfig config = session.getConfig();
        int waveIdx = session.getCurrentWaveIndex();
        List<EncounterWave> waves = config.getWaves();

        Location center = new Location(world, zone.getCenterX(), zone.getCenterY(), zone.getCenterZ());

        if (!waves.isEmpty() && !"VANILLA_RAID_MODE".equalsIgnoreCase(config.getRaidMode())) {
            EncounterWave wave = waveIdx < waves.size() ? waves.get(waveIdx) : waves.get(waves.size() - 1);
            double bonusScale = waveIdx >= waves.size() ? 1.0 + 0.25 * (waveIdx - waves.size() + 1) : 1.0;

            // Spawn des variantes
            for (Map.Entry<String, Integer> entry : wave.getVariantSpawns().entrySet()) {
                String varId = entry.getKey();
                int count = (int) Math.ceil(entry.getValue() * bonusScale);
                MobVariant variant = plugin.getVariantManager().getVariant(varId);
                for (int i = 0; i < count; i++) {
                    Location loc = EncounterSpawnUtil.getSafeSpawnLocation(zone, center, wave, config);
                    if (variant != null) {
                        LivingEntity mob = plugin.getVariantManager().spawnVariantMob(variant, loc);
                        if (mob != null) {
                            session.getAliveMobUuids().add(mob.getUniqueId());
                        }
                    }
                }
            }

            // Spawn des escouades
            for (Map.Entry<String, Integer> entry : wave.getSquadSpawns().entrySet()) {
                String squadId = entry.getKey();
                int count = (int) Math.ceil(entry.getValue() * bonusScale);
                for (int i = 0; i < count; i++) {
                    Location loc = EncounterSpawnUtil.getSafeSpawnLocation(zone, center, wave, config);
                    List<LivingEntity> squadMobs = plugin.getVariantManager().spawnSquad(squadId, loc);
                    for (LivingEntity m : squadMobs) {
                        session.getAliveMobUuids().add(m.getUniqueId());
                    }
                }
            }
        } else {
            // Vague de raid Vanilla automatique
            double bonusScale = waveIdx >= 5 ? 1.0 + 0.25 * (waveIdx - 4) : 1.0;
            spawnVanillaRaidWave(world, zone, center, waveIdx, bonusScale, config, session);
        }

        // Effets visuels & sonores
        world.playSound(center, Sound.EVENT_RAID_HORN, 2.0f, 1.0f);
    }

    private void spawnVanillaRaidWave(World world, DifficultyZone zone, Location center, int waveIdx, double scale, EncounterConfig config, EncounterSession session) {
        int pillagers = (int) Math.ceil((3 + waveIdx) * scale);
        int vindicators = (int) Math.ceil((waveIdx >= 1 ? waveIdx : 0) * scale);
        int witches = (int) Math.ceil((waveIdx >= 3 ? 1 + (waveIdx - 3) : 0) * scale);
        int evokers = (int) Math.ceil((waveIdx >= 3 ? 1 : 0) * scale);
        int ravagers = (int) Math.ceil((waveIdx == 2 || waveIdx >= 4 ? 1 : 0) * scale);

        spawnMobsOfType(world, zone, center, EntityType.PILLAGER, pillagers, config, session);
        spawnMobsOfType(world, zone, center, EntityType.VINDICATOR, vindicators, config, session);
        spawnMobsOfType(world, zone, center, EntityType.WITCH, witches, config, session);
        spawnMobsOfType(world, zone, center, EntityType.EVOKER, evokers, config, session);
        spawnMobsOfType(world, zone, center, EntityType.RAVAGER, ravagers, config, session);
    }

    private void spawnMobsOfType(World world, DifficultyZone zone, Location center, EntityType type, int count, EncounterConfig config, EncounterSession session) {
        for (int i = 0; i < count; i++) {
            Location loc = EncounterSpawnUtil.getSafeSpawnLocation(zone, center, null, config);
            LivingEntity entity = (LivingEntity) world.spawnEntity(loc, type);
            session.getAliveMobUuids().add(entity.getUniqueId());
        }
    }

    @Override
    public void onMobDeath(DifficultyZone zone, EncounterSession session, LivingEntity entity, Player killer) {
        session.getAliveMobUuids().remove(entity.getUniqueId());
    }

    @Override
    public void end(DifficultyZone zone, EncounterSession session, boolean success) {
        session.setStatus(EncounterStatus.COOLDOWN);
        session.setCooldownEndTimeMillis(System.currentTimeMillis() + (session.getConfig().getCooldownSeconds() * 1000L));

        String victoryMsg = plugin.getLangManager().get("encounter.raid_victory", Map.of("zone", zone.getId()));
        String defeatMsg = plugin.getLangManager().get("encounter.raid_defeat", Map.of("zone", zone.getId()));

        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                if (success) {
                    p.sendMessage(victoryMsg != null ? victoryMsg : ChatColor.GREEN + "★ [Victoire] Vous avez défendu " + zone.getId() + " avec succès !");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                    // Distribution des récompenses
                    distributeRewards(p, session.getConfig().getRewards());

                    // Notification BetonQuest
                    plugin.getBetonQuestHook().notifyEncounterSuccess(p, zone.getId(), "BASE_RAID");
                } else {
                    p.sendMessage(defeatMsg != null ? defeatMsg : ChatColor.RED + "☠ [Défaite] L'invasion a submergé le camp...");
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
                    plugin.getBetonQuestHook().notifyEncounterDefeat(p, zone.getId(), "BASE_RAID");
                }
            }
        }

        session.cleanup();
    }

    private void distributeRewards(Player player, EncounterReward reward) {
        if (reward == null || player == null) return;

        // XP
        if (reward.getXpAmount() > 0) {
            player.giveExp(reward.getXpAmount());
        }

        // Items
        for (EncounterReward.RewardItem ri : reward.getItems()) {
            if (random.nextDouble() <= ri.getChance()) {
                Material mat = Material.matchMaterial(ri.getMaterialName());
                if (mat != null) {
                    ItemStack is = new ItemStack(mat, ri.getAmount());
                    player.getInventory().addItem(is);
                }
            }
        }

        // Commandes console
        for (String cmd : reward.getConsoleCommands()) {
            String parsed = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}
