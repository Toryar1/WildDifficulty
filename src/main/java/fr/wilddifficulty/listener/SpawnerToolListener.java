package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.spawner.CustomSpawner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerToolListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public SpawnerToolListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.NETHERITE_SHOVEL) return;
        if (!item.hasItemMeta()) return;

        org.bukkit.NamespacedKey toolKey = new org.bukkit.NamespacedKey(plugin, "wd_tool_type");
        String toolType = item.getItemMeta().getPersistentDataContainer().get(toolKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (!"spawner".equals(toolType)) {
            if (!item.getItemMeta().hasDisplayName()) return;
            String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
            if (!plainName.contains("Outil de Spawner") && !plainName.contains("Spawner Tool")) return;
        }

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Location loc = event.getClickedBlock().getLocation();
            plugin.getSpawnerManager().getOrCreateSpawner(loc);
            
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.0f);
            plugin.getGuiManager().openSpawnerEditor(player, loc);
        }
    }
}
