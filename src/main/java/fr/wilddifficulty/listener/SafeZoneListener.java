package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Location;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class SafeZoneListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public SafeZoneListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Enemy)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // 1. Safe zone entry prevention
        DifficultyZone toZone = plugin.getZoneManager().getZoneAt(to.getWorld().getName(), to.getX(), to.getY(), to.getZ());
        if (toZone != null && toZone.isSafeZone()) {
            DifficultyZone fromZone = plugin.getZoneManager().getZoneAt(from.getWorld().getName(), from.getX(), from.getY(), from.getZ());
            if (fromZone == null || !fromZone.isSafeZone()) {
                event.setCancelled(true);
                org.bukkit.util.Vector push = from.toVector().subtract(to.toVector()).normalize().multiply(0.25);
                entity.setVelocity(push);
                return;
            }
        }

        // 2. Crossing restrictions
        DifficultyZone fromZone = plugin.getZoneManager().getZoneAt(from.getWorld().getName(), from.getX(), from.getY(), from.getZ());
        if (fromZone != toZone) {
            boolean block = false;
            if (fromZone != null && !fromZone.isMobsCanCross()) {
                block = true;
            } else if (toZone != null && !toZone.isMobsCanCross()) {
                block = true;
            }

            if (block) {
                event.setCancelled(true);
                org.bukkit.util.Vector push = from.toVector().subtract(to.toVector()).normalize().multiply(0.25);
                entity.setVelocity(push);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (target == null) return;

        DifficultyZone zone = plugin.getZoneManager().getZoneAt(target.getWorld().getName(), target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ());
        if (zone != null && zone.isSafeZone()) {
            event.setCancelled(true);
            if (event.getEntity() instanceof org.bukkit.entity.Mob mob) {
                mob.setTarget(null);
                try {
                    mob.getPathfinder().stopPathfinding();
                } catch (Throwable ignored) {}
            }
        }
    }
}
