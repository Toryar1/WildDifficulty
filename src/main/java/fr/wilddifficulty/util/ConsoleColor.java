package fr.wilddifficulty.util;

/**
 * Utilité de couleurs ANSI pour la console du serveur.
 * Provides ANSI color formatting for professional console logs matching WildTimber style.
 */
public class ConsoleColor {
    public static final String ORANGE  = "\033[38;5;208m";
    public static final String GRAY    = "\033[90m";
    public static final String GREEN   = "\033[92m";
    public static final String RED     = "\033[91m";
    public static final String YELLOW  = "\033[93m";
    public static final String CYAN    = "\033[96m";
    public static final String RESET   = "\033[0m";
    public static final String BOLD    = "\033[1m";

    public static final String PREFIX       = ORANGE + BOLD + "[WildDifficulty]" + RESET + " ";
    public static final String DEBUG_PREFIX = ORANGE + BOLD + "[WildDifficulty]" + RESET + " " + GRAY + "[DEBUG]" + RESET + " ";
    public static final String WARN_PREFIX  = ORANGE + BOLD + "[WildDifficulty]" + RESET + " " + RED + "[WARN]" + RESET + " ";
    public static final String INFO_PREFIX  = ORANGE + BOLD + "[WildDifficulty]" + RESET + " " + GREEN + "[INFO]" + RESET + " ";
}
