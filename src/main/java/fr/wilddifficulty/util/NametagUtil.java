package fr.wilddifficulty.util;

import fr.wilddifficulty.variant.MobVariant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public final class NametagUtil {

    private NametagUtil() {}

    /**
     * Applique un nametag à une entité en remplaçant les placeholders.
     */
    public static void applyNametag(LivingEntity entity, String format, int niveau, MobVariant variant) {
        if (format == null || format.isBlank()) return;

        // Si l'entité est un animal / entité passive et qu'un joueur lui a mis une étiquette (Name Tag), préserver son nom
        if (!(entity instanceof org.bukkit.entity.Monster) && !entity.getPersistentDataContainer().has(fr.wilddifficulty.listener.MobSpawnListener.KEY_VARIANT_ID, org.bukkit.persistence.PersistentDataType.STRING)) {
            if (entity.getCustomName() != null) {
                entity.setCustomNameVisible(true);
                return;
            }
        }

        int pvActuels = (int) entity.getHealth();
        int pvMax = pvActuels;
        AttributeInstance maxHpAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            pvMax = (int) maxHpAttr.getValue();
        }

        String nomAffiche = null;
        if (variant != null) {
            nomAffiche = variant.getDisplayName();
            if (variant.getConditionalNames() != null && !variant.getConditionalNames().isEmpty()) {
                double pct = (double) pvActuels / pvMax;
                double lowestMatchedThreshold = Double.MAX_VALUE;
                for (MobVariant.ConditionalName cn : variant.getConditionalNames()) {
                    if (pct <= cn.getThreshold() && cn.getThreshold() < lowestMatchedThreshold) {
                        lowestMatchedThreshold = cn.getThreshold();
                        nomAffiche = cn.getName();
                    }
                }
            }
        }

        if (nomAffiche == null) {
            String nomVanilla = entity.getType().name().toLowerCase().replace("_", " ");
            nomAffiche = Character.toUpperCase(nomVanilla.charAt(0)) + nomVanilla.substring(1);
        }

        String texte = format
                .replaceAll("(?i)\\[Niv\\.?\\s*\\{niveau}\\s*\\]", "")
                .replaceAll("(?i)\\[Niv\\.?\\s*\\]", "")
                .replaceAll("(?i)Niv\\.?\\s*\\{niveau}", "")
                .replaceAll("(?i)Niv\\.?", "")
                .replaceAll("(?i)\\[Lvl\\.?\\s*\\{niveau}\\s*\\]", "")
                .replaceAll("(?i)\\[Lvl\\.?\\s*\\]", "")
                .replaceAll("(?i)Lvl\\.?\\s*\\{niveau}", "")
                .replaceAll("(?i)Lvl\\.?", "")
                .replace("{niveau}", "")
                .replace("{nom}", nomAffiche)
                .replace("{pv}", String.valueOf(pvActuels))
                .replace("{pv_max}", String.valueOf(pvMax));

        // Remplacer les doubles espaces par un seul espace
        texte = texte.replaceAll("\\s+", " ");

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(texte.trim());

        entity.customName(component);
        entity.setCustomNameVisible(false); // BossBar-only: never show floating name above mob head
    }

    public static int computeLevel(double healthMult) {
        return (int) Math.max(0, Math.round((healthMult - 1.0) * 10));
    }
}
