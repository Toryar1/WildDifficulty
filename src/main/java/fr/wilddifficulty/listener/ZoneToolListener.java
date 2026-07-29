package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ZoneToolListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public ZoneToolListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.GOLDEN_HOE) return;
        if (!item.hasItemMeta()) return;

        org.bukkit.NamespacedKey toolKey = new org.bukkit.NamespacedKey(plugin, "wd_tool_type");
        String toolType = item.getItemMeta().getPersistentDataContainer().get(toolKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (!"zone".equals(toolType)) {
            if (!item.getItemMeta().hasDisplayName()) return;
            String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
            if (!plainName.contains("Outil de Zone") && !plainName.contains("Zone Tool")) return;
        }

        event.setCancelled(true); // Annule l'action par défaut (labourage)

        UUID uuid = player.getUniqueId();
        
        // 1. Check if editing an existing zone
        String editingZoneId = plugin.getEditingZoneId().get(uuid);
        DifficultyZone zone = null;
        boolean isEditing = false;
        
        if (editingZoneId != null) {
            zone = plugin.getZoneManager().getZone(editingZoneId);
            isEditing = true;
        }
        
        // 2. Fallback to pending zone (legacy /wd zone create)
        if (zone == null) {
            zone = plugin.getZoneManager().getPendingZone(uuid);
            isEditing = false;
        }

        if (zone == null) {
            player.sendMessage(plugin.getLangManager().get("zone.no_active_zone"));
            return;
        }

        Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation();

        switch (zone.getType()) {
            case CUBOID -> {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    zone.setPos1(loc.getX(), loc.getY(), loc.getZ());
                    zone.finalizeCuboid();
                    plugin.getZoneManager().save();
                    player.sendMessage(plugin.getLangManager().get("zone.pos1_set", java.util.Map.of("x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ()))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) && event.getClickedBlock() != null) {
                    zone.setPos2(loc.getX(), loc.getY(), loc.getZ());
                    zone.finalizeCuboid();
                    plugin.getZoneManager().save();
                    player.sendMessage(plugin.getLangManager().get("zone.pos2_set", java.util.Map.of("x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ()))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
            case RADIUS -> {
                if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) && event.getClickedBlock() != null) {
                    zone.setCenter(loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5);
                    plugin.getZoneManager().save();
                    player.sendMessage(plugin.getLangManager().get("zone.center_set", java.util.Map.of("x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ()))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    Location centerLoc = new Location(player.getWorld(), zone.getCenterX(), zone.getCenterY(), zone.getCenterZ());
                    double distance = centerLoc.distance(loc);
                    zone.setRadius(distance);
                    plugin.getZoneManager().save();
                    player.sendMessage(plugin.getLangManager().get("zone.radius_set", java.util.Map.of("radius", String.format("%.1f", distance))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
            case POLYGON -> {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    double x = loc.getX() + 0.5;
                    double z = loc.getZ() + 0.5;
                    zone.getPoints().add(new double[]{x, z});
                    plugin.getZoneManager().save();
                    player.sendMessage(plugin.getLangManager().get("zone.point_added", java.util.Map.of("x", String.format("%.1f", x), "z", String.format("%.1f", z), "total", String.valueOf(zone.getPoints().size()))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (player.isSneaking()) {
                        zone.getPoints().clear();
                        plugin.getZoneManager().save();
                        player.sendMessage(plugin.getLangManager().get("zone.points_reset"));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        return;
                    }

                    if (zone.getPoints().size() < 3) {
                        player.sendMessage("§cIl vous faut au moins 3 points pour fermer le polygone ! (Shift+Clic Gauche pour réinitialiser)");
                        return;
                    }

                    if (!isEditing) {
                        plugin.getZoneManager().addZone(zone);
                        plugin.getZoneManager().clearPendingZone(uuid);
                    } else {
                        plugin.getEditingZoneId().remove(uuid);
                    }
                    plugin.getZoneManager().save();
                    player.sendMessage("§a[WD] Polygone fermé et enregistré avec succès !");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        }
    }
}
