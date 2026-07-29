package fr.wilddifficulty.config;

import java.util.ArrayList;
import java.util.List;

public class StatModifiers {

    // Stats Absolues (-1.0 = non défini)
    private double healthValue = -1.0;
    private double damageValue = -1.0;
    private double speedValue = -1.0;
    private double followRangeValue = -1.0;
    private double knockbackValue = -1.0;
    private double regenerationValue = -1.0; // Régénération Absolue

    // Equipement
    private String equipmentTier = "none";
    private double equipmentChance = 0.0;

    // Equipement spécifique
    private String helmetItem = "none";
    private double helmetChance = 0.0;
    private String helmetColor = "none";
    private String chestplateItem = "none";
    private double chestplateChance = 0.0;
    private String chestplateColor = "none";
    private String leggingsItem = "none";
    private double leggingsChance = 0.0;
    private String leggingsColor = "none";
    private String bootsItem = "none";
    private double bootsChance = 0.0;
    private String bootsColor = "none";
    private String mainHandItem = "none";
    private double mainHandChance = 0.0;
    private String offHandItem = "none";
    private double offHandChance = 0.0;

    // Résistances élémentaires (1.0 = normal, 0.0 = immunisé, 2.0 = vulnérable)
    private double resistanceFeu = 1.0;
    private double resistanceMagie = 1.0;
    private double resistanceProjectile = 1.0;

    // Comportements (Goals/AI)
    private boolean goalWallVision = false;
    private boolean immuneFire = false;
    private boolean immuneLava = false;
    private boolean immuneDrowning = false;
    private boolean immuneFall = false;
    private boolean explodeOnDeath = false;
    private double fleeUnderHealth = 0.0; // 0.0 = désactivé, e.g. 0.2 = 20%
    private double passiveRegen = 0.0;     // HP/sec
    private boolean camouflage = false;
    private boolean teleportToTarget = false;
    private boolean spawnReinforcements = false;
    private boolean dashAttack = false;
    private boolean groupAggro = false;
    private boolean rangedAttack = false;
    private String targetPriority = "none"; // none | closest | weakest | random

    // Nouvelles options d'IA et cosmétiques
    private boolean fleeWhenSolo = false;
    private String deathSpawnVariant = "none";
    private int deathSpawnAmount = 0;
    
    private boolean creeperPowered = false;
    private String golemCracked = "none";
    private boolean sheepSheared = false;
    private boolean smartClimb = false;
    private String endermanBlock = "none";
    private String entitySubtype = "none";
    private String skullSkin = "none"; // pseudo ou base64 url texture

    private int teleportMaxUses = -1;
    private int dashMaxUses = -1;
    private boolean jumpAttack = false;
    private String rangedAttackType = "ARROW";
    private String onHitPotionEffect = "none";
    private double onHitPotionChance = 0.0;
    private int onHitPotionDuration = 100;
    private int onHitPotionAmplifier = 0;

    // Effets visuels & BossBar
    private String particleAuraType = "none";
    private String particleAuraColor = "255,255,255"; // RGB
    private int particleAuraFreq = 20; // ticks
    private String particleSpawnType = "none";
    private String particleDeathType = "none";
    private boolean particleDeathColorful = false;
    private String particleTrailType = "none";
    
    private boolean bossBarEnabled = false;
    private String bossBarColor = "RED"; // BLUE | GREEN | PINK | PURPLE | RED | WHITE | YELLOW
    private String bossBarStyle = "PROGRESS"; // PROGRESS | NOTCHED_6 | NOTCHED_10 | NOTCHED_12 | NOTCHED_20
    private boolean bossBarDarkenSky = false;

    private List<String> potionEffects = new ArrayList<>();

    public StatModifiers() {}

