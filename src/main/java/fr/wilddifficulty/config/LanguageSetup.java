package fr.wilddifficulty.config;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.util.ConsoleColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gère la sélection et le changement dynamique de langue du plugin.
 * Handles language selection and dynamic language switching for the plugin.
 */
public class LanguageSetup {

    private static final Map<String, String> LANGUAGES = new LinkedHashMap<>();

    static {
        LANGUAGES.put("fr",    "Français");
        LANGUAGES.put("en",    "English");
        LANGUAGES.put("de",    "Deutsch");
        LANGUAGES.put("es",    "Español");
        LANGUAGES.put("pt_BR", "Português Brasileiro");
        LANGUAGES.put("nl",    "Nederlands");
        LANGUAGES.put("pl",    "Polski");
        LANGUAGES.put("ru",    "Русский");
        LANGUAGES.put("zh_CN", "简体中文");
        LANGUAGES.put("it",    "Italiano");
    }

    private final WildDifficultyPlugin plugin;
    private final Logger logger;

    public LanguageSetup(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Vérifie et applique la langue configurée. Si c'est le premier démarrage, propose
     * une sélection interactive en console. Sinon, relit plugin.language depuis config.yml
     * et ré-extrait toujours le bon fichier de langue pour garantir la cohérence.
     */
    public void setupIfNeeded() {
        plugin.reloadConfig();
        String configured = plugin.getConfig().getString("plugin.language", null);
        File langFile = new File(plugin.getDataFolder(), "lang.yml");

        // Premier démarrage absolu : ni lang.yml ni langue configurée
        if (!langFile.exists() && (configured == null || !LANGUAGES.containsKey(configured))) {
            printSelectionMenu();
            String choice = readConsoleInput();
            String selectedCode = resolveChoice(choice);

            if (selectedCode == null) {
                logger.warning(ConsoleColor.WARN_PREFIX + "Choix invalide. Default language: English (en)");
                selectedCode = "en";
            }

            plugin.getConfig().set("plugin.language", selectedCode);
            plugin.saveConfig();

            extractLang(selectedCode);
            logger.info(ConsoleColor.INFO_PREFIX + "Langue sélectionnée : " + LANGUAGES.get(selectedCode) + " (" + selectedCode + ")");
            return;
        }

        // Langue déjà configurée : toujours ré-extraire pour garantir la cohérence
        if (configured != null && LANGUAGES.containsKey(configured)) {
            extractLang(configured);
            logger.info(ConsoleColor.INFO_PREFIX + "Langue active : " + LANGUAGES.get(configured) + " (" + configured + ")");
            return;
        }

        // lang.yml existe mais pas de langue configurée : on garde le fichier existant
        // et on détermine la langue par défaut (fr)
        if (langFile.exists()) {
            plugin.getConfig().set("plugin.language", "en");
            plugin.saveConfig();
            logger.info(ConsoleColor.INFO_PREFIX + "Langue non configurée. Default language: English (en)");
        }
    }

    private void printSelectionMenu() {
        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║          WildDifficulty — Sélection de la langue            ║");
        logger.info("║            WildDifficulty — Language Selection              ║");
        logger.info("╠══════════════════════════════════════════════════════════════╣");

        int i = 1;
        for (Map.Entry<String, String> entry : LANGUAGES.entrySet()) {
            logger.info(String.format("║  %2d. %-10s  %s", i++, "[" + entry.getKey() + "]", entry.getValue()));
        }

        logger.info("╚══════════════════════════════════════════════════════════════╝");
        logger.info("Entrez le numéro ou le code de langue dans la console :");
        logger.info("Enter the number or language code in the console:");
    }

    private String readConsoleInput() {
        try {
            long timeout = System.currentTimeMillis() + 60_000;
            while (System.currentTimeMillis() < timeout) {
                if (System.in.available() > 0) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) return line.trim();
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            logger.warning(ConsoleColor.WARN_PREFIX + "Impossible de lire la console : " + e.getMessage());
        }
        return null;
    }

    private String resolveChoice(String input) {
        if (input == null) return null;
        if (LANGUAGES.containsKey(input)) return input;
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx >= 0 && idx < LANGUAGES.size()) {
                return (String) LANGUAGES.keySet().toArray()[idx];
            }
        } catch (NumberFormatException ignored) {}

