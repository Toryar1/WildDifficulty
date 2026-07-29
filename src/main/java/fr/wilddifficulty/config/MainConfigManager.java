package fr.wilddifficulty.config;

import fr.wilddifficulty.WildDifficultyPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Gestionnaire de config.yml.
 * Lit et met en cache tous les paramètres globaux du plugin.
 */
public class MainConfigManager {

    private final WildDifficultyPlugin plugin;

    // ── Paramètres généraux ──────────────────────────────────
    private boolean debug;
    private boolean blockVanillaHostiles;
    private boolean blockVanillaPassives;
    private boolean zoneBorderParticles;
    private int maxSpawnDistance;

    // ── Lune de Sang ─────────────────────────────────────────
    private boolean bloodMoonEnabled;
    private double bloodMoonChance;
    private double bloodMoonHpMultiplier;
    private double bloodMoonDamageMultiplier;
    private double bloodMoonSpeedMultiplier;
    private double bloodMoonDropsMultiplier;
    private double bloodMoonSpawnMultiplier;
    private boolean bloodMoonActive = false;
    private String bloodMoonStartMessage = "&cLa Lune de Sang se lève... Les monstres sont enragés !";
    private String bloodMoonEndMessage = "&aLa Lune de Sang se couche. Le calme revient...";
    private String bloodMoonStartSound;
    private String bloodMoonEndSound;
    private String bloodMoonStartParticle;
    private String bloodMoonEndParticle;
    private java.util.List<String> bloodMoonStartPotions = new java.util.ArrayList<>();
    private java.util.List<String> bloodMoonEndPotions = new java.util.ArrayList<>();

    // ── Modificateurs globaux ────────────────────────────────
    private double globalHealthMult;
    private double globalDamageMult;
    private double globalSpeedMult;
    private double globalFollowRangeMult;
    private double globalKnockbackMult;

    // ── Scaling par distance ─────────────────────────────────
    private boolean distanceScalingEnabled;
    private String distanceWorld;
    private double distanceCenterX, distanceCenterY, distanceCenterZ;
    private double stepBlocks;
    private double percentPerStep;
    private double maxMultiplier;
    private boolean distanceApplyHealth;
    private boolean distanceApplyDamage;
    private boolean distanceApplySpeed;
    private boolean distanceApplyFollowRange;
    private boolean distanceApplyKnockback;

    // ── Nametags ─────────────────────────────────────────────
    private boolean nametagsEnabled;
    private String nametagFormat;
    private boolean nametagVisibleOnLook;

    // ── Soleil ───────────────────────────────────────────────
    private boolean disableBurningGlobally;
    private boolean allowDaySpawnGlobally;
    private int capVariantesParJoueur;

    // ── Soif / Hardcore / Despawn Mort ───────────────────────
    private boolean thirstEnabled = true;
    private double thirstDrainMultiplier = 1.0;
    private boolean thirstHeatDrainEnabled = true;
    private double thirstHeatDrainMultiplier = 1.0;
    private int thirstRestoreWaterBucket = 20;
    private int thirstRestoreWaterBottle = 8;
    private int thirstRestorePotion = 6;
    private int thirstRestoreCauldron = 6;
    private int thirstRestoreWaterBlock = 5;
    private int thirstRestoreMilkBucket = 10;
    private int thirstRestoreHoneyBottle = 6;
    private int thirstRestoreMelonSlice = 3;
    private int thirstRestoreApple = 2;
    private boolean hardcoreEnabled = false;
    private boolean hardcoreNoRegen = true;
    private double hardcoreDamageTakenMult = 1.25;
    private double hardcoreHungerDrainMult = 1.5;
    private int deathItemDespawnSeconds = 300;

