package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.zone.DifficultyZone;
import fr.wilddifficulty.zone.ZoneMember;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ZoneProtectionListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public ZoneProtectionListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isProtected(Player player, DifficultyZone zone, int requiredLevel) {
        if (zone == null) return false;
        if (player.hasPermission("wilddifficulty.zone.admin")) return false;

        int memberLevel = zone.getMemberLevel(player.getUniqueId());
        return memberLevel < requiredLevel;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        DifficultyZone zone = plugin.getZoneManager().getZoneAt(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        if (isProtected(player, zone, ZoneMember.LEVEL_BUILDER)) {
            event.setCancelled(true);
            player.sendMessage("§cCette zone est protégée ! Vous devez être au moins Bâtisseur (Niv. 2) pour casser des blocs.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        DifficultyZone zone = plugin.getZoneManager().getZoneAt(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        if (isProtected(player, zone, ZoneMember.LEVEL_BUILDER)) {
            event.setCancelled(true);
            player.sendMessage("§cCette zone est protégée ! Vous devez être au moins Bâtisseur (Niv. 2) pour poser des blocs.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Location loc = block.getLocation();

        DifficultyZone zone = plugin.getZoneManager().getZoneAt(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        if (zone == null) return;

        Material type = block.getType();
        boolean isContainerOrInteractive = type.name().contains("CHEST") 
                || type.name().contains("BARREL") 
                || type.name().contains("FURNACE") 
                || type.name().contains("SHULKER") 
                || type.name().contains("HOPPER") 
                || type.name().contains("DISPENSER") 
                || type.name().contains("DROPPER") 
                || type.name().contains("DOOR") 
                || type.name().contains("GATE") 
                || type.name().contains("TRAPDOOR") 
                || type.name().contains("BUTTON") 
                || type.name().contains("LEVER") 
                || type.name().contains("ANVIL")
                || type == Material.CRAFTING_TABLE
                || type == Material.ENCHANTING_TABLE;

        if (isContainerOrInteractive && isProtected(player, zone, ZoneMember.LEVEL_CONTAINERS)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLangManager().get("zone.container_denied"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof ItemFrame || event.getEntity() instanceof ArmorStand || event.getEntity() instanceof Painting)) return;

        Location loc = event.getEntity().getLocation();
        DifficultyZone zone = plugin.getZoneManager().getZoneAt(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        if (isProtected(player, zone, ZoneMember.LEVEL_BUILDER)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLangManager().get("zone.protection_denied"));
        }
    }
}
