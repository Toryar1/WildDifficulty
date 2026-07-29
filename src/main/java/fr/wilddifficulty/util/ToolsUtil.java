package fr.wilddifficulty.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public final class ToolsUtil {

    private ToolsUtil() {}

    public static ItemStack getZoneTool() {
        ItemStack hoe = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§eOutil de Zone WildDifficulty"));
            meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Clic Droit sur un bloc pour ajouter un point."),
                net.kyori.adventure.text.Component.text("§7Clic Gauche pour fermer le polygone."),
                net.kyori.adventure.text.Component.text("§7Sneak + Clic Gauche pour annuler le dernier point.")
            ));
            hoe.setItemMeta(meta);
        }
        return hoe;
    }

    public static ItemStack getSpawnerTool() {
        ItemStack shovel = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§eOutil de Spawner"));
            meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Clic Droit sur un bloc pour ouvrir"),
                net.kyori.adventure.text.Component.text("§7le panneau de configuration de spawner.")
            ));
            shovel.setItemMeta(meta);
        }
        return shovel;
    }

    public static ItemStack getBiomeTool() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§dConfigure Spawns de Biome"));
            meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Clic Droit pour ouvrir la configuration"),
                net.kyori.adventure.text.Component.text("§7des variantes de votre biome actuel.")
            ));
            compass.setItemMeta(meta);
        }
        return compass;
    }

    public static ItemStack getInspectorTool() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§bInspecteur de Mobs"));
            meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Clic Droit sur une entité pour analyser"),
                net.kyori.adventure.text.Component.text("§7ses caractéristiques et ses modificateurs.")
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
