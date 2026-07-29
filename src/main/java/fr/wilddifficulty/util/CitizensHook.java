package fr.wilddifficulty.util;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.metadata.FixedMetadataValue;
import fr.wilddifficulty.listener.MobSpawnListener;

public class CitizensHook {

    public static Entity spawnNPCPlayer(String variantId, Location loc, String name, String skin, double health, double damage, double speed, org.bukkit.plugin.Plugin plugin) {
        try {
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            String finalName = (name != null && !name.isEmpty()) ? name : "Monstre";
            NPC npc = registry.createNPC(EntityType.PLAYER, finalName);

            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
            if (skin != null && !"none".equalsIgnoreCase(skin) && !skin.isEmpty()) {
                skinTrait.setSkinName(skin, true);
            } else {
                skinTrait.setFetchDefaultSkin(false);
            }

            npc.data().setPersistent("wd_variant_id", variantId);
            npc.data().setPersistent("wd_npc", true);

            npc.setProtected(false);
            try {
                Class<?> traitClass = Class.forName("net.citizensnpcs.trait.LookClose");
                net.citizensnpcs.api.trait.Trait lookCloseTrait = npc.getOrAddTrait((Class<? extends net.citizensnpcs.api.trait.Trait>) traitClass);
                lookCloseTrait.getClass().getMethod("lookClose", boolean.class).invoke(lookCloseTrait, true);
            } catch (Throwable ignored) {}

            try {
                Class<?> targetableClass = Class.forName("net.citizensnpcs.api.trait.trait.Targetable");
                net.citizensnpcs.api.trait.Trait targetableTrait = npc.getOrAddTrait((Class<? extends net.citizensnpcs.api.trait.Trait>) targetableClass);
                targetableClass.getMethod("setTargetable", boolean.class).invoke(targetableTrait, false);
            } catch (Throwable ignored) {}

            npc.spawn(loc);

            Entity spawned = npc.getEntity();
            if (spawned instanceof LivingEntity le) {
                le.getPersistentDataContainer().set(MobSpawnListener.KEY_VARIANT_ID, org.bukkit.persistence.PersistentDataType.STRING, variantId);
                // Apply variant equipment
                if (plugin instanceof fr.wilddifficulty.WildDifficultyPlugin mainPlugin) {
                    fr.wilddifficulty.variant.MobVariant var = mainPlugin.getVariantManager().getVariant(variantId);
                    if (var != null && var.getModifiers() != null) {
                        fr.wilddifficulty.util.EquipmentUtil.applyEquipment(le, var.getModifiers(), var.getCustomModelData());
                    }
                }
                try {
                    if (le.getAttribute(Attribute.MAX_HEALTH) != null) {
                        le.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
                        le.setHealth(health);
                    }
                } catch (Exception ignored) {}
                try {
                    if (le.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                        le.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
                    }
                } catch (Exception ignored) {}
                try {
                    if (le.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                        le.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
                    }
                } catch (Exception ignored) {}
                le.setMetadata("wd_npc", new FixedMetadataValue(plugin, true));
                le.setMetadata("no-stack", new FixedMetadataValue(plugin, true));
            }
            return spawned;
        } catch (Throwable t) {
            plugin.getLogger().warning("[WD] Erreur Citizens spawn: " + t.getMessage());
            return null;
        }
    }

    public static void navigateNPC(Entity entity, LivingEntity target) {
        try {
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
            if (npc != null) {
                if (target != null) {
                    npc.getNavigator().getLocalParameters().speedModifier(1.4f);
                    npc.getNavigator().setTarget(target, true);
                    
                    if (npc.getEntity() instanceof Player p) {
                        p.setSprinting(true);
                    }

                    // Face the target's eye location to prevent looking at the ground
                    try {
                        npc.faceLocation(target.getEyeLocation());
                    } catch (Throwable ignored) {}
                } else {
                    npc.getNavigator().cancelNavigation();
                    if (npc.getEntity() instanceof Player p) {
                        p.setSprinting(false);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void destroyNPC(Entity entity) {
        try {
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
            if (npc != null) {
                npc.destroy();
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isNPC(Entity entity) {
        try {
            return CitizensAPI.getNPCRegistry().isNPC(entity);
        } catch (Throwable t) {
            return false;
        }
    }
}
