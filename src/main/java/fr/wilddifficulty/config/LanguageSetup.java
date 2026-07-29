package fr.wilddifficulty.config;

import fr.wilddifficulty.WildDifficultyPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Gère la sélection de la langue au premier démarrage du plugin.
 * Handles language selection on the first plugin startup.
 *
 * Langues disponibles / Available languages:
 *   fr - Français (French)
 *   en - English
 *   de - Deutsch (German)
 *   es - Español (Spanish)
 *   pt_BR - Português Brasileiro (Brazilian Portuguese)
 *   nl - Nederlands (Dutch)
 *   pl - Polski (Polish)
 *   ru - Русский (Russian)
 *   zh_CN - 简体中文 (Chinese Simplified)
 *   it - Italiano (Italian)
 *
 * Les fichiers de langue sont embarqués dans le JAR (répertoire lang/).
 * Language files are bundled inside the JAR (lang/ directory).
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
     * Checks if it's the first startup (lang.yml missing) and triggers selection if needed.
     */
    public void setupIfNeeded() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (langFile.exists()) return; // Already configured

        // Vérifie si une langue est définie dans config.yml
        plugin.reloadConfig();
        String configured = plugin.getConfig().getString("plugin.language", null);
        if (configured != null && LANGUAGES.containsKey(configured)) {
            extractLang(configured);
            return;
        }

        // Affiche le menu de sélection dans la console
        printSelectionMenu();

        // Lecture depuis la console (stdin, bloquant)
        String choice = readConsoleInput();
        String selectedCode = resolveChoice(choice);

        if (selectedCode == null) {
            logger.warning("Choix invalide. Langue par défaut : Français (fr)");
            selectedCode = "fr";
        }

        // Sauvegarde dans config.yml
        plugin.getConfig().set("plugin.language", selectedCode);
        plugin.saveConfig();

        extractLang(selectedCode);
        logger.info("✔ Langue sélectionnée : " + LANGUAGES.get(selectedCode) + " (" + selectedCode + ")");
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
            // Attend jusqu'à 60 secondes pour une entrée console
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
            logger.warning("Impossible de lire la console : " + e.getMessage());
        }
        return null;
    }

    private String resolveChoice(String input) {
        if (input == null) return null;

        // Try direct code
        if (LANGUAGES.containsKey(input)) return input;

        // Try number
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx >= 0 && idx < LANGUAGES.size()) {
                return (String) LANGUAGES.keySet().toArray()[idx];
            }
        } catch (NumberFormatException ignored) {}

        // Try case-insensitive code
        for (String code : LANGUAGES.keySet()) {
            if (code.equalsIgnoreCase(input)) return code;
        }

        return null;
    }

    /**
     * Extrait le fichier de langue depuis le JAR vers le dossier du plugin.
     * Extracts the language file from the JAR to the plugin data folder.
     */
    private void extractLang(String langCode) {
        String resourcePath = "lang/" + langCode + ".yml";
        File dest = new File(plugin.getDataFolder(), "lang.yml");

        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) {
                logger.warning("Fichier de langue introuvable dans le JAR : " + resourcePath + ". Utilisation du fallback fr.yml");
                try (InputStream fallback = plugin.getResource("lang/fr.yml")) {
                    if (fallback != null) Files.copy(fallback, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            }
            Files.copy(is, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.severe("Erreur lors de l'extraction du fichier de langue : " + e.getMessage());
        }
    }

    /**
     * Change la langue et recharge le lang.yml.
     * Changes the language and reloads lang.yml.
     */
    public void changeLanguage(String langCode) {
        if (!LANGUAGES.containsKey(langCode)) {
            logger.warning("Code de langue inconnu : " + langCode);
            return;
        }
        plugin.getConfig().set("plugin.language", langCode);
        plugin.saveConfig();
        File dest = new File(plugin.getDataFolder(), "lang.yml");
        extractLang(langCode);
        plugin.getLangManager().load();
        logger.info("✔ Langue changée : " + LANGUAGES.get(langCode));
    }

    public static Map<String, String> getAvailableLanguages() {
        return LANGUAGES;
    }
}