    public MainConfigManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge (ou recharge) la configuration depuis config.yml.
     * Toutes les valeurs manquantes utilisent des valeurs par défaut sûres.
     */
    public void load() {
        FileConfiguration cfg = plugin.getConfig();

        // Général
        debug = cfg.getBoolean("plugin.debug", false);
        boolean oldVal = cfg.getBoolean("plugin.bloquer-mobs-vanilla", false);
        blockVanillaHostiles = cfg.getBoolean("plugin.bloquer-hostiles-vanilla", oldVal);
        blockVanillaPassives = cfg.getBoolean("plugin.bloquer-passifs-vanilla", false);
        zoneBorderParticles = cfg.getBoolean("plugin.particules-bordure-zone", true);
        maxSpawnDistance    = cfg.getInt("plugin.distance-spawn-max-joueur", 96);
        capVariantesParJoueur = cfg.getInt("plugin.cap-variantes-par-joueur", 50);

        // Modificateurs globaux
        globalHealthMult    = cfg.getDouble("modificateurs-globaux.multiplicateur-pv", 1.0);
        globalDamageMult    = cfg.getDouble("modificateurs-globaux.multiplicateur-degats", 1.0);
        globalSpeedMult     = cfg.getDouble("modificateurs-globaux.multiplicateur-vitesse", 1.0);
        globalFollowRangeMult = cfg.getDouble("modificateurs-globaux.multiplicateur-portee-detection", 1.0);
        globalKnockbackMult = cfg.getDouble("modificateurs-globaux.multiplicateur-resistance-knockback", 1.0);

        // Scaling distance
        distanceScalingEnabled = cfg.getBoolean("scaling-distance.actif", true);
        distanceWorld          = cfg.getString("scaling-distance.monde", "world");
        distanceCenterX        = cfg.getDouble("scaling-distance.x", 0);
        distanceCenterY        = cfg.getDouble("scaling-distance.y", 64);
        distanceCenterZ        = cfg.getDouble("scaling-distance.z", 0);
        stepBlocks             = cfg.getDouble("scaling-distance.pas-blocs", 250.0);
        percentPerStep         = cfg.getDouble("scaling-distance.pourcent-par-pas", 0.08);
        maxMultiplier          = cfg.getDouble("scaling-distance.max-multiplicateur", 3.0);
        distanceApplyHealth    = cfg.getBoolean("scaling-distance.appliquer-sur.pv", true);
        distanceApplyDamage    = cfg.getBoolean("scaling-distance.appliquer-sur.degats", true);
        distanceApplySpeed     = cfg.getBoolean("scaling-distance.appliquer-sur.vitesse", false);
        distanceApplyFollowRange = cfg.getBoolean("scaling-distance.appliquer-sur.portee-detection", false);
        distanceApplyKnockback = cfg.getBoolean("scaling-distance.appliquer-sur.resistance-knockback", false);

        // Nametags
        nametagsEnabled       = cfg.getBoolean("nametags.actif", false);
        nametagFormat         = cfg.getString("nametags.format", "&f{nom} &7({pv}/{pv_max}❤)");
        if (nametagFormat.contains("{niveau}") || nametagFormat.contains("Niv.")) {
            nametagFormat = "&f{nom} &7({pv}/{pv_max}❤)";
            cfg.set("nametags.format", nametagFormat);
        }
        nametagVisibleOnLook  = cfg.getBoolean("nametags.visible-seulement-au-regard", false);

        // Soleil
        disableBurningGlobally = cfg.getBoolean("soleil.desactiver-combustion-globalement", false);
        allowDaySpawnGlobally  = cfg.getBoolean("soleil.autoriser-spawn-de-jour-globalement", false);

        // Soif & Hardcore & Mort
        thirstEnabled = cfg.getBoolean("soif.actif", true);
        thirstDrainMultiplier = cfg.getDouble("soif.multiplicateur-degradation", 1.0);
        thirstHeatDrainEnabled = cfg.getBoolean("soif.dehydratation-chaleur.actif", true);
        thirstHeatDrainMultiplier = cfg.getDouble("soif.dehydratation-chaleur.multiplicateur", 1.0);

        thirstRestoreWaterBucket = cfg.getInt("soif.restauration.seau-eau", 20);
        thirstRestoreWaterBottle = cfg.getInt("soif.restauration.bouteille-eau", 8);
        thirstRestorePotion = cfg.getInt("soif.restauration.autre-potion", 6);
        thirstRestoreCauldron = cfg.getInt("soif.restauration.chaudron", 6);
        thirstRestoreWaterBlock = cfg.getInt("soif.restauration.source-eau", 5);
        thirstRestoreMilkBucket = cfg.getInt("soif.restauration.seau-lait", 10);
        thirstRestoreHoneyBottle = cfg.getInt("soif.restauration.fiole-miel", 6);
        thirstRestoreMelonSlice = cfg.getInt("soif.restauration.tranche-melon", 3);
        thirstRestoreApple = cfg.getInt("soif.restauration.pomme", 2);

        hardcoreEnabled = cfg.getBoolean("hardcore.actif", false);
        hardcoreNoRegen = cfg.getBoolean("hardcore.desactiver-regene-naturelle", true);
        hardcoreDamageTakenMult = cfg.getDouble("hardcore.multiplicateur-degats-subis", 1.25);
        hardcoreHungerDrainMult = cfg.getDouble("hardcore.multiplicateur-faim", 1.5);
        deathItemDespawnSeconds = cfg.getInt("mort.temps-despawn-stuff-secondes", 300);

        // Blood Moon
        bloodMoonEnabled = cfg.getBoolean("bloodmoon.actif", false);
        bloodMoonChance = cfg.getDouble("bloodmoon.chance", 0.1);
        bloodMoonHpMultiplier = cfg.getDouble("bloodmoon.multiplicateur-pv", 1.5);
        bloodMoonDamageMultiplier = cfg.getDouble("bloodmoon.multiplicateur-degats", 1.5);
        bloodMoonSpeedMultiplier = cfg.getDouble("bloodmoon.multiplicateur-vitesse", 1.2);
        bloodMoonDropsMultiplier = cfg.getDouble("bloodmoon.multiplicateur-drops", 2.0);
        bloodMoonSpawnMultiplier = cfg.getDouble("bloodmoon.multiplicateur-spawn", 1.5);
        bloodMoonStartMessage = cfg.getString("bloodmoon.message-debut", "&cLa Lune de Sang se lève... Les monstres sont enragés !");
        bloodMoonEndMessage = cfg.getString("bloodmoon.message-fin", "&aLa Lune de Sang se couche. Le calme revient...");
        bloodMoonStartSound = cfg.getString("bloodmoon.sound-debut", "ENTITY_WITHER_SPAWN");
        bloodMoonEndSound = cfg.getString("bloodmoon.sound-fin", "ENTITY_PLAYER_LEVELUP");
        bloodMoonStartParticle = cfg.getString("bloodmoon.particule-debut", "FLASH");
        bloodMoonEndParticle = cfg.getString("bloodmoon.particule-fin", "HAPPY_VILLAGER");
        bloodMoonStartPotions = cfg.getStringList("bloodmoon.effets-debut");
        if (bloodMoonStartPotions.isEmpty()) {
            bloodMoonStartPotions = java.util.List.of("DARKNESS:100:0");
        }
        bloodMoonEndPotions = cfg.getStringList("bloodmoon.effets-fin");

        // Validation des valeurs critiques
        if (stepBlocks <= 0) {
            plugin.getLogger().warning("[Config] scaling-distance.pas-blocs doit être > 0. Valeur forcée à 250.");
            stepBlocks = 250.0;
        }
        if (percentPerStep < 0) {
            plugin.getLogger().warning("[Config] scaling-distance.pourcent-par-pas doit être >= 0. Valeur forcée à 0.08.");
            percentPerStep = 0.08;
        }
        if (maxMultiplier < 1.0) {
            plugin.getLogger().warning("[Config] scaling-distance.max-multiplicateur doit être >= 1.0. Valeur forcée à 1.0.");
            maxMultiplier = 1.0;
        }

        if (debug) {
            plugin.getLogger().info("[DEBUG] config.yml chargé — debug=" + debug
                    + ", distanceScaling=" + distanceScalingEnabled
                    + ", nametags=" + nametagsEnabled);
        }
    }

