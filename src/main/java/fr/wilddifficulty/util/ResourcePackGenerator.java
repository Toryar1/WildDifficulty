package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.variant.MobVariant;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackGenerator {

    public static void generate(WildDifficultyPlugin plugin) {
        File skinsDir = new File(plugin.getDataFolder(), "skins");
        if (!skinsDir.exists() || !skinsDir.isDirectory()) {
            return;
        }

        File rpDir = new File(plugin.getDataFolder(), "resourcepack_temp");
        if (rpDir.exists()) {
            deleteDirectory(rpDir);
        }
        rpDir.mkdirs();

        // 1. Create pack.mcmeta
        try {
            File mcmeta = new File(rpDir, "pack.mcmeta");
            String metaContent = "{\n" +
                    "  \"pack\": {\n" +
                    "    \"pack_format\": 15,\n" +
                    "    \"description\": \"WildDifficulty Mobs Resource Pack\"\n" +
                    "  }\n" +
                    "}";
            Files.writeString(mcmeta.toPath(), metaContent);
        } catch (Exception e) {
            plugin.getLogger().warning("[WD] Impossible de creer pack.mcmeta : " + e.getMessage());
            return;
        }

        // 2. Group variants by entity type
        Map<EntityType, List<MobVariant>> variantsByMob = new HashMap<>();
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            File textureFile = new File(skinsDir, var.getId() + ".png");
            if (textureFile.exists()) {
                variantsByMob.computeIfAbsent(var.getType(), k -> new ArrayList<>()).add(var);
            }
        }

        if (variantsByMob.isEmpty()) {
            plugin.getLogger().info("[WD] Aucune texture PNG trouvee dans skins/ correspondant a une variante. Pas de resource pack genere.");
            return;
        }

        // 3. Generate OptiFine random entity structure
        File optifineDir = new File(rpDir, "assets/minecraft/optifine/random/entity");
        optifineDir.mkdirs();

        for (Map.Entry<EntityType, List<MobVariant>> entry : variantsByMob.entrySet()) {
            EntityType type = entry.getKey();
            List<MobVariant> list = entry.getValue();
            String folderName = type.name().toLowerCase();

            File mobFolder = new File(optifineDir, folderName);
            mobFolder.mkdirs();

            StringBuilder properties = new StringBuilder();
            properties.append("# Genere par WildDifficulty pour ").append(type.name()).append("\n");

            int index = 2; // Index starts at 2 (1 is default vanilla)
            for (MobVariant var : list) {
                File srcPng = new File(skinsDir, var.getId() + ".png");
                File destPng = new File(mobFolder, folderName + index + ".png");
                try {
                    Files.copy(srcPng.toPath(), destPng.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    String cleanName = ChatColor.stripColor(var.getDisplayName());
                    if (cleanName == null || cleanName.isEmpty()) {
                        cleanName = var.getId();
                    }

                    properties.append("skins.").append(index).append("=").append(index).append("\n");
                    properties.append("name.").append(index).append("=ipattern:*").append(cleanName).append("*\n\n");

                    index++;
                } catch (Exception e) {
                    plugin.getLogger().warning("[WD] Impossible de copier la texture pour " + var.getId() + " : " + e.getMessage());
                }
            }

            if (index > 2) {
                try {
                    File propFile = new File(optifineDir, folderName + ".properties");
                    Files.writeString(propFile.toPath(), properties.toString());
                } catch (Exception e) {
                    plugin.getLogger().warning("[WD] Impossible de creer le fichier properties pour " + folderName + " : " + e.getMessage());
                }
            }
        }

        // 4. Zip the resource pack
        File zipFile = new File(plugin.getDataFolder(), "resourcepack.zip");
        if (zipFile.exists()) {
            zipFile.delete();
        }

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(rpDir, rpDir, zos);
            plugin.getLogger().info("[WD] Resource pack genere avec succes : " + zipFile.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().warning("[WD] Erreur lors du zipping du resource pack : " + e.getMessage());
        }

        // 5. Clean up temp folder
        deleteDirectory(rpDir);
    }

    private static void zipDirectory(File root, File source, ZipOutputStream zos) throws IOException {
        File[] files = source.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[1024];
        for (File f : files) {
            if (f.isDirectory()) {
                zipDirectory(root, f, zos);
            } else {
                String relativePath = root.toPath().relativize(f.toPath()).toString().replace("\\", "/");
                ZipEntry ze = new ZipEntry(relativePath);
                zos.putNextEntry(ze);
                try (FileInputStream fis = new FileInputStream(f)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
}