    public StatModifiers(StatModifiers other) {
        this.healthValue = other.healthValue;
        this.damageValue = other.damageValue;
        this.speedValue = other.speedValue;
        this.followRangeValue = other.followRangeValue;
        this.knockbackValue = other.knockbackValue;
        this.regenerationValue = other.regenerationValue;
        this.equipmentTier = other.equipmentTier;
        this.equipmentChance = other.equipmentChance;
        this.helmetItem = other.helmetItem;
        this.helmetChance = other.helmetChance;
        this.helmetColor = other.helmetColor;
        this.chestplateItem = other.chestplateItem;
        this.chestplateChance = other.chestplateChance;
        this.chestplateColor = other.chestplateColor;
        this.leggingsItem = other.leggingsItem;
        this.leggingsChance = other.leggingsChance;
        this.leggingsColor = other.leggingsColor;
        this.bootsItem = other.bootsItem;
        this.bootsChance = other.bootsChance;
        this.bootsColor = other.bootsColor;
        this.mainHandItem = other.mainHandItem;
        this.mainHandChance = other.mainHandChance;
        this.offHandItem = other.offHandItem;
        this.offHandChance = other.offHandChance;
        this.resistanceFeu = other.resistanceFeu;
        this.resistanceMagie = other.resistanceMagie;
        this.resistanceProjectile = other.resistanceProjectile;
        this.goalWallVision = other.goalWallVision;
        this.immuneFire = other.immuneFire;
        this.immuneLava = other.immuneLava;
        this.immuneDrowning = other.immuneDrowning;
        this.immuneFall = other.immuneFall;
        this.explodeOnDeath = other.explodeOnDeath;
        this.fleeUnderHealth = other.fleeUnderHealth;
        this.passiveRegen = other.passiveRegen;
        this.camouflage = other.camouflage;
        this.teleportToTarget = other.teleportToTarget;
        this.spawnReinforcements = other.spawnReinforcements;
        this.dashAttack = other.dashAttack;
        this.groupAggro = other.groupAggro;
        this.rangedAttack = other.rangedAttack;
        this.targetPriority = other.targetPriority;
        this.fleeWhenSolo = other.fleeWhenSolo;
        this.deathSpawnVariant = other.deathSpawnVariant;
        this.deathSpawnAmount = other.deathSpawnAmount;
        this.creeperPowered = other.creeperPowered;
        this.golemCracked = other.golemCracked;
        this.sheepSheared = other.sheepSheared;
        this.smartClimb = other.smartClimb;
        this.endermanBlock = other.endermanBlock;
        this.entitySubtype = other.entitySubtype;
        this.skullSkin = other.skullSkin;
        this.teleportMaxUses = other.teleportMaxUses;
        this.dashMaxUses = other.dashMaxUses;
        this.jumpAttack = other.jumpAttack;
        this.rangedAttackType = other.rangedAttackType;
        this.onHitPotionEffect = other.onHitPotionEffect;
        this.onHitPotionChance = other.onHitPotionChance;
        this.onHitPotionDuration = other.onHitPotionDuration;
        this.onHitPotionAmplifier = other.onHitPotionAmplifier;
        this.particleAuraType = other.particleAuraType;
        this.particleAuraColor = other.particleAuraColor;
        this.particleAuraFreq = other.particleAuraFreq;
        this.particleSpawnType = other.particleSpawnType;
        this.particleDeathType = other.particleDeathType;
        this.particleDeathColorful = other.particleDeathColorful;
        this.particleTrailType = other.particleTrailType;
        this.bossBarEnabled = other.bossBarEnabled;
        this.bossBarColor = other.bossBarColor;
        this.bossBarStyle = other.bossBarStyle;
        this.bossBarDarkenSky = other.bossBarDarkenSky;
        this.potionEffects = new ArrayList<>(other.potionEffects);
    }