    public void save() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("plugin.debug", debug);
        cfg.set("plugin.bloquer-hostiles-vanilla", blockVanillaHostiles);
        cfg.set("plugin.bloquer-passifs-vanilla", blockVanillaPassives);
        cfg.set("plugin.particules-bordure-zone", zoneBorderParticles);
        cfg.set("plugin.distance-spawn-max-joueur", maxSpawnDistance);
        cfg.set("plugin.cap-variantes-par-joueur", capVariantesParJoueur);
        cfg.set("modificateurs-globaux.multiplicateur-pv", globalHealthMult);
        cfg.set("modificateurs-globaux.multiplicateur-degats", globalDamageMult);
        cfg.set("modificateurs-globaux.multiplicateur-vitesse", globalSpeedMult);
        cfg.set("modificateurs-globaux.multiplicateur-portee-detection", globalFollowRangeMult);
        cfg.set("modificateurs-globaux.multiplicateur-resistance-knockback", globalKnockbackMult);
        
        cfg.set("soleil.desactiver-combustion-globalement", disableBurningGlobally);
        cfg.set("soleil.autoriser-spawn-de-jour-globalement", allowDaySpawnGlobally);
        cfg.set("nametags.actif", nametagsEnabled);
        cfg.set("nametags.format", nametagFormat);
        cfg.set("soif.actif", thirstEnabled);
        cfg.set("soif.multiplicateur-degradation", thirstDrainMultiplier);
        cfg.set("soif.dehydratation-chaleur.actif", thirstHeatDrainEnabled);
        cfg.set("soif.dehydratation-chaleur.multiplicateur", thirstHeatDrainMultiplier);
        cfg.set("soif.restauration.seau-eau", thirstRestoreWaterBucket);
        cfg.set("soif.restauration.bouteille-eau", thirstRestoreWaterBottle);
        cfg.set("soif.restauration.autre-potion", thirstRestorePotion);
        cfg.set("soif.restauration.chaudron", thirstRestoreCauldron);
        cfg.set("soif.restauration.source-eau", thirstRestoreWaterBlock);
        cfg.set("soif.restauration.seau-lait", thirstRestoreMilkBucket);
        cfg.set("soif.restauration.fiole-miel", thirstRestoreHoneyBottle);
        cfg.set("soif.restauration.tranche-melon", thirstRestoreMelonSlice);
        cfg.set("soif.restauration.pomme", thirstRestoreApple);

