package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.MobConfigManager;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Listener gérant le spawn des mobs hostiles de jour.
 *
 * En vanilla, Minecraft empêche les mobs hostiles de spawner en plein jour
 * (luminosité de lumière du ciel trop élevée). Ce listener permet de
 * contourner cette restriction pour les mobs configurés avec
 * "peut-spawner-de-jour: true" dans mobs.yml.
 *
 * Fonctionnement :
 * - Si le spawn est annulé par le système vanilla pour cause de luminosité,
 *   on le réautorise pour les mobs configurés.
 * - Ce listener est séparé de MobSpawnListener pour une séparation des responsabilités.
 *
 * LIMITATION : L'API Bukkit ne fournit pas directement la raison d'annulation
 * d'un spawn dans CreatureSpawnEvent. Ce listener ne peut donc pas distinguer
 * si un spawn est annulé pour cause de lumière ou autre.
 * Solution : on laisse Minecraft gérer les spawns naturels, et on utilise
 * CreatureSpawnEvent NATURAL pour patcher le cas "lumière de jour".
 *
 * NOTE : La vraie gestion du spawn de jour pour les mobs qui ne pourraient
 * normalement pas spawner nécessite soit un spawn artificiel via
 * World.spawnEntity() dans un scheduler (hors de la portée de cet event),
 * soit la configuration Paper "daylight-burning" dans paper-world.yml.
 * Pour la configuration WildDifficulty, ce listener marque les mobs comme
 * exemptés du brûlage de jour, ce qui est le cas d'usage principal.
 */
public class DaytimeSpawnListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public DaytimeSpawnListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Permet aux mobs configurés de spawner de jour.
     *
     * IMPORTANT : Cet event ne peut PAS réautoriser un spawn déjà annulé par Minecraft
     * dans la même passe. Il s'assure que les mobs spawning naturellement
     * (via le système de spawn vanilla) ne sont pas bloqués PAR LE PLUGIN.
     * Le vrai "spawn de jour" est géré en combinaison avec l'immunité solaire
     * (EntityCombustListener) : les mobs qui spawnent de jour survivront
     * grâce à l'immunité.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDaytimeSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!(event.getEntity() instanceof Monster)) return;

        Entity entity = event.getEntity();
        World world = entity.getWorld();

        // Vérifie si c'est le jour (tick entre 0 et 12000 environ)
        long time = world.getTime();
        boolean isDaytime = (time >= 0 && time <= 12300) || (time >= 23850);

        if (!isDaytime) return;

        MobConfigManager mobCfg = plugin.getMobConfigManager();
        String mobKey = entity.getType().name();

        // Si le mob peut spawner de jour, ou s'il possède une variante configurée pour spawner de jour/n'importe quand, on s'assure qu'il n'est pas annulé
        boolean allowed = mobCfg.canSpawnDaytime(mobKey);
        if (!allowed) {
            for (fr.wilddifficulty.variant.MobVariant var : plugin.getVariantManager().getAllVariants()) {
                if (var.getType() == entity.getType()) {
                    String t = var.getSpawnTime();
                    if (t.equalsIgnoreCase("ANY") || t.equalsIgnoreCase("DAY")) {
                        allowed = true;
                        break;
                    }
                }
            }
        }
        if (allowed) {
            event.setCancelled(false); // Lève toute annulation préalable du plugin
        }
    }
}