    public void stackWith(StatModifiers other) {
        if (other.healthValue != -1.0) this.healthValue = other.healthValue;
        if (other.damageValue != -1.0) this.damageValue = other.damageValue;
        if (other.speedValue != -1.0) this.speedValue = other.speedValue;
        if (other.followRangeValue != -1.0) this.followRangeValue = other.followRangeValue;
        if (other.knockbackValue != -1.0) this.knockbackValue = other.knockbackValue;
        if (other.regenerationValue != -1.0) this.regenerationValue = other.regenerationValue;

        if (!"none".equalsIgnoreCase(other.equipmentTier)) {
            this.equipmentTier = other.equipmentTier;
            this.equipmentChance = other.equipmentChance;
        }

        if (!"none".equalsIgnoreCase(other.helmetItem)) {
            this.helmetItem = other.helmetItem;
            this.helmetChance = other.helmetChance;
            this.helmetColor = other.helmetColor;
        }
        if (!"none".equalsIgnoreCase(other.chestplateItem)) {
            this.chestplateItem = other.chestplateItem;
            this.chestplateChance = other.chestplateChance;
            this.chestplateColor = other.chestplateColor;
        }
        if (!"none".equalsIgnoreCase(other.leggingsItem)) {
            this.leggingsItem = other.leggingsItem;
            this.leggingsChance = other.leggingsChance;
            this.leggingsColor = other.leggingsColor;
        }
        if (!"none".equalsIgnoreCase(other.bootsItem)) {
            this.bootsItem = other.bootsItem;
            this.bootsChance = other.bootsChance;
            this.bootsColor = other.bootsColor;
        }
        if (!"none".equalsIgnoreCase(other.mainHandItem)) {
            this.mainHandItem = other.mainHandItem;
            this.mainHandChance = other.mainHandChance;
        }
        if (!"none".equalsIgnoreCase(other.offHandItem)) {
            this.offHandItem = other.offHandItem;
            this.offHandChance = other.offHandChance;
        }

        if (other.resistanceFeu != 1.0) this.resistanceFeu = other.resistanceFeu;
        if (other.resistanceMagie != 1.0) this.resistanceMagie = other.resistanceMagie;
        if (other.resistanceProjectile != 1.0) this.resistanceProjectile = other.resistanceProjectile;

        if (other.goalWallVision) this.goalWallVision = true;
        if (other.immuneFire) this.immuneFire = true;
        if (other.immuneLava) this.immuneLava = true;
        if (other.immuneDrowning) this.immuneDrowning = true;
        if (other.immuneFall) this.immuneFall = true;
        if (other.explodeOnDeath) this.explodeOnDeath = true;
        if (other.fleeUnderHealth > 0.0) this.fleeUnderHealth = other.fleeUnderHealth;
        if (other.passiveRegen > 0.0) this.passiveRegen = other.passiveRegen;
        if (other.camouflage) this.camouflage = true;
        if (other.teleportToTarget) this.teleportToTarget = true;
        if (other.spawnReinforcements) this.spawnReinforcements = true;
        if (other.dashAttack) this.dashAttack = true;
        if (other.groupAggro) this.groupAggro = true;
        if (other.rangedAttack) this.rangedAttack = true;
        if (!"none".equals(other.targetPriority)) this.targetPriority = other.targetPriority;
        if (other.fleeWhenSolo) this.fleeWhenSolo = true;
        if (!"none".equals(other.deathSpawnVariant)) {
            this.deathSpawnVariant = other.deathSpawnVariant;
            this.deathSpawnAmount = other.deathSpawnAmount;
        }
        if (other.creeperPowered) this.creeperPowered = true;
        if (!"none".equals(other.golemCracked)) this.golemCracked = other.golemCracked;
        if (other.sheepSheared) this.sheepSheared = true;
        if (other.smartClimb) this.smartClimb = true;
        if (!"none".equals(other.endermanBlock)) this.endermanBlock = other.endermanBlock;
        if (!"none".equals(other.entitySubtype)) this.entitySubtype = other.entitySubtype;
        if (!"none".equals(other.skullSkin)) this.skullSkin = other.skullSkin;
        if (other.teleportMaxUses != -1) this.teleportMaxUses = other.teleportMaxUses;
        if (other.dashMaxUses != -1) this.dashMaxUses = other.dashMaxUses;
        if (other.jumpAttack) this.jumpAttack = true;
        if (!"ARROW".equals(other.rangedAttackType)) this.rangedAttackType = other.rangedAttackType;
        if (!"none".equals(other.onHitPotionEffect)) {
            this.onHitPotionEffect = other.onHitPotionEffect;
            this.onHitPotionChance = other.onHitPotionChance;
            this.onHitPotionDuration = other.onHitPotionDuration;
            this.onHitPotionAmplifier = other.onHitPotionAmplifier;
        }

        if (!"none".equals(other.particleAuraType)) {
            this.particleAuraType = other.particleAuraType;
            this.particleAuraColor = other.particleAuraColor;
            this.particleAuraFreq = other.particleAuraFreq;
        }
        if (!"none".equals(other.particleSpawnType)) this.particleSpawnType = other.particleSpawnType;
        if (!"none".equals(other.particleDeathType)) {
            this.particleDeathType = other.particleDeathType;
            this.particleDeathColorful = other.particleDeathColorful;
        }
        if (!"none".equals(other.particleTrailType)) this.particleTrailType = other.particleTrailType;

        if (other.bossBarEnabled) {
            this.bossBarEnabled = true;
            this.bossBarColor = other.bossBarColor;
            this.bossBarStyle = other.bossBarStyle;
            this.bossBarDarkenSky = other.bossBarDarkenSky;
        }

        if (!other.potionEffects.isEmpty()) {
            this.potionEffects.addAll(other.potionEffects);
        }
    }