        cfg.set("hardcore.actif", hardcoreEnabled);
        cfg.set("hardcore.desactiver-regene-naturelle", hardcoreNoRegen);
        cfg.set("hardcore.multiplicateur-degats-subis", hardcoreDamageTakenMult);
        cfg.set("hardcore.multiplicateur-faim", hardcoreHungerDrainMult);
        cfg.set("mort.temps-despawn-stuff-secondes", deathItemDespawnSeconds);

        cfg.set("bloodmoon.actif", bloodMoonEnabled);
        cfg.set("bloodmoon.chance", bloodMoonChance);
        cfg.set("bloodmoon.multiplicateur-pv", bloodMoonHpMultiplier);
        cfg.set("bloodmoon.multiplicateur-degats", bloodMoonDamageMultiplier);
        cfg.set("bloodmoon.multiplicateur-vitesse", bloodMoonSpeedMultiplier);
        cfg.set("bloodmoon.multiplicateur-drops", bloodMoonDropsMultiplier);
        cfg.set("bloodmoon.multiplicateur-spawn", bloodMoonSpawnMultiplier);
        cfg.set("bloodmoon.message-debut", bloodMoonStartMessage);
        cfg.set("bloodmoon.message-fin", bloodMoonEndMessage);
        cfg.set("bloodmoon.sound-debut", bloodMoonStartSound);
        cfg.set("bloodmoon.sound-fin", bloodMoonEndSound);
        cfg.set("bloodmoon.particule-debut", bloodMoonStartParticle);
        cfg.set("bloodmoon.particule-fin", bloodMoonEndParticle);
        cfg.set("bloodmoon.effets-debut", bloodMoonStartPotions);
        cfg.set("bloodmoon.effets-fin", bloodMoonEndPotions);
        plugin.saveConfig();
    }

    /**
     * Calcule le multiplicateur de distance pour une position XZ donnée.
     * Formule : min(maxMult, 1.0 + floor(distance / step) * percent)
     *
     * @param worldName nom du monde du mob
     * @param x coordonnée X du mob
     * @param z coordonnée Z du mob
     * @return multiplicateur de distance (1.0 si désactivé ou monde différent)
     */
    public double computeDistanceMultiplier(String worldName, double x, double z) {
        if (!distanceScalingEnabled) return 1.0;
        if (!distanceWorld.equals(worldName)) return 1.0;

        double dx = x - distanceCenterX;
        double dz = z - distanceCenterZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        long steps = (long) Math.floor(distance / stepBlocks);
        double mult = 1.0 + steps * percentPerStep;
        return Math.min(maxMultiplier, mult);
    }

    public boolean isDebug() { return debug; }
    public void setDebug(boolean v) { this.debug = v; }
    public boolean isBlockVanillaHostiles() { return blockVanillaHostiles; }
    public void setBlockVanillaHostiles(boolean v) { this.blockVanillaHostiles = v; }
    public boolean isBlockVanillaPassives() { return blockVanillaPassives; }
    public void setBlockVanillaPassives(boolean v) { this.blockVanillaPassives = v; }
    public boolean isZoneBorderParticles() { return zoneBorderParticles; }
    public void setZoneBorderParticles(boolean v) { this.zoneBorderParticles = v; }

    public double getGlobalHealthMult()     { return globalHealthMult; }
    public void setGlobalHealthMult(double v) { this.globalHealthMult = Math.max(0.1, v); }
    public double getGlobalDamageMult()      { return globalDamageMult; }
    public void setGlobalDamageMult(double v) { this.globalDamageMult = Math.max(0.1, v); }
    public double getGlobalSpeedMult()       { return globalSpeedMult; }
    public void setGlobalSpeedMult(double v) { this.globalSpeedMult = Math.max(0.1, v); }
    public double getGlobalFollowRangeMult() { return globalFollowRangeMult; }
    public void setGlobalFollowRangeMult(double v) { this.globalFollowRangeMult = Math.max(0.1, v); }
    public double getGlobalKnockbackMult()   { return globalKnockbackMult; }
    public void setGlobalKnockbackMult(double v) { this.globalKnockbackMult = Math.max(0.1, v); }

    public boolean isDistanceScalingEnabled() { return distanceScalingEnabled; }
    public String getDistanceWorld()           { return distanceWorld; }
    public boolean isDistanceApplyHealth()     { return distanceApplyHealth; }
    public boolean isDistanceApplyDamage()     { return distanceApplyDamage; }
    public boolean isDistanceApplySpeed()      { return distanceApplySpeed; }
    public boolean isDistanceApplyFollowRange(){ return distanceApplyFollowRange; }
    public boolean isDistanceApplyKnockback()  { return distanceApplyKnockback; }

    public boolean isNametagsEnabled()        { return nametagsEnabled; }
    public String getNametagFormat()           { return nametagFormat; }
    public boolean isNametagVisibleOnLook()    { return nametagVisibleOnLook; }

    public boolean isDisableBurningGlobally() { return disableBurningGlobally; }
    public boolean isAllowDaySpawnGlobally()  { return allowDaySpawnGlobally; }

    public boolean isBloodMoonEnabled() { return bloodMoonEnabled; }
    public void setBloodMoonEnabled(boolean v) { this.bloodMoonEnabled = v; }
    public double getBloodMoonChance() { return bloodMoonChance; }
    public void setBloodMoonChance(double v) { this.bloodMoonChance = Math.max(0.0, Math.min(1.0, v)); }
    public double getBloodMoonHpMultiplier() { return bloodMoonHpMultiplier; }
    public void setBloodMoonHpMultiplier(double v) { this.bloodMoonHpMultiplier = Math.max(0.1, v); }
    public double getBloodMoonDamageMultiplier() { return bloodMoonDamageMultiplier; }
    public void setBloodMoonDamageMultiplier(double v) { this.bloodMoonDamageMultiplier = Math.max(0.1, v); }
    public double getBloodMoonSpeedMultiplier() { return bloodMoonSpeedMultiplier; }
    public void setBloodMoonSpeedMultiplier(double v) { this.bloodMoonSpeedMultiplier = Math.max(0.1, v); }
    public double getBloodMoonDropsMultiplier() { return bloodMoonDropsMultiplier; }
    public void setBloodMoonDropsMultiplier(double v) { this.bloodMoonDropsMultiplier = Math.max(0.0, v); }
    public double getBloodMoonSpawnMultiplier() { return bloodMoonSpawnMultiplier; }
    public void setBloodMoonSpawnMultiplier(double v) { this.bloodMoonSpawnMultiplier = Math.max(1.0, v); }
    public boolean isBloodMoonActive() { return bloodMoonActive; }
    public void setBloodMoonActive(boolean v) { this.bloodMoonActive = v; }

    public String getBloodMoonStartMessage() { return bloodMoonStartMessage; }
    public void setBloodMoonStartMessage(String msg) { this.bloodMoonStartMessage = msg; }
    public String getBloodMoonEndMessage() { return bloodMoonEndMessage; }
    public void setBloodMoonEndMessage(String msg) { this.bloodMoonEndMessage = msg; }

    public int getMaxSpawnDistance() { return maxSpawnDistance; }
    public void setMaxSpawnDistance(int v) { this.maxSpawnDistance = v; }

    public String getBloodMoonStartSound() { return bloodMoonStartSound; }
    public void setBloodMoonStartSound(String v) { this.bloodMoonStartSound = v; }
    public String getBloodMoonEndSound() { return bloodMoonEndSound; }
    public void setBloodMoonEndSound(String v) { this.bloodMoonEndSound = v; }

    public String getBloodMoonStartParticle() { return bloodMoonStartParticle; }
    public void setBloodMoonStartParticle(String v) { this.bloodMoonStartParticle = v; }
    public String getBloodMoonEndParticle() { return bloodMoonEndParticle; }
    public void setBloodMoonEndParticle(String v) { this.bloodMoonEndParticle = v; }

    public java.util.List<String> getBloodMoonStartPotions() { return bloodMoonStartPotions; }
    public void setBloodMoonStartPotions(java.util.List<String> v) { this.bloodMoonStartPotions = v; }
    public java.util.List<String> getBloodMoonEndPotions() { return bloodMoonEndPotions; }
    public void setBloodMoonEndPotions(java.util.List<String> v) { this.bloodMoonEndPotions = v; }

    public int getCapVariantesParJoueur() { return capVariantesParJoueur; }
    public void setCapVariantesParJoueur(int v) { this.capVariantesParJoueur = Math.max(1, v); }
    public void setNametagsEnabled(boolean v) { this.nametagsEnabled = v; }
    public void setNametagFormat(String v) { this.nametagFormat = v; }
    public void setDisableBurningGlobally(boolean v) { this.disableBurningGlobally = v; }
    public void setAllowDaySpawnGlobally(boolean v) { this.allowDaySpawnGlobally = v; }

    public boolean isThirstEnabled() { return thirstEnabled; }
    public void setThirstEnabled(boolean v) { this.thirstEnabled = v; }
    public double getThirstDrainMultiplier() { return thirstDrainMultiplier; }
    public void setThirstDrainMultiplier(double v) { this.thirstDrainMultiplier = Math.max(0.1, Math.min(10.0, v)); }
    public boolean isThirstHeatDrainEnabled() { return thirstHeatDrainEnabled; }
    public void setThirstHeatDrainEnabled(boolean v) { this.thirstHeatDrainEnabled = v; }
    public double getThirstHeatDrainMultiplier() { return thirstHeatDrainMultiplier; }
    public void setThirstHeatDrainMultiplier(double v) { this.thirstHeatDrainMultiplier = Math.max(0.1, Math.min(10.0, v)); }

    public int getThirstRestoreWaterBucket() { return thirstRestoreWaterBucket; }
    public void setThirstRestoreWaterBucket(int v) { this.thirstRestoreWaterBucket = Math.max(0, Math.min(20, v)); }
    public int getThirstRestoreWaterBottle() { return thirstRestoreWaterBottle; }
    public void setThirstRestoreWaterBottle(int v) { this.thirstRestoreWaterBottle = Math.max(0, Math.min(20, v)); }
    public int getThirstRestorePotion() { return thirstRestorePotion; }
    public void setThirstRestorePotion(int v) { this.thirstRestorePotion = Math.max(0, Math.min(20, v)); }
    public int getThirstRestoreCauldron() { return thirstRestoreCauldron; }
    public void setThirstRestoreCauldron(int v) { this.thirstRestoreCauldron = Math.max(0, Math.min(20, v)); }
    public int getThirstRestoreWaterBlock() { return thirstRestoreWaterBlock; }
    public void setThirstRestoreWaterBlock(int v) { this.thirstRestoreWaterBlock = Math.max(0, Math.min(20, v)); }
    public int getThirstRestoreMilkBucket() { return thirstRestoreMilkBucket; }
    public void setThirstRestoreMilkBucket(int v) { this.thirstRestoreMilkBucket = Math.max(0, Math.min(20, v)); }
    public int getThirstRestoreHoneyBottle() { return thirstRestoreHoneyBottle; }
    public void setThirstRestoreHoneyBottle(int v) { this.thirstRestoreHoneyBottle = Math.max(0, Math.min(20, v)); }
    public int getThirstRestoreMelonSlice() { return thirstRestoreMelonSlice; }
    public void setThirstRestoreMelonSlice(int v) { this.thirstRestoreMelonSlice = Math.max(0, Math.min(20, v)); }
    public int getThirstRestoreApple() { return thirstRestoreApple; }
    public void setThirstRestoreApple(int v) { this.thirstRestoreApple = Math.max(0, Math.min(20, v)); }
    public boolean isHardcoreEnabled() { return hardcoreEnabled; }
    public void setHardcoreEnabled(boolean v) { this.hardcoreEnabled = v; }
    public boolean isHardcoreNoRegen() { return hardcoreNoRegen; }
    public void setHardcoreNoRegen(boolean v) { this.hardcoreNoRegen = v; }
    public double getHardcoreDamageTakenMult() { return hardcoreDamageTakenMult; }
    public void setHardcoreDamageTakenMult(double v) { this.hardcoreDamageTakenMult = Math.max(0.5, v); }
    public double getHardcoreHungerDrainMult() { return hardcoreHungerDrainMult; }
    public void setHardcoreHungerDrainMult(double v) { this.hardcoreHungerDrainMult = Math.max(0.5, v); }
    public int getDeathItemDespawnSeconds() { return deathItemDespawnSeconds; }
    public void setDeathItemDespawnSeconds(int v) { this.deathItemDespawnSeconds = Math.max(10, v); }
}
