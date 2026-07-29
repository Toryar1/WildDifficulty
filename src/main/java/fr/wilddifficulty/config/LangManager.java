package fr.wilddifficulty.config;

import fr.wilddifficulty.WildDifficultyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private final WildDifficultyPlugin plugin;
    private File langFile;
    private FileConfiguration langConfig;
    private final Map<String, String> cache = new HashMap<>();

    public LangManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Define default entries if missing
        setIfNotExists("prefix", "&8[&6WildDifficulty&8] &7");

        setIfNotExists("general.no_permission", "&cVous n'avez pas la permission d'exécuter cette commande.");
        setIfNotExists("general.config_reloaded", "&aConfiguration rechargée avec succès !");
        setIfNotExists("general.invalid_number", "&cNombre invalide.");
        setIfNotExists("general.player_only", "&cSeuls les joueurs peuvent exécuter cette commande.");
        setIfNotExists("general.unknown_command", "&cCommande inconnue. Tapez /wd pour ouvrir le menu.");
        setIfNotExists("general.spawn_distance_updated", "&aDistance de spawn maximale définie.");
        setIfNotExists("general.variant_cap_updated", "&aCap maximum de variantes défini.");
        setIfNotExists("general.nametag_format_updated", "&aFormat des nametags mis à jour.");
        setIfNotExists("general.thirst_multiplier_updated", "&aMultiplicateur de dégradation de soif mis à jour : {val}");
        setIfNotExists("general.heat_multiplier_updated", "&aMultiplicateur de chaleur mis à jour : {val}");
        setIfNotExists("general.despawn_time_updated", "&aTemps de despawn mis à jour.");

        setIfNotExists("tools.given_biome", "&a[WD] Outil de configuration de biome donné.");
        setIfNotExists("tools.given_spawner", "&a[WD] Outil de Spawner donné. Clic droit sur un bloc avec cet outil pour éditer le spawner.");
        setIfNotExists("tools.given_zone", "&a[WD] Outil de Zone donné. Clic droit pour placer des points.");
        setIfNotExists("tools.given_inspector", "&a[WD] Outil d'Analyse (Inspecteur) donné. Clic droit sur un monstre pour l'analyser.");
        setIfNotExists("tools.given_all", "&a[WD] Tous les outils d'administration vous ont été donnés.");

        setIfNotExists("command.killall", "&a[WD] {count} monstres de WildDifficulty ont été supprimés.");
        setIfNotExists("command.scoreboard_on", "&a[WD] Scoreboard de debug activé.");
        setIfNotExists("command.scoreboard_off", "&c[WD] Scoreboard de debug désactivé.");
        setIfNotExists("command.bloodmoon_scheduled", "&a[WD] Lune de Sang planifiée pour la prochaine nuit !");
        setIfNotExists("command.debug_toggled", "&a[WD] Mode debug défini sur : {status}");
        setIfNotExists("command.spawn_player", "&aSpawné NPC Joueur : {id}");
        setIfNotExists("command.spawn_mob", "&aSpawné : {id} {mode}");
        setIfNotExists("command.variant_not_found", "&cVariante introuvable.");
        setIfNotExists("command.squad_not_found", "&cEscouade introuvable.");
        setIfNotExists("command.squad_spawned", "&aEscouade {id} invoquée devant vous.");
        setIfNotExists("command.teleport_zone", "&aTéléporté à la zone '{zone}'.");
        setIfNotExists("command.zone_not_found", "&cZone introuvable : {zone}");
        setIfNotExists("command.no_target", "&cAucune entité valide pointée.");
        setIfNotExists("command.target_no_variant", "&cCette entité ne possède pas de variante (c'est un mob vanilla).");

        setIfNotExists("help.header", "&6★ WildDifficulty - Aide ★");
        setIfNotExists("help.gui", "&e/wd gui &7- Ouvre le menu d'administration principal");
        setIfNotExists("help.reload", "&e/wd reload &7- Recharge les fichiers de configuration");
        setIfNotExists("help.killall", "&e/wd killall &7- Supprime toutes les entités personnalisées");
        setIfNotExists("help.biome_tool", "&e/wd biome_tool &7- Donne la boussole de gestion des biomes");
        setIfNotExists("help.spawner_tool", "&e/wd spawner_tool &7- Donne la pelle de gestion des spawners");
        setIfNotExists("help.zone_tool", "&e/wd zone_tool &7- Donne la houe de gestion des zones");
        setIfNotExists("help.inspector_tool", "&e/wd inspector_tool &7- Donne l'outil d'analyse (Inspecteur de Mobs)");
        setIfNotExists("help.tools", "&e/wd tools &7- Donne tous les outils d'administration");
        setIfNotExists("help.scoreboard", "&e/wd scoreboard &7- Active ou désactive le scoreboard de debug");
        setIfNotExists("help.debug", "&e/wd debug &7- Active ou désactive les logs de debug");
        setIfNotExists("help.bloodmoon", "&e/wd bloodmoon &7- Force une lune de sang pour la prochaine nuit");
        setIfNotExists("help.spawn", "&e/wd spawn <variant> [normal|static] &7- Fait apparaître un monstre");
        setIfNotExists("help.spawnsquad", "&e/wd spawnsquad <escouade> &7- Fait apparaître une escouade");
        setIfNotExists("help.edit", "&e/wd edit &7- Ouvre le GUI d'édition de la variante pointée");
        setIfNotExists("help.tp", "&e/wd tp <zone> &7- Téléporte à une zone de difficulté");
        setIfNotExists("help.zone", "&e/wd zone <args> &7- Commandes manuelles pour les zones");
        setIfNotExists("help.help", "&e/wd help &7- Affiche ce message d'aide");

        setIfNotExists("bloodmoon.start_message", "&cLa Lune de Sang se lève... Les monstres sont enragés !");
        setIfNotExists("bloodmoon.end_message", "&aLa Lune de Sang se couche. Le calme revient...");

        setIfNotExists("hardcore.instant_death_despawn", "&cMode Hardcore : Votre équipement a disparu instantanément à votre mort.");

        setIfNotExists("zone.created", "&aZone &e{zone}&a créée avec succès !");
        setIfNotExists("zone.deleted", "&cZone &e{zone}&c supprimée.");
        setIfNotExists("zone.protection_denied", "&cCette zone est protégée ! Vous n'êtes pas autorisé à agir ici.");
        setIfNotExists("zone.container_denied", "&cCette zone est protégée ! Vous n'êtes pas autorisé à utiliser ces éléments.");
        setIfNotExists("zone.no_active_zone", "&cAucune zone en cours d'édition ou de création. Ouvrez l'éditeur d'une zone et prenez l'outil.");
        setIfNotExists("zone.pos1_set", "&aPosition 1 (CUBOID) définie sur : {x}, {y}, {z}");
        setIfNotExists("zone.pos2_set", "&aPosition 2 (CUBOID) définie sur : {x}, {y}, {z}");
        setIfNotExists("zone.center_set", "&aCentre (RADIUS) défini sur : {x}, {y}, {z}");
        setIfNotExists("zone.radius_set", "&aRayon (RADIUS) défini à {radius} blocs.");
        setIfNotExists("zone.point_added", "&aPoint ajouté : [{x}, {z}] (Total: {total} points)");
        setIfNotExists("zone.points_reset", "&cPoints du polygone réinitialisés.");

        setIfNotExists("spawner.created", "&aSpawner personnalisé créé en X: {x}, Y: {y}, Z: {z}.");
        setIfNotExists("spawner.removed", "&cSpawner personnalisé supprimé.");
        setIfNotExists("spawner.no_spawner_clicked", "&cAucun spawner trouvé sur ce bloc. Clic-droit sur un spawner.");

        setIfNotExists("inspector.header", "&e=== Inspecteur de Mobs WildDifficulty ===");
        setIfNotExists("inspector.type", "&7Type: &f{type}");
        setIfNotExists("inspector.variant", "&7Variante: &f{variant}");
        setIfNotExists("inspector.health", "&7PV: &e{hp}/{max_hp}");
        setIfNotExists("inspector.damage", "&7Dégâts: &c{dmg}");
        setIfNotExists("inspector.speed", "&7Vitesse: &b{speed}");

        try {
            langConfig.save(langFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder lang.yml : " + e.getMessage());
        }

        // Cache messages
        cache.clear();
        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                cache.put(key, langConfig.getString(key));
            }
        }
    }

    private void setIfNotExists(String path, String defaultValue) {
        if (!langConfig.contains(path)) {
            langConfig.set(path, defaultValue);
        }
    }

    public String getPrefix() {
        return format(cache.getOrDefault("prefix", "&8[&6WildDifficulty&8] &7"));
    }

    public String getRaw(String key) {
        String msg = cache.getOrDefault(key, key);
        return format(msg);
    }

    public String getRaw(String key, Map<String, String> replacements) {
        String msg = cache.getOrDefault(key, key);
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return format(msg);
    }

    public String get(String key) {
        return getPrefix() + getRaw(key);
    }

    public String get(String key, Map<String, String> replacements) {
        return getPrefix() + getRaw(key, replacements);
    }

    public Component getComponent(String key) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(get(key));
    }

    public Component getComponent(String key, Map<String, String> replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(get(key, replacements));
    }

    public Component getRawComponent(String key) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getRaw(key));
    }

    public Component getRawComponent(String key, Map<String, String> replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getRaw(key, replacements));
    }

    public static String format(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