    // ── Getters / Setters ────────────────────────────────────

    public double getHealthValue() { return healthValue; }
    public void setHealthValue(double v) { this.healthValue = v; }

    public double getDamageValue() { return damageValue; }
    public void setDamageValue(double v) { this.damageValue = v; }

    public double getSpeedValue() { return speedValue; }
    public void setSpeedValue(double v) { this.speedValue = v; }

    public double getFollowRangeValue() { return followRangeValue; }
    public void setFollowRangeValue(double v) { this.followRangeValue = v; }

    public double getKnockbackValue() { return knockbackValue; }
    public void setKnockbackValue(double v) { this.knockbackValue = v; }

    public String getEquipmentTier() { return equipmentTier; }
    public void setEquipmentTier(String v) { this.equipmentTier = (v == null) ? "none" : v; }

    public double getEquipmentChance() { return equipmentChance; }
    public void setEquipmentChance(double v) { this.equipmentChance = Math.max(0.0, Math.min(1.0, v)); }

    public double getResistanceFeu() { return resistanceFeu; }
    public void setResistanceFeu(double v) { this.resistanceFeu = v; }

    public double getResistanceMagie() { return resistanceMagie; }
    public void setResistanceMagie(double v) { this.resistanceMagie = v; }

    public double getResistanceProjectile() { return resistanceProjectile; }
    public void setResistanceProjectile(double v) { this.resistanceProjectile = v; }

    public boolean isGoalWallVision() { return goalWallVision; }
    public void setGoalWallVision(boolean v) { this.goalWallVision = v; }

    public boolean isImmuneFire() { return immuneFire; }
    public void setImmuneFire(boolean v) { this.immuneFire = v; }

    public boolean isImmuneLava() { return immuneLava; }
    public void setImmuneLava(boolean v) { this.immuneLava = v; }

    public boolean isImmuneDrowning() { return immuneDrowning; }
    public void setImmuneDrowning(boolean v) { this.immuneDrowning = v; }

    public boolean isImmuneFall() { return immuneFall; }
    public void setImmuneFall(boolean v) { this.immuneFall = v; }

    public boolean isExplodeOnDeath() { return explodeOnDeath; }
    public void setExplodeOnDeath(boolean v) { this.explodeOnDeath = v; }

    public double getFleeUnderHealth() { return fleeUnderHealth; }
    public void setFleeUnderHealth(double v) { this.fleeUnderHealth = v; }

