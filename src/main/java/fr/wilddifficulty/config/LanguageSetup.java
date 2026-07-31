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
        // Map common inline comments per language
        Map<String, Map<String, String>> dict = Map.of(
            "fr", Map.of(
                "Enable language code", "Code de langue activé",
                "Enable debug mode", "Activer le mode debug",
                "Maximum spawn distance", "Distance maximale de spawn",
                "Base Global Modifiers", "Modificateurs globaux de base",
                "Distance Scaling", "Scaling par distance",
                "Cap of variants per player", "Cap de variantes par joueur",
                "Individual configuration per vanilla mob type", "Configuration individuelle par type de mob vanilla",
                "Per-biome difficulty rules", "Règles de difficulté par biome",
                "Custom in-game difficulty zones", "Zones de difficulté personnalisées créées en jeu",
                "Monster variants and squads configuration", "Configuration des variantes de monstres et des escouades"
            ),
            "es", Map.of(
                "Enable language code", "Código de idioma habilitado",
                "Enable debug mode", "Activar el modo depuración",
                "Maximum spawn distance", "Distancia máxima de generación",
                "Base Global Modifiers", "Modificadores globales base",
                "Distance Scaling", "Escalado por distancia",
                "Cap of variants per player", "Límite de variantes por jugador"
            ),
            "de", Map.of(
                "Enable language code", "Aktivierter Sprachcode",
                "Enable debug mode", "Debug-Modus aktivieren",
                "Maximum spawn distance", "Maximale Spawn-Distanz",
                "Base Global Modifiers", "Globale Basismodifikatoren",
                "Distance Scaling", "Entfernungsskalierung",
                "Cap of variants per player", "Variantenlimit pro Spieler"
            )
        );

        Map<String, String> langDict = dict.getOrDefault(langCode.toLowerCase(), Map.of());
        for (Map.Entry<String, String> entry : langDict.entrySet()) {
            if (comment.contains(entry.getKey())) {
                return comment.replace(entry.getKey(), entry.getValue());
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
