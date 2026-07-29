package fr.wilddifficulty.config;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.util.ConsoleColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
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
     * Vérifie si c'est le premier démarrage (lang.yml absent) et lance la sélection si nécessaire.
     */
    public void setupIfNeeded() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (langFile.exists()) return;

        plugin.reloadConfig();
        String configured = plugin.getConfig().getString("plugin.language", null);
        if (configured != null && LANGUAGES.containsKey(configured)) {
            extractLang(configured);
            return;
        }

        printSelectionMenu();

        String choice = readConsoleInput();
        String selectedCode = resolveChoice(choice);

        if (selectedCode == null) {
            logger.warning(ConsoleColor.WARN_PREFIX + "Choix invalide. Langue par défaut : Français (fr)");
            selectedCode = "fr";
        }

        plugin.getConfig().set("plugin.language", selectedCode);
        plugin.saveConfig();

        extractLang(selectedCode);
        logger.info(ConsoleColor.INFO_PREFIX + "Langue sélectionnée : " + LANGUAGES.get(selectedCode) + " (" + selectedCode + ")");
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
