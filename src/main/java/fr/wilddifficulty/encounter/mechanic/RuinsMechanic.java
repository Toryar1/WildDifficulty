package fr.wilddifficulty.encounter.mechanic;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.encounter.EncounterConfig;
import fr.wilddifficulty.encounter.EncounterReward;
import fr.wilddifficulty.encounter.EncounterSession;
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
 * Mécanique de Ruines & Vestiges Anciens (RUINS).
 * Événements localisés déclenchés par la proximité des joueurs ou l'ouverture de coffres,
 * faisant apparaître des gardiens anciens et mini-boss.
 */
public class RuinsMechanic implements EncounterMechanic {

    private final WildDifficultyPlugin plugin;
    private final Random random = new Random();

    public RuinsMechanic(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(DifficultyZone zone, List<Player> players, EncounterSession session) {
        String startMsg = plugin.getLangManager().get("encounter.ruins_trigger", Map.of("zone", zone.getId()));
        for (Player p : players) {
            session.addParticipant(p);
            p.sendMessage(startMsg != null ? startMsg : ChatColor.DARK_AQUA + "🏛 [Ruines Anciennes] Les gardiens ancestraux de " + zone.getId() + " s'éveillent !");
            p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.2f, 0.8f);
        }

        session.updateBossBar("&3🏛 Gardiens des Ruines", 1.0);
        spawnGuardians(zone, session);
    }

    private void spawnGuardians(DifficultyZone zone, EncounterSession session) {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        Location center = new Location(world, zone.getCenterX(), zone.getCenterY(), zone.getCenterZ());
        EncounterConfig config = session.getConfig();
        List<EncounterWave> waves = config.getWaves();

        if (!waves.isEmpty()) {
            EncounterWave wave = waves.get(0);
            for (Map.Entry<String, Integer> entry : wave.getVariantSpawns().entrySet()) {
                MobVariant variant = plugin.getVariantManager().getVariant(entry.getKey());
                for (int i = 0; i < entry.getValue(); i++) {
                    Location loc = fr.wilddifficulty.encounter.EncounterSpawnUtil.getSafeSpawnLocation(zone, center, wave, config);
                    if (variant != null) {
                        LivingEntity mob = plugin.getVariantManager().spawnVariantMob(variant, loc);
                        if (mob != null) {
                            session.getAliveMobUuids().add(mob.getUniqueId());
                        }
                    }
                }
            }
        } else {
            // Gardiens par défaut (Wither Skeleton / Bogged / Husk)
            int count = 3 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                Location loc = fr.wilddifficulty.encounter.EncounterSpawnUtil.getSafeSpawnLocation(zone, center, null, config);
                LivingEntity guard = (LivingEntity) world.spawnEntity(loc, EntityType.WITHER_SKELETON);
                session.getAliveMobUuids().add(guard.getUniqueId());
            }
        }
    }

    @Override
    public void tick(DifficultyZone zone, EncounterSession session) {
        session.incrementTicks();
        session.syncBossBarPlayers();

        // Nettoyage des gardiens éliminés
        session.getAliveMobUuids().removeIf(mobUuid -> {
            Entity ent = Bukkit.getEntity(mobUuid);
            return ent == null || ent.isDead() || !ent.isValid();
        });

        int remaining = session.getAliveMobUuids().size();
        if (remaining == 0) {
            end(zone, session, true);
        } else {
            session.updateBossBar("&3🏛 Gardiens des Ruines : " + remaining + " restants", (double) remaining / 5.0);
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

        String victoryMsg = plugin.getLangManager().get("encounter.ruins_cleared", Map.of("zone", zone.getId()));
        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && success) {
                p.sendMessage(victoryMsg != null ? victoryMsg : ChatColor.DARK_AQUA + "★ [Ruines Conquises] Les gardiens ont été vaincus ! Le sanctuaire s'apaise.");
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.0f);

                distributeRewards(p, session.getConfig().getRewards());
                plugin.getBetonQuestHook().notifyEncounterSuccess(p, zone.getId(), "RUINS");
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
                    player.getInventory().addItem(new ItemStack(mat, ri.getAmount()));
                }
            }
        }

        for (String cmd : reward.getConsoleCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }
    }
}