    public double getPassiveRegen() { return passiveRegen; }
    public void setPassiveRegen(double v) { this.passiveRegen = v; }

    public boolean isCamouflage() { return camouflage; }
    public void setCamouflage(boolean v) { this.camouflage = v; }

    public boolean isTeleportToTarget() { return teleportToTarget; }
    public void setTeleportToTarget(boolean v) { this.teleportToTarget = v; }

    public boolean isSpawnReinforcements() { return spawnReinforcements; }
    public void setSpawnReinforcements(boolean v) { this.spawnReinforcements = v; }

    public boolean isDashAttack() { return dashAttack; }
    public void setDashAttack(boolean v) { this.dashAttack = v; }

    public boolean isGroupAggro() { return groupAggro; }
    public void setGroupAggro(boolean v) { this.groupAggro = v; }

    public boolean isRangedAttack() { return rangedAttack; }
    public void setRangedAttack(boolean v) { this.rangedAttack = v; }

    public String getTargetPriority() { return targetPriority; }
    public void setTargetPriority(String v) { this.targetPriority = v; }

    public String getParticleAuraType() { return particleAuraType; }
    public void setParticleAuraType(String v) { this.particleAuraType = v; }

    public String getParticleAuraColor() { return particleAuraColor; }
    public void setParticleAuraColor(String v) { this.particleAuraColor = v; }

    public int getParticleAuraFreq() { return particleAuraFreq; }
    public void setParticleAuraFreq(int v) { this.particleAuraFreq = v; }

    public String getParticleSpawnType() { return particleSpawnType; }
    public void setParticleSpawnType(String v) { this.particleSpawnType = v; }

    public String getParticleDeathType() { return particleDeathType; }
    public void setParticleDeathType(String v) { this.particleDeathType = v; }

    public boolean isParticleDeathColorful() { return particleDeathColorful; }
    public void setParticleDeathColorful(boolean v) { this.particleDeathColorful = v; }

    public String getParticleTrailType() { return particleTrailType; }
    public void setParticleTrailType(String v) { this.particleTrailType = v; }

    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public void setBossBarEnabled(boolean v) { this.bossBarEnabled = v; }

    public String getBossBarColor() { return bossBarColor; }
    public void setBossBarColor(String v) { this.bossBarColor = v; }

    public String getBossBarStyle() { return bossBarStyle; }
    public void setBossBarStyle(String v) { this.bossBarStyle = v; }

    public boolean isBossBarDarkenSky() { return bossBarDarkenSky; }
    public void setBossBarDarkenSky(boolean v) { this.bossBarDarkenSky = v; }

    public List<String> getPotionEffects() { return potionEffects; }
    public void setPotionEffects(List<String> v) { this.potionEffects = v; }

    public double getRegenerationValue() { return regenerationValue; }
    public void setRegenerationValue(double v) { this.regenerationValue = v; }

    public String getHelmetItem() { return helmetItem; }
    public void setHelmetItem(String v) { this.helmetItem = (v == null) ? "none" : v; }
    public double getHelmetChance() { return helmetChance; }
    public void setHelmetChance(double v) { this.helmetChance = v; }
    public String getHelmetColor() { return helmetColor; }
    public void setHelmetColor(String v) { this.helmetColor = (v == null) ? "none" : v; }

    public String getChestplateItem() { return chestplateItem; }
    public void setChestplateItem(String v) { this.chestplateItem = (v == null) ? "none" : v; }
    public double getChestplateChance() { return chestplateChance; }
    public void setChestplateChance(double v) { this.chestplateChance = v; }
    public String getChestplateColor() { return chestplateColor; }
    public void setChestplateColor(String v) { this.chestplateColor = (v == null) ? "none" : v; }

    public String getLeggingsItem() { return leggingsItem; }
    public void setLeggingsItem(String v) { this.leggingsItem = (v == null) ? "none" : v; }
    public double getLeggingsChance() { return leggingsChance; }
    public void setLeggingsChance(double v) { this.leggingsChance = v; }
    public String getLeggingsColor() { return leggingsColor; }
    public void setLeggingsColor(String v) { this.leggingsColor = (v == null) ? "none" : v; }

