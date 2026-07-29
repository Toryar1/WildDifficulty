package fr.wilddifficulty.config;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Gestionnaire de biomes.yml.
 *
 * Cache en mémoire :
 * - biomeModifiers : modificateurs globaux par biome
 * - biomeAllowedMobs : whitelist mobs par biome
 * - biomeDeniedMobs : blacklist mobs par biome
 * - biomeMobOverrides : surcharges par (biome, type de mob)
 *
 * Tout est invalidé au rechargement.
 */
public class BiomeConfigManager {

    private final WildDifficultyPlugin plugin;

    // Modificateurs de stats par biome (clé = nom biome en majuscules)
    private final Map<String, StatModifiers> biomeModifiers = new HashMap<>();

    // Whitelist de mobs par biome (null = pas de restriction = tous autorisés)
    private final Map<String, Set<String>> biomeAllowedMobs = new HashMap<>();

    // Blacklist de mobs par biome
    private final Map<String, Set<String>> biomeDeniedMobs = new HashMap<>();

    private final Map<String, List<String>> biomeAllowedVariants = new HashMap<>();
    private final Map<String, List<String>> biomeDeniedVariants = new HashMap<>();
    private final Map<String, List<String>> biomeAllowedSquads = new HashMap<>();
    private final Map<String, List<String>> biomeDeniedSquads = new HashMap<>();

    // Surcharges spécifiques par biome + type de mob : biomeKey -> mobKey -> StatModifiers
    private final Map<String, Map<String, StatModifiers>> biomeMobOverrides = new HashMap<>();

    public BiomeConfigManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge ou recharge biomes.yml en mémoire.
     */
    public void load() {
        biomeModifiers.clear();
        biomeAllowedMobs.clear();
        biomeDeniedMobs.clear();
        biomeAllowedVariants.clear();
        biomeDeniedVariants.clear();
        biomeAllowedSquads.clear();
        biomeDeniedSquads.clear();
        biomeMobOverrides.clear();

        File file = new File(plugin.getDataFolder(), "biomes.yml");
        if (!file.exists()) {
            plugin.getLogger().warning("[Biomes] biomes.yml introuvable, aucune règle de biome chargée.");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        for (String biomeKey : cfg.getKeys(false)) {
            // Validation du nom de biome
            String upperKey = biomeKey.toUpperCase();
            try {
                Biome.valueOf(upperKey);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Biomes] Biome inconnu ignoré : " + biomeKey
                        + " (vérifiez l'orthographe dans biomes.yml)");
                continue;
            }

            ConfigurationSection section = cfg.getConfigurationSection(biomeKey);
            if (section == null) continue;

            // ── Whitelist mobs ──────────────────────────────
            if (section.isList("mobs-autorises")) {
                Set<String> allowed = new HashSet<>();
                for (String mob : section.getStringList("mobs-autorises")) {
                    allowed.add(mob.toUpperCase());
                }
                biomeAllowedMobs.put(upperKey, allowed);
            }

            // ── Blacklist mobs ──────────────────────────────
            if (section.isList("mobs-interdits")) {
                Set<String> denied = new HashSet<>();
                for (String mob : section.getStringList("mobs-interdits")) {
                    denied.add(mob.toUpperCase());
                }
                biomeDeniedMobs.put(upperKey, denied);
            }

            if (section.isList("variantes-autorisees")) biomeAllowedVariants.put(upperKey, section.getStringList("variantes-autorisees"));
            if (section.isList("variantes-interdites")) biomeDeniedVariants.put(upperKey, section.getStringList("variantes-interdites"));
            if (section.isList("escouades-autorisees")) biomeAllowedSquads.put(upperKey, section.getStringList("escouades-autorisees"));
            if (section.isList("escouades-interdites")) biomeDeniedSquads.put(upperKey, section.getStringList("escouades-interdites"));

            // ── Modificateurs globaux du biome ──────────────
            ConfigurationSection modSection = section.getConfigurationSection("modificateurs");
            if (modSection != null) {
                biomeModifiers.put(upperKey, readModifiers(modSection));
            }

            // ── Surcharges par type de mob dans ce biome ────
            ConfigurationSection mobsSection = section.getConfigurationSection("mobs");
            if (mobsSection != null) {
                Map<String, StatModifiers> mobOverrides = new HashMap<>();
                for (String mobKey : mobsSection.getKeys(false)) {
                    ConfigurationSection mobSection = mobsSection.getConfigurationSection(mobKey);
                    if (mobSection != null) {
                        mobOverrides.put(mobKey.toUpperCase(), readModifiers(mobSection));
                    }
                }
                if (!mobOverrides.isEmpty()) {
                    biomeMobOverrides.put(upperKey, mobOverrides);
                }
            }

            count++;
        }

