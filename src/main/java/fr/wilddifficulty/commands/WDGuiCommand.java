package fr.wilddifficulty.commands;

import fr.wilddifficulty.WildDifficultyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.wilddifficulty.listener.MobSpawnListener;
import fr.wilddifficulty.variant.MobSquad;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WDGuiCommand implements CommandExecutor, TabCompleter {

    private final WildDifficultyPlugin plugin;

    public WDGuiCommand(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("settings")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLangManager().getRaw("general.player_only"));
                return true;
            }
            if (!player.hasPermission("wilddifficulty.player.settings") && !player.hasPermission("wilddifficulty.admin")) {
                player.sendMessage(plugin.getLangManager().getRaw("general.no_permission"));
                return true;
            }
            plugin.getGuiManager().openPlayerSettingsGui(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("editzone")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLangManager().getRaw("general.player_only"));
                return true;
            }
            if (args.length == 1) {
                plugin.getGuiManager().openPlayerEditableZonesGui(player);
                return true;
            }
            String zoneId = args[1].toLowerCase();
            DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
            if (zone != null && (zone.getMemberLevel(player.getUniqueId()) >= 3 || player.hasPermission("wilddifficulty.admin") || player.hasPermission("wilddifficulty.zone.manage"))) {
                plugin.getGuiManager().openZoneEditor(player, zone.getId());
                return true;
            }
            player.sendMessage(plugin.getLangManager().get("zone.protection_denied"));
            return true;
        }

        if (!sender.hasPermission("wilddifficulty.admin")) {
            // Level 3 Zone Manager access
            if (sender instanceof Player player && args.length >= 2 && args[0].equalsIgnoreCase("zone") && (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("gui"))) {
                String zoneId = args.length > 2 ? args[2].toLowerCase() : null;
                if (zoneId != null) {
                    DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
                    if (zone != null && (zone.getMemberLevel(player.getUniqueId()) >= 3 || player.hasPermission("wilddifficulty.zone.manage"))) {
                        plugin.getGuiManager().openZoneEditor(player, zone.getId());
                        return true;
                    }
                }
            }
            sender.sendMessage(plugin.getLangManager().getComponent("general.no_permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLangManager().getRaw("general.player_only"));
                return true;
            }
            plugin.getGuiManager().openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(plugin.getLangManager().get("general.config_reloaded"));
            return true;
        }

        if (sub.equalsIgnoreCase("killall")) {
            int killed = 0;
            if (plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                try {
                    List<net.citizensnpcs.api.npc.NPC> toDestroy = new ArrayList<>();
                    for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
                        if (npc.data().has("wd_variant_id")) {
                            toDestroy.add(npc);
                        }
                    }
                    for (net.citizensnpcs.api.npc.NPC npc : toDestroy) {
                        npc.destroy();
                        killed++;
                    }
                } catch (Throwable ignored) {}
            }
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                        if (entity.hasMetadata("wd_npc") && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                            fr.wilddifficulty.util.CitizensHook.destroyNPC(entity);
                        } else {
                            entity.remove();
                        }
                        killed++;
                    }
                }
            }
            sender.sendMessage(plugin.getLangManager().get("command.killall", Map.of("count", String.valueOf(killed))));
            return true;
        }

        if (sub.equalsIgnoreCase("zone")) {
            String[] zoneArgs = new String[args.length - 1];
            System.arraycopy(args, 1, zoneArgs, 0, zoneArgs.length);
            MobZoneCommand zoneCmd = new MobZoneCommand(plugin);
            return zoneCmd.onCommand(sender, command, label, zoneArgs);
        }

        // Commande nécessitant un joueur
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLangManager().getRaw("general.player_only"));
            return true;
        }

        if (sub.equalsIgnoreCase("biome_tool")) {
            giveBiomeTool(player);
            player.sendMessage(plugin.getLangManager().get("tools.given_biome"));
            return true;
        }

        if (sub.equalsIgnoreCase("spawner_tool")) {
            giveSpawnerTool(player);
            player.sendMessage(plugin.getLangManager().get("tools.given_spawner"));
            return true;
        }

        if (sub.equalsIgnoreCase("zone_tool")) {
            player.getInventory().addItem(fr.wilddifficulty.util.ToolsUtil.getZoneTool());
            player.sendMessage(plugin.getLangManager().get("tools.given_zone"));
            return true;
        }

        if (sub.equalsIgnoreCase("inspector_tool")) {
            player.getInventory().addItem(fr.wilddifficulty.util.ToolsUtil.getInspectorTool());
            player.sendMessage(plugin.getLangManager().get("tools.given_inspector"));
            return true;
        }

        if (sub.equalsIgnoreCase("tools")) {
            fr.wilddifficulty.util.ToolsUtil.giveAllTools(player);
            player.sendMessage(plugin.getLangManager().get("tools.given_all"));
            return true;
        }

        if (sub.equalsIgnoreCase("scoreboard")) {
            if (plugin.getActiveScoreboards().contains(player.getUniqueId())) {
                plugin.getActiveScoreboards().remove(player.getUniqueId());
                fr.wilddifficulty.util.ScoreboardUtil.clearScoreboard(player);
                player.sendMessage(plugin.getLangManager().get("command.scoreboard_off"));
            } else {
                plugin.getActiveScoreboards().add(player.getUniqueId());
                fr.wilddifficulty.util.ScoreboardUtil.updateScoreboard(plugin, player);
                player.sendMessage(plugin.getLangManager().get("command.scoreboard_on"));
            }
            return true;
        }

        if (sub.equalsIgnoreCase("bloodmoon")) {
            player.getWorld().setMetadata("wd_bloodmoon_forced", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            player.getWorld().removeMetadata("wd_bloodmoon_rolled", plugin);
            player.sendMessage(plugin.getLangManager().get("command.bloodmoon_scheduled"));
            return true;
        }

        if (sub.equalsIgnoreCase("debug")) {
            boolean current = plugin.getMainConfigManager().isDebug();
            plugin.getMainConfigManager().setDebug(!current);
            plugin.getMainConfigManager().save();
            player.sendMessage(plugin.getLangManager().get("command.debug_toggled", Map.of("status", (!current ? "§2ACTIVÉ" : "§cDÉSACTIVÉ"))));
            return true;
        }

        if (sub.equalsIgnoreCase("spawn") && args.length > 1) {
            String varId = args[1].toLowerCase();
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var == null) {
                player.sendMessage(plugin.getLangManager().get("command.variant_not_found"));
                return true;
            }
            
            org.bukkit.util.Vector dir = player.getLocation().getDirection();
            dir.setY(0);
            if (dir.lengthSquared() == 0) {
                dir = new org.bukkit.util.Vector(0, 0, 1);
            } else {
                dir.normalize();
            }
            Location loc = player.getLocation().add(dir.multiply(2));
            double startY = player.getLocation().getY() + 1.0;
            double groundY = startY;
            boolean foundGround = false;
            for (double y = startY; y >= startY - 6.0; y -= 1.0) {
                Location checkLoc = loc.clone();
                checkLoc.setY(y);
                if (checkLoc.getBlock().getType().isSolid()) {
                    groundY = y + 1.0;
                    foundGround = true;
                    break;
                }
            }
            if (!foundGround) {
                groundY = player.getLocation().getY();
            }
            loc.setY(groundY);

            boolean staticMode = true;
            if (args.length > 2) {
                if (args[2].equalsIgnoreCase("normal")) {
                    staticMode = false;
                }
            }

            final boolean isStatic = staticMode;

            // Citizens PLAYER NPC support
            if (var.getType() == org.bukkit.entity.EntityType.PLAYER && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                double hp = var.getModifiers().getHealthValue() > 0 ? var.getModifiers().getHealthValue() : 20.0;
                double dmg = var.getModifiers().getDamageValue() > 0 ? var.getModifiers().getDamageValue() : 4.0;
                double spd = isStatic ? 0.0 : (var.getModifiers().getSpeedValue() > 0 ? var.getModifiers().getSpeedValue() : 0.25);
                String rawName = var.getDisplayName();
                final String finalNpcName = (rawName == null || rawName.isEmpty()) ? var.getId() : rawName;
                final String finalSkin = var.getModifiers().getSkullSkin();
                final double finalHp = hp;
                final double finalDmg = dmg;
                final double finalSpd = spd;
                final Location spawnLoc = loc;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    org.bukkit.entity.Entity npc = fr.wilddifficulty.util.CitizensHook.spawnNPCPlayer(var.getId(), spawnLoc, finalNpcName, finalSkin, finalHp, finalDmg, finalSpd, plugin);
                    if (npc != null && isStatic) {
                        if (npc instanceof LivingEntity le) {
                            le.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "wd_visualize"), PersistentDataType.BYTE, (byte) 1);
                        }
                    }
                });
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.updateVisualizedMobs(var.getId()), 5L);
                player.sendMessage(plugin.getLangManager().get("command.spawn_player", Map.of("id", var.getId())));
                return true;
            }

            Class<? extends Entity> eClass = var.getType().getEntityClass();
            if (var.getType() == org.bukkit.entity.EntityType.PLAYER) {
                eClass = org.bukkit.entity.Zombie.class;
            }
            if (player.getWorld().getDifficulty() == org.bukkit.Difficulty.PEACEFUL && eClass != null && org.bukkit.entity.Enemy.class.isAssignableFrom(eClass)) {
                player.sendMessage("§cImpossible de faire spawner ce monstre en difficulté Paisible (Peaceful).");
                return true;
            }
            Location safeLoc = MobSpawnListener.ensureSafeLocation(loc);
            if (safeLoc == null) {
                safeLoc = loc;
            }
            try {
                player.getWorld().spawn(safeLoc, eClass, CreatureSpawnEvent.SpawnReason.COMMAND, entity -> {
                    entity.getPersistentDataContainer().set(MobSpawnListener.KEY_SQUAD_SPAWNED, PersistentDataType.BYTE, (byte) 1);
                    entity.getPersistentDataContainer().set(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING, var.getId());
                    if (isStatic && entity instanceof LivingEntity le) {
                        le.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "wd_visualize"), PersistentDataType.BYTE, (byte) 1);
                        org.bukkit.attribute.AttributeInstance speedAttr = le.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                        if (speedAttr != null) {
                            speedAttr.setBaseValue(0.0);
                        }
                    }
                });
            } catch (Throwable t) {
                player.sendMessage("§cImpossible de faire spawner l'entité : " + t.getMessage());
                return true;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.updateVisualizedMobs(var.getId()));

            if (plugin.getMainConfigManager().isDebug()) {
                plugin.getLogger().info("[DEBUG] Spawn de la variante " + var.getId() + " à " + loc + " (statique: " + isStatic + ")");
            }

            player.sendMessage(plugin.getLangManager().get("command.spawn_mob", Map.of("id", var.getId(), "mode", (isStatic ? "§7(Mode Visualisation - Figé avec gravité)" : "§7(Mode Normal)"))));
            return true;
        }

        if (sub.equalsIgnoreCase("help") || sub.equalsIgnoreCase("?")) {
            sendHelp(sender);
            return true;
        }

        if (sub.equalsIgnoreCase("tp") && args.length > 1) {
            String zoneId = args[1].toLowerCase();
            DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
            if (zone == null) {
                player.sendMessage(plugin.getLangManager().get("command.zone_not_found", Map.of("zone", zoneId)));
                return true;
            }
            Location tpLoc;
            if (zone.hasCustomTeleport()) {
                tpLoc = new Location(player.getWorld(), zone.getTeleportX(), zone.getTeleportY(), zone.getTeleportZ());
            } else {
                double cx = zone.getCenterX();
                double cz = zone.getCenterZ();
                double cy = zone.getCenterY();
                org.bukkit.block.Block highest = player.getWorld().getHighestBlockAt((int)Math.floor(cx), (int)Math.floor(cz));
                if (highest.getY() > cy) {
                    cy = highest.getY() + 1.0;
                } else {
                    cy += 1.0;
                }
                tpLoc = new Location(player.getWorld(), cx, cy, cz);
            }
            player.teleport(tpLoc);
            player.sendMessage(plugin.getLangManager().get("command.teleport_zone", Map.of("zone", zone.getId())));
            return true;
        }

        if (sub.equalsIgnoreCase("spawnsquad") && args.length > 1) {
            String squadId = args[1].toLowerCase();
            MobSquad squad = plugin.getVariantManager().getSquad(squadId);
            if (squad == null) {
                player.sendMessage(plugin.getLangManager().get("command.squad_not_found"));
                return true;
            }
            Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
            for (Map.Entry<String, MobSquad.SquadMemberRange> entry : squad.getMembers().entrySet()) {
                MobVariant v = plugin.getVariantManager().getVariant(entry.getKey());
                if (v == null) continue;
                int count = plugin.getVariantManager().getRandomMemberCount(entry.getValue());
                for (int i = 0; i < count; i++) {
                    Location spawnLoc = loc.clone().add((Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3);
                    spawnLoc.setY(loc.getY());
                    if (v.getType() == org.bukkit.entity.EntityType.PLAYER && plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                        final MobVariant fVar = v;
                        final String fSquadId = squad.getId();
                        final Location fSpawnLoc = spawnLoc;
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            double hp = fVar.getModifiers().getHealthValue() > 0 ? fVar.getModifiers().getHealthValue() : 20.0;
                            double dmg = fVar.getModifiers().getDamageValue() > 0 ? fVar.getModifiers().getDamageValue() : 4.0;
                            double spd = fVar.getModifiers().getSpeedValue() > 0 ? fVar.getModifiers().getSpeedValue() : 0.25;
                            
                            hp *= squad.getBonusHealth();
                            dmg *= squad.getBonusDamage();
                            spd *= squad.getBonusSpeed();
                            
                            String skin = fVar.getModifiers().getSkullSkin();
                            String name = fVar.getDisplayName();
                            if (name == null || name.isEmpty()) name = fVar.getId();
                            
                            org.bukkit.entity.Entity spawned = fr.wilddifficulty.util.CitizensHook.spawnNPCPlayer(
                                fVar.getId(), fSpawnLoc, name, skin, hp, dmg, spd, plugin
                            );
                            if (spawned != null) {
                                spawned.getPersistentDataContainer().set(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING, fSquadId);
                                spawned.getPersistentDataContainer().set(MobSpawnListener.KEY_SQUAD_SPAWNED, PersistentDataType.BYTE, (byte) 1);
                            }
                        });
                    } else {
                        Class<? extends org.bukkit.entity.Entity> eClass = v.getType().getEntityClass();
                        if (v.getType() == org.bukkit.entity.EntityType.PLAYER) {
                            eClass = org.bukkit.entity.Zombie.class;
                        }
                        if (player.getWorld().getDifficulty() == org.bukkit.Difficulty.PEACEFUL && eClass != null && org.bukkit.entity.Enemy.class.isAssignableFrom(eClass)) {
                            continue;
                        }
                        final String fSquadId = squad.getId();
                        final MobVariant fVar = v;
                        try {
                            player.getWorld().spawn(spawnLoc, eClass, CreatureSpawnEvent.SpawnReason.COMMAND, member -> {
                                member.getPersistentDataContainer().set(MobSpawnListener.KEY_SQUAD_SPAWNED, PersistentDataType.BYTE, (byte) 1);
                                member.getPersistentDataContainer().set(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING, fVar.getId());
                                member.getPersistentDataContainer().set(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING, fSquadId);
                            });
                        } catch (Throwable ignored) {}
                    }
                }
            }
            player.sendMessage(plugin.getLangManager().get("command.squad_spawned", Map.of("id", squad.getId())));
            return true;
        }

        if (sub.equalsIgnoreCase("edit")) {
            Entity target = player.getTargetEntity(15);
            if (!(target instanceof LivingEntity)) {
                player.sendMessage(plugin.getLangManager().get("command.no_target"));
                return true;
            }
            if (target.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                String varId = target.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
                if (plugin.getVariantManager().getVariant(varId) != null) {
                    plugin.getGuiManager().openVariantEditor(player, varId);
                } else {
                    player.sendMessage(plugin.getLangManager().get("command.variant_not_found"));
                }
            } else {
                player.sendMessage(plugin.getLangManager().get("command.target_no_variant"));
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLangManager().getRaw("help.header"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.gui"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.reload"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.killall"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.biome_tool"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.spawner_tool"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.zone_tool"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.inspector_tool"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.tools"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.scoreboard"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.debug"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.bloodmoon"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.spawn"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.spawnsquad"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.edit"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.tp"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.zone"));
        sender.sendMessage(plugin.getLangManager().getRaw("help.help"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : List.of("gui", "reload", "settings", "editzone", "zone", "killall", "spawn", "spawnsquad", "edit", "biome_tool", "spawner_tool", "zone_tool", "inspector_tool", "tools", "scoreboard", "debug", "bloodmoon", "tp", "help")) {
                if (sub.startsWith(input)) list.add(sub);
            }
            return list;
        }
        String sub = args[0].toLowerCase();
        if (sub.equalsIgnoreCase("zone")) {
            String[] zoneArgs = new String[args.length - 1];
            System.arraycopy(args, 1, zoneArgs, 0, zoneArgs.length);
            MobZoneCommand zoneCmd = new MobZoneCommand(plugin);
            return zoneCmd.onTabComplete(sender, command, alias, zoneArgs);
        }
        if (sub.equalsIgnoreCase("tp") || sub.equalsIgnoreCase("editzone")) {
            if (args.length == 2) {
                String input = args[1].toLowerCase();
                for (DifficultyZone zone : plugin.getZoneManager().getAllZones()) {
                    if (zone.getId().startsWith(input)) list.add(zone.getId());
                }
            }
        }
        if (sub.equalsIgnoreCase("spawn")) {
            if (args.length == 2) {
                String input = args[1].toLowerCase();
                for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
                    if (var.getId().startsWith(input)) list.add(var.getId());
                }
            } else if (args.length == 3) {
                String input = args[2].toLowerCase();
                for (String mode : List.of("normal", "static")) {
                    if (mode.startsWith(input)) list.add(mode);
                }
            }
        }
        if (sub.equalsIgnoreCase("spawnsquad") && args.length == 2) {
            String input = args[1].toLowerCase();
            for (MobSquad sq : plugin.getVariantManager().getAllSquads()) {
                if (sq.getId().startsWith(input)) list.add(sq.getId());
            }
        }
        return list;
    }

    private void giveBiomeTool(Player player) {
        org.bukkit.inventory.ItemStack compass = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS);
        org.bukkit.inventory.meta.ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§dConfigure Spawns de Biome"));
            meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Clic Droit pour ouvrir la configuration"),
                net.kyori.adventure.text.Component.text("§7des variantes de votre biome actuel.")
            ));
            compass.setItemMeta(meta);
        }
        player.getInventory().addItem(compass);
    }

    private void giveSpawnerTool(Player player) {
        org.bukkit.inventory.ItemStack shovel = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_SHOVEL);
        org.bukkit.inventory.meta.ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§eOutil de Spawner"));
            meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Clic Droit sur un bloc pour ouvrir"),
                net.kyori.adventure.text.Component.text("§7le panneau de configuration de spawner.")
            ));
            shovel.setItemMeta(meta);
        }
        player.getInventory().addItem(shovel);
    }
}