    public String getBootsItem() { return bootsItem; }
    public void setBootsItem(String v) { this.bootsItem = (v == null) ? "none" : v; }
    public double getBootsChance() { return bootsChance; }
    public void setBootsChance(double v) { this.bootsChance = v; }
    public String getBootsColor() { return bootsColor; }
    public void setBootsColor(String v) { this.bootsColor = (v == null) ? "none" : v; }

    public String getMainHandItem() { return mainHandItem; }
    public void setMainHandItem(String v) { this.mainHandItem = (v == null) ? "none" : v; }
    public double getMainHandChance() { return mainHandChance; }
    public void setMainHandChance(double v) { this.mainHandChance = v; }

    public String getOffHandItem() { return offHandItem; }
    public void setOffHandItem(String v) { this.offHandItem = (v == null) ? "none" : v; }
    public double getOffHandChance() { return offHandChance; }
    public void setOffHandChance(double v) { this.offHandChance = v; }

    public boolean isFleeWhenSolo() { return fleeWhenSolo; }
    public void setFleeWhenSolo(boolean v) { this.fleeWhenSolo = v; }
    public String getDeathSpawnVariant() { return deathSpawnVariant; }
    public void setDeathSpawnVariant(String v) { this.deathSpawnVariant = (v == null) ? "none" : v; }
    public int getDeathSpawnAmount() { return deathSpawnAmount; }
    public void setDeathSpawnAmount(int v) { this.deathSpawnAmount = v; }
    public boolean isCreeperPowered() { return creeperPowered; }
    public void setCreeperPowered(boolean v) { this.creeperPowered = v; }
    public String getGolemCracked() { return golemCracked; }
    public void setGolemCracked(String v) { this.golemCracked = (v == null) ? "none" : v; }
    public boolean isSheepSheared() { return sheepSheared; }
    public void setSheepSheared(boolean v) { this.sheepSheared = v; }
    public boolean isSmartClimb() { return smartClimb; }
    public void setSmartClimb(boolean v) { this.smartClimb = v; }
    public String getEndermanBlock() { return endermanBlock; }
    public void setEndermanBlock(String v) { this.endermanBlock = (v == null) ? "none" : v; }
    public String getSkullSkin() { return skullSkin; }
    public void setSkullSkin(String v) { this.skullSkin = (v == null) ? "none" : v; }
    public String getEntitySubtype() { return entitySubtype; }
    public void setEntitySubtype(String v) { this.entitySubtype = (v == null) ? "none" : v; }

    public int getTeleportMaxUses() { return teleportMaxUses; }
    public void setTeleportMaxUses(int v) { this.teleportMaxUses = v; }
    public int getDashMaxUses() { return dashMaxUses; }
    public void setDashMaxUses(int v) { this.dashMaxUses = v; }
    public boolean isJumpAttack() { return jumpAttack; }
    public void setJumpAttack(boolean v) { this.jumpAttack = v; }
    public String getRangedAttackType() { return rangedAttackType; }
    public void setRangedAttackType(String v) { this.rangedAttackType = (v == null) ? "ARROW" : v; }
    public String getOnHitPotionEffect() { return onHitPotionEffect; }
    public void setOnHitPotionEffect(String v) { this.onHitPotionEffect = (v == null) ? "none" : v; }
    public double getOnHitPotionChance() { return onHitPotionChance; }
    public void setOnHitPotionChance(double v) { this.onHitPotionChance = v; }
    public int getOnHitPotionDuration() { return onHitPotionDuration; }
    public void setOnHitPotionDuration(int v) { this.onHitPotionDuration = v; }
    public int getOnHitPotionAmplifier() { return onHitPotionAmplifier; }
    public void setOnHitPotionAmplifier(int v) { this.onHitPotionAmplifier = v; }
}
