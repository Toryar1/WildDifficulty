package fr.wilddifficulty.commands;

import fr.wilddifficulty.WildDifficultyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Commande /wdreload
 * Recharge toute la configuration de WildDifficulty à chaud.
 * Permission : wilddifficulty.admin
 */
public class WDReloadCommand implements CommandExecutor {

    private final WildDifficultyPlugin plugin;

    public WDReloadCommand(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wilddifficulty.admin")) {
            sender.sendMessage(plugin.getLangManager().getComponent("general.no_permission"));
            return true;
        }

        try {
            plugin.reloadAll();
            sender.sendMessage(plugin.getLangManager().getComponent("general.config_reloaded"));
        } catch (Exception e) {
            sender.sendMessage(Component.text("✘ Erreur lors du rechargement : " + e.getMessage())
                    .color(NamedTextColor.RED));
            plugin.getLogger().severe("Erreur lors du rechargement via /wdreload : " + e.getMessage());
        }

        return true;
    }
}
