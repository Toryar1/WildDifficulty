package fr.wilddifficulty.encounter.mechanic;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.encounter.EncounterConfig;
import fr.wilddifficulty.encounter.EncounterSession;
import fr.wilddifficulty.encounter.EncounterStatus;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Mécanique d'Avant-Poste Illager (OUTPOST).
 * Gère les patrouilles renforcées, les capitaines de raid et les mini-invasions
 * si les joueurs s'attardent trop longtemps dans la zone.
 */
public class OutpostMechanic implements EncounterMechanic {

    private final WildDifficultyPlugin plugin;
    private final Random random = new Random();

    public OutpostMechanic(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(DifficultyZone zone, List<Player> players, EncounterSession session) {
        session.setLingeringSeconds(0);
        String startMsg = plugin.getLangManager().get("encounter.outpost_enter", Map.of("zone", zone.getId()));
        for (Player p : players) {
            session.addParticipant(p);
            p.sendMessage(startMsg != null ? startMsg : ChatColor.RED + "⚔ [Avant-Poste] Vous approchez du campement illager " + zone.getId() + "...");
        }
        session.updateBossBar("&c⚔ Avant-Poste Illager", 1.0);
    }

    @Override
    public void tick(DifficultyZone zone, EncounterSession session) {
        EncounterConfig config = session.getConfig();
        session.incrementTicks();
        session.syncBossBarPlayers();

        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        boolean playersPresent = false;
        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && zone.contains(p.getLocation())) {
                playersPresent = true;
                break;
            }
        }

        if (!playersPresent) {
            session.setLingeringSeconds(Math.max(0, session.getLingeringSeconds() - 1));
            return;
        }

        session.addLingeringSeconds(1.0);
        double maxTime = config.getOutpostLingeringInvasionTimeSeconds();
        double meter = Math.min(1.0, session.getLingeringSeconds() / maxTime);

        session.updateBossBar("&c⚔ Alerte Avant-Poste : " + (int) session.getLingeringSeconds() + "s/" + (int) maxTime + "s", meter);

        // Déclenchement de la mini-invasion si les joueurs restent trop longtemps
        if (session.getLingeringSeconds() >= maxTime) {
            triggerMiniInvasion(zone, session);
            session.setLingeringSeconds(0); // Reset le timer
        }
    }

    private void triggerMiniInvasion(DifficultyZone zone, EncounterSession session) {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;

        Location center = new Location(world, zone.getCenterX(), zone.getCenterY(), zone.getCenterZ());
        world.playSound(center, Sound.EVENT_RAID_HORN, 2.0f, 1.2f);

        for (UUID uuid : session.getParticipatingPlayerUuids()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(ChatColor.DARK_RED + "☠ [Alerte] Des renforts illagers arrivent en trombe !");
            }
        }

        // Spawn de patrouille avec Capitaine
        int count = 5 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            Location loc = fr.wilddifficulty.encounter.EncounterSpawnUtil.getSafeSpawnLocation(zone, center, null, session.getConfig());
            LivingEntity illager = (LivingEntity) world.spawnEntity(loc, random.nextBoolean() ? EntityType.PILLAGER : EntityType.VINDICATOR);
            if (i == 0 && illager instanceof Pillager && random.nextDouble() <= session.getConfig().getOutpostCaptainChance()) {
                // Équiper la bannière Ominous / Capitaine
                illager.getEquipment().setHelmet(new ItemStack(Material.WHITE_BANNER));
            }
            session.getAliveMobUuids().add(illager.getUniqueId());
        }
    }

    @Override
    public void onMobDeath(DifficultyZone zone, EncounterSession session, LivingEntity entity, Player killer) {
        session.getAliveMobUuids().remove(entity.getUniqueId());
    }

    @Override
    public void end(DifficultyZone zone, EncounterSession session, boolean success) {
        session.setStatus(EncounterStatus.IDLE);
        session.cleanup();
    }
}
