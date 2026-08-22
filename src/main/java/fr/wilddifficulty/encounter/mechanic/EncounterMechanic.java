package fr.wilddifficulty.encounter.mechanic;

import fr.wilddifficulty.encounter.EncounterSession;
import fr.wilddifficulty.zone.DifficultyZone;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface définissant le comportement mécanique d'un type d'Encounter spécifique.
 */
public interface EncounterMechanic {

    /**
     * Démarre l'Encounter dans la zone avec les joueurs participants initiaux.
     */
    void start(DifficultyZone zone, List<Player> players, EncounterSession session);

    /**
     * Exécuté à chaque seconde (20 ticks) pour actualiser la logique de l'Encounter.
     */
    void tick(DifficultyZone zone, EncounterSession session);

    /**
     * Exécuté lorsqu'un monstre appartenant à l'Encounter est éliminé.
     */
    void onMobDeath(DifficultyZone zone, EncounterSession session, LivingEntity entity, Player killer);

    /**
     * Clôture l'Encounter avec succès ou échec.
     */
    void end(DifficultyZone zone, EncounterSession session, boolean success);
}
