package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public final class ToolsUtil {

    private ToolsUtil() {}

    public static ItemStack getZoneTool() {
        WildDifficultyPlugin plugin = WildDifficultyPlugin.getInstance();
        ItemStack hoe = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLangManager().getRawComponent("tools.item_zone_title"));
            meta.lore(List.of(
                plugin.getLangManager().getRawComponent("tools.item_zone_lore1"),
                plugin.getLangManager().getRawComponent("tools.item_zone_lore2"),
                plugin.getLangManager().getRawComponent("tools.item_zone_lore3")
            ));
            hoe.setItemMeta(meta);
        }
        return hoe;
    }

    public static ItemStack getSpawnerTool() {
        WildDifficultyPlugin plugin = WildDifficultyPlugin.getInstance();
        ItemStack shovel = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLangManager().getRawComponent("tools.item_spawner_title"));
            meta.lore(List.of(
                plugin.getLangManager().getRawComponent("tools.item_spawner_lore1"),
                plugin.getLangManager().getRawComponent("tools.item_spawner_lore2")
            ));
            shovel.setItemMeta(meta);
        }
        return shovel;
    }

    public static ItemStack getBiomeTool() {
        WildDifficultyPlugin plugin = WildDifficultyPlugin.getInstance();
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLangManager().getRawComponent("tools.item_biome_title"));
            meta.lore(List.of(
                plugin.getLangManager().getRawComponent("tools.item_biome_lore1"),
                plugin.getLangManager().getRawComponent("tools.item_biome_lore2")
            ));
            compass.setItemMeta(meta);
        }
        return compass;
    }

    public static ItemStack getInspectorTool() {
        WildDifficultyPlugin plugin = WildDifficultyPlugin.getInstance();
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLangManager().getRawComponent("tools.item_inspector_title"));
            meta.lore(List.of(
                plugin.getLangManager().getRawComponent("tools.item_inspector_lore1"),
                plugin.getLangManager().getRawComponent("tools.item_inspector_lore2")
            ));
            stick.setItemMeta(meta);
        }
        return stick;
    }

    public static void giveAllTools(Player player) {
        player.getInventory().addItem(getZoneTool());
        player.getInventory().addItem(getSpawnerTool());
        player.getInventory().addItem(getBiomeTool());
        player.getInventory().addItem(getInspectorTool());
    }
}
