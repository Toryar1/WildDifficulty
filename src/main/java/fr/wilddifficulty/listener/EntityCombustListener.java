package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.MobConfigManager;
import fr.wilddifficulty.variant.MobVariant;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;

/**
 * Listener pour empêcher la combustion solaire des mobs configurés.
 *
 * Utilise EntityCombustEvent (et sa sous-classe EntityCombustByBlockEvent
 * qui couvre la combustion par la lumière du soleil) pour annuler la
 * combustion uniquement si le mob est marqué comme immunisé dans mobs.yml.
 *
 * PERFORMANCE : Cet événement n'est déclenché que lors d'une combustion
 * effective — aucune boucle par tick.
 */
public class EntityCombustListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public EntityCombustListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Annule la combustion solaire pour les mobs marqués ignore-soleil.
     * EntityCombustByBlockEvent couvre la combustion par l'exposition au soleil.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityCombustByBlock(EntityCombustByBlockEvent event) {
        checkAndCancelCombustion(event);
    }

    /**
     * Couverture plus large : EntityCombustEvent pour la combustion en général.
     * Utile si la plateforme déclenche EntityCombustEvent au lieu de la sous-classe.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        // Évite le double-traitement si c'est déjà une EntityCombustByBlockEvent
        if (event instanceof EntityCombustByBlockEvent) return;
        checkAndCancelCombustion(event);
    }

    private void checkAndCancelCombustion(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        MobConfigManager mobCfg = plugin.getMobConfigManager();

        String mobKey = entity.getType().name();

        // 1. Check variant ignoreSunlight or DAY time or wd_no_burn metadata
        if (entity.hasMetadata("wd_no_burn")) {
            event.setCancelled(true);
            if (plugin.getMainConfigManager().isDebug()) {
                plugin.getLogger().info("[DEBUG] Combustion annulée pour l'entité (wd_no_burn) : " + entity.getType().name());
            }
            return;
        }

        if (entity.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, org.bukkit.persistence.PersistentDataType.STRING)) {
            String varId = entity.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, org.bukkit.persistence.PersistentDataType.STRING);
            MobVariant var = plugin.getVariantManager().getVariant(varId);
            if (var != null && (var.isIgnoreSunlight() || "DAY".equalsIgnoreCase(var.getSpawnTime()) || "ANY".equalsIgnoreCase(var.getSpawnTime()))) {
                event.setCancelled(true);
                if (plugin.getMainConfigManager().isDebug()) {
                    plugin.getLogger().info("[DEBUG] Combustion annulée pour la variante (DAY/ignore-soleil) : " + var.getId());
                }
                return;
            }
        }

        // 2. Check global mobs.yml ignoreSunlight
        if (mobCfg.ignoreSunlight(mobKey)) {
            event.setCancelled(true);

            if (plugin.getMainConfigManager().isDebug()) {
                plugin.getLogger().info("[DEBUG] Combustion annulée pour " + mobKey);
            }
        }
    }
}
