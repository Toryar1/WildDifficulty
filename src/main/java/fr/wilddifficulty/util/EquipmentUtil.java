package fr.wilddifficulty.util;

import fr.wilddifficulty.config.StatModifiers;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public final class EquipmentUtil {

    private static final Random RANDOM = new Random();

    private EquipmentUtil() {}

    public static void applyEquipment(LivingEntity entity, StatModifiers modifiers, int customModelData) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        // 1. Generic Tier Equipment
        String tier = modifiers.getEquipmentTier();
        double chance = modifiers.getEquipmentChance();

        if (!"none".equalsIgnoreCase(tier) && chance > 0.0 && RANDOM.nextDouble() <= chance) {
            Material[] armorSet = getArmorSet(tier);
            if (armorSet != null) {
                if (RANDOM.nextBoolean()) eq.setHelmet(new ItemStack(armorSet[0]));
                if (RANDOM.nextBoolean()) eq.setChestplate(new ItemStack(armorSet[1]));
                if (RANDOM.nextBoolean()) eq.setLeggings(new ItemStack(armorSet[2]));
                if (RANDOM.nextBoolean()) eq.setBoots(new ItemStack(armorSet[3]));
            }

            if (("diamond".equalsIgnoreCase(tier) || "netherite".equalsIgnoreCase(tier))
                    && RANDOM.nextDouble() < 0.10) {
                ItemStack chestplate = eq.getChestplate();
                if (chestplate != null && chestplate.getType() != Material.AIR) {
                    chestplate.addUnsafeEnchantment(Enchantment.PROTECTION, 1);
                    eq.setChestplate(chestplate);
                }
            }
        }

        // 2. Specific Item Slots (Override or add)
        applySlot(eq, modifiers.getHelmetItem(), modifiers.getHelmetChance(), 0, modifiers.getHelmetColor());
        applySlot(eq, modifiers.getChestplateItem(), modifiers.getChestplateChance(), 1, modifiers.getChestplateColor());
        applySlot(eq, modifiers.getLeggingsItem(), modifiers.getLeggingsChance(), 2, modifiers.getLeggingsColor());
        applySlot(eq, modifiers.getBootsItem(), modifiers.getBootsChance(), 3, modifiers.getBootsColor());
        applySlot(eq, modifiers.getMainHandItem(), modifiers.getMainHandChance(), 4, "none");
        applySlot(eq, modifiers.getOffHandItem(), modifiers.getOffHandChance(), 5, "none");

        // 3. Custom Model Data for entity skin representation (head slot)
        if (customModelData > 0) {
            ItemStack helmet = eq.getHelmet();
            if (helmet == null || helmet.getType() == Material.AIR) {
                helmet = new ItemStack(Material.LEATHER_HELMET);
            }
            org.bukkit.inventory.meta.ItemMeta meta = helmet.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                helmet.setItemMeta(meta);
            }
            eq.setHelmet(helmet);
        }

        // 4. Custom player head skin (skullSkin)
        String skullSkin = modifiers.getSkullSkin();
        if (skullSkin != null && !"none".equalsIgnoreCase(skullSkin)) {
            eq.setHelmet(createSkull(skullSkin));
        }

        // Taux de drop nul par défaut pour ne pas surcharger l'économie
        if (!(eq instanceof org.bukkit.inventory.PlayerInventory)) {
            try {
                eq.setHelmetDropChance(0.0f);
                eq.setChestplateDropChance(0.0f);
                eq.setLeggingsDropChance(0.0f);
                eq.setBootsDropChance(0.0f);
                eq.setItemInMainHandDropChance(0.0f);
                eq.setItemInOffHandDropChance(0.0f);
            } catch (UnsupportedOperationException ignored) {}
        }
    }

    private static void applySlot(EntityEquipment eq, String itemName, double chance, int slot, String colorHex) {
        if ("none".equalsIgnoreCase(itemName) || chance <= 0.0) return;
        if (RANDOM.nextDouble() > chance) return;
        Material mat = Material.matchMaterial(itemName.toUpperCase());
        if (mat == null) return;
        ItemStack item = new ItemStack(mat);
        if (itemName.toUpperCase().startsWith("LEATHER_") && colorHex != null && !"none".equalsIgnoreCase(colorHex)) {
            item = dyeLeather(item, colorHex);
        }
        switch (slot) {
            case 0 -> eq.setHelmet(item);
            case 1 -> eq.setChestplate(item);
            case 2 -> eq.setLeggings(item);
            case 3 -> eq.setBoots(item);
            case 4 -> eq.setItemInMainHand(item);
            case 5 -> eq.setItemInOffHand(item);
        }
    }

    private static ItemStack dyeLeather(ItemStack item, String hexColor) {
        if (hexColor == null || "none".equalsIgnoreCase(hexColor)) return item;
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            try {
                String cleaned = hexColor.replace("#", "");
                int rgb = Integer.parseInt(cleaned, 16);
                meta.setColor(org.bukkit.Color.fromRGB(rgb));
                item.setItemMeta(meta);
            } catch (NumberFormatException ignored) {}
        }
        return item;
    }

    private static Material[] getArmorSet(String tier) {
        return switch (tier.toLowerCase()) {
            case "leather"   -> new Material[]{
                    Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                    Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS};
            case "iron"      -> new Material[]{
                    Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                    Material.IRON_LEGGINGS, Material.IRON_BOOTS};
            case "diamond"   -> new Material[]{
                    Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS};
            case "netherite" -> new Material[]{
                    Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS};
            default -> null;
        };
    }

    public static ItemStack createSkull(String skullSkin) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            if (skullSkin != null && !"none".equalsIgnoreCase(skullSkin)) {
                if (skullSkin.length() > 30) {
                    org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(java.util.UUID.randomUUID(), "CustomSkin");
                    try {
                        String url = skullSkin;
                        if (!url.startsWith("http")) {
                            if (url.length() == 64) {
                                url = "http://textures.minecraft.net/texture/" + url;
                            } else {
                                try {
                                    String decoded = new String(java.util.Base64.getDecoder().decode(skullSkin));
                                    if (decoded.contains("\"url\":")) {
                                        url = decoded.split("\"url\":\"")[1].split("\"")[0];
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        profile.getTextures().setSkin(new java.net.URL(url));
                    } catch (Exception ex) {
                        // fallback
                    }
                    skullMeta.setPlayerProfile((com.destroystokyo.paper.profile.PlayerProfile) profile);
                } else {
                    skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(skullSkin));
                }
            }
            skull.setItemMeta(skullMeta);
        }
        return skull;
    }
}
