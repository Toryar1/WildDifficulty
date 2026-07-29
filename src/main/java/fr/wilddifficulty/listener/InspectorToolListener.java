package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InspectorToolListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public InspectorToolListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != org.bukkit.Material.STICK) return;
        if (!item.hasItemMeta()) return;

        org.bukkit.NamespacedKey toolKey = new org.bukkit.NamespacedKey(plugin, "wd_tool_type");
        String toolType = item.getItemMeta().getPersistentDataContainer().get(toolKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (!"inspector".equals(toolType)) {
            if (!item.getItemMeta().hasDisplayName()) return;
            String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
            if (!plainName.contains("Inspecteur") && !plainName.contains("Inspector")) return;
        }

        event.setCancelled(true);

        if (!(event.getRightClicked() instanceof LivingEntity victim)) {
            player.sendMessage("§c[WD] L'entité ciblée n'est pas vivante.");
            return;
        }

        // Gather details
        String baseType = victim.getType().name();
        String variantId = "§7Aucune (Vanilla)";
        String displayName = victim.getCustomName() != null ? victim.getCustomName() : victim.getName();
        
        double currentHp = victim.getHealth();
        double maxHp = currentHp;
        AttributeInstance maxHpAttr = victim.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHp = maxHpAttr.getValue();
        }

        double damage = 0.0;
        AttributeInstance dmgAttr = victim.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            damage = dmgAttr.getValue();
        }

        double speed = 0.0;
        AttributeInstance spdAttr = victim.getAttribute(Attribute.MOVEMENT_SPEED);
        if (spdAttr != null) {
            speed = spdAttr.getValue();
        }

        double followRange = 0.0;
        AttributeInstance frAttr = victim.getAttribute(Attribute.FOLLOW_RANGE);
        if (frAttr != null) {
            followRange = frAttr.getValue();
        }

        double kbRes = 0.0;
        AttributeInstance kbAttr = victim.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbRes = kbAttr.getValue();
        }

        String varId = null;
        if (victim.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
            varId = victim.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
            variantId = "§a" + varId;
        }

        Location loc = victim.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        DifficultyZone zone = plugin.getZoneManager().getZoneAt(worldName, loc.getX(), loc.getY(), loc.getZ());
        boolean isInside = true;
        if (zone == null) {
            zone = plugin.getZoneManager().getExternalScalingZone(worldName, loc.getX(), loc.getZ());
            isInside = false;
        }
        String zoneStr = zone != null ? "§d" + zone.getId() + (isInside ? " (Dedans)" : " (Hors Zone)") : "§7Aucune";

        // Let's retrieve modifiers if it is a variant
        double distMult = plugin.getMainConfigManager().computeDistanceMultiplier(worldName, loc.getX(), loc.getZ());

        player.sendMessage("§e§m----------------------------------------");
        player.sendMessage("§6§lWildDifficulty - Inspecteur de Mob");
        player.sendMessage("§fType de Base: §e" + baseType);
        player.sendMessage("§fNom/Affichage: §f" + displayName);
        player.sendMessage("§fVariante: " + variantId);
        player.sendMessage("§fZone: " + zoneStr);
        player.sendMessage("§e§lStatistiques en Jeu :");
        player.sendMessage("§f  - PV: §a" + String.format("%.1f", currentHp) + "§7/§a" + String.format("%.1f", maxHp));
        player.sendMessage("§f  - Dégâts: §c" + String.format("%.1f", damage));
        player.sendMessage("§f  - Vitesse: §b" + String.format("%.3f", speed));
        player.sendMessage("§f  - Portée de détection: §e" + String.format("%.1f", followRange) + " blocs");
        player.sendMessage("§f  - Résistance Knockback: §7" + String.format("%.1f", kbRes));
        player.sendMessage("§e§lModificateurs de Distance :");
        player.sendMessage("§f  - Mult. Distance Central: §e" + String.format("%.2f", distMult) + "x");
        
        if (zone != null) {
            double dx = loc.getX() - zone.getCenterX();
            double dz = loc.getZ() - zone.getCenterZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            player.sendMessage("§f  - Distance centre Zone: §b" + String.format("%.1f", distance) + "m");
            double hpMult = 1.0;
            double dmgMult = 1.0;
            double spdMult = 1.0;
            if (distance > zone.getRadius() && zone.hasExtScaling()) {
                hpMult = zone.computeHpExtMult(distance);
                dmgMult = zone.computeDmgExtMult(distance);
                spdMult = zone.computeSpdExtMult(distance);
            }
            player.sendMessage("§f  - Mult. Extérieur PV: §c" + String.format("%.2f", hpMult) + "x");
            player.sendMessage("§f  - Mult. Extérieur Dégâts: §6" + String.format("%.2f", dmgMult) + "x");
            player.sendMessage("§f  - Mult. Extérieur Vitesse: §b" + String.format("%.2f", spdMult) + "x");
            player.sendMessage("§f  - Cap max global: §f" + zone.getExtMaxMult() + "x");
        }

        if (varId != null) {
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null && var.getModifiers() != null) {
                StatModifiers mods = var.getModifiers();
                player.sendMessage("§e§lPropriétés de la Variante :");
                if (mods.getRegenerationValue() > 0) {
                    player.sendMessage("§f  - Régénération: §d+" + mods.getRegenerationValue() + " HP/s");
                }
                if (mods.getOnHitPotionEffect() != null && !"none".equalsIgnoreCase(mods.getOnHitPotionEffect())) {
                    player.sendMessage("§f  - Effet au coup donné: §d" + mods.getOnHitPotionEffect() + " (Niv." + (mods.getOnHitPotionAmplifier()+1) + ", " + (mods.getOnHitPotionDuration()/20) + "s, " + String.format("%.0f%%", mods.getOnHitPotionChance()*100) + ")");
                }
                if (mods.getTeleportMaxUses() > 0) {
                    player.sendMessage("§f  - Téléportations max: §e" + mods.getTeleportMaxUses());
                }
                if (mods.getDashMaxUses() > 0) {
                    player.sendMessage("§f  - Dashs max: §e" + mods.getDashMaxUses());
                }
                if (mods.isJumpAttack()) {
                    player.sendMessage("§f  - Attaque sautée: §aOui");
                }
                if (mods.getRangedAttackType() != null && !"none".equalsIgnoreCase(mods.getRangedAttackType())) {
                    player.sendMessage("§f  - Projectile à distance: §a" + mods.getRangedAttackType());
                }
            }
        }

        if (victim.getPersistentDataContainer().has(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING)) {
            String squadId = victim.getPersistentDataContainer().get(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING);
            player.sendMessage("§e§lEscouade: §b" + squadId);
        }

        player.sendMessage("§e§m----------------------------------------");
    }
}
