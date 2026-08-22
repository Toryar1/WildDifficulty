package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener pour les interactions et morts de mobs liées aux Encounters.
 */
public class EncounterListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public EncounterListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (plugin.getEncounterManager() != null) {
            plugin.getEncounterManager().onMobDeath(entity, killer);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST || block.getType() == Material.BARREL) {
            Player player = event.getPlayer();
            DifficultyZone zone = plugin.getZoneManager().getZoneAt(block.getLocation());
            if (zone != null && plugin.getEncounterManager() != null) {
                plugin.getEncounterManager().onChestOpen(player, zone);
            }
        }
    }
}
