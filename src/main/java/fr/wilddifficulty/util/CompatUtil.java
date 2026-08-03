package fr.wilddifficulty.util;

import org.bukkit.Particle;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;

/**
 * Cross-version compatibility helper for Minecraft 1.19 through 1.21.4 / Paper 26.2.
 */
public final class CompatUtil {

    private CompatUtil() {}

    public static PotionEffectType getPotionEffectType(String... names) {
        for (String name : names) {
            if (name == null) continue;
            try {
                PotionEffectType type = PotionEffectType.getByName(name.toUpperCase());
                if (type != null) return type;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static Enchantment getEnchantment(String... names) {
        for (String name : names) {
            if (name == null) continue;
            try {
                Enchantment ench = Enchantment.getByName(name.toUpperCase());
                if (ench != null) return ench;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static EntityType getEntityType(String... names) {
        for (String name : names) {
            if (name == null) continue;
            try {
                EntityType type = EntityType.valueOf(name.toUpperCase());
                if (type != null) return type;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static Particle getParticle(String... names) {
        for (String name : names) {
            if (name == null) continue;
            try {
                Particle p = Particle.valueOf(name.toUpperCase());
                if (p != null) return p;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static String getBiomeKeyString(Biome biome) {
        if (biome == null) return "";
        try {
            Method getKeyMethod = biome.getClass().getMethod("getKey");
            Object key = getKeyMethod.invoke(biome);
            if (key != null) return key.toString();
        } catch (Throwable ignored) {}
        return biome.name().toLowerCase();
    }

    public static String getPotionTypeName(PotionMeta meta) {
        if (meta == null) return "";
        try {
            Method getBaseType = PotionMeta.class.getMethod("getBasePotionType");
            Object type = getBaseType.invoke(meta);
            if (type != null) return type.toString();
        } catch (Throwable ignored) {}

        try {
            Method getBaseData = PotionMeta.class.getMethod("getBasePotionData");
            Object data = getBaseData.invoke(meta);
            if (data != null) {
                Method getType = data.getClass().getMethod("getType");
                Object type = getType.invoke(data);
                if (type != null) return type.toString();
            }
        } catch (Throwable ignored) {}

        return "";
    }
}
