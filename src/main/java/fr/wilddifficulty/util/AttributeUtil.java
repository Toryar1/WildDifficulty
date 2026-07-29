package fr.wilddifficulty.util;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class AttributeUtil {

    private static NamespacedKey KEY_HEALTH;
    private static NamespacedKey KEY_DAMAGE;
    private static NamespacedKey KEY_SPEED;
    private static NamespacedKey KEY_FOLLOW_RANGE;
    private static NamespacedKey KEY_KNOCKBACK;
    private static NamespacedKey KEY_SCALE;

    private AttributeUtil() {}

    public static void init(Plugin plugin) {
        KEY_HEALTH       = new NamespacedKey(plugin, "wd_health");
        KEY_DAMAGE       = new NamespacedKey(plugin, "wd_damage");
        KEY_SPEED        = new NamespacedKey(plugin, "wd_speed");
        KEY_FOLLOW_RANGE = new NamespacedKey(plugin, "wd_follow_range");
        KEY_KNOCKBACK    = new NamespacedKey(plugin, "wd_knockback");
        KEY_SCALE        = new NamespacedKey(plugin, "wd_scale");
    }

    /**
     * Applique une valeur absolue à un attribut d'entité.
     * Calcule la différence (valeur_cible - valeur_de_base) et applique un modificateur ADD_NUMBER.
     */
    public static void applyAbsolute(LivingEntity entity, Attribute attribute, NamespacedKey key, double targetValue) {
        if (targetValue < 0 || key == null) return;
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;

        // Supprime le modificateur existant pour cette clé
        instance.getModifiers().stream()
                .filter(m -> m.getKey().equals(key))
                .findFirst()
                .ifPresent(instance::removeModifier);

        double base = instance.getBaseValue();
        double amount = targetValue - base;

        AttributeModifier mod = new AttributeModifier(
                key,
                amount,
                AttributeModifier.Operation.ADD_NUMBER
        );
        instance.addModifier(mod);
    }

    public static void applyHealth(LivingEntity entity, double targetValue) {
        if (targetValue <= 0 || KEY_HEALTH == null) return;
        applyAbsolute(entity, Attribute.MAX_HEALTH, KEY_HEALTH, targetValue);
        // Synchroniser la santé
        AttributeInstance maxHp = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            entity.setHealth(maxHp.getValue());
        }
    }

    public static void applyDamage(LivingEntity entity, double targetValue) {
        if (KEY_DAMAGE == null) return;
        applyAbsolute(entity, Attribute.ATTACK_DAMAGE, KEY_DAMAGE, targetValue);
    }

    public static void applySpeed(LivingEntity entity, double targetValue) {
        if (KEY_SPEED == null) return;
        applyAbsolute(entity, Attribute.MOVEMENT_SPEED, KEY_SPEED, targetValue);
    }

    public static void applyFollowRange(LivingEntity entity, double targetValue) {
        if (KEY_FOLLOW_RANGE == null) return;
        applyAbsolute(entity, Attribute.FOLLOW_RANGE, KEY_FOLLOW_RANGE, targetValue);
    }

    public static void applyKnockbackResistance(LivingEntity entity, double targetValue) {
        if (KEY_KNOCKBACK == null) return;
        applyAbsolute(entity, Attribute.KNOCKBACK_RESISTANCE, KEY_KNOCKBACK, targetValue);
    }

    public static void applyScale(LivingEntity entity, double targetValue) {
        if (targetValue <= 0 || KEY_SCALE == null) return;
        try {
            applyAbsolute(entity, Attribute.SCALE, KEY_SCALE, targetValue);
        } catch (Throwable t) {
            // Ignoré si l'attribut n'existe pas
        }
    }
}