        for (String code : LANGUAGES.keySet()) {
            if (code.equalsIgnoreCase(input)) return code;
        }
        return null;
    }

    
    
    private void updateConfigFileComments(String langCode) {
        try {
            translateYamlFileComments("config.yml", langCode);
            translateYamlFileComments("mobs.yml", langCode);
            translateYamlFileComments("biomes.yml", langCode);
            translateYamlFileComments("zones.yml", langCode);
            translateYamlFileComments("mob-variants.yml", langCode);
        } catch (Exception e) {
            logger.warning("[Lang] Error updating YML comments: " + e.getMessage());
        }
    }

    private void translateYamlFileComments(String fileName, String langCode) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) return;
        try {
            List<String> lines = java.nio.file.Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) {
                    String commentText = trimmed.substring(1).trim();
                    String translatedComment = translateCommentText(commentText, langCode);
                    String indent = line.substring(0, line.indexOf("#"));
                    updatedLines.add(indent + "# " + translatedComment);
                } else if (line.contains(" #")) {
                    int hashIdx = line.indexOf(" #");
                    String codePart = line.substring(0, hashIdx);
                    String commentText = line.substring(hashIdx + 2).trim();
                    String translatedComment = translateCommentText(commentText, langCode);
                    updatedLines.add(codePart + " # " + translatedComment);
                } else {
                    updatedLines.add(line);
                }
            }
            java.nio.file.Files.write(file.toPath(), updatedLines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

        private String translateCommentText(String comment, String langCode) {
        if (comment.isEmpty()) return "";
        String lc = langCode.toLowerCase();
        switch (lc) {
            case "fr" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(Aucune zone définie par défaut — utilisez /wd zone create pour en créer une)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(Aucune zone définie par défaut — utilisez /wd zone create pour en créer une)");
                if (comment.equals("- SPIDER")) return "- ARAIGNÉE";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- ARAIGNÉE");
                if (comment.equals("- ZOMBIE")) return "- ZOMBI";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ZOMBI");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME : # Key = Nom de l'énumération du biome de Bukkit (par exemple DESERT, JUNGLE...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME : # Key = Nom de l'énumération du biome de Bukkit (par exemple DESERT, JUNGLE...)");
                if (comment.equals("Base Global Modifiers")) return "Modificateurs globaux de base";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Modificateurs globaux de base");
                if (comment.equals("Cap of variants per player")) return "Plafond de variantes par joueur";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Plafond de variantes par joueur");
                if (comment.equals("Custom in-game difficulty zones")) return "Zones de difficulté personnalisées dans le jeu";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Zones de difficulté personnalisées dans le jeu");
                if (comment.equals("Desert mobs are fire resistant")) return "Les foules du désert sont résistantes au feu";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "Les foules du désert sont résistantes au feu");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "La difficulté augmente à mesure que les joueurs s’éloignent du point d’origine central.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "La difficulté augmente à mesure que les joueurs s’éloignent du point d’origine central.");
                if (comment.equals("Distance Scaling")) return "Mise à l'échelle des distances";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Mise à l'échelle des distances");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "Ne modifiez pas manuellement sauf si cela est absolument nécessaire.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "Ne modifiez pas manuellement sauf si cela est absolument nécessaire.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Activer le mode débogage (journaux supplémentaires dans la console – désactiver en production)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Activer le mode débogage (journaux supplémentaires dans la console – désactiver en production)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Activer le code langue (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Activer le code langue (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "Fichier de stockage des paramètres personnels des joueurs";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "Fichier de stockage des paramètres personnels des joueurs");
                if (comment.equals("General Biome Format:")) return "Format général du biome :";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Format général du biome :");
                if (comment.equals("General Format:")) return "Format général :";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Format général :");
                if (comment.equals("General Plugin Settings")) return "Paramètres généraux du plugin";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Paramètres généraux du plugin");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Global × Biome × Distance × Zone × Mob-Override = Final";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Global × Biome × Distance × Zone × Mob-Override = Final");
                if (comment.equals("Husks have a bonus HP multiplier")) return "Les coques ont un multiplicateur de HP bonus";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "Les coques ont un multiplicateur de HP bonus");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Configuration individuelle par type de mob vanille";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Configuration individuelle par type de mob vanille");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE : # Key = nom de l'énumération Bukkit EntityType (par exemple ZOMBIE, SKELETON...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE : # Key = nom de l'énumération Bukkit EntityType (par exemple ZOMBIE, SKELETON...)");
                if (comment.equals("Main plugin configuration file")) return "Fichier de configuration principal du plugin";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "Fichier de configuration principal du plugin");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Distance d'apparition maximale (en blocs) du joueur au-delà de laquelle les monstres naturels n'apparaissent pas (-1 pour désactiver)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Distance d'apparition maximale (en blocs) du joueur au-delà de laquelle les monstres naturels n'apparaissent pas (-1 pour désactiver)");
                if (comment.equals("Monster variants and squads configuration")) return "Variantes de monstres et configuration des escouades";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Variantes de monstres et configuration des escouades");
                if (comment.equals("Multiplier Priority Order:")) return "Ordre de priorité multiplicateur :";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Ordre de priorité multiplicateur :");
                if (comment.equals("Only these mobs can spawn in desert")) return "Seuls ces monstres peuvent apparaître dans le désert";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Seuls ces monstres peuvent apparaître dans le désert");
                if (comment.equals("Per-biome difficulty rules")) return "Règles de difficulté par biome";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Règles de difficulté par biome");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Plus lent, mais tire des flèches de lenteur";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Plus lent, mais tire des flèches de lenteur");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Cible : Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Cible : Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Ces monstres NE PEUVENT PAS apparaître dans le désert";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Ces monstres NE PEUVENT PAS apparaître dans le désert");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Ces multiplicateurs s'appliquent à TOUS les monstres, quel que soit le biome, la distance ou la zone. 1,0 = neutre.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Ces multiplicateurs s'appliquent à TOUS les monstres, quel que soit le biome, la distance ou la zone. 1,0 = neutre.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Ce fichier est AUTOMATIQUEMENT géré par le plugin.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Ce fichier est AUTOMATIQUEMENT géré par le plugin.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Utilisez les commandes de zone /wd pour créer et modifier des zones.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Utilisez les commandes de zone /wd pour créer et modifier des zones.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildDifficulty — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildDifficulty — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty — config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty — config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficulty — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficulty — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildDifficulty — zones.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildDifficulty — zones.yml");
                if (comment.equals("ZOMBIE:")) return "ZOMBI:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "ZOMBI:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Les zombies apparaissent pendant la journée et ne brûlent pas";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Les zombies apparaissent pendant la journée et ne brûlent pas");
                if (comment.equals("Zone Format (for reference):")) return "Format de zone (pour référence) :";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Format de zone (pour référence) :");
                if (comment.equals("chance-equipement: 0.0")) return "équipement de chance : 0,0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "équipement de chance : 0,0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "équipement de chance : 0,0 # 0,0 à 1,0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "équipement de chance : 0,0 # 0,0 à 1,0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignore-soleil : false # true = ne brûle pas au soleil";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignore-soleil : false # true = ne brûle pas au soleil");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorer-regles-biome : false # true = ignore les modificateurs de biome";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorer-regles-biome : false # true = ignore les modificateurs de biome");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises : # (facultatif) Seuls ces monstres peuvent apparaître dans ce biome";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises : # (facultatif) Seuls ces monstres peuvent apparaître dans ce biome");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises : [] # Liste blanche des mobs (vide = tous autorisés)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises : [] # Liste blanche des mobs (vide = tous autorisés)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits : # (facultatif) Ces monstres NE PEUVENT PAS apparaître dans ce biome";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits : # (facultatif) Ces monstres NE PEUVENT PAS apparaître dans ce biome");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Liste noire des mobs";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Liste noire des mobs");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs : # (facultatif) Remplacements spécifiques aux foules dans ce biome";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs : # (facultatif) Remplacements spécifiques aux foules dans ce biome");
                if (comment.equals("modificateurs:")) return "modificateurs :";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "modificateurs :");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificateurs : # (facultatif) Multiplicateurs appliqués aux mobs apparaissant ici";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificateurs : # (facultatif) Multiplicateurs appliqués aux mobs apparaissant ici");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificateurs : # (facultatif) Remplacements spécifiques pour ce type de mob";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificateurs : # (facultatif) Remplacements spécifiques pour ce type de mob");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"monde\" # Nom du monde";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"monde\" # Nom du monde");
                if (comment.equals("multiplicateur-degats: 1.0")) return "multiplicateur-dégâts : 1.0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "multiplicateur-dégâts : 1.0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "multiplicateur-dégâts : 1.2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "multiplicateur-dégâts : 1.2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "multiplicateur-portee-detection : 1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "multiplicateur-portee-detection : 1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "multiplicateur-pv : 1.0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "multiplicateur-pv : 1.0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # Appliqué EN PLUS aux modificateurs de biome/distance/zone";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # Appliqué EN PLUS aux modificateurs de biome/distance/zone");
                if (comment.equals("multiplicateur-pv: 1.5")) return "multiplicateur-pv : 1,5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "multiplicateur-pv : 1,5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "multiplicateur-résistance-recul : 1,0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "multiplicateur-résistance-recul : 1,0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "multiplicateur-vitesse : 1.0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "multiplicateur-vitesse : 1.0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = peut apparaître à la lumière du jour";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = peut apparaître à la lumière du jour");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "priorité : 0 # Priorité (supérieure = priorité sur chevauchement)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "priorité : 0 # Priorité (supérieure = priorité sur chevauchement)");
                if (comment.equals("resistance-feu: false")) return "résistance-feu : faux";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "résistance-feu : faux");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "résistance-feu : faux # vrai = immunité au feu (effet potion permanent)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "résistance-feu : faux # vrai = immunité au feu (effet potion permanent)");
                if (comment.equals("tier-equipement: \"none\"")) return "équipement de niveau : \"aucun\"";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "équipement de niveau : \"aucun\"");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "tier-equipement : \"aucun\" # aucun | cuir | fer | diamant | néthérite";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "tier-equipement : \"aucun\" # aucun | cuir | fer | diamant | néthérite");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "tier-equipement : \"aucun\" # aucun | cuir | fer | diamant | néthérite";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "tier-equipement : \"aucun\" # aucun | cuir | fer | diamant | néthérite");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "tapez : CUBOÏDE # CUBOÏDE ou RAYON";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "tapez : CUBOÏDE # CUBOÏDE ou RAYON");
                if (comment.equals("zone-name:")) return "nom de zone :";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "nom de zone :");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "zone-sure : false # true = désactive les apparitions hostiles dans la zone";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "zone-sure : false # true = désactive les apparitions hostiles dans la zone");
            }
            case "de" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(Standardmäßig sind keine Zonen definiert – verwenden Sie /wd zone create, um eine zu erstellen.)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(Standardmäßig sind keine Zonen definiert – verwenden Sie /wd zone create, um eine zu erstellen.)");
                if (comment.equals("- SPIDER")) return "- SPINNE";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- SPINNE");
                if (comment.equals("- ZOMBIE")) return "- ZOMBIE";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ZOMBIE");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Key = Bukkit Biome Enum-Name (z. B. DESERT, JUNGLE...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Key = Bukkit Biome Enum-Name (z. B. DESERT, JUNGLE...)");
                if (comment.equals("Base Global Modifiers")) return "Globale Basismodifikatoren";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Globale Basismodifikatoren");
                if (comment.equals("Cap of variants per player")) return "Obergrenze der Varianten pro Spieler";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Obergrenze der Varianten pro Spieler");
                if (comment.equals("Custom in-game difficulty zones")) return "Benutzerdefinierte Schwierigkeitszonen im Spiel";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Benutzerdefinierte Schwierigkeitszonen im Spiel");
                if (comment.equals("Desert mobs are fire resistant")) return "Wüstenmobs sind feuerbeständig";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "Wüstenmobs sind feuerbeständig");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "Je weiter sich der Spieler vom zentralen Ausgangspunkt entfernt, desto schwieriger wird es.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "Je weiter sich der Spieler vom zentralen Ausgangspunkt entfernt, desto schwieriger wird es.");
                if (comment.equals("Distance Scaling")) return "Entfernungsskalierung";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Entfernungsskalierung");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "Bearbeiten Sie sie nicht manuell, es sei denn, dies ist unbedingt erforderlich.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "Bearbeiten Sie sie nicht manuell, es sei denn, dies ist unbedingt erforderlich.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Debug-Modus aktivieren (zusätzliche Protokolle in der Konsole – in der Produktion deaktivieren)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Debug-Modus aktivieren (zusätzliche Protokolle in der Konsole – in der Produktion deaktivieren)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Sprachcode aktivieren (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Sprachcode aktivieren (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "Archivierung der Parameter des Personals des Kindes";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "Archivierung der Parameter des Personals des Kindes");
                if (comment.equals("General Biome Format:")) return "Allgemeines Biomformat:";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Allgemeines Biomformat:");
                if (comment.equals("General Format:")) return "Allgemeines Format:";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Allgemeines Format:");
                if (comment.equals("General Plugin Settings")) return "Allgemeine Plugin-Einstellungen";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Allgemeine Plugin-Einstellungen");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Global × Biom × Entfernung × Zone × Mob-Override = Endgültig";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Global × Biom × Entfernung × Zone × Mob-Override = Endgültig");
                if (comment.equals("Husks have a bonus HP multiplier")) return "Hüllen haben einen Bonus-HP-Multiplikator";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "Hüllen haben einen Bonus-HP-Multiplikator");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Individuelle Konfiguration pro Vanilla-Mob-Typ";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Individuelle Konfiguration pro Vanilla-Mob-Typ");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Key = Bukkit EntityType Enum-Name (z. B. ZOMBIE, SKELETON ...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Key = Bukkit EntityType Enum-Name (z. B. ZOMBIE, SKELETON ...)");
                if (comment.equals("Main plugin configuration file")) return "Haupt-Plugin-Konfigurationsdatei";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "Haupt-Plugin-Konfigurationsdatei");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Maximale Spawn-Entfernung (in Blöcken) vom Spieler, über die hinaus keine natürlichen Mobs erscheinen (-1 zum Deaktivieren)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Maximale Spawn-Entfernung (in Blöcken) vom Spieler, über die hinaus keine natürlichen Mobs erscheinen (-1 zum Deaktivieren)");
                if (comment.equals("Monster variants and squads configuration")) return "Monstervarianten und Truppkonfiguration";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Monstervarianten und Truppkonfiguration");
                if (comment.equals("Multiplier Priority Order:")) return "Prioritätsreihenfolge der Multiplikatoren:";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Prioritätsreihenfolge der Multiplikatoren:");
                if (comment.equals("Only these mobs can spawn in desert")) return "Nur diese Mobs können in der Wüste spawnen";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Nur diese Mobs können in der Wüste spawnen");
                if (comment.equals("Per-biome difficulty rules")) return "Schwierigkeitsregeln pro Biom";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Schwierigkeitsregeln pro Biom");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Langsamer, schießt aber Langsamkeitspfeile";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Langsamer, schießt aber Langsamkeitspfeile");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Ziel: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Ziel: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Diese Mobs KÖNNEN NICHT in der Wüste spawnen";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Diese Mobs KÖNNEN NICHT in der Wüste spawnen");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Diese Multiplikatoren gelten für ALLE Mobs, unabhängig von Biom, Entfernung oder Zone. 1,0 = neutral.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Diese Multiplikatoren gelten für ALLE Mobs, unabhängig von Biom, Entfernung oder Zone. 1,0 = neutral.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Diese Datei wird AUTOMATISCH vom Plugin verwaltet.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Diese Datei wird AUTOMATISCH vom Plugin verwaltet.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Verwenden Sie die Zonenbefehle /wd, um Zonen zu erstellen und zu bearbeiten.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Verwenden Sie die Zonenbefehle /wd, um Zonen zu erstellen und zu bearbeiten.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildDifficulty – biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildDifficulty – biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty – config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty – config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty – mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty – mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficulty – mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficulty – mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildDifficulty – zones.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildDifficulty – zones.yml");
                if (comment.equals("ZOMBIE:")) return "ZOMBIE:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "ZOMBIE:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Zombies spawnen bei Tageslicht und brennen nicht";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Zombies spawnen bei Tageslicht und brennen nicht");
                if (comment.equals("Zone Format (for reference):")) return "Zonenformat (als Referenz):";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Zonenformat (als Referenz):");
                if (comment.equals("chance-equipement: 0.0")) return "Zufallsausrüstung: 0.0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "Zufallsausrüstung: 0.0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "Zufallsausrüstung: 0,0 # 0,0 bis 1,0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "Zufallsausrüstung: 0,0 # 0,0 bis 1,0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignore-soleil: false # true = brennt nicht im Sonnenlicht";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignore-soleil: false # true = brennt nicht im Sonnenlicht");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorer-regles-biome: false # true = ignoriert Biommodifikatoren";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorer-regles-biome: false # true = ignoriert Biommodifikatoren");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (optional) Nur diese Mobs können in diesem Biom spawnen";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (optional) Nur diese Mobs können in diesem Biom spawnen");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # Whitelist der Mobs (leer = alle erlaubt)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # Whitelist der Mobs (leer = alle erlaubt)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (optional) Diese Mobs KÖNNEN NICHT in diesem Biom spawnen";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (optional) Diese Mobs KÖNNEN NICHT in diesem Biom spawnen");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Schwarze Liste der Mobs";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Schwarze Liste der Mobs");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs: # (optional) Mob-spezifische Überschreibungen in diesem Biom";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs: # (optional) Mob-spezifische Überschreibungen in diesem Biom");
                if (comment.equals("modificateurs:")) return "Modifikatoren:";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "Modifikatoren:");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificateurs: # (optional) Multiplikatoren, die auf hier spawnende Mobs angewendet werden";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificateurs: # (optional) Multiplikatoren, die auf hier spawnende Mobs angewendet werden");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificateurs: # (optional) Spezifische Überschreibungen für diesen Mob-Typ";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificateurs: # (optional) Spezifische Überschreibungen für diesen Mob-Typ");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"world\" # Weltname";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"world\" # Weltname");
                if (comment.equals("multiplicateur-degats: 1.0")) return "Multiplikator-Degats: 1,0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "Multiplikator-Degats: 1,0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "Multiplikator-Degats: 1.2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "Multiplikator-Degats: 1.2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "Multiplikator-Porte-Erkennung: 1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "Multiplikator-Porte-Erkennung: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "Multiplikator-PV: 1,0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "Multiplikator-PV: 1,0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # Wird ZUSÄTZLICH auf Biom-/Entfernungs-/Zonenmodifikatoren angewendet";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # Wird ZUSÄTZLICH auf Biom-/Entfernungs-/Zonenmodifikatoren angewendet");
                if (comment.equals("multiplicateur-pv: 1.5")) return "Multiplikator-PV: 1,5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "Multiplikator-PV: 1,5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "Multiplikator-Widerstand-Rückstoß: 1,0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "Multiplikator-Widerstand-Rückstoß: 1,0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "Multiplikator-Vitesse: 1.0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "Multiplikator-Vitesse: 1.0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = kann bei Tageslicht spawnen";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = kann bei Tageslicht spawnen");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "priorite: 0 # Priorität (höher = Priorität bei Überlappung)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "priorite: 0 # Priorität (höher = Priorität bei Überlappung)");
                if (comment.equals("resistance-feu: false")) return "Widerstandsfeu: falsch";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "Widerstandsfeu: falsch");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "Resistance-Feu: false # true = Feuerimmunität (permanente Trankwirkung)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "Resistance-Feu: false # true = Feuerimmunität (permanente Trankwirkung)");
                if (comment.equals("tier-equipement: \"none\"")) return "Tier-Ausrüstung: „keine“";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "Tier-Ausrüstung: „keine“");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "tier-equipement: \"none\" # none | Leder | Eisen | Diamant | Netherit";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "tier-equipement: \"none\" # none | Leder | Eisen | Diamant | Netherit");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "tier-equipement: \"none\" # none | Leder | Eisen | Diamant | Netherit";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "tier-equipement: \"none\" # none | Leder | Eisen | Diamant | Netherit");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "Typ: CUBOID # CUBOID oder RADIUS";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "Typ: CUBOID # CUBOID oder RADIUS");
                if (comment.equals("zone-name:")) return "Zonenname:";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "Zonenname:");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "zone-sure: false # true = deaktiviert feindliche Spawns in der Zone";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "zone-sure: false # true = deaktiviert feindliche Spawns in der Zone");
            }
            case "es" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(No hay zonas definidas de forma predeterminada; use /wd zona crear para crear una)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(No hay zonas definidas de forma predeterminada; use /wd zona crear para crear una)");
                if (comment.equals("- SPIDER")) return "- ARAÑA";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- ARAÑA");
                if (comment.equals("- ZOMBIE")) return "- ZOMBI";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ZOMBI");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Clave = Nombre de enumeración del bioma de Bukkit (por ejemplo, DESIERTO, SELVA...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Clave = Nombre de enumeración del bioma de Bukkit (por ejemplo, DESIERTO, SELVA...)");
                if (comment.equals("Base Global Modifiers")) return "Modificadores globales base";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Modificadores globales base");
                if (comment.equals("Cap of variants per player")) return "Límite de variantes por jugador";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Límite de variantes por jugador");
                if (comment.equals("Custom in-game difficulty zones")) return "Zonas de dificultad personalizadas en el juego";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Zonas de dificultad personalizadas en el juego");
                if (comment.equals("Desert mobs are fire resistant")) return "Las turbas del desierto son resistentes al fuego.";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "Las turbas del desierto son resistentes al fuego.");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "La dificultad aumenta a medida que los jugadores se alejan del punto de origen central.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "La dificultad aumenta a medida que los jugadores se alejan del punto de origen central.");
                if (comment.equals("Distance Scaling")) return "Escala de distancia";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Escala de distancia");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "No edite manualmente a menos que sea absolutamente necesario.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "No edite manualmente a menos que sea absolutamente necesario.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Habilite el modo de depuración (registros adicionales en la consola; deshabilite en producción)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Habilite el modo de depuración (registros adicionales en la consola; deshabilite en producción)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Habilitar código de idioma (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Habilitar código de idioma (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "Fichier de stockage des paramètres personals des joueurs";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "Fichier de stockage des paramètres personals des joueurs");
                if (comment.equals("General Biome Format:")) return "Formato general del bioma:";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Formato general del bioma:");
                if (comment.equals("General Format:")) return "Formato general:";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Formato general:");
                if (comment.equals("General Plugin Settings")) return "Configuración general del complemento";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Configuración general del complemento");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Global × Bioma × Distancia × Zona × Anulación de Mob = Final";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Global × Bioma × Distancia × Zona × Anulación de Mob = Final");
                if (comment.equals("Husks have a bonus HP multiplier")) return "Las cáscaras tienen un multiplicador de HP adicional";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "Las cáscaras tienen un multiplicador de HP adicional");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Configuración individual por tipo de mob básico";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Configuración individual por tipo de mob básico");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Clave = nombre de enumeración Bukkit EntityType (por ejemplo, ZOMBIE, SKELETON...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Clave = nombre de enumeración Bukkit EntityType (por ejemplo, ZOMBIE, SKELETON...)");
                if (comment.equals("Main plugin configuration file")) return "Archivo de configuración del complemento principal";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "Archivo de configuración del complemento principal");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Distancia máxima de generación (en bloques) desde el jugador más allá de la cual los mobs naturales no aparecen (-1 para desactivar)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Distancia máxima de generación (en bloques) desde el jugador más allá de la cual los mobs naturales no aparecen (-1 para desactivar)");
                if (comment.equals("Monster variants and squads configuration")) return "Variantes de monstruos y configuración de escuadrones.";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Variantes de monstruos y configuración de escuadrones.");
                if (comment.equals("Multiplier Priority Order:")) return "Orden de prioridad del multiplicador:";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Orden de prioridad del multiplicador:");
                if (comment.equals("Only these mobs can spawn in desert")) return "Sólo estos mobs pueden aparecer en el desierto.";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Sólo estos mobs pueden aparecer en el desierto.");
                if (comment.equals("Per-biome difficulty rules")) return "Reglas de dificultad por bioma";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Reglas de dificultad por bioma");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Más lento, pero dispara flechas de lentitud.";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Más lento, pero dispara flechas de lentitud.");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Objetivo: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Objetivo: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Estos mobs NO PUEDEN aparecer en el desierto.";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Estos mobs NO PUEDEN aparecer en el desierto.");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Estos multiplicadores se aplican a TODOS los mobs independientemente del bioma, la distancia o la zona. 1,0 = neutro.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Estos multiplicadores se aplican a TODOS los mobs independientemente del bioma, la distancia o la zona. 1,0 = neutro.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Este archivo es administrado AUTOMÁTICAMENTE por el complemento.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Este archivo es administrado AUTOMÁTICAMENTE por el complemento.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Utilice los comandos de zona /wd para crear y editar zonas.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Utilice los comandos de zona /wd para crear y editar zonas.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "Dificultad salvaje — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "Dificultad salvaje — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "Dificultad salvaje: config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "Dificultad salvaje: config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "Dificultad salvaje — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "Dificultad salvaje — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "Dificultad salvaje — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "Dificultad salvaje — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "Dificultad salvaje — zonas.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "Dificultad salvaje — zonas.yml");
                if (comment.equals("ZOMBIE:")) return "ZOMBI:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "ZOMBI:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Los zombis aparecen durante el día y no se queman.";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Los zombis aparecen durante el día y no se queman.");
                if (comment.equals("Zone Format (for reference):")) return "Formato de zona (como referencia):";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Formato de zona (como referencia):");
                if (comment.equals("chance-equipement: 0.0")) return "equipo de oportunidad: 0.0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "equipo de oportunidad: 0.0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "equipo-oportunidad: 0.0 # 0.0 a 1.0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "equipo-oportunidad: 0.0 # 0.0 a 1.0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignore-soleil: false # true = no arde con la luz del sol";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignore-soleil: false # true = no arde con la luz del sol");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorer-regles-biome: false # true = ignora los modificadores del bioma";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorer-regles-biome: false # true = ignora los modificadores del bioma");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (opcional) Solo estos mobs pueden aparecer en este bioma";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (opcional) Solo estos mobs pueden aparecer en este bioma");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # Lista blanca de mobs (vacía = todos permitidos)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # Lista blanca de mobs (vacía = todos permitidos)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (opcional) Estos mobs NO PUEDEN aparecer en este bioma";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (opcional) Estos mobs NO PUEDEN aparecer en este bioma");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Lista negra de mobs";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Lista negra de mobs");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs: # (opcional) Anulaciones específicas de mobs en este bioma";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs: # (opcional) Anulaciones específicas de mobs en este bioma");
                if (comment.equals("modificateurs:")) return "modificadores:";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "modificadores:");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificateurs: # (opcional) Multiplicadores aplicados a los mobs que aparecen aquí";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificateurs: # (opcional) Multiplicadores aplicados a los mobs que aparecen aquí");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificateurs: # (opcional) Anulaciones específicas para este tipo de mob";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificateurs: # (opcional) Anulaciones específicas para este tipo de mob");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"mundo\" # Nombre del mundo";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"mundo\" # Nombre del mundo");
                if (comment.equals("multiplicateur-degats: 1.0")) return "multiplicador-degats: 1.0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "multiplicador-degats: 1.0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "multiplicador-degats: 1.2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "multiplicador-degats: 1.2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "detección-de-puerta-multiplicador: 1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "detección-de-puerta-multiplicador: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "multiplicador-pv: 1.0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "multiplicador-pv: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # Aplicado ADEMÁS a modificadores de bioma/distancia/zona";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # Aplicado ADEMÁS a modificadores de bioma/distancia/zona");
                if (comment.equals("multiplicateur-pv: 1.5")) return "multiplicador-pv: 1,5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "multiplicador-pv: 1,5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "multiplicador-resistencia-retroceso: 1.0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "multiplicador-resistencia-retroceso: 1.0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "multiplicador-vitesse: 1.0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "multiplicador-vitesse: 1.0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = puede aparecer durante el día";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = puede aparecer durante el día");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "prioridad: 0 # Prioridad (mayor = prioridad en superposición)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "prioridad: 0 # Prioridad (mayor = prioridad en superposición)");
                if (comment.equals("resistance-feu: false")) return "resistencia-fuego: falso";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "resistencia-fuego: falso");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "Resistance-feu: false # true = inmunidad al fuego (efecto de poción permanente)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "Resistance-feu: false # true = inmunidad al fuego (efecto de poción permanente)");
                if (comment.equals("tier-equipement: \"none\"")) return "equipo de nivel: \"ninguno\"";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "equipo de nivel: \"ninguno\"");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "equipo de nivel: \"ninguno\" # ninguno | cuero | hierro | diamante | netherita";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "equipo de nivel: \"ninguno\" # ninguno | cuero | hierro | diamante | netherita");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "equipo de nivel: \"ninguno\" # ninguno | cuero | hierro | diamante | netherita";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "equipo de nivel: \"ninguno\" # ninguno | cuero | hierro | diamante | netherita");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "tipo: CUBOIDE # CUBOIDE o RADIO";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "tipo: CUBOIDE # CUBOIDE o RADIO");
                if (comment.equals("zone-name:")) return "nombre de zona:";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "nombre de zona:");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "zona-segura: falso # verdadero = desactiva la generación hostil en la zona";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "zona-segura: falso # verdadero = desactiva la generación hostil en la zona");
            }
            case "pt_br" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(Nenhuma zona definida por padrão – use /wd zone create para criar uma)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(Nenhuma zona definida por padrão – use /wd zone create para criar uma)");
                if (comment.equals("- SPIDER")) return "- ARANHA";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- ARANHA");
                if (comment.equals("- ZOMBIE")) return "- ZUMBI";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ZUMBI");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Key = nome da enumeração do bioma Bukkit (por exemplo, DESERTO, SELVA...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Key = nome da enumeração do bioma Bukkit (por exemplo, DESERTO, SELVA...)");
                if (comment.equals("Base Global Modifiers")) return "Modificadores globais básicos";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Modificadores globais básicos");
                if (comment.equals("Cap of variants per player")) return "Limite de variantes por jogador";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Limite de variantes por jogador");
                if (comment.equals("Custom in-game difficulty zones")) return "Zonas de dificuldade personalizadas no jogo";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Zonas de dificuldade personalizadas no jogo");
                if (comment.equals("Desert mobs are fire resistant")) return "Mobs do deserto são resistentes ao fogo";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "Mobs do deserto são resistentes ao fogo");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "A dificuldade aumenta à medida que os jogadores se afastam do ponto central de origem.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "A dificuldade aumenta à medida que os jogadores se afastam do ponto central de origem.");
                if (comment.equals("Distance Scaling")) return "Escala de distância";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Escala de distância");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "Não edite manualmente, a menos que seja absolutamente necessário.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "Não edite manualmente, a menos que seja absolutamente necessário.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Habilite o modo de depuração (logs extras no console – desabilite na produção)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Habilite o modo de depuração (logs extras no console – desabilite na produção)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Habilitar código de idioma (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Habilitar código de idioma (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "Arquivo de armazenamento de parâmetros pessoais de jogadores";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "Arquivo de armazenamento de parâmetros pessoais de jogadores");
                if (comment.equals("General Biome Format:")) return "Formato Geral do Bioma:";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Formato Geral do Bioma:");
                if (comment.equals("General Format:")) return "Formato Geral:";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Formato Geral:");
                if (comment.equals("General Plugin Settings")) return "Configurações gerais do plug-in";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Configurações gerais do plug-in");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Global × Bioma × Distância × Zona × Substituição de Mob = Final";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Global × Bioma × Distância × Zona × Substituição de Mob = Final");
                if (comment.equals("Husks have a bonus HP multiplier")) return "As cascas têm um multiplicador de HP bônus";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "As cascas têm um multiplicador de HP bônus");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Configuração individual por tipo de mob vanilla";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Configuração individual por tipo de mob vanilla");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Key = nome da enumeração Bukkit EntityType (por exemplo, ZOMBIE, SKELETON...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Key = nome da enumeração Bukkit EntityType (por exemplo, ZOMBIE, SKELETON...)");
                if (comment.equals("Main plugin configuration file")) return "Arquivo de configuração do plugin principal";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "Arquivo de configuração do plugin principal");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Distância máxima de spawn (em blocos) do jogador além da qual mobs naturais não aparecem (-1 para desabilitar)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Distância máxima de spawn (em blocos) do jogador além da qual mobs naturais não aparecem (-1 para desabilitar)");
                if (comment.equals("Monster variants and squads configuration")) return "Variantes de monstros e configuração de esquadrões";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Variantes de monstros e configuração de esquadrões");
                if (comment.equals("Multiplier Priority Order:")) return "Ordem de prioridade do multiplicador:";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Ordem de prioridade do multiplicador:");
                if (comment.equals("Only these mobs can spawn in desert")) return "Somente esses mobs podem aparecer no deserto";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Somente esses mobs podem aparecer no deserto");
                if (comment.equals("Per-biome difficulty rules")) return "Regras de dificuldade por bioma";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Regras de dificuldade por bioma");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Mais lento, mas atira flechas de lentidão";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Mais lento, mas atira flechas de lentidão");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Alvo: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Alvo: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Esses mobs NÃO PODEM aparecer no deserto";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Esses mobs NÃO PODEM aparecer no deserto");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Esses multiplicadores se aplicam a TODOS os mobs, independentemente do bioma, distância ou zona. 1,0 = neutro.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Esses multiplicadores se aplicam a TODOS os mobs, independentemente do bioma, distância ou zona. 1,0 = neutro.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Este arquivo é gerenciado AUTOMATICAMENTE pelo plugin.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Este arquivo é gerenciado AUTOMATICAMENTE pelo plugin.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Use os comandos /wd zone para criar e editar zonas.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Use os comandos /wd zone para criar e editar zonas.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildDifficulty — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildDifficulty — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty - config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty - config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficulty — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficulty — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildDifficulty — zonas.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildDifficulty — zonas.yml");
                if (comment.equals("ZOMBIE:")) return "ZUMBI:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "ZUMBI:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Zumbis aparecem durante o dia e não queimam";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Zumbis aparecem durante o dia e não queimam");
                if (comment.equals("Zone Format (for reference):")) return "Formato da zona (para referência):";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Formato da zona (para referência):");
                if (comment.equals("chance-equipement: 0.0")) return "equipamento de chance: 0,0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "equipamento de chance: 0,0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "equipamento de chance: 0,0 # 0,0 a 1,0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "equipamento de chance: 0,0 # 0,0 a 1,0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignore-soleil: false # true = não queima na luz solar";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignore-soleil: false # true = não queima na luz solar");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorer-regles-biome: false # true = ignora modificadores de bioma";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorer-regles-biome: false # true = ignora modificadores de bioma");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (opcional) Somente esses mobs podem aparecer neste bioma";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (opcional) Somente esses mobs podem aparecer neste bioma");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # Whitelist de mobs (vazio = todos permitidos)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # Whitelist de mobs (vazio = todos permitidos)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (opcional) Esses mobs NÃO PODEM aparecer neste bioma";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (opcional) Esses mobs NÃO PODEM aparecer neste bioma");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Lista negra de mobs";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Lista negra de mobs");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs: # (opcional) substituições específicas de mobs neste bioma";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs: # (opcional) substituições específicas de mobs neste bioma");
                if (comment.equals("modificateurs:")) return "modificadores:";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "modificadores:");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificadores: # (opcional) Multiplicadores aplicados aos mobs que aparecem aqui";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificadores: # (opcional) Multiplicadores aplicados aos mobs que aparecem aqui");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificadores: # (opcional) Substituições específicas para este tipo de mob";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificadores: # (opcional) Substituições específicas para este tipo de mob");
                if (comment.equals("monde: \"world\"                    # World name")) return "mundo: \"mundo\" # Nome do mundo";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "mundo: \"mundo\" # Nome do mundo");
                if (comment.equals("multiplicateur-degats: 1.0")) return "multiplicador-degats: 1,0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "multiplicador-degats: 1,0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "multiplicador-degats: 1,2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "multiplicador-degats: 1,2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "detecção de multiplicador-portee: 1,0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "detecção de multiplicador-portee: 1,0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "multiplicador-pv: 1,0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "multiplicador-pv: 1,0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # Aplicado ALÉM dos modificadores de bioma/distância/zona";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # Aplicado ALÉM dos modificadores de bioma/distância/zona");
                if (comment.equals("multiplicateur-pv: 1.5")) return "multiplicador-pv: 1,5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "multiplicador-pv: 1,5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "multiplicador-resistência-knockback: 1,0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "multiplicador-resistência-knockback: 1,0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "multiplicador-vitesse: 1,0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "multiplicador-vitesse: 1,0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = pode aparecer durante o dia";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = pode aparecer durante o dia");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "priorite: 0 # Prioridade (maior = prioridade na sobreposição)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "priorite: 0 # Prioridade (maior = prioridade na sobreposição)");
                if (comment.equals("resistance-feu: false")) return "resistência-feu: falso";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "resistência-feu: falso");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "Resistance-feu: false # true = imunidade ao fogo (efeito de poção permanente)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "Resistance-feu: false # true = imunidade ao fogo (efeito de poção permanente)");
                if (comment.equals("tier-equipement: \"none\"")) return "equipamento de nível: \"nenhum\"";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "equipamento de nível: \"nenhum\"");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "equipamento de camada: \"nenhum\" # nenhum | couro | ferro | diamante | netherita";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "equipamento de camada: \"nenhum\" # nenhum | couro | ferro | diamante | netherita");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "equipamento de camada: \"nenhum\" # nenhum | couro | ferro | diamante | netherita";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "equipamento de camada: \"nenhum\" # nenhum | couro | ferro | diamante | netherita");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "tipo: CUBOID # CUBOID ou RADIUS";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "tipo: CUBOID # CUBOID ou RADIUS");
                if (comment.equals("zone-name:")) return "nome da zona:";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "nome da zona:");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "zone-sure: false # true = desativa spawns hostis na zona";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "zone-sure: false # true = desativa spawns hostis na zona");
            }
            case "nl" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(Er zijn standaard geen zones gedefinieerd - gebruik /wd zone create om er één te maken)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(Er zijn standaard geen zones gedefinieerd - gebruik /wd zone create om er één te maken)");
                if (comment.equals("- SPIDER")) return "- SPIN";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- SPIN");
                if (comment.equals("- ZOMBIE")) return "- ZOMBIE";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ZOMBIE");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Sleutel = Bukkit Biome-enumnaam (bijvoorbeeld DESERT, JUNGLE...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Sleutel = Bukkit Biome-enumnaam (bijvoorbeeld DESERT, JUNGLE...)");
                if (comment.equals("Base Global Modifiers")) return "Basis globale modificatoren";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Basis globale modificatoren");
                if (comment.equals("Cap of variants per player")) return "Maximaal aantal varianten per speler";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Maximaal aantal varianten per speler");
                if (comment.equals("Custom in-game difficulty zones")) return "Aangepaste moeilijkheidszones in de game";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Aangepaste moeilijkheidszones in de game");
                if (comment.equals("Desert mobs are fire resistant")) return "Woestijnmobs zijn brandwerend";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "Woestijnmobs zijn brandwerend");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "De moeilijkheidsgraad neemt toe naarmate spelers verder van het centrale oorsprongspunt komen.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "De moeilijkheidsgraad neemt toe naarmate spelers verder van het centrale oorsprongspunt komen.");
                if (comment.equals("Distance Scaling")) return "Afstandsschalen";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Afstandsschalen");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "Bewerk niet handmatig, tenzij dit absoluut noodzakelijk is.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "Bewerk niet handmatig, tenzij dit absoluut noodzakelijk is.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Schakel debug-modus in (extra logs in console - uitschakelen in productie)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Schakel debug-modus in (extra logs in console - uitschakelen in productie)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Taalcode inschakelen (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Taalcode inschakelen (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "Documentatie van de opslag van personeelsparameters van de joueurs";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "Documentatie van de opslag van personeelsparameters van de joueurs");
                if (comment.equals("General Biome Format:")) return "Algemeen bioomformaat:";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Algemeen bioomformaat:");
                if (comment.equals("General Format:")) return "Algemeen formaat:";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Algemeen formaat:");
                if (comment.equals("General Plugin Settings")) return "Algemene plug-ininstellingen";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Algemene plug-ininstellingen");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Globaal × Bioom × Afstand × Zone × Mob-Override = Final";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Globaal × Bioom × Afstand × Zone × Mob-Override = Final");
                if (comment.equals("Husks have a bonus HP multiplier")) return "Schillen hebben een bonus-HP-vermenigvuldiger";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "Schillen hebben een bonus-HP-vermenigvuldiger");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Individuele configuratie per vanilla mob-type";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Individuele configuratie per vanilla mob-type");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Sleutel = Bukkit EntityType enumnaam (bijv. ZOMBIE, SKELETON...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Sleutel = Bukkit EntityType enumnaam (bijv. ZOMBIE, SKELETON...)");
                if (comment.equals("Main plugin configuration file")) return "Configuratiebestand van de hoofdplug-in";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "Configuratiebestand van de hoofdplug-in");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Maximale spawnafstand (in blokken) vanaf de speler waarboven natuurlijke mobs niet spawnen (-1 om uit te schakelen)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Maximale spawnafstand (in blokken) vanaf de speler waarboven natuurlijke mobs niet spawnen (-1 om uit te schakelen)");
                if (comment.equals("Monster variants and squads configuration")) return "Monstervarianten en squadronconfiguratie";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Monstervarianten en squadronconfiguratie");
                if (comment.equals("Multiplier Priority Order:")) return "Prioriteitsvolgorde vermenigvuldiger:";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Prioriteitsvolgorde vermenigvuldiger:");
                if (comment.equals("Only these mobs can spawn in desert")) return "Alleen deze mobs kunnen in de woestijn spawnen";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Alleen deze mobs kunnen in de woestijn spawnen");
                if (comment.equals("Per-biome difficulty rules")) return "Moeilijkheidsregels per bioom";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Moeilijkheidsregels per bioom");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Langzamer, maar schiet langzaamheidspijlen";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Langzamer, maar schiet langzaamheidspijlen");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Doel: Purpur 1.21.4 / Papier-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Doel: Purpur 1.21.4 / Papier-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Deze mobs KUNNEN NIET spawnen in de woestijn";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Deze mobs KUNNEN NIET spawnen in de woestijn");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Deze vermenigvuldigers zijn van toepassing op ALLE mobs, ongeacht hun bioom, afstand of zone. 1,0 = neutraal.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Deze vermenigvuldigers zijn van toepassing op ALLE mobs, ongeacht hun bioom, afstand of zone. 1,0 = neutraal.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Dit bestand wordt AUTOMATISCH beheerd door de plug-in.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Dit bestand wordt AUTOMATISCH beheerd door de plug-in.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Gebruik /wd zone-opdrachten om zones te maken en te bewerken.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Gebruik /wd zone-opdrachten om zones te maken en te bewerken.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildMoeilijkheid — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildMoeilijkheid — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty — config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty — config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficulty — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficulty — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildMoeilijkheid — zones.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildMoeilijkheid — zones.yml");
                if (comment.equals("ZOMBIE:")) return "ZOMBIE:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "ZOMBIE:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Zombies spawnen overdag en verbranden niet";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Zombies spawnen overdag en verbranden niet");
                if (comment.equals("Zone Format (for reference):")) return "Zoneformaat (ter referentie):";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Zoneformaat (ter referentie):");
                if (comment.equals("chance-equipement: 0.0")) return "kansuitrusting: 0,0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "kansuitrusting: 0,0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "kans-uitrusting: 0,0 # 0,0 tot 1,0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "kans-uitrusting: 0,0 # 0,0 tot 1,0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "negeer-soleil: false # true = brandt niet in zonlicht";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "negeer-soleil: false # true = brandt niet in zonlicht");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "neger-regles-biome: false # true = negeert biome-modificatoren";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "neger-regles-biome: false # true = negeert biome-modificatoren");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (optioneel) Alleen deze mobs kunnen in dit bioom spawnen";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (optioneel) Alleen deze mobs kunnen in dit bioom spawnen");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # Witte lijst met mobs (leeg = allemaal toegestaan)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # Witte lijst met mobs (leeg = allemaal toegestaan)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (optioneel) Deze mobs KUNNEN NIET spawnen in dit bioom";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (optioneel) Deze mobs KUNNEN NIET spawnen in dit bioom");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Zwarte lijst met mobs";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Zwarte lijst met mobs");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs: # (optioneel) Mob-specifieke overschrijvingen in dit bioom";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs: # (optioneel) Mob-specifieke overschrijvingen in dit bioom");
                if (comment.equals("modificateurs:")) return "modificaties:";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "modificaties:");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificateurs: # (optioneel) Multipliers toegepast op mobs die hier spawnen";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificateurs: # (optioneel) Multipliers toegepast op mobs die hier spawnen");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificateurs: # (optioneel) Specifieke overschrijvingen voor dit mob-type";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificateurs: # (optioneel) Specifieke overschrijvingen voor dit mob-type");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"wereld\" # Wereldnaam";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"wereld\" # Wereldnaam");
                if (comment.equals("multiplicateur-degats: 1.0")) return "multiplicateur-degats: 1.0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "multiplicateur-degats: 1.0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "multiplicateur-degats: 1.2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "multiplicateur-degats: 1.2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "multiplicateur-portee-detectie: 1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "multiplicateur-portee-detectie: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "multiplicator-pv: 1,0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "multiplicator-pv: 1,0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # AANVULLEND toegepast op bioom/afstand/zone-modificatoren";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # AANVULLEND toegepast op bioom/afstand/zone-modificatoren");
                if (comment.equals("multiplicateur-pv: 1.5")) return "multiplicateur-pv: 1,5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "multiplicateur-pv: 1,5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "multiplicateur-weerstand-terugslag: 1.0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "multiplicateur-weerstand-terugslag: 1.0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "multiplicateur-vitesse: 1.0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "multiplicateur-vitesse: 1.0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = kan overdag spawnen";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = kan overdag spawnen");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "prioriteit: 0 # Prioriteit (hoger = prioriteit bij overlap)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "prioriteit: 0 # Prioriteit (hoger = prioriteit bij overlap)");
                if (comment.equals("resistance-feu: false")) return "weerstand-feu: onwaar";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "weerstand-feu: onwaar");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "weerstand-feu: false # true = brandimmuniteit (permanent drankjeeffect)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "weerstand-feu: false # true = brandimmuniteit (permanent drankjeeffect)");
                if (comment.equals("tier-equipement: \"none\"")) return "niveau-uitrusting: \"geen\"";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "niveau-uitrusting: \"geen\"");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "niveau-uitrusting: \"geen\" # geen | leer | ijzer | diamant | netheriet";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "niveau-uitrusting: \"geen\" # geen | leer | ijzer | diamant | netheriet");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "niveau-uitrusting: \"geen\" # geen | leer | ijzer | diamant | netheriet";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "niveau-uitrusting: \"geen\" # geen | leer | ijzer | diamant | netheriet");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "type: CUBOID # CUBOID of RADIUS";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "type: CUBOID # CUBOID of RADIUS");
                if (comment.equals("zone-name:")) return "zonenaam:";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "zonenaam:");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "zone-sure: false # true = schakelt vijandige spawns in zone uit";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "zone-sure: false # true = schakelt vijandige spawns in zone uit");
            }
            case "pl" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(Domyślnie nie zdefiniowano żadnych stref — użyj /wdzone create, aby je utworzyć)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(Domyślnie nie zdefiniowano żadnych stref — użyj /wdzone create, aby je utworzyć)");
                if (comment.equals("- SPIDER")) return "- PAJĄK";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- PAJĄK");
                if (comment.equals("- ZOMBIE")) return "- ZOMBIE";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ZOMBIE");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Klucz = nazwa wyliczeniowa biomu Bukkit (np. PUSTYNIA, DŻUNGLA...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Klucz = nazwa wyliczeniowa biomu Bukkit (np. PUSTYNIA, DŻUNGLA...)");
                if (comment.equals("Base Global Modifiers")) return "Podstawowe modyfikatory globalne";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Podstawowe modyfikatory globalne");
                if (comment.equals("Cap of variants per player")) return "Limit wariantów na gracza";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Limit wariantów na gracza");
                if (comment.equals("Custom in-game difficulty zones")) return "Niestandardowe strefy trudności w grze";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Niestandardowe strefy trudności w grze");
                if (comment.equals("Desert mobs are fire resistant")) return "Moby pustynne są odporne na ogień";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "Moby pustynne są odporne na ogień");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "Trudność wzrasta w miarę oddalania się graczy od centralnego punktu początkowego.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "Trudność wzrasta w miarę oddalania się graczy od centralnego punktu początkowego.");
                if (comment.equals("Distance Scaling")) return "Skalowanie odległości";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Skalowanie odległości");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "Nie edytuj ręcznie, jeśli nie jest to absolutnie konieczne.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "Nie edytuj ręcznie, jeśli nie jest to absolutnie konieczne.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Włącz tryb debugowania (dodatkowe logi w konsoli — wyłącz w środowisku produkcyjnym)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Włącz tryb debugowania (dodatkowe logi w konsoli — wyłącz w środowisku produkcyjnym)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Włącz kod języka (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Włącz kod języka (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "Fichier de stockage des paramètres staffs des joueurs";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "Fichier de stockage des paramètres staffs des joueurs");
                if (comment.equals("General Biome Format:")) return "Ogólny format biomu:";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Ogólny format biomu:");
                if (comment.equals("General Format:")) return "Ogólny format:";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Ogólny format:");
                if (comment.equals("General Plugin Settings")) return "Ogólne ustawienia wtyczki";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Ogólne ustawienia wtyczki");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Globalne × Biom × Odległość × Strefa × Przejęcie mobów = Ostateczne";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Globalne × Biom × Odległość × Strefa × Przejęcie mobów = Ostateczne");
                if (comment.equals("Husks have a bonus HP multiplier")) return "Pustaki mają dodatkowy mnożnik HP";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "Pustaki mają dodatkowy mnożnik HP");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Indywidualna konfiguracja dla każdego typu moba";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Indywidualna konfiguracja dla każdego typu moba");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Klucz = nazwa wyliczeniowa Bukkit EntityType (np. ZOMBIE, SZKIELET...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Klucz = nazwa wyliczeniowa Bukkit EntityType (np. ZOMBIE, SZKIELET...)");
                if (comment.equals("Main plugin configuration file")) return "Główny plik konfiguracyjny wtyczki";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "Główny plik konfiguracyjny wtyczki");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Maksymalna odległość odradzania (w blokach) od gracza, powyżej której nie pojawiają się naturalne moby (-1, aby wyłączyć)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Maksymalna odległość odradzania (w blokach) od gracza, powyżej której nie pojawiają się naturalne moby (-1, aby wyłączyć)");
                if (comment.equals("Monster variants and squads configuration")) return "Warianty potworów i konfiguracja drużyn";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Warianty potworów i konfiguracja drużyn");
                if (comment.equals("Multiplier Priority Order:")) return "Kolejność priorytetów mnożnika:";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Kolejność priorytetów mnożnika:");
                if (comment.equals("Only these mobs can spawn in desert")) return "Tylko te moby mogą pojawiać się na pustyni";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Tylko te moby mogą pojawiać się na pustyni");
                if (comment.equals("Per-biome difficulty rules")) return "Zasady trudności dla poszczególnych biomów";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Zasady trudności dla poszczególnych biomów");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Wolniejszy, ale strzela strzałami spowalniającymi";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Wolniejszy, ale strzela strzałami spowalniającymi");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Cel: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Cel: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Te moby NIE MOGĄ odradzać się na pustyni";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Te moby NIE MOGĄ odradzać się na pustyni");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Te mnożniki dotyczą WSZYSTKICH mobów, niezależnie od biomu, odległości czy strefy. 1,0 = neutralny.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Te mnożniki dotyczą WSZYSTKICH mobów, niezależnie od biomu, odległości czy strefy. 1,0 = neutralny.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Ten plik jest AUTOMATYCZNIE zarządzany przez wtyczkę.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Ten plik jest AUTOMATYCZNIE zarządzany przez wtyczkę.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Użyj poleceń strefy /wd, aby tworzyć i edytować strefy.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Użyj poleceń strefy /wd, aby tworzyć i edytować strefy.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildDifficulty — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildDifficulty — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty — config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty — config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficulty — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficulty — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildDifficulty — Zones.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildDifficulty — Zones.yml");
                if (comment.equals("ZOMBIE:")) return "BAŁWAN:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "BAŁWAN:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Zombie pojawiają się w świetle dziennym i nie płoną";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Zombie pojawiają się w świetle dziennym i nie płoną");
                if (comment.equals("Zone Format (for reference):")) return "Format strefy (w celach informacyjnych):";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Format strefy (w celach informacyjnych):");
                if (comment.equals("chance-equipement: 0.0")) return "szansa na wyposażenie: 0,0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "szansa na wyposażenie: 0,0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "szansa na sprzęt: 0,0 # 0,0 do 1,0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "szansa na sprzęt: 0,0 # 0,0 do 1,0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignorowanie-soleil: false # true = nie pali się na słońcu";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignorowanie-soleil: false # true = nie pali się na słońcu");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorowanie-regles-biome: false # true = ignoruje modyfikatory biomu";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorowanie-regles-biome: false # true = ignoruje modyfikatory biomu");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (opcjonalnie) Tylko te moby mogą pojawiać się w tym biomie";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (opcjonalnie) Tylko te moby mogą pojawiać się w tym biomie");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # Biała lista mobów (pusta = wszystkie dozwolone)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # Biała lista mobów (pusta = wszystkie dozwolone)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (opcjonalnie) Te moby NIE MOGĄ odradzać się w tym biomie";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (opcjonalnie) Te moby NIE MOGĄ odradzać się w tym biomie");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Czarna lista mobów";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Czarna lista mobów");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "moby: # (opcjonalnie) Nadpisania specyficzne dla mobów w tym biomie";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "moby: # (opcjonalnie) Nadpisania specyficzne dla mobów w tym biomie");
                if (comment.equals("modificateurs:")) return "modyfikatorzy:";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "modyfikatorzy:");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificateurs: # (opcjonalne) Mnożniki stosowane do mobów pojawiających się tutaj";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificateurs: # (opcjonalne) Mnożniki stosowane do mobów pojawiających się tutaj");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificateurs: # (opcjonalne) Specyficzne nadpisania dla tego typu moba";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificateurs: # (opcjonalne) Specyficzne nadpisania dla tego typu moba");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"świat\" # Nazwa świata";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"świat\" # Nazwa świata");
                if (comment.equals("multiplicateur-degats: 1.0")) return "multiplicateur-degats: 1,0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "multiplicateur-degats: 1,0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "multiplikator-degat: 1.2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "multiplikator-degat: 1.2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "wykrywanie multiplicateur-portee: 1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "wykrywanie multiplicateur-portee: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "multiplikator-pv: 1.0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "multiplikator-pv: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # Stosowany dodatkowo do modyfikatorów biomu/odległości/strefy";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # Stosowany dodatkowo do modyfikatorów biomu/odległości/strefy");
                if (comment.equals("multiplicateur-pv: 1.5")) return "multiplikator-pv: 1,5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "multiplikator-pv: 1,5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "Odporność na odrzut multiplikatorowy: 1,0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "Odporność na odrzut multiplikatorowy: 1,0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "multiplicateur-vitesse: 1,0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "multiplicateur-vitesse: 1,0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = może pojawiać się w ciągu dnia";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = może pojawiać się w ciągu dnia");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "priorytet: 0 # Priorytet (wyższy = priorytet w przypadku nakładania się)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "priorytet: 0 # Priorytet (wyższy = priorytet w przypadku nakładania się)");
                if (comment.equals("resistance-feu: false")) return "opór-feu: fałsz";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "opór-feu: fałsz");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "Resistance-feu: false # true = odporność na ogień (trwały efekt mikstury)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "Resistance-feu: false # true = odporność na ogień (trwały efekt mikstury)");
                if (comment.equals("tier-equipement: \"none\"")) return "wyposażenie poziomu: „brak”";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "wyposażenie poziomu: „brak”");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "sprzęt-tier: \"brak\" # brak | skóra | żelazo | diament | netheryt";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "sprzęt-tier: \"brak\" # brak | skóra | żelazo | diament | netheryt");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "sprzęt-tier: \"brak\" # brak | skóra | żelazo | diament | netheryt";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "sprzęt-tier: \"brak\" # brak | skóra | żelazo | diament | netheryt");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "wpisz: Prostopadłościan # Prostopadłościan lub PROMIeń";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "wpisz: Prostopadłościan # Prostopadłościan lub PROMIeń");
                if (comment.equals("zone-name:")) return "nazwa-strefy:";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "nazwa-strefy:");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "strefa-sure: false # true = wyłącza wrogie spawnowanie w strefie";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "strefa-sure: false # true = wyłącza wrogie spawnowanie w strefie");
            }
            case "ru" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(По умолчанию зоны не определены — для их создания используйте /wd Zone create)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(По умолчанию зоны не определены — для их создания используйте /wd Zone create)");
                if (comment.equals("- SPIDER")) return "- ПАУК";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- ПАУК");
                if (comment.equals("- ZOMBIE")) return "- ЗОМБИ";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ЗОМБИ");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Key = имя перечисления Bukkit Biome (например, ПУСТЫНЯ, ДЖУНГЛИ...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Key = имя перечисления Bukkit Biome (например, ПУСТЫНЯ, ДЖУНГЛИ...)");
                if (comment.equals("Base Global Modifiers")) return "Базовые глобальные модификаторы";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Базовые глобальные модификаторы");
                if (comment.equals("Cap of variants per player")) return "Максимальное количество вариантов на игрока";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Максимальное количество вариантов на игрока");
                if (comment.equals("Custom in-game difficulty zones")) return "Пользовательские игровые зоны сложности.";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Пользовательские игровые зоны сложности.");
                if (comment.equals("Desert mobs are fire resistant")) return "Пустынные мобы устойчивы к огню.";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "Пустынные мобы устойчивы к огню.");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "Сложность возрастает по мере удаления игроков от центральной точки отправления.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "Сложность возрастает по мере удаления игроков от центральной точки отправления.");
                if (comment.equals("Distance Scaling")) return "Масштабирование расстояния";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Масштабирование расстояния");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "Не редактируйте вручную без крайней необходимости.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "Не редактируйте вручную без крайней необходимости.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Включить режим отладки (дополнительные журналы в консоли — отключить в рабочей среде)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Включить режим отладки (дополнительные журналы в консоли — отключить в рабочей среде)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Включить код языка (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Включить код языка (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "Справочник запасов параметров персонала для посетителей";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "Справочник запасов параметров персонала для посетителей");
                if (comment.equals("General Biome Format:")) return "Общий формат биома:";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Общий формат биома:");
                if (comment.equals("General Format:")) return "Общий формат:";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Общий формат:");
                if (comment.equals("General Plugin Settings")) return "Общие настройки плагина";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Общие настройки плагина");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Глобальный × Биом × Расстояние × Зона × Переопределение мобов = Финал";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Глобальный × Биом × Расстояние × Зона × Переопределение мобов = Финал");
                if (comment.equals("Husks have a bonus HP multiplier")) return "Хаски имеют бонусный множитель HP.";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "Хаски имеют бонусный множитель HP.");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Индивидуальная конфигурация для каждого типа моба.";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Индивидуальная конфигурация для каждого типа моба.");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Key = имя перечисления Bukkit EntityType (например, ZOMBIE, SKELETON...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Key = имя перечисления Bukkit EntityType (например, ZOMBIE, SKELETON...)");
                if (comment.equals("Main plugin configuration file")) return "Основной файл конфигурации плагина";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "Основной файл конфигурации плагина");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Максимальное расстояние появления (в блоках) от игрока, за пределами которого не появляются естественные мобы (-1 для отключения)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Максимальное расстояние появления (в блоках) от игрока, за пределами которого не появляются естественные мобы (-1 для отключения)");
                if (comment.equals("Monster variants and squads configuration")) return "Варианты монстров и конфигурация отрядов";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Варианты монстров и конфигурация отрядов");
                if (comment.equals("Multiplier Priority Order:")) return "Порядок приоритета множителя:";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Порядок приоритета множителя:");
                if (comment.equals("Only these mobs can spawn in desert")) return "Только эти мобы могут спауниться в пустыне.";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Только эти мобы могут спауниться в пустыне.");
                if (comment.equals("Per-biome difficulty rules")) return "Правила сложности для каждого биома";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Правила сложности для каждого биома");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Медленнее, но стреляет стрелами замедления.";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Медленнее, но стреляет стрелами замедления.");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Цель: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Цель: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Эти мобы НЕ МОГУТ появиться в пустыне.";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Эти мобы НЕ МОГУТ появиться в пустыне.");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Эти множители применяются ко ВСЕМ мобам, независимо от биома, расстояния или зоны. 1,0 = нейтрально.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Эти множители применяются ко ВСЕМ мобам, независимо от биома, расстояния или зоны. 1,0 = нейтрально.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Этот файл АВТОМАТИЧЕСКИ управляется плагином.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Этот файл АВТОМАТИЧЕСКИ управляется плагином.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Используйте команды зоны /wd для создания и редактирования зон.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Используйте команды зоны /wd для создания и редактирования зон.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildDifficulty — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildDifficulty — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty — config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty — config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficulty — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficulty — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildDifficulty — зоны.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildDifficulty — зоны.yml");
                if (comment.equals("ZOMBIE:")) return "ЗОМБИ:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "ЗОМБИ:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Зомби появляются днем ​​и не горят.";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Зомби появляются днем ​​и не горят.");
                if (comment.equals("Zone Format (for reference):")) return "Формат зоны (для справки):";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Формат зоны (для справки):");
                if (comment.equals("chance-equipement: 0.0")) return "шанс-оборудование: 0.0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "шанс-оборудование: 0.0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "шанс-оборудование: 0.0 # от 0,0 до 1,0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "шанс-оборудование: 0.0 # от 0,0 до 1,0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignore-soleil: false # true = не горит на солнце";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignore-soleil: false # true = не горит на солнце");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorer-regles-biome: false # true = игнорирует модификаторы биома";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorer-regles-biome: false # true = игнорирует модификаторы биома");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (необязательно) Только эти мобы могут появляться в этом биоме.";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (необязательно) Только эти мобы могут появляться в этом биоме.");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # Белый список мобов (пустой = все разрешены)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # Белый список мобов (пустой = все разрешены)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (необязательно) Эти мобы НЕ МОГУТ появляться в этом биоме.";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (необязательно) Эти мобы НЕ МОГУТ появляться в этом биоме.");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Черный список мобов";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Черный список мобов");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs: # (необязательно) Переопределения, специфичные для моба в этом биоме";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs: # (необязательно) Переопределения, специфичные для моба в этом биоме");
                if (comment.equals("modificateurs:")) return "модификаторы:";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "модификаторы:");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificateurs: # (необязательно) Множители, применяемые к появляющимся здесь мобам";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificateurs: # (необязательно) Множители, применяемые к появляющимся здесь мобам");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificateurs: # (необязательно) Определенные переопределения для этого типа моба.";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificateurs: # (необязательно) Определенные переопределения для этого типа моба.");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"world\" # Имя мира";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"world\" # Имя мира");
                if (comment.equals("multiplicateur-degats: 1.0")) return "мультипликатор-дегат: 1.0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "мультипликатор-дегат: 1.0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "мультипликатор-дегаты: 1,2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "мультипликатор-дегаты: 1,2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "обнаружение мультипликатора-порта: 1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "обнаружение мультипликатора-порта: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "мультипликатор-PV: 1.0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "мультипликатор-PV: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplieur-pv: 1.0 # Применяется ДОПОЛНИТЕЛЬНО к модификаторам биома/расстояния/зоны";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplieur-pv: 1.0 # Применяется ДОПОЛНИТЕЛЬНО к модификаторам биома/расстояния/зоны");
                if (comment.equals("multiplicateur-pv: 1.5")) return "мультипликатор-PV: 1,5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "мультипликатор-PV: 1,5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "мультипликатор-сопротивление-отбрасыванию: 1.0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "мультипликатор-сопротивление-отбрасыванию: 1.0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "мультипликатор-витесс: 1.0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "мультипликатор-витесс: 1.0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = может появляться в дневное время";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = может появляться в дневное время");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "Priorite: 0 # Приоритет (выше = приоритет при перекрытии)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "Priorite: 0 # Приоритет (выше = приоритет при перекрытии)");
                if (comment.equals("resistance-feu: false")) return "сопротивление-feu: ложь";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "сопротивление-feu: ложь");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "сопротивление-feu: false # true = иммунитет к огню (постоянный эффект зелья)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "сопротивление-feu: false # true = иммунитет к огню (постоянный эффект зелья)");
                if (comment.equals("tier-equipement: \"none\"")) return "уровень оборудования: «нет»";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "уровень оборудования: «нет»");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "tier-equipment: \"none\" # none | кожа | железо | алмаз | незерит";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "tier-equipment: \"none\" # none | кожа | железо | алмаз | незерит");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "tier-equipment: \"none\" # none | кожа | железо | алмаз | незерит";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "tier-equipment: \"none\" # none | кожа | железо | алмаз | незерит");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "тип: КУБОВИД # КУБОИД или РАДИУС";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "тип: КУБОВИД # КУБОИД или РАДИУС");
                if (comment.equals("zone-name:")) return "имя зоны:";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "имя зоны:");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "Zone-sure: false # true = отключает появление враждебных объектов в зоне";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "Zone-sure: false # true = отключает появление враждебных объектов в зоне");
            }
            case "zh_cn" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "（默认情况下没有定义区域 - 使用 /wd zone create 创建一个区域）";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "（默认情况下没有定义区域 - 使用 /wd zone create 创建一个区域）");
                if (comment.equals("- SPIDER")) return "- 蜘蛛";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- 蜘蛛");
                if (comment.equals("- ZOMBIE")) return "- 僵尸";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- 僵尸");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Key = Bukkit Biome 枚举名称（例如 DESERT、JUNGLE...）";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Key = Bukkit Biome 枚举名称（例如 DESERT、JUNGLE...）");
                if (comment.equals("Base Global Modifiers")) return "基础全局修饰符";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "基础全局修饰符");
                if (comment.equals("Cap of variants per player")) return "每个玩家的变体上限";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "每个玩家的变体上限");
                if (comment.equals("Custom in-game difficulty zones")) return "自定义游戏内难度区域";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "自定义游戏内难度区域");
                if (comment.equals("Desert mobs are fire resistant")) return "沙漠生物具有抗火能力";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "沙漠生物具有抗火能力");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "随着玩家距离中心原点越来越远，难度也会增加。";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "随着玩家距离中心原点越来越远，难度也会增加。");
                if (comment.equals("Distance Scaling")) return "距离缩放";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "距离缩放");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "除非绝对必要，否则不要手动编辑。";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "除非绝对必要，否则不要手动编辑。");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "启用调试模式（控制台中的额外日志 - 在生产中禁用）";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "启用调试模式（控制台中的额外日志 - 在生产中禁用）");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "启用语言代码（fr、en、de、es、pt_BR、nl、pl、ru、zh_CN、it）";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "启用语言代码（fr、en、de、es、pt_BR、nl、pl、ru、zh_CN、it）");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "工作人员参数库存信息";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "工作人员参数库存信息");
                if (comment.equals("General Biome Format:")) return "一般生物群落格式：";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "一般生物群落格式：");
                if (comment.equals("General Format:")) return "一般格式：";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "一般格式：");
                if (comment.equals("General Plugin Settings")) return "常规插件设置";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "常规插件设置");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "全局 × 生物群落 × 距离 × 区域 × 生物覆盖 = 最终";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "全局 × 生物群落 × 距离 × 区域 × 生物覆盖 = 最终");
                if (comment.equals("Husks have a bonus HP multiplier")) return "尸壳有额外的生命值乘数";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "尸壳有额外的生命值乘数");
                if (comment.equals("Individual configuration per vanilla mob type")) return "每个原版生物类型的单独配置";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "每个原版生物类型的单独配置");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Key = Bukkit EntityType 枚举名称（例如 ZOMBIE、SKELETON...）";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Key = Bukkit EntityType 枚举名称（例如 ZOMBIE、SKELETON...）");
                if (comment.equals("Main plugin configuration file")) return "主要插件配置文件";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "主要插件配置文件");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "距玩家的最大生成距离（以方块为单位），超出该距离自然生物不会生成（-1 表示禁用）";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "距玩家的最大生成距离（以方块为单位），超出该距离自然生物不会生成（-1 表示禁用）");
                if (comment.equals("Monster variants and squads configuration")) return "怪物变体和小队配置";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "怪物变体和小队配置");
                if (comment.equals("Multiplier Priority Order:")) return "乘数优先顺序：";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "乘数优先顺序：");
                if (comment.equals("Only these mobs can spawn in desert")) return "只有这些生物可以在沙漠中生成";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "只有这些生物可以在沙漠中生成");
                if (comment.equals("Per-biome difficulty rules")) return "每个生物群落的难度规则";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "每个生物群落的难度规则");
                if (comment.equals("Slower, but shoots slowness arrows")) return "较慢，但射出缓慢箭";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "较慢，但射出缓慢箭");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "目标：Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "目标：Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "这些生物不能在沙漠中生成";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "这些生物不能在沙漠中生成");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "这些乘数适用于所有生物，无论生物群落、距离或区域如何。 1.0 = 中性。";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "这些乘数适用于所有生物，无论生物群落、距离或区域如何。 1.0 = 中性。");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "该文件由插件自动管理。";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "该文件由插件自动管理。");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "使用 /wd zone 命令创建和编辑区域。";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "使用 /wd zone 命令创建和编辑区域。");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildDifficulty — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildDifficulty — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty — config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty — config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficulty — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficulty — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildDifficulty — zone.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildDifficulty — zone.yml");
                if (comment.equals("ZOMBIE:")) return "僵尸：";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "僵尸：");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "僵尸在白天生成并且不会燃烧";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "僵尸在白天生成并且不会燃烧");
                if (comment.equals("Zone Format (for reference):")) return "区域格式（供参考）：";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "区域格式（供参考）：");
                if (comment.equals("chance-equipement: 0.0")) return "机会装备：0.0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "机会装备：0.0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "机会装备: 0.0 # 0.0 到 1.0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "机会装备: 0.0 # 0.0 到 1.0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignore-soleil: false # true = 在阳光下不会燃烧";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignore-soleil: false # true = 在阳光下不会燃烧");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorer-regles-biome: false # true = 忽略生物群系修饰符";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorer-regles-biome: false # true = 忽略生物群系修饰符");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (可选) 只有这些小怪可以在此生物群落中生成";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (可选) 只有这些小怪可以在此生物群落中生成");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # 小怪白名单（空=全部允许）";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # 小怪白名单（空=全部允许）");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (可选)这些小怪不能在此生物群系中生成";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (可选)这些小怪不能在此生物群系中生成");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # 小怪黑名单";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # 小怪黑名单");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs: # (可选)此生物群系中特定于生物的覆盖";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs: # (可选)此生物群系中特定于生物的覆盖");
                if (comment.equals("modificateurs:")) return "修改者：";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "修改者：");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificateurs: # （可选）应用于此处生成的小怪的乘数";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificateurs: # （可选）应用于此处生成的小怪的乘数");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificateurs: # (可选)此生物类型的特定覆盖";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificateurs: # (可选)此生物类型的特定覆盖");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"world\" # 世界名称";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"world\" # 世界名称");
                if (comment.equals("multiplicateur-degats: 1.0")) return "多重倍数：1.0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "多重倍数：1.0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "多重倍数：1.2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "多重倍数：1.2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "多重端口检测：1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "多重端口检测：1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "多重 pv：1.0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "多重 pv：1.0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # 另外应用于生物群系/距离/区域修改器";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # 另外应用于生物群系/距离/区域修改器");
                if (comment.equals("multiplicateur-pv: 1.5")) return "多重 pv：1.5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "多重 pv：1.5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "多重抵抗击退：1.0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "多重抵抗击退：1.0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "多重生命力：1.0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "多重生命力：1.0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = 可以在白天生成";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = 可以在白天生成");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "Priorite: 0 # 优先级（较高=重叠优先级）";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "Priorite: 0 # 优先级（较高=重叠优先级）");
                if (comment.equals("resistance-feu: false")) return "电阻-feu：假";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "电阻-feu：假");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "sistance-feu: false # true = 火焰免疫（永久药水效果）";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "sistance-feu: false # true = 火焰免疫（永久药水效果）");
                if (comment.equals("tier-equipement: \"none\"")) return "装备等级：“无”";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "装备等级：“无”");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "等级装备：“无”# 无 |皮革 |铁|钻石 |下界合金";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "等级装备：“无”# 无 |皮革 |铁|钻石 |下界合金");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "等级装备：“无”# 无 |皮革 |铁|钻石 |下界合金";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "等级装备：“无”# 无 |皮革 |铁|钻石 |下界合金");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "类型：CUBOID # CUBOID 或 RADIUS";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "类型：CUBOID # CUBOID 或 RADIUS");
                if (comment.equals("zone-name:")) return "区域名称：";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "区域名称：");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "zone-sure: false # true = 禁用区域中的敌对生成";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "zone-sure: false # true = 禁用区域中的敌对生成");
            }
            case "it" -> {
                if (comment.equals("(No zones defined by default — use /wd zone create to create one)")) return "(Nessuna zona definita per impostazione predefinita: utilizza /wd zone create per crearne una)";
                if (comment.contains("(No zones defined by default — use /wd zone create to create one)")) return comment.replace("(No zones defined by default — use /wd zone create to create one)", "(Nessuna zona definita per impostazione predefinita: utilizza /wd zone create per crearne una)");
                if (comment.equals("- SPIDER")) return "- RAGNO";
                if (comment.contains("- SPIDER")) return comment.replace("- SPIDER", "- RAGNO");
                if (comment.equals("- ZOMBIE")) return "- ZOMBIE";
                if (comment.contains("- ZOMBIE")) return comment.replace("- ZOMBIE", "- ZOMBIE");
                if (comment.equals("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return "BIOME_NAME: # Key = Nome enum del bioma Bukkit (ad esempio DESERT, JUNGLE...)";
                if (comment.contains("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)")) return comment.replace("BIOME_NAME:                         # Key = Bukkit Biome enum name (e.g. DESERT, JUNGLE...)", "BIOME_NAME: # Key = Nome enum del bioma Bukkit (ad esempio DESERT, JUNGLE...)");
                if (comment.equals("Base Global Modifiers")) return "Modificatori globali di base";
                if (comment.contains("Base Global Modifiers")) return comment.replace("Base Global Modifiers", "Modificatori globali di base");
                if (comment.equals("Cap of variants per player")) return "Limite di varianti per giocatore";
                if (comment.contains("Cap of variants per player")) return comment.replace("Cap of variants per player", "Limite di varianti per giocatore");
                if (comment.equals("Custom in-game difficulty zones")) return "Zone di difficoltà di gioco personalizzate";
                if (comment.contains("Custom in-game difficulty zones")) return comment.replace("Custom in-game difficulty zones", "Zone di difficoltà di gioco personalizzate");
                if (comment.equals("Desert mobs are fire resistant")) return "I mob del deserto sono resistenti al fuoco";
                if (comment.contains("Desert mobs are fire resistant")) return comment.replace("Desert mobs are fire resistant", "I mob del deserto sono resistenti al fuoco");
                if (comment.equals("Difficulty increases as players get further from the central origin point.")) return "La difficoltà aumenta man mano che i giocatori si allontanano dal punto di origine centrale.";
                if (comment.contains("Difficulty increases as players get further from the central origin point.")) return comment.replace("Difficulty increases as players get further from the central origin point.", "La difficoltà aumenta man mano che i giocatori si allontanano dal punto di origine centrale.");
                if (comment.equals("Distance Scaling")) return "Scala della distanza";
                if (comment.contains("Distance Scaling")) return comment.replace("Distance Scaling", "Scala della distanza");
                if (comment.equals("Do not edit manually unless absolutely necessary.")) return "Non modificare manualmente se non assolutamente necessario.";
                if (comment.contains("Do not edit manually unless absolutely necessary.")) return comment.replace("Do not edit manually unless absolutely necessary.", "Non modificare manualmente se non assolutamente necessario.");
                if (comment.equals("Enable debug mode (extra logs in console — disable in production)")) return "Abilita la modalità debug (log aggiuntivi nella console - disabilita in produzione)";
                if (comment.contains("Enable debug mode (extra logs in console — disable in production)")) return comment.replace("Enable debug mode (extra logs in console — disable in production)", "Abilita la modalità debug (log aggiuntivi nella console - disabilita in produzione)");
                if (comment.equals("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return "Abilita il codice della lingua (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)";
                if (comment.contains("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)")) return comment.replace("Enable language code (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)", "Abilita il codice della lingua (fr, en, de, es, pt_BR, nl, pl, ru, zh_CN, it)");
                if (comment.equals("Fichier de stockage des paramètres personnels des joueurs")) return "File di archiviazione dei parametri personali dei giocatori";
                if (comment.contains("Fichier de stockage des paramètres personnels des joueurs")) return comment.replace("Fichier de stockage des paramètres personnels des joueurs", "File di archiviazione dei parametri personali dei giocatori");
                if (comment.equals("General Biome Format:")) return "Formato generale del bioma:";
                if (comment.contains("General Biome Format:")) return comment.replace("General Biome Format:", "Formato generale del bioma:");
                if (comment.equals("General Format:")) return "Formato generale:";
                if (comment.contains("General Format:")) return comment.replace("General Format:", "Formato generale:");
                if (comment.equals("General Plugin Settings")) return "Impostazioni generali del plugin";
                if (comment.contains("General Plugin Settings")) return comment.replace("General Plugin Settings", "Impostazioni generali del plugin");
                if (comment.equals("Global × Biome × Distance × Zone × Mob-Override = Final")) return "Globale × Bioma × Distanza × Zona × Mob-Override = Finale";
                if (comment.contains("Global × Biome × Distance × Zone × Mob-Override = Final")) return comment.replace("Global × Biome × Distance × Zone × Mob-Override = Final", "Globale × Bioma × Distanza × Zona × Mob-Override = Finale");
                if (comment.equals("Husks have a bonus HP multiplier")) return "I mutanti hanno un moltiplicatore HP bonus";
                if (comment.contains("Husks have a bonus HP multiplier")) return comment.replace("Husks have a bonus HP multiplier", "I mutanti hanno un moltiplicatore HP bonus");
                if (comment.equals("Individual configuration per vanilla mob type")) return "Configurazione individuale per tipo di mob vanilla";
                if (comment.contains("Individual configuration per vanilla mob type")) return comment.replace("Individual configuration per vanilla mob type", "Configurazione individuale per tipo di mob vanilla");
                if (comment.equals("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return "MOB_TYPE: # Key = nome enum Bukkit EntityType (ad esempio ZOMBIE, SKELETON...)";
                if (comment.contains("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)")) return comment.replace("MOB_TYPE:                           # Key = Bukkit EntityType enum name (e.g. ZOMBIE, SKELETON...)", "MOB_TYPE: # Key = nome enum Bukkit EntityType (ad esempio ZOMBIE, SKELETON...)");
                if (comment.equals("Main plugin configuration file")) return "File di configurazione del plugin principale";
                if (comment.contains("Main plugin configuration file")) return comment.replace("Main plugin configuration file", "File di configurazione del plugin principale");
                if (comment.equals("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return "Distanza massima di spawn (in blocchi) dal giocatore oltre la quale i mob naturali non si generano (-1 per disabilitare)";
                if (comment.contains("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)")) return comment.replace("Maximum spawn distance (in blocks) from player beyond which natural mobs do not spawn (-1 to disable)", "Distanza massima di spawn (in blocchi) dal giocatore oltre la quale i mob naturali non si generano (-1 per disabilitare)");
                if (comment.equals("Monster variants and squads configuration")) return "Varianti dei mostri e configurazione delle squadre";
                if (comment.contains("Monster variants and squads configuration")) return comment.replace("Monster variants and squads configuration", "Varianti dei mostri e configurazione delle squadre");
                if (comment.equals("Multiplier Priority Order:")) return "Ordine di priorità del moltiplicatore:";
                if (comment.contains("Multiplier Priority Order:")) return comment.replace("Multiplier Priority Order:", "Ordine di priorità del moltiplicatore:");
                if (comment.equals("Only these mobs can spawn in desert")) return "Solo questi mob possono spawnare nel deserto";
                if (comment.contains("Only these mobs can spawn in desert")) return comment.replace("Only these mobs can spawn in desert", "Solo questi mob possono spawnare nel deserto");
                if (comment.equals("Per-biome difficulty rules")) return "Regole di difficoltà per bioma";
                if (comment.contains("Per-biome difficulty rules")) return comment.replace("Per-biome difficulty rules", "Regole di difficoltà per bioma");
                if (comment.equals("Slower, but shoots slowness arrows")) return "Più lento, ma lancia frecce di lentezza";
                if (comment.contains("Slower, but shoots slowness arrows")) return comment.replace("Slower, but shoots slowness arrows", "Più lento, ma lancia frecce di lentezza");
                if (comment.equals("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return "Obiettivo: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT";
                if (comment.contains("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT")) return comment.replace("Target: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT", "Obiettivo: Purpur 1.21.4 / Paper-API 1.21.4-R0.1-SNAPSHOT");
                if (comment.equals("These mobs CANNOT spawn in desert")) return "Questi mob NON POSSONO spawnare nel deserto";
                if (comment.contains("These mobs CANNOT spawn in desert")) return comment.replace("These mobs CANNOT spawn in desert", "Questi mob NON POSSONO spawnare nel deserto");
                if (comment.equals("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return "Questi moltiplicatori si applicano a TUTTI i mob indipendentemente dal bioma, dalla distanza o dalla zona. 1.0 = neutro.";
                if (comment.contains("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.")) return comment.replace("These multipliers apply to ALL mobs regardless of biome, distance or zone. 1.0 = neutral.", "Questi moltiplicatori si applicano a TUTTI i mob indipendentemente dal bioma, dalla distanza o dalla zona. 1.0 = neutro.");
                if (comment.equals("This file is AUTOMATICALLY managed by the plugin.")) return "Questo file è gestito AUTOMATICAMENTE dal plugin.";
                if (comment.contains("This file is AUTOMATICALLY managed by the plugin.")) return comment.replace("This file is AUTOMATICALLY managed by the plugin.", "Questo file è gestito AUTOMATICAMENTE dal plugin.");
                if (comment.equals("Use /wd zone commands to create and edit zones.")) return "Utilizza i comandi /wd zone per creare e modificare le zone.";
                if (comment.contains("Use /wd zone commands to create and edit zones.")) return comment.replace("Use /wd zone commands to create and edit zones.", "Utilizza i comandi /wd zone per creare e modificare le zone.");
                if (comment.equals("WildDifficulty — biomes.yml")) return "WildDifficoltà — biomes.yml";
                if (comment.contains("WildDifficulty — biomes.yml")) return comment.replace("WildDifficulty — biomes.yml", "WildDifficoltà — biomes.yml");
                if (comment.equals("WildDifficulty — config.yml")) return "WildDifficulty — config.yml";
                if (comment.contains("WildDifficulty — config.yml")) return comment.replace("WildDifficulty — config.yml", "WildDifficulty — config.yml");
                if (comment.equals("WildDifficulty — mob-variants.yml")) return "WildDifficulty — mob-variants.yml";
                if (comment.contains("WildDifficulty — mob-variants.yml")) return comment.replace("WildDifficulty — mob-variants.yml", "WildDifficulty — mob-variants.yml");
                if (comment.equals("WildDifficulty — mobs.yml")) return "WildDifficoltà — mobs.yml";
                if (comment.contains("WildDifficulty — mobs.yml")) return comment.replace("WildDifficulty — mobs.yml", "WildDifficoltà — mobs.yml");
                if (comment.equals("WildDifficulty — zones.yml")) return "WildDifficoltà — zones.yml";
                if (comment.contains("WildDifficulty — zones.yml")) return comment.replace("WildDifficulty — zones.yml", "WildDifficoltà — zones.yml");
                if (comment.equals("ZOMBIE:")) return "ZOMBIE:";
                if (comment.contains("ZOMBIE:")) return comment.replace("ZOMBIE:", "ZOMBIE:");
                if (comment.equals("Zombies spawn during daylight and do not burn")) return "Gli zombi si generano durante il giorno e non bruciano";
                if (comment.contains("Zombies spawn during daylight and do not burn")) return comment.replace("Zombies spawn during daylight and do not burn", "Gli zombi si generano durante il giorno e non bruciano");
                if (comment.equals("Zone Format (for reference):")) return "Formato zona (per riferimento):";
                if (comment.contains("Zone Format (for reference):")) return comment.replace("Zone Format (for reference):", "Formato zona (per riferimento):");
                if (comment.equals("chance-equipement: 0.0")) return "attrezzatura casuale: 0,0";
                if (comment.contains("chance-equipement: 0.0")) return comment.replace("chance-equipement: 0.0", "attrezzatura casuale: 0,0");
                if (comment.equals("chance-equipement: 0.0           # 0.0 to 1.0")) return "equipaggiamento casuale: 0.0 # da 0.0 a 1.0";
                if (comment.contains("chance-equipement: 0.0           # 0.0 to 1.0")) return comment.replace("chance-equipement: 0.0           # 0.0 to 1.0", "equipaggiamento casuale: 0.0 # da 0.0 a 1.0");
                if (comment.equals("ignore-soleil: false              # true = does not burn in sunlight")) return "ignore-soleil: false # true = non brucia alla luce del sole";
                if (comment.contains("ignore-soleil: false              # true = does not burn in sunlight")) return comment.replace("ignore-soleil: false              # true = does not burn in sunlight", "ignore-soleil: false # true = non brucia alla luce del sole");
                if (comment.equals("ignorer-regles-biome: false       # true = ignores biome modifiers")) return "ignorer-regles-biome: false # true = ignora i modificatori del bioma";
                if (comment.contains("ignorer-regles-biome: false       # true = ignores biome modifiers")) return comment.replace("ignorer-regles-biome: false       # true = ignores biome modifiers", "ignorer-regles-biome: false # true = ignora i modificatori del bioma");
                if (comment.equals("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return "mobs-autorises: # (opzionale) Solo questi mob possono spawnare in questo bioma";
                if (comment.contains("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome")) return comment.replace("mobs-autorises:                    # (optional) Only these mobs can spawn in this biome", "mobs-autorises: # (opzionale) Solo questi mob possono spawnare in questo bioma");
                if (comment.equals("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return "mobs-autorises: [] # Whitelist di mob (vuoto = tutto consentito)";
                if (comment.contains("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)")) return comment.replace("mobs-autorises: []                # Whitelist of mobs (empty = all allowed)", "mobs-autorises: [] # Whitelist di mob (vuoto = tutto consentito)");
                if (comment.equals("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return "mobs-interdits: # (opzionale) Questi mob NON POSSONO spawnare in questo bioma";
                if (comment.contains("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome")) return comment.replace("mobs-interdits:                    # (optional) These mobs CANNOT spawn in this biome", "mobs-interdits: # (opzionale) Questi mob NON POSSONO spawnare in questo bioma");
                if (comment.equals("mobs-interdits: []                # Blacklist of mobs")) return "mobs-interdits: [] # Lista nera dei mob";
                if (comment.contains("mobs-interdits: []                # Blacklist of mobs")) return comment.replace("mobs-interdits: []                # Blacklist of mobs", "mobs-interdits: [] # Lista nera dei mob");
                if (comment.equals("mobs:                              # (optional) Mob-specific overrides in this biome")) return "mobs: # (opzionale) Sostituzioni specifiche del mob in questo bioma";
                if (comment.contains("mobs:                              # (optional) Mob-specific overrides in this biome")) return comment.replace("mobs:                              # (optional) Mob-specific overrides in this biome", "mobs: # (opzionale) Sostituzioni specifiche del mob in questo bioma");
                if (comment.equals("modificateurs:")) return "modificatori:";
                if (comment.contains("modificateurs:")) return comment.replace("modificateurs:", "modificatori:");
                if (comment.equals("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return "modificatori: # (opzionale) Moltiplicatori applicati ai mob che si generano qui";
                if (comment.contains("modificateurs:                     # (optional) Multipliers applied to mobs spawning here")) return comment.replace("modificateurs:                     # (optional) Multipliers applied to mobs spawning here", "modificatori: # (opzionale) Moltiplicatori applicati ai mob che si generano qui");
                if (comment.equals("modificateurs:                    # (optional) Specific overrides for this mob type")) return "modificatori: # (opzionale) Sostituzioni specifiche per questo tipo di mob";
                if (comment.contains("modificateurs:                    # (optional) Specific overrides for this mob type")) return comment.replace("modificateurs:                    # (optional) Specific overrides for this mob type", "modificatori: # (opzionale) Sostituzioni specifiche per questo tipo di mob");
                if (comment.equals("monde: \"world\"                    # World name")) return "monde: \"mondo\" # Nome del mondo";
                if (comment.contains("monde: \"world\"                    # World name")) return comment.replace("monde: \"world\"                    # World name", "monde: \"mondo\" # Nome del mondo");
                if (comment.equals("multiplicateur-degats: 1.0")) return "moltiplicatore-degats: 1.0";
                if (comment.contains("multiplicateur-degats: 1.0")) return comment.replace("multiplicateur-degats: 1.0", "moltiplicatore-degats: 1.0");
                if (comment.equals("multiplicateur-degats: 1.2")) return "moltiplicatore-degats: 1.2";
                if (comment.contains("multiplicateur-degats: 1.2")) return comment.replace("multiplicateur-degats: 1.2", "moltiplicatore-degats: 1.2");
                if (comment.equals("multiplicateur-portee-detection: 1.0")) return "rilevamento moltiplicatore-portee: 1.0";
                if (comment.contains("multiplicateur-portee-detection: 1.0")) return comment.replace("multiplicateur-portee-detection: 1.0", "rilevamento moltiplicatore-portee: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0")) return "moltiplicatore-pv: 1.0";
                if (comment.contains("multiplicateur-pv: 1.0")) return comment.replace("multiplicateur-pv: 1.0", "moltiplicatore-pv: 1.0");
                if (comment.equals("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return "multiplicateur-pv: 1.0 # Applicato IN AGGIUNTA ai modificatori bioma/distanza/zona";
                if (comment.contains("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers")) return comment.replace("multiplicateur-pv: 1.0         #   Applied IN ADDITION to biome/distance/zone modifiers", "multiplicateur-pv: 1.0 # Applicato IN AGGIUNTA ai modificatori bioma/distanza/zona");
                if (comment.equals("multiplicateur-pv: 1.5")) return "moltiplicatore-pv: 1.5";
                if (comment.contains("multiplicateur-pv: 1.5")) return comment.replace("multiplicateur-pv: 1.5", "moltiplicatore-pv: 1.5");
                if (comment.equals("multiplicateur-resistance-knockback: 1.0")) return "moltiplicatore-resistenza-knockback: 1.0";
                if (comment.contains("multiplicateur-resistance-knockback: 1.0")) return comment.replace("multiplicateur-resistance-knockback: 1.0", "moltiplicatore-resistenza-knockback: 1.0");
                if (comment.equals("multiplicateur-vitesse: 1.0")) return "moltiplicatore-vitesse: 1.0";
                if (comment.contains("multiplicateur-vitesse: 1.0")) return comment.replace("multiplicateur-vitesse: 1.0", "moltiplicatore-vitesse: 1.0");
                if (comment.equals("peut-spawner-de-jour: false       # true = can spawn during daylight")) return "peut-spawner-de-jour: false # true = può spawnare durante il giorno";
                if (comment.contains("peut-spawner-de-jour: false       # true = can spawn during daylight")) return comment.replace("peut-spawner-de-jour: false       # true = can spawn during daylight", "peut-spawner-de-jour: false # true = può spawnare durante il giorno");
                if (comment.equals("priorite: 0                       # Priority (higher = priority on overlap)")) return "priorità: 0 # Priorità (più alta = priorità in caso di sovrapposizione)";
                if (comment.contains("priorite: 0                       # Priority (higher = priority on overlap)")) return comment.replace("priorite: 0                       # Priority (higher = priority on overlap)", "priorità: 0 # Priorità (più alta = priorità in caso di sovrapposizione)");
                if (comment.equals("resistance-feu: false")) return "resistenza-feu: falso";
                if (comment.contains("resistance-feu: false")) return comment.replace("resistance-feu: false", "resistenza-feu: falso");
                if (comment.equals("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return "resistenza-feu: false # true = immunità al fuoco (effetto pozione permanente)";
                if (comment.contains("resistance-feu: false            # true = fire immunity (permanent potion effect)")) return comment.replace("resistance-feu: false            # true = fire immunity (permanent potion effect)", "resistenza-feu: false # true = immunità al fuoco (effetto pozione permanente)");
                if (comment.equals("tier-equipement: \"none\"")) return "equipaggiamento di livello: \"nessuno\"";
                if (comment.contains("tier-equipement: \"none\"")) return comment.replace("tier-equipement: \"none\"", "equipaggiamento di livello: \"nessuno\"");
                if (comment.equals("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return "equipaggiamento di livello: \"none\" # none | pelle | ferro | diamante | netherite";
                if (comment.contains("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"          # none | leather | iron | diamond | netherite", "equipaggiamento di livello: \"none\" # none | pelle | ferro | diamante | netherite");
                if (comment.equals("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return "equipaggiamento di livello: \"none\" # none | pelle | ferro | diamante | netherite";
                if (comment.contains("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite")) return comment.replace("tier-equipement: \"none\"        # none | leather | iron | diamond | netherite", "equipaggiamento di livello: \"none\" # none | pelle | ferro | diamante | netherite");
                if (comment.equals("type: CUBOID                      # CUBOID or RADIUS")) return "tipo: CUBOIDE # CUBOIDE o RAGGIO";
                if (comment.contains("type: CUBOID                      # CUBOID or RADIUS")) return comment.replace("type: CUBOID                      # CUBOID or RADIUS", "tipo: CUBOIDE # CUBOIDE o RAGGIO");
                if (comment.equals("zone-name:")) return "nome della zona:";
                if (comment.contains("zone-name:")) return comment.replace("zone-name:", "nome della zona:");
                if (comment.equals("zone-sure: false                  # true = disables hostile spawns in zone")) return "zone-sure: false # true = disabilita gli spawn ostili nella zona";
                if (comment.contains("zone-sure: false                  # true = disables hostile spawns in zone")) return comment.replace("zone-sure: false                  # true = disables hostile spawns in zone", "zone-sure: false # true = disabilita gli spawn ostili nella zona");
            }
        }
        return comment;
    }

    public void extractLang(String langCode) {
        String resourcePath = "lang/" + langCode + ".yml";
        File dest = new File(plugin.getDataFolder(), "lang.yml");

        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) {
                logger.warning(ConsoleColor.WARN_PREFIX + "Fichier de langue introuvable dans le JAR : " + resourcePath + ". Utilisation du fallback fr.yml");
                try (InputStream fallback = plugin.getResource("lang/fr.yml")) {
                    if (fallback != null) Files.copy(fallback, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            }
            Files.copy(is, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.severe(ConsoleColor.WARN_PREFIX + "Erreur lors de l'extraction du fichier de langue : " + e.getMessage());
        }
    }

    public void changeLanguage(String langCode) {
        changeLanguage(langCode, null);
    }

    public void changeLanguage(String langCode, CommandSender sender) {
        if (!LANGUAGES.containsKey(langCode)) {
            if (sender != null) {
                sender.sendMessage(plugin.getLangManager().get("general.invalid_language", Map.of("code", langCode)));
            } else {
                logger.warning(ConsoleColor.WARN_PREFIX + "Code de langue inconnu : " + langCode);
            }
            return;
        }

        plugin.getConfig().set("plugin.language", langCode);
        plugin.saveConfig();

        extractLang(langCode);
        plugin.getLangManager().load();
        updateConfigFileComments(langCode);

        String langName = LANGUAGES.get(langCode);
        String msg = plugin.getLangManager().get("general.language_changed", Map.of("lang", langName + " (" + langCode + ")"));
        if (sender != null) {
            sender.sendMessage(msg);
        }
        logger.info(ConsoleColor.INFO_PREFIX + "Langue définie sur : " + langName + " (" + langCode + ")");

        // Rafraîchit l'interface GUI des joueurs ayant un menu ouvert
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof fr.wilddifficulty.gui.WDMenuHolder holder) {
                    refreshPlayerGui(player, holder);
                }
            }
        }
    }

    private void refreshPlayerGui(Player player, fr.wilddifficulty.gui.WDMenuHolder holder) {
        String type = holder.getMenuType();
        if (type == null) return;

        switch (type) {
            case "MAIN" -> plugin.getGuiManager().openMainMenu(player);
            case "GENERAL_CONFIG" -> plugin.getGuiManager().openGeneralConfig(player);
            case "LANGUAGE_SELECT" -> plugin.getGuiManager().openLanguageSelector(player);
            case "ADMIN_TOOLS" -> plugin.getGuiManager().openAdminToolsMenu(player);
            case "BLOODMOON_EDIT" -> plugin.getGuiManager().openBloodMoonEditor(player);
            case "VARIANTS_LIST" -> plugin.getGuiManager().openVariantList(player);
            case "SQUADS_LIST" -> plugin.getGuiManager().openSquadList(player);
            case "ZONES_LIST" -> plugin.getGuiManager().openZoneList(player);
            default -> {
                if (type.startsWith("ZONE_EDIT:")) {
                    plugin.getGuiManager().openZoneEditor(player, type.substring("ZONE_EDIT:".length()));
                } else if (type.startsWith("VARIANT_EDIT:")) {
                    plugin.getGuiManager().openVariantEditor(player, type.substring("VARIANT_EDIT:".length()));
                } else if (type.startsWith("BIOME_CONFIG:")) {
                    plugin.getGuiManager().openBiomeSpawnConfig(player, type.substring("BIOME_CONFIG:".length()));
                }
            }
        }
    }

    public static Map<String, String> getAvailableLanguages() {
        return LANGUAGES;
    }
}
