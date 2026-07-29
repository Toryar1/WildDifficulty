package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public final class SkinCacheUtil {

    private SkinCacheUtil() {}

    /**
     * Tente de télécharger et de mettre en cache localement le skin PNG sous plugins/WildDifficulty/skins/<variantId>.png
     */
    public static void cacheSkin(WildDifficultyPlugin plugin, String variantId, String skinInput) {
        if (skinInput == null || skinInput.equalsIgnoreCase("none") || skinInput.length() < 10) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String textureUrl = null;

                // 1. Détecter si c'est un lien URL Mojang direct ou autre URL
                if (skinInput.startsWith("http")) {
                    textureUrl = skinInput;
                }
                // 2. Détecter si c'est une chaîne Base64
                else if (skinInput.length() > 100) {
                    try {
                        String decoded = new String(Base64.getDecoder().decode(skinInput));
                        if (decoded.contains("\"url\":")) {
                            textureUrl = decoded.split("\"url\":\"")[1].split("\"")[0];
                        }
                    } catch (Exception ignored) {}
                }
                // 3. Détecter si c'est un UUID ou pseudo (on ne télécharge pas, Mojang s'en charge)
                
                if (textureUrl == null) return;

                File skinsDir = new File(plugin.getDataFolder(), "skins");
                if (!skinsDir.exists()) {
                    skinsDir.mkdirs();
                }

                File destFile = new File(skinsDir, variantId + ".png");
                
                // Téléchargement du PNG
                URL url = new URL(textureUrl);
                try (InputStream in = url.openStream()) {
                    Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                
                if (plugin.getMainConfigManager().isDebug()) {
                    plugin.getLogger().info("[SkinCache] Skin sauvegardé localement pour " + variantId + " dans " + destFile.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[SkinCache] Impossible de télécharger le skin pour " + variantId + " : " + e.getMessage());
            }
        });
    }
}
