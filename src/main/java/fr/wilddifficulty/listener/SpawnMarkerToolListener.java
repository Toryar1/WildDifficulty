package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.zone.DifficultyZone;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class SpawnMarkerToolListener implements Listener {

    private final WildDifficultyPlugin plugin;
    private final NamespacedKey markerKey;

    public SpawnMarkerToolListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "wd_marker_tool");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)) return;

        String zoneId = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
        if (zoneId == null || zoneId.isEmpty()) return;

        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            player.sendMessage(plugin.getLangManager().get("command.zone_not_found", Map.of("zone", zoneId)));
            return;
        }

        event.setCancelled(true);

        // Shift + Clic droit : Ouvrir le GUI des marqueurs
        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            plugin.getGuiManager().openEncounterSpawnMarkersMenu(player, zoneId);
            return;
        }

        // Clic droit sur un bloc : Ajouter un marqueur
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            Location spawnLoc = block.getLocation().clone().add(0.5, 1.0, 0.5);
            zone.getEncounterConfig().addSpawnMarker(
                    Math.round(spawnLoc.getX() * 10.0) / 10.0,
                    Math.round(spawnLoc.getY() * 10.0) / 10.0,
                    Math.round(spawnLoc.getZ() * 10.0) / 10.0
            );
            plugin.getZoneManager().save();

            player.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 20, 0.1, 0.3, 0.1, 0.05);
            player.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 10, 0.1, 0.3, 0.1, 0.02);
            player.playSound(spawnLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

            int total = zone.getEncounterConfig().getSpawnMarkers().size();
            String msg = plugin.getLangManager().get("encounter.marker_tool_added", Map.of(
                    "count", String.valueOf(total),
                    "x", String.format("%.1f", spawnLoc.getX()),
                    "y", String.format("%.1f", spawnLoc.getY()),
                    "z", String.format("%.1f", spawnLoc.getZ())
            ));
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
            return;
        }

        // Clic gauche sur un bloc : Supprimer le marqueur le plus proche
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            Location clickedLoc = block.getLocation().clone().add(0.5, 1.0, 0.5);
            List<double[]> markers = zone.getEncounterConfig().getSpawnMarkers();
            int closestIdx = -1;
            double closestDistSq = 4.0; // Dans un rayon de 2 blocs

            for (int i = 0; i < markers.size(); i++) {
                double[] m = markers.get(i);
                double distSq = clickedLoc.distanceSquared(new Location(block.getWorld(), m[0], m[1], m[2]));
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestIdx = i;
                }
            }

            if (closestIdx != -1) {
                zone.getEncounterConfig().removeSpawnMarker(closestIdx);
                plugin.getZoneManager().save();
                player.playSound(clickedLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                String msg = plugin.getLangManager().get("encounter.marker_removed");
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
            } else {
                int count = markers.size();
                String msg = plugin.getLangManager().get("encounter.marker_tool_info", Map.of("count", String.valueOf(count)));
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
            }
        }
    }
}
