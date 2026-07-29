package fr.wilddifficulty.commands;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.BiomeConfigManager;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.zone.DifficultyZone;
import fr.wilddifficulty.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Commande /mobzone — Gestion des zones de difficulté personnalisées.
 *
 * Sous-commandes :
 *   create <id> CUBOID|RADIUS   — Démarre la création d'une zone
 *   pos1                        — Définit le coin 1 (CUBOID)
 *   pos2                        — Définit le coin 2 (CUBOID) et finalise
 *   center                      — Définit le centre (RADIUS)
 *   radius <valeur>             — Définit le rayon (RADIUS) et finalise
 *   priority <valeur>           — Définit la priorité de la zone en cours
 *   set <id> <prop> <valeur>    — Modifie une propriété d'une zone existante
 *   delete <id>                 — Supprime une zone
 *   list                        — Liste toutes les zones
 *   info <id>                   — Affiche les détails d'une zone
 *   visualize <id> [secondes]   — Affiche des particules pour visualiser la zone
 *   reload                      — Recharge les zones depuis zones.yml
 *
 * Permission : wilddifficulty.admin
 */
public class MobZoneCommand implements CommandExecutor, TabCompleter {

    private final WildDifficultyPlugin plugin;

    public MobZoneCommand(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Messages formatés ────────────────────────────────────

    private static final Component PREFIX = Component.text("[WD] ").color(NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true);

    private Component ok(String msg) {
        return PREFIX.append(Component.text(msg).color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, false));
    }
    private Component err(String msg) {
        return PREFIX.append(Component.text(msg).color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, false));
    }
    private Component info(String msg) {
        return PREFIX.append(Component.text(msg).color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, false));
    }

    // ── Exécution ────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wilddifficulty.admin")) {
            sender.sendMessage(err("Vous n'avez pas la permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // Sous-commandes accessibles depuis la console
        switch (sub) {
            case "list"         -> handleList(sender);
            case "info"         -> handleInfo(sender, args);
            case "delete"       -> handleDelete(sender, args);
            case "reload"       -> handleReload(sender);
            case "set"          -> handleSet(sender, args);
            case "addmember"    -> handleAddMember(sender, args);
            case "removemember" -> handleRemoveMember(sender, args);
            default -> {
                // Sous-commandes nécessitant un joueur en jeu
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(err("Cette sous-commande nécessite d'être en jeu."));
                    return true;
                }
                switch (sub) {
                    case "create"    -> handleCreate(player, args);
                    case "pos1"      -> handlePos(player, 1);
                    case "pos2"      -> handlePos(player, 2);
                    case "center"    -> handleCenter(player);
                    case "radius"    -> handleRadius(player, args);
                    case "priority"  -> handlePriority(player, args);
                    case "visualize" -> handleVisualize(player, args);
                    case "tool"      -> {
                        giveZoneTool(player);
                        player.sendMessage(ok("Outil de zone donné."));
                    }
                    default          -> sendHelp(sender);
                }
            }
        }
        return true;
    }

    // ── Sous-commandes ───────────────────────────────────────

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(info("Usage : /wd zone create <id> CUBOID|RADIUS|POLYGON"));
            return;
        }
        String id = args[1];
        String typeStr = args[2].toUpperCase();

        if (plugin.getZoneManager().getZone(id) != null) {
            player.sendMessage(err("Une zone avec l'id '" + id + "' existe déjà."));
            return;
        }

        DifficultyZone.ZoneType type;
        try {
            type = DifficultyZone.ZoneType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(err("Type invalide. Utilisez CUBOID, RADIUS ou POLYGON."));
            return;
        }

        DifficultyZone zone = new DifficultyZone(id, type, player.getWorld().getName());
        plugin.getZoneManager().setPendingZone(player.getUniqueId(), zone);

        player.sendMessage(ok("Zone '" + id + "' de type " + type + " créée."));
        if (type == DifficultyZone.ZoneType.CUBOID) {
            player.sendMessage(info("Faites /wd zone pos1 et /wd zone pos2 pour définir la zone."));
        } else if (type == DifficultyZone.ZoneType.RADIUS) {
            player.sendMessage(info("Faites /wd zone center puis /wd zone radius <valeur>."));
        } else {
            player.sendMessage(info("Outil de zone donné. Faites un Clic Droit avec la houe en or pour ajouter des points, puis un Clic Gauche pour fermer le polygone."));
            giveZoneTool(player);
        }
    }

    public void giveZoneTool(Player player) {
        player.getInventory().addItem(fr.wilddifficulty.util.ToolsUtil.getZoneTool());
    }

    private void handlePos(Player player, int posNum) {
        DifficultyZone zone = plugin.getZoneManager().getPendingZone(player.getUniqueId());
        if (zone == null) {
            player.sendMessage(err("Aucune zone en cours de création. Utilisez /wd zone create d'abord."));
            return;
        }
        if (zone.getType() != DifficultyZone.ZoneType.CUBOID) {
            player.sendMessage(err("Cette commande est réservée aux zones CUBOID."));
            return;
        }

        Location loc = player.getLocation();
        if (posNum == 1) {
            zone.setPos1(loc.getX(), loc.getY(), loc.getZ());
            player.sendMessage(ok(String.format("Pos1 définie : [%.0f, %.0f, %.0f]",
                    loc.getX(), loc.getY(), loc.getZ())));
        } else {
            zone.setPos2(loc.getX(), loc.getY(), loc.getZ());
            player.sendMessage(ok(String.format("Pos2 définie : [%.0f, %.0f, %.0f]",
                    loc.getX(), loc.getY(), loc.getZ())));
        }

        // Si les deux positions sont définies, finaliser la zone
        if (zone.isCuboidComplete()) {
            zone.finalizeCuboid();
            plugin.getZoneManager().addZone(zone);
            plugin.getZoneManager().clearPendingZone(player.getUniqueId());
            player.sendMessage(ok("Zone '" + zone.getId() + "' finalisée et sauvegardée ! "
                    + zone.getGeometryDescription()));
            player.sendMessage(info("Utilisez /wd zone set " + zone.getId() + " pour configurer les modificateurs."));
        }
    }

    private void handleCenter(Player player) {
        DifficultyZone zone = plugin.getZoneManager().getPendingZone(player.getUniqueId());
        if (zone == null) {
            player.sendMessage(err("Aucune zone en cours de création."));
            return;
        }
        if (zone.getType() != DifficultyZone.ZoneType.RADIUS) {
            player.sendMessage(err("Cette commande est réservée aux zones RADIUS."));
            return;
        }

        Location loc = player.getLocation();
        zone.setCenter(loc.getX(), loc.getY(), loc.getZ());
        player.sendMessage(ok(String.format("Centre défini : [%.0f, %.0f, %.0f]. Maintenant : /mobzone radius <valeur>",
                loc.getX(), loc.getY(), loc.getZ())));
    }

    private void handleRadius(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(info("Usage : /mobzone radius <valeur>"));
            return;
        }
        DifficultyZone zone = plugin.getZoneManager().getPendingZone(player.getUniqueId());
        if (zone == null) {
            player.sendMessage(err("Aucune zone en cours de création."));
            return;
        }
        if (zone.getType() != DifficultyZone.ZoneType.RADIUS) {
            player.sendMessage(err("Cette commande est réservée aux zones RADIUS."));
            return;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[1]);
            if (radius <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(err("Rayon invalide. Entrez un nombre positif."));
            return;
        }

        zone.setRadius(radius);

        if (zone.getCenterX() == 0 && zone.getCenterZ() == 0) {
            player.sendMessage(info("Rayon défini. N'oubliez pas de définir le centre avec /mobzone center."));
        } else {
            plugin.getZoneManager().addZone(zone);
            plugin.getZoneManager().clearPendingZone(player.getUniqueId());
            player.sendMessage(ok("Zone '" + zone.getId() + "' finalisée ! " + zone.getGeometryDescription()));
            player.sendMessage(info("Utilisez /mobzone set " + zone.getId() + " pour configurer les modificateurs."));
        }
    }

    private void handlePriority(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(info("Usage : /mobzone priority <valeur>"));
            return;
        }
        DifficultyZone zone = plugin.getZoneManager().getPendingZone(player.getUniqueId());
        if (zone == null) {
            player.sendMessage(err("Aucune zone en cours de création. Utilisez /mobzone set <id> priorite <val> pour une zone existante."));
            return;
        }
        try {
            zone.setPriority(Integer.parseInt(args[1]));
            player.sendMessage(ok("Priorité de la zone en cours définie à " + args[1]));
        } catch (NumberFormatException e) {
            player.sendMessage(err("Valeur de priorité invalide."));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(info("Usage : /mobzone set <id> <propriété> <valeur>"));
            sender.sendMessage(info("Propriétés : priorite, zone-sure, ignorer-regles-biome, " +
                    "pv, degats, vitesse, portee, knockback, resistance-feu, " +
                    "tier-equipement, chance-equipement"));
            return;
        }

        String id = args[1];
        DifficultyZone zone = plugin.getZoneManager().getZone(id);
        if (zone == null) {
            sender.sendMessage(err("Zone introuvable : " + id));
            return;
        }

        String prop = args[2].toLowerCase();
        String valeur = args[3];

        try {
            switch (prop) {
                case "priorite"            -> zone.setPriority(Integer.parseInt(valeur));
                case "zone-sure"           -> zone.setSafeZone(Boolean.parseBoolean(valeur));
                case "ignorer-regles-biome"-> zone.setOverrideBiomeRules(Boolean.parseBoolean(valeur));
                case "pv"                  -> zone.getModifiers().setHealthValue(Double.parseDouble(valeur));
                case "degats"              -> zone.getModifiers().setDamageValue(Double.parseDouble(valeur));
                case "vitesse"             -> zone.getModifiers().setSpeedValue(Double.parseDouble(valeur));
                case "portee"              -> zone.getModifiers().setFollowRangeValue(Double.parseDouble(valeur));
                case "knockback"           -> zone.getModifiers().setKnockbackValue(Double.parseDouble(valeur));
                case "resistance-feu"      -> zone.getModifiers().setResistanceFeu(Double.parseDouble(valeur));
                case "tier-equipement"     -> zone.getModifiers().setEquipmentTier(valeur);
                case "chance-equipement"   -> zone.getModifiers().setEquipmentChance(Double.parseDouble(valeur));
                default -> {
                    sender.sendMessage(err("Propriété inconnue : " + prop));
                    return;
                }
            }
            plugin.getZoneManager().addZone(zone); // Re-sauvegarde
            sender.sendMessage(ok("Zone '" + id + "' mise à jour : " + prop + " = " + valeur));
        } catch (NumberFormatException e) {
            sender.sendMessage(err("Valeur numérique invalide : " + valeur));
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(info("Usage : /mobzone delete <id>"));
            return;
        }
        String id = args[1];
        if (plugin.getZoneManager().removeZone(id)) {
            sender.sendMessage(ok("Zone '" + id + "' supprimée."));
        } else {
            sender.sendMessage(err("Zone introuvable : " + id));
        }
    }

    private void handleList(CommandSender sender) {
        List<DifficultyZone> zones = plugin.getZoneManager().getAllZones();
        if (zones.isEmpty()) {
            sender.sendMessage(info("Aucune zone définie. Utilisez /mobzone create pour en créer une."));
            return;
        }
        sender.sendMessage(info("=== Zones WildDifficulty (" + zones.size() + ") ==="));
        for (DifficultyZone zone : zones) {
            sender.sendMessage(Component.text(" • ").color(NamedTextColor.GOLD)
                    .append(Component.text(zone.getId()).color(NamedTextColor.WHITE))
                    .append(Component.text(" [" + zone.getType() + "] prio=" + zone.getPriority()
                            + " monde=" + zone.getWorld()).color(NamedTextColor.GRAY)));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(info("Usage : /mobzone info <id>"));
            return;
        }
        DifficultyZone zone = plugin.getZoneManager().getZone(args[1]);
        if (zone == null) {
            sender.sendMessage(err("Zone introuvable : " + args[1]));
            return;
        }

        StatModifiers m = zone.getModifiers();
        sender.sendMessage(info("═══ Zone : " + zone.getId() + " ═══"));
        sender.sendMessage(info("  Type      : " + zone.getType()));
        sender.sendMessage(info("  Monde     : " + zone.getWorld()));
        sender.sendMessage(info("  Priorité  : " + zone.getPriority()));
        sender.sendMessage(info("  Géométrie : " + zone.getGeometryDescription()));
        sender.sendMessage(info("  Zone sûre : " + zone.isSafeZone()));
        sender.sendMessage(info("  Ignore biome : " + zone.isOverrideBiomeRules()));
        sender.sendMessage(info("  Modificateurs :"));
        sender.sendMessage(info("    PV=" + m.getHealthValue() + " Dégâts=" + m.getDamageValue()
                + " Vitesse=" + m.getSpeedValue() + " Portée=" + m.getFollowRangeValue()
                + " Knockback=" + m.getKnockbackValue()));
        sender.sendMessage(info("    Feu=" + m.getResistanceFeu()
                + " Équip=" + m.getEquipmentTier() + "@" + m.getEquipmentChance()));
        if (!zone.getAllowedMobs().isEmpty())
            sender.sendMessage(info("  Mobs autorisés : " + zone.getAllowedMobs()));
        if (!zone.getDeniedMobs().isEmpty())
            sender.sendMessage(info("  Mobs interdits  : " + zone.getDeniedMobs()));
    }

    /**
     * Affiche des particules temporaires pour visualiser la zone.
     */
    private void handleVisualize(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(info("Usage : /mobzone visualize <id> [secondes]"));
            return;
        }
        DifficultyZone zone = plugin.getZoneManager().getZone(args[1]);
        if (zone == null) {
            player.sendMessage(err("Zone introuvable : " + args[1]));
            return;
        }
        if (!zone.getWorld().equals(player.getWorld().getName())) {
            player.sendMessage(err("Vous n'êtes pas dans le monde de cette zone (" + zone.getWorld() + ")."));
            return;
        }

        int duree = 10; // secondes par défaut
        if (args.length >= 3) {
            try { duree = Math.min(60, Math.max(1, Integer.parseInt(args[2]))); }
            catch (NumberFormatException ignored) {}
        }

        player.sendMessage(ok("Visualisation de la zone '" + zone.getId() + "' pendant " + duree + "s..."));
        spawnZoneParticles(player, zone, duree);
    }

    private void handleReload(CommandSender sender) {
        plugin.getZoneManager().reload();
        sender.sendMessage(ok("Zones rechargées depuis zones.yml."));
    }

    // ── Particules de visualisation ──────────────────────────

    private void spawnZoneParticles(Player player, DifficultyZone zone, int dureeSecondes) {
        new BukkitRunnable() {
            int ticks = dureeSecondes * 20;

            @Override
            public void run() {
                if (ticks <= 0 || !player.isOnline()) {
                    cancel();
                    return;
                }
                ticks -= 5;

                if (zone.getType() == DifficultyZone.ZoneType.CUBOID) {
                    drawCuboidParticles(player, zone);
                } else {
                    drawRadiusParticles(player, zone);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void drawCuboidParticles(Player player, DifficultyZone zone) {
        double y = player.getLocation().getY();
        double minX = zone.getMinX(), maxX = zone.getMaxX();
        double minZ = zone.getMinZ(), maxZ = zone.getMaxZ();

        // Dessine les 4 arêtes horizontales du cuboid à la hauteur du joueur
        drawLine(player, minX, y, minZ, maxX, y, minZ, 1.5);
        drawLine(player, maxX, y, minZ, maxX, y, maxZ, 1.5);
        drawLine(player, maxX, y, maxZ, minX, y, maxZ, 1.5);
        drawLine(player, minX, y, maxZ, minX, y, minZ, 1.5);
    }

    private void drawRadiusParticles(Player player, DifficultyZone zone) {
        double cx = zone.getCenterX(), cy = player.getLocation().getY(), cz = zone.getCenterZ();
        double r = zone.getRadius();
        int points = Math.min(72, (int) (r * 0.5) + 20);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double px = cx + r * Math.cos(angle);
            double pz = cz + r * Math.sin(angle);
            Location loc = new Location(player.getWorld(), px, cy, pz);
            player.spawnParticle(Particle.DUST,
                    loc, 1,
                    new Particle.DustOptions(Color.fromRGB(255, 80, 0), 1.5f));
        }
    }

    private void drawLine(Player player, double x1, double y1, double z1,
                          double x2, double y2, double z2, double spacing) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        int count = (int) Math.ceil(len / spacing);
        for (int i = 0; i <= count; i++) {
            double t = (double) i / count;
            Location loc = new Location(player.getWorld(),
                    x1 + dx * t, y1 + dy * t, z1 + dz * t);
            player.spawnParticle(Particle.DUST,
                    loc, 1,
                    new Particle.DustOptions(Color.fromRGB(0, 180, 255), 1.5f));
        }
    }

    private void handleAddMember(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(info("Usage : /wd zone addmember <zoneId> <joueur> <1|2|3>"));
            sender.sendMessage(info("  Niveau 1: Conteneurs & Objets"));
            sender.sendMessage(info("  Niveau 2: Poser / Casser & Conteneurs"));
            sender.sendMessage(info("  Niveau 3: Gestionnaire complet"));
            return;
        }
        String zoneId = args[1];
        String targetName = args[2];
        int level;
        try {
            level = Integer.parseInt(args[3]);
            if (level < 1 || level > 3) throw new NumberFormatException();
        } catch (Exception e) {
            sender.sendMessage(err("Niveau invalide. Choisissez 1 (Conteneurs), 2 (Pose/Casse) ou 3 (Gestionnaire)."));
            return;
        }

        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            sender.sendMessage(err("La zone '" + zoneId + "' n'existe pas."));
            return;
        }

        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        fr.wilddifficulty.zone.ZoneMember member = new fr.wilddifficulty.zone.ZoneMember(target.getUniqueId(), target.getName() != null ? target.getName() : targetName, level);
        zone.addMember(member);
        plugin.getZoneManager().save();

        sender.sendMessage(ok("Joueur '" + member.getLastKnownName() + "' ajouté à la zone '" + zoneId + "' avec le niveau " + level + " (" + member.getRoleName() + ")."));
    }

    private void handleRemoveMember(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(info("Usage : /wd zone removemember <zoneId> <joueur>"));
            return;
        }
        String zoneId = args[1];
        String targetName = args[2];

        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            sender.sendMessage(err("La zone '" + zoneId + "' n'existe pas."));
            return;
        }

        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        zone.removeMember(target.getUniqueId());
        plugin.getZoneManager().save();

        sender.sendMessage(ok("Joueur '" + targetName + "' retiré des membres de la zone '" + zoneId + "'."));
    }

    // ── Tab complétion ───────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("create", "pos1", "pos2", "center", "radius",
                    "priority", "set", "delete", "list", "info", "visualize", "reload", "addmember", "removemember"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("delete", "info", "set", "visualize", "addmember", "removemember").contains(sub)) {
                plugin.getZoneManager().getAllZones().forEach(z -> completions.add(z.getId()));
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equalsIgnoreCase("create")) {
                completions.addAll(List.of("CUBOID", "RADIUS", "POLYGON"));
            } else if (sub.equalsIgnoreCase("addmember") || sub.equalsIgnoreCase("removemember")) {
                plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("addmember")) {
            completions.addAll(List.of("1", "2", "3"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            completions.addAll(List.of("priorite", "zone-sure", "ignorer-regles-biome",
                    "pv", "degats", "vitesse", "portee", "knockback",
                    "resistance-feu", "tier-equipement", "chance-equipement"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            String prop = args[2].toLowerCase();
            if (prop.equals("tier-equipement")) {
                completions.addAll(List.of("none", "leather", "iron", "diamond", "netherite"));
            } else if (prop.equals("zone-sure") || prop.equals("ignorer-regles-biome")
                    || prop.equals("resistance-feu")) {
                completions.addAll(List.of("true", "false"));
            }
        }
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }

    // ── Aide ─────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(info("═══ WildDifficulty /wd zone ═══"));
        sender.sendMessage(info("  /wd zone create <id> CUBOID|RADIUS|POLYGON"));
        sender.sendMessage(info("  /wd zone addmember <zoneId> <joueur> <1|2|3>"));
        sender.sendMessage(info("  /wd zone removemember <zoneId> <joueur>"));
        sender.sendMessage(info("  /wd zone pos1 | pos2"));
        sender.sendMessage(info("  /wd zone center | radius <val>"));
        sender.sendMessage(info("  /wd zone priority <val>"));
        sender.sendMessage(info("  /wd zone set <id> <prop> <val>"));
        sender.sendMessage(info("  /wd zone delete <id>"));
        sender.sendMessage(info("  /wd zone list | info <id>"));
        sender.sendMessage(info("  /wd zone visualize <id> [secondes]"));
        sender.sendMessage(info("  /wd zone reload"));
    }
}