        plugin.getLogger().info("[Biomes] " + count + " biome(s) chargé(s) depuis biomes.yml.");
    }

    /**
     * Lit un StatModifiers depuis une ConfigurationSection.
     */
    public static StatModifiers readModifiers(ConfigurationSection section) {
        StatModifiers m = new StatModifiers();
        m.setHealthValue(section.getDouble("valeur-pv", -1.0));
        m.setDamageValue(section.getDouble("valeur-degats", -1.0));
        m.setSpeedValue(section.getDouble("valeur-vitesse", -1.0));
        m.setFollowRangeValue(section.getDouble("valeur-portee-detection", -1.0));
        m.setKnockbackValue(section.getDouble("valeur-resistance-knockback", -1.0));
        m.setRegenerationValue(section.getDouble("valeur-regen", -1.0));
        
        m.setEquipmentTier(section.getString("tier-equipement", "none"));
        m.setEquipmentChance(section.getDouble("chance-equipement", 0.0));

        m.setHelmetItem(section.getString("casque-item", "none"));
        m.setHelmetChance(section.getDouble("casque-chance", 0.0));
        m.setHelmetColor(section.getString("casque-couleur", "none"));
        m.setChestplateItem(section.getString("plastron-item", "none"));
        m.setChestplateChance(section.getDouble("plastron-chance", 0.0));
        m.setChestplateColor(section.getString("plastron-couleur", "none"));
        m.setLeggingsItem(section.getString("jambieres-item", "none"));
        m.setLeggingsChance(section.getDouble("jambieres-chance", 0.0));
        m.setLeggingsColor(section.getString("jambieres-couleur", "none"));
        m.setBootsItem(section.getString("bottes-item", "none"));
        m.setBootsChance(section.getDouble("bottes-chance", 0.0));
        m.setBootsColor(section.getString("bottes-couleur", "none"));
        m.setMainHandItem(section.getString("main-principale-item", "none"));
        m.setMainHandChance(section.getDouble("main-principale-chance", 0.0));
        m.setOffHandItem(section.getString("main-secondaire-item", "none"));
        m.setOffHandChance(section.getDouble("main-secondaire-chance", 0.0));

        m.setResistanceFeu(section.getDouble("resistance-feu", 1.0));
        m.setResistanceMagie(section.getDouble("resistance-magie", 1.0));
        m.setResistanceProjectile(section.getDouble("resistance-projectile", 1.0));

        m.setGoalWallVision(section.getBoolean("vision-murs", false));
        m.setImmuneFire(section.getBoolean("immunite-feu", false));
        m.setImmuneLava(section.getBoolean("immunite-lave", false));
        m.setImmuneDrowning(section.getBoolean("immunite-noyade", false));
        m.setImmuneFall(section.getBoolean("immunite-chute", false));
        m.setExplodeOnDeath(section.getBoolean("explosion-mort", false));
        m.setFleeUnderHealth(section.getDouble("fuite-seuil-pv", 0.0));
        m.setPassiveRegen(section.getDouble("regen-passive", 0.0));
        m.setCamouflage(section.getBoolean("camouflage", false));
        m.setTeleportToTarget(section.getBoolean("teleportation-cible", false));
        m.setSpawnReinforcements(section.getBoolean("renforts-aggro", false));
        m.setDashAttack(section.getBoolean("charge-cible", false));
        m.setGroupAggro(section.getBoolean("comportement-groupe", false));
        m.setRangedAttack(section.getBoolean("attaque-distance-simulee", false));
        m.setTargetPriority(section.getString("ciblage-prioritaire", "none"));

        m.setFleeWhenSolo(section.getBoolean("fuite-solo", false));
        m.setDeathSpawnVariant(section.getString("spawn-mort-variante", "none"));
        m.setDeathSpawnAmount(section.getInt("spawn-mort-quantite", 0));
        m.setCreeperPowered(section.getBoolean("creeper-charge", false));
        m.setGolemCracked(section.getString("golem-fissure", "none"));
        m.setSheepSheared(section.getBoolean("mouton-tondu", false));
        m.setEndermanBlock(section.getString("enderman-bloc", "none"));
        m.setSkullSkin(section.getString("skin-head-skull", "none"));

        m.setTeleportMaxUses(section.getInt("teleport-max-uses", -1));
        m.setDashMaxUses(section.getInt("dash-max-uses", -1));
        m.setJumpAttack(section.getBoolean("jump-attack", false));
        m.setRangedAttackType(section.getString("ranged-attack-type", "ARROW"));
        m.setOnHitPotionEffect(section.getString("on-hit-potion-effect", "none"));
        m.setOnHitPotionChance(section.getDouble("on-hit-potion-chance", 0.0));
        m.setOnHitPotionDuration(section.getInt("on-hit-potion-duration", 100));
        m.setOnHitPotionAmplifier(section.getInt("on-hit-potion-amplifier", 0));

        m.setParticleAuraType(section.getString("particule-aura-type", "none"));
        m.setParticleAuraColor(section.getString("particule-aura-couleur", "255,255,255"));
        m.setParticleAuraFreq(section.getInt("particule-aura-frequence", 20));
        m.setParticleSpawnType(section.getString("particule-spawn-type", "none"));
        m.setParticleDeathType(section.getString("particule-mort-type", "none"));
        m.setParticleDeathColorful(section.getBoolean("particule-mort-colore", false));
        m.setParticleTrailType(section.getString("particule-trail-type", "none"));

        m.setBossBarEnabled(section.getBoolean("bossbar-active", false));
        m.setBossBarColor(section.getString("bossbar-couleur", "RED"));
        m.setBossBarStyle(section.getString("bossbar-style", "PROGRESS"));
        m.setBossBarDarkenSky(section.getBoolean("bossbar-ciel-sombre", false));

        m.setPotionEffects(section.getStringList("effets-statut"));

        return m;
    }

    // ── API publique ─────────────────────────────────────────

    /**
     * Retourne les modificateurs pour un biome donné (ou null si non défini).
     */
    public StatModifiers getBiomeModifiers(String biomeKey) {
        return biomeModifiers.get(biomeKey.toUpperCase());
    }

    /**
     * Retourne la surcharge pour un mob spécifique dans un biome donné (ou null).
     */
    public StatModifiers getBiomeMobOverride(String biomeKey, String mobKey) {
        Map<String, StatModifiers> overrides = biomeMobOverrides.get(biomeKey.toUpperCase());
        if (overrides == null) return null;
        return overrides.get(mobKey.toUpperCase());
    }

    /**
     * Vérifie si un mob est autorisé à spawner dans un biome.
     *
     * @param biomeKey clé du biome (ex: "DESERT")
     * @param mobKey   clé du mob (ex: "ZOMBIE")
     * @return true si autorisé (ou si biome non configuré)
     */
    public boolean isMobAllowedInBiome(String biomeKey, String mobKey) {
        String upperBiome = biomeKey.toUpperCase();
        String upperMob   = mobKey.toUpperCase();

        // Blacklist : priorité sur tout
        Set<String> denied = biomeDeniedMobs.get(upperBiome);
        if (denied != null && denied.contains(upperMob)) return false;

        // Whitelist : si définie, le mob doit en faire partie
        Set<String> allowed = biomeAllowedMobs.get(upperBiome);
        if (allowed != null && !allowed.isEmpty()) {
            return allowed.contains(upperMob);
        }

        // Ni whitelist ni blacklist pour ce mob dans ce biome : comportement vanilla
        return true;
    }

    public List<String> getAllowedVariants(String biomeKey) { return biomeAllowedVariants.getOrDefault(biomeKey.toUpperCase(), new ArrayList<>()); }
    public List<String> getDeniedVariants(String biomeKey) { return biomeDeniedVariants.getOrDefault(biomeKey.toUpperCase(), new ArrayList<>()); }
    public List<String> getAllowedSquads(String biomeKey) { return biomeAllowedSquads.getOrDefault(biomeKey.toUpperCase(), new ArrayList<>()); }
    public List<String> getDeniedSquads(String biomeKey) { return biomeDeniedSquads.getOrDefault(biomeKey.toUpperCase(), new ArrayList<>()); }

    /**
     * Indique si ce biome a des règles spécifiques définies.
     */
    public boolean hasBiomeRules(String biomeKey) {
        String key = biomeKey.toUpperCase();
        return biomeModifiers.containsKey(key)
                || biomeAllowedMobs.containsKey(key)
                || biomeDeniedMobs.containsKey(key);
    }
}
