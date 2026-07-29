package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.player.PlayerData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

public class HardcoreDeathListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public HardcoreDeathListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean globalHardcore = plugin.getMainConfigManager().isHardcoreEnabled();
        PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
        boolean personalHardcore = pd.isHardcoreEnabled();

        if (globalHardcore || personalHardcore) {
            boolean noRegen = (globalHardcore && plugin.getMainConfigManager().isHardcoreNoRegen()) || (personalHardcore && pd.isHardcoreNoRegen());
            EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();

            // Check safezone regen
            org.bukkit.Location loc = player.getLocation();
            fr.wilddifficulty.zone.DifficultyZone zone = plugin.getZoneManager().getZoneAt(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
            boolean inSafeZone = zone != null && zone.isSafeZone();

            if (inSafeZone && pd.isHardcoreAllowSafezoneRegen()) {
                return; // Safezone regen allowed
            }

            if (reason == EntityRegainHealthEvent.RegainReason.SATIATED || reason == EntityRegainHealthEvent.RegainReason.REGEN) {
                if (noRegen) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (reason == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN || reason == EntityRegainHealthEvent.RegainReason.MAGIC) {
                if (personalHardcore && !pd.isHardcoreAllowPotions()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean globalHardcore = plugin.getMainConfigManager().isHardcoreEnabled();
        PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());

        double mult = 1.0;
        if (globalHardcore) {
            mult *= plugin.getMainConfigManager().getHardcoreDamageTakenMult();
        }
        if (pd.getDamageMultiplier() != 1.0) {
            mult *= pd.getDamageMultiplier();
        }

        if (mult != 1.0) {
            event.setDamage(event.getDamage() * mult);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean globalHardcore = plugin.getMainConfigManager().isHardcoreEnabled();
        if (globalHardcore) {
            int oldLevel = player.getFoodLevel();
            int newLevel = event.getFoodLevel();
            if (newLevel < oldLevel) {
                int diff = oldLevel - newLevel;
                double mult = plugin.getMainConfigManager().getHardcoreHungerDrainMult();
                int extraDiff = (int) Math.round(diff * mult);
                event.setFoodLevel(Math.max(0, oldLevel - extraDiff));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());

        if (pd.isHardcoreEnabled() && pd.isHardcoreInstantDeathDespawn()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            player.sendMessage(plugin.getLangManager().get("hardcore.instant_death_despawn"));
            return;
        }

        int despawnSeconds = plugin.getMainConfigManager().getDeathItemDespawnSeconds();
        if (despawnSeconds <= 0) return;

        List<org.bukkit.inventory.ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) return;

        org.bukkit.Location deathLoc = player.getLocation();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.entity.Entity nearby : deathLoc.getWorld().getNearbyEntities(deathLoc, 5, 5, 5)) {
                if (nearby instanceof Item item) {
                    int targetTicks = despawnSeconds * 20;
                    int ticksLived = Math.max(1, Math.min(5999, 6000 - targetTicks));
                    item.setTicksLived(ticksLived);
                }
            }
        }, 2L);
    }
}
