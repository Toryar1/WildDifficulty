package fr.wilddifficulty.variant;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.BiomeConfigManager;
import fr.wilddifficulty.config.StatModifiers;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import fr.wilddifficulty.listener.MobSpawnListener;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class VariantManager {

    private final WildDifficultyPlugin plugin;
    private final Map<String, MobVariant> variants = new LinkedHashMap<>();
    private final Map<String, MobSquad> squads = new LinkedHashMap<>();
    private final Random random = new Random();

    public VariantManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        variants.clear();
        squads.clear();
        loadCustomHeads();
        File file = new File(plugin.getDataFolder(), "mob-variants.yml");
        if (!file.exists()) {
            plugin.saveResource("mob-variants.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Load Variants
        ConfigurationSection varSection = cfg.getConfigurationSection("variantes");
        if (varSection != null) {
            for (String id : varSection.getKeys(false)) {
                ConfigurationSection sec = varSection.getConfigurationSection(id);
                if (sec == null) continue;
                try {
                    EntityType type = EntityType.valueOf(sec.getString("type", "").toUpperCase());
                    String displayName = sec.getString("nom-affiche", null);
                    double weight = sec.getDouble("poids", 1.0);
                    boolean ignoreSunlight = sec.getBoolean("ignore-soleil", false);
                    boolean baby = sec.getBoolean("bebe", false);
                    double scale = sec.getDouble("taille", 1.0);
                    double scaleVariance = sec.getDouble("taille-variance", 0.0);

                    StatModifiers modifiers = new StatModifiers();
                    ConfigurationSection modSec = sec.getConfigurationSection("modificateurs");
                    if (modSec != null) {
                        modifiers = BiomeConfigManager.readModifiers(modSec);
                    }
                    
                    MobVariant variant = new MobVariant(id, type, displayName, weight, ignoreSunlight, modifiers);
                    variant.setBaby(baby);
                    variant.setScale(scale);
                    variant.setScaleVariance(scaleVariance);
                    variant.setAllowedBiomes(sec.getStringList("biomes-autorises"));
                    variant.setSpawnWeather(sec.getString("meteo", "ANY"));
                    variant.setSpawnTime(sec.getString("moment-journee", "ANY"));
                    variant.setCaveSpawn(sec.getString("cave-spawn", "ANY"));
                    variant.setAggroMode(sec.getString("comportement-aggro", "PASSIVE"));
                    variant.setCustomModelData(sec.getInt("custom-model-data", 0));
                    variant.setXpOnDeath(sec.getInt("xp-mort", -1));

                    // conditionalNames
                    List<MobVariant.ConditionalName> conds = new ArrayList<>();
                    if (sec.isList("noms-conditionnels")) {
                        for (Map<?, ?> map : sec.getMapList("noms-conditionnels")) {
                            double threshold = ((Number) map.get("seuil")).doubleValue();
                            String name = (String) map.get("nom");
                            conds.add(new MobVariant.ConditionalName(threshold, name));
                        }
                    }
                    variant.setConditionalNames(conds);

                    // possibleHandItems
                    List<MobVariant.PossibleItem> hands = new ArrayList<>();
                    if (sec.isList("equipement-main")) {
                        for (Map<?, ?> map : sec.getMapList("equipement-main")) {
                            String matName = (String) map.get("materiau");
                            double chance = ((Number) map.get("chance")).doubleValue();
                            hands.add(new MobVariant.PossibleItem(matName, chance));
                        }
                    }
                    variant.setPossibleHandItems(hands);

                    // customDrops
                    List<MobVariant.CustomDrop> drops = new ArrayList<>();
                    if (sec.isList("drops-custom")) {
                        for (Map<?, ?> map : sec.getMapList("drops-custom")) {
                            String matName = (String) map.get("materiau");
                            double chance = ((Number) map.get("chance")).doubleValue();
                            int min = ((Number) map.get("min")).intValue();
                            int max = ((Number) map.get("max")).intValue();
                            int xp = map.containsKey("xp") ? ((Number) map.get("xp")).intValue() : 0;
                            String cond = map.containsKey("condition") ? (String) map.get("condition") : "none";
                            drops.add(new MobVariant.CustomDrop(matName, chance, min, max, xp, cond));
                        }
                    }
                    variant.setCustomDrops(drops);

                    // customSounds
                    Map<String, MobVariant.SoundConfig> sounds = new HashMap<>();
                    ConfigurationSection soundSec = sec.getConfigurationSection("sons");
                    if (soundSec != null) {
                        for (String key : soundSec.getKeys(false)) {
                            ConfigurationSection sSec = soundSec.getConfigurationSection(key);
                            if (sSec != null) {
                                String sKey = sSec.getString("son");
                                float vol = (float) sSec.getDouble("volume", 1.0);
                                float pitch = (float) sSec.getDouble("pitch", 1.0);
                                sounds.put(key, new MobVariant.SoundConfig(sKey, vol, pitch));
                            }
                        }
                    }
                    variant.setCustomSounds(sounds);

                    variants.put(id, variant);
                    if (variant.getModifiers() != null && !"none".equalsIgnoreCase(variant.getModifiers().getSkullSkin())) {
                        fr.wilddifficulty.util.SkinCacheUtil.cacheSkin(plugin, variant.getId(), variant.getModifiers().getSkullSkin());
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Type d'entité invalide pour la variante : " + id);
                }
            }
        }

        // Load Squads
        ConfigurationSection squadSection = cfg.getConfigurationSection("escouades");
        if (squadSection != null) {
            for (String id : squadSection.getKeys(false)) {
                ConfigurationSection sec = squadSection.getConfigurationSection(id);
                if (sec == null) continue;
                List<String> triggerTypes = sec.getStringList("types-declencheurs").stream().map(String::toUpperCase).collect(Collectors.toList());
                double chance = sec.getDouble("chance-declenchement", 0.0);
                List<String> allowedBiomes = sec.getStringList("biomes-autorises").stream().map(String::toUpperCase).collect(Collectors.toList());
                List<String> allowedZones = sec.getStringList("zones-autorisees");
                
                Map<String, MobSquad.SquadMemberRange> members = new HashMap<>();
                ConfigurationSection memSec = sec.getConfigurationSection("membres");
                if (memSec != null) {
                    for (String varId : memSec.getKeys(false)) {
                        ConfigurationSection mSec = memSec.getConfigurationSection(varId);
                        if (mSec != null) {
                            members.put(varId, new MobSquad.SquadMemberRange(mSec.getInt("min", 1), mSec.getInt("max", 1)));
                        }
                    }
                }
                MobSquad squad = new MobSquad(id, triggerTypes, chance, allowedBiomes, allowedZones, members);
                squad.setBonusHealth(sec.getDouble("bonus-pv", 1.0));
                squad.setBonusDamage(sec.getDouble("bonus-degats", 1.0));
                squad.setBonusSpeed(sec.getDouble("bonus-vitesse", 1.0));
                squad.setBonusRegen(sec.getDouble("bonus-regen", 0.0));
                squads.put(id, squad);
            }
        }
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "mob-variants.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Clear existing keys
        cfg.set("variantes", null);
        cfg.set("escouades", null);

        // Save Variants
        for (MobVariant var : variants.values()) {
            String path = "variantes." + var.getId() + ".";
            cfg.set(path + "type", var.getType().name());
            if (var.getDisplayName() != null) cfg.set(path + "nom-affiche", var.getDisplayName());
            cfg.set(path + "poids", var.getWeight());
            if (var.isIgnoreSunlight()) cfg.set(path + "ignore-soleil", true);
            if (var.isBaby()) cfg.set(path + "bebe", true);
            if (var.getScale() != 1.0) cfg.set(path + "taille", var.getScale());
            if (var.getScaleVariance() != 0.0) cfg.set(path + "taille-variance", var.getScaleVariance());
            if (!var.getAllowedBiomes().isEmpty()) cfg.set(path + "biomes-autorises", var.getAllowedBiomes());
            if (!var.getSpawnWeather().equalsIgnoreCase("ANY")) cfg.set(path + "meteo", var.getSpawnWeather());
            if (!var.getSpawnTime().equalsIgnoreCase("ANY")) cfg.set(path + "moment-journee", var.getSpawnTime());
            if (!var.getCaveSpawn().equalsIgnoreCase("ANY")) cfg.set(path + "cave-spawn", var.getCaveSpawn());
            if (!var.getAggroMode().equalsIgnoreCase("PASSIVE")) cfg.set(path + "comportement-aggro", var.getAggroMode());
            if (var.getCustomModelData() > 0) cfg.set(path + "custom-model-data", var.getCustomModelData());
            if (var.getXpOnDeath() >= 0) cfg.set(path + "xp-mort", var.getXpOnDeath());

            // conditionalNames
            if (!var.getConditionalNames().isEmpty()) {
                List<Map<String, Object>> condList = new ArrayList<>();
                for (MobVariant.ConditionalName cn : var.getConditionalNames()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("seuil", cn.getThreshold());
                    map.put("nom", cn.getName());
                    condList.add(map);
                }
                cfg.set(path + "noms-conditionnels", condList);
            }

            // possibleHandItems
            if (!var.getPossibleHandItems().isEmpty()) {
                List<Map<String, Object>> handList = new ArrayList<>();
                for (MobVariant.PossibleItem pi : var.getPossibleHandItems()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("materiau", pi.getMaterialName());
                    map.put("chance", pi.getChance());
                    handList.add(map);
                }
                cfg.set(path + "equipement-main", handList);
            }

            // customDrops
            if (!var.getCustomDrops().isEmpty()) {
                List<Map<String, Object>> dropList = new ArrayList<>();
                for (MobVariant.CustomDrop cd : var.getCustomDrops()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("materiau", cd.getMaterialName());
                    map.put("chance", cd.getChance());
                    map.put("min", cd.getMinAmount());
                    map.put("max", cd.getMaxAmount());
                    map.put("xp", cd.getXp());
                    map.put("condition", cd.getDeathCondition());
                    dropList.add(map);
                }
                cfg.set(path + "drops-custom", dropList);
            }

            // customSounds
            if (!var.getCustomSounds().isEmpty()) {
                for (Map.Entry<String, MobVariant.SoundConfig> entry : var.getCustomSounds().entrySet()) {
                    String sub = path + "sons." + entry.getKey() + ".";
                    cfg.set(sub + "son", entry.getValue().getSoundKey());
                    cfg.set(sub + "volume", entry.getValue().getVolume());
                    cfg.set(sub + "pitch", entry.getValue().getPitch());
                }
            }

            StatModifiers mods = var.getModifiers();
            if (mods != null) {
                String mPath = path + "modificateurs.";
                if (mods.getHealthValue() != -1.0) cfg.set(mPath + "valeur-pv", mods.getHealthValue());
                if (mods.getDamageValue() != -1.0) cfg.set(mPath + "valeur-degats", mods.getDamageValue());
                if (mods.getSpeedValue() != -1.0) cfg.set(mPath + "valeur-vitesse", mods.getSpeedValue());
                if (mods.getFollowRangeValue() != -1.0) cfg.set(mPath + "valeur-portee-detection", mods.getFollowRangeValue());
                if (mods.getKnockbackValue() != -1.0) cfg.set(mPath + "valeur-resistance-knockback", mods.getKnockbackValue());
                if (mods.getRegenerationValue() != -1.0) cfg.set(mPath + "valeur-regen", mods.getRegenerationValue());
                
                if (!mods.getEquipmentTier().equals("none")) cfg.set(mPath + "tier-equipement", mods.getEquipmentTier());
                if (mods.getEquipmentChance() > 0) cfg.set(mPath + "chance-equipement", mods.getEquipmentChance());

                if (!mods.getHelmetItem().equals("none")) cfg.set(mPath + "casque-item", mods.getHelmetItem());
                if (mods.getHelmetChance() > 0) cfg.set(mPath + "casque-chance", mods.getHelmetChance());
                if (!mods.getChestplateItem().equals("none")) cfg.set(mPath + "plastron-item", mods.getChestplateItem());
                if (mods.getChestplateChance() > 0) cfg.set(mPath + "plastron-chance", mods.getChestplateChance());
                if (!mods.getLeggingsItem().equals("none")) cfg.set(mPath + "jambieres-item", mods.getLeggingsItem());
                if (mods.getLeggingsChance() > 0) cfg.set(mPath + "jambieres-chance", mods.getLeggingsChance());
                if (!mods.getBootsItem().equals("none")) cfg.set(mPath + "bottes-item", mods.getBootsItem());
                if (mods.getBootsChance() > 0) cfg.set(mPath + "bottes-chance", mods.getBootsChance());
                if (!mods.getMainHandItem().equals("none")) cfg.set(mPath + "main-principale-item", mods.getMainHandItem());
                if (mods.getMainHandChance() > 0) cfg.set(mPath + "main-principale-chance", mods.getMainHandChance());
                if (!mods.getOffHandItem().equals("none")) cfg.set(mPath + "main-secondaire-item", mods.getOffHandItem());
                if (mods.getOffHandChance() > 0) cfg.set(mPath + "main-secondaire-chance", mods.getOffHandChance());

                if (mods.getResistanceFeu() != 1.0) cfg.set(mPath + "resistance-feu", mods.getResistanceFeu());
                if (mods.getResistanceMagie() != 1.0) cfg.set(mPath + "resistance-magie", mods.getResistanceMagie());
                if (mods.getResistanceProjectile() != 1.0) cfg.set(mPath + "resistance-projectile", mods.getResistanceProjectile());

                if (mods.isGoalWallVision()) cfg.set(mPath + "vision-murs", true);
                if (mods.isImmuneFire()) cfg.set(mPath + "immunite-feu", true);
                if (mods.isImmuneLava()) cfg.set(mPath + "immunite-lave", true);
                if (mods.isImmuneDrowning()) cfg.set(mPath + "immunite-noyade", true);
                if (mods.isImmuneFall()) cfg.set(mPath + "immunite-chute", true);
                if (mods.isExplodeOnDeath()) cfg.set(mPath + "explosion-mort", true);
                if (mods.getFleeUnderHealth() > 0.0) cfg.set(mPath + "fuite-seuil-pv", mods.getFleeUnderHealth());
                if (mods.getPassiveRegen() > 0.0) cfg.set(mPath + "regen-passive", mods.getPassiveRegen());
                if (mods.isCamouflage()) cfg.set(mPath + "camouflage", true);
                if (mods.isTeleportToTarget()) cfg.set(mPath + "teleportation-cible", true);
                if (mods.isSpawnReinforcements()) cfg.set(mPath + "renforts-aggro", true);
                if (mods.isDashAttack()) cfg.set(mPath + "charge-cible", true);
                if (mods.isGroupAggro()) cfg.set(mPath + "comportement-groupe", true);
                if (mods.isRangedAttack()) cfg.set(mPath + "attaque-distance-simulee", true);
                if (!"none".equals(mods.getTargetPriority())) cfg.set(mPath + "ciblage-prioritaire", mods.getTargetPriority());

                if (mods.isFleeWhenSolo()) cfg.set(mPath + "fuite-solo", true);
                if (!"none".equals(mods.getDeathSpawnVariant())) {
                    cfg.set(mPath + "spawn-mort-variante", mods.getDeathSpawnVariant());
                    cfg.set(mPath + "spawn-mort-quantite", mods.getDeathSpawnAmount());
                }
                if (mods.isCreeperPowered()) cfg.set(mPath + "creeper-charge", true);
                if (!"none".equalsIgnoreCase(mods.getGolemCracked())) cfg.set(mPath + "golem-fissure", mods.getGolemCracked());
                if (mods.isSheepSheared()) cfg.set(mPath + "mouton-tondu", true);
                if (!"none".equals(mods.getEndermanBlock())) cfg.set(mPath + "enderman-bloc", mods.getEndermanBlock());
                if (!"none".equals(mods.getSkullSkin())) cfg.set(mPath + "skin-head-skull", mods.getSkullSkin());

                if (mods.getTeleportMaxUses() != -1) cfg.set(mPath + "teleport-max-uses", mods.getTeleportMaxUses());
                if (mods.getDashMaxUses() != -1) cfg.set(mPath + "dash-max-uses", mods.getDashMaxUses());
                if (mods.isJumpAttack()) cfg.set(mPath + "jump-attack", true);
                if (!"ARROW".equalsIgnoreCase(mods.getRangedAttackType())) cfg.set(mPath + "ranged-attack-type", mods.getRangedAttackType());
                if (!"none".equals(mods.getOnHitPotionEffect())) {
                    cfg.set(mPath + "on-hit-potion-effect", mods.getOnHitPotionEffect());
                    cfg.set(mPath + "on-hit-potion-chance", mods.getOnHitPotionChance());
                    cfg.set(mPath + "on-hit-potion-duration", mods.getOnHitPotionDuration());
                    cfg.set(mPath + "on-hit-potion-amplifier", mods.getOnHitPotionAmplifier());
                }

                if (!"none".equals(mods.getParticleAuraType())) {
                    cfg.set(mPath + "particule-aura-type", mods.getParticleAuraType());
                    cfg.set(mPath + "particule-aura-couleur", mods.getParticleAuraColor());
                    cfg.set(mPath + "particule-aura-frequence", mods.getParticleAuraFreq());
                }
                if (!"none".equals(mods.getParticleSpawnType())) cfg.set(mPath + "particule-spawn-type", mods.getParticleSpawnType());
                if (!"none".equals(mods.getParticleDeathType())) {
                    cfg.set(mPath + "particule-mort-type", mods.getParticleDeathType());
                    cfg.set(mPath + "particule-mort-colore", mods.isParticleDeathColorful());
                }
                if (!"none".equals(mods.getParticleTrailType())) cfg.set(mPath + "particule-trail-type", mods.getParticleTrailType());

                if (mods.isBossBarEnabled()) {
                    cfg.set(mPath + "bossbar-active", true);
                    cfg.set(mPath + "bossbar-couleur", mods.getBossBarColor());
                    cfg.set(mPath + "bossbar-style", mods.getBossBarStyle());
                    cfg.set(mPath + "bossbar-ciel-sombre", mods.isBossBarDarkenSky());
                }

                if (!mods.getPotionEffects().isEmpty()) cfg.set(mPath + "effets-statut", mods.getPotionEffects());
            }
        }

        // Save Squads
        for (MobSquad sq : squads.values()) {
            String path = "escouades." + sq.getId() + ".";
            cfg.set(path + "types-declencheurs", sq.getTriggerTypes());
            cfg.set(path + "chance-declenchement", sq.getSpawnChance());
            if (!sq.getAllowedBiomes().isEmpty()) cfg.set(path + "biomes-autorises", sq.getAllowedBiomes());
            if (!sq.getAllowedZones().isEmpty()) cfg.set(path + "zones-autorisees", sq.getAllowedZones());
            
            cfg.set(path + "bonus-pv", sq.getBonusHealth());
            cfg.set(path + "bonus-degats", sq.getBonusDamage());
            cfg.set(path + "bonus-vitesse", sq.getBonusSpeed());
            cfg.set(path + "bonus-regen", sq.getBonusRegen());
            
            for (Map.Entry<String, MobSquad.SquadMemberRange> entry : sq.getMembers().entrySet()) {
                cfg.set(path + "membres." + entry.getKey() + ".min", entry.getValue().getMin());
                cfg.set(path + "membres." + entry.getKey() + ".max", entry.getValue().getMax());
            }
        }

        try {
            cfg.save(file);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (MobVariant var : variants.values()) {
                    plugin.updateVisualizedMobs(var.getId());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde de mob-variants.yml");
            e.printStackTrace();
        }
    }

    public void addVariant(MobVariant variant) {
        variants.put(variant.getId(), variant);
    }

    public void removeVariant(String id) {
        variants.remove(id);
    }

    public void addSquad(MobSquad squad) {
        squads.put(squad.getId(), squad);
    }

    public void removeSquad(String id) {
        squads.remove(id);
    }

    public MobVariant getVariant(String id) {
        return variants.get(id);
    }

    public MobSquad getSquad(String id) {
        return squads.get(id);
    }

    public Collection<MobVariant> getAllVariants() {
        return variants.values();
    }

    public Collection<MobSquad> getAllSquads() {
        return squads.values();
    }

    public MobVariant getRandomVariantFor(EntityType type, List<String> allowedVariants, List<String> deniedVariants, org.bukkit.World world, String biomeKey, org.bukkit.Location loc) {
        List<MobVariant> candidates = new ArrayList<>();
        double totalWeight = 0.0;
        
        for (MobVariant variant : variants.values()) {
            if (variant.getType() == type) {
                if (deniedVariants != null && deniedVariants.contains(variant.getId())) continue;
                if (allowedVariants != null && !allowedVariants.isEmpty() && !allowedVariants.contains(variant.getId())) continue;
                
                // Weather condition check
                if (!variant.getSpawnWeather().equalsIgnoreCase("ANY")) {
                    boolean isRaining = world.hasStorm();
                    if (variant.getSpawnWeather().equalsIgnoreCase("RAINY") && !isRaining) continue;
                    if (variant.getSpawnWeather().equalsIgnoreCase("CLEAR") && isRaining) continue;
                }
                
                // Time condition check
                if (!variant.getSpawnTime().equalsIgnoreCase("ANY")) {
                    boolean isCaveLoc = loc.getBlock().getLightFromSky() == 0 && loc.getWorld().getHighestBlockYAt(loc) > loc.getY();
                    if (!isCaveLoc) {
                        long time = world.getTime();
                        boolean isDay = time > 0 && time < 12300;
                        if (variant.getSpawnTime().equalsIgnoreCase("DAY") && !isDay) continue;
                        if (variant.getSpawnTime().equalsIgnoreCase("NIGHT") && isDay) continue;
                    }
                }

                // Cave condition check
                if (!variant.getCaveSpawn().equalsIgnoreCase("ANY")) {
                    boolean isCave = loc.getBlock().getLightFromSky() == 0 && loc.getWorld().getHighestBlockYAt(loc) > loc.getY();
                    if (variant.getCaveSpawn().equalsIgnoreCase("ONLY_CAVES") && !isCave) continue;
                    if (variant.getCaveSpawn().equalsIgnoreCase("NO_CAVES") && isCave) continue;
                }
 
                // Biome condition check
                if (!isBiomeMatch(variant.getAllowedBiomes(), biomeKey)) continue;

                if (variant.getWeight() > 0.0) {
                    candidates.add(variant);
                    totalWeight += variant.getWeight();
                }
            }
        }

        if (candidates.isEmpty() || totalWeight <= 0.0) return null;

        double roll = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;
        for (MobVariant variant : candidates) {
            currentWeight += variant.getWeight();
            if (roll < currentWeight) {
                return variant;
            }
        }
        return null;
    }

    public static boolean isBiomeMatch(List<String> allowedBiomes, String biomeKey) {
        if (allowedBiomes == null || allowedBiomes.isEmpty()) return true;
        for (String allowed : allowedBiomes) {
            if (allowed.equalsIgnoreCase(biomeKey)) return true;
            String cleanAllowed = allowed.toLowerCase().contains(":") ? allowed.substring(allowed.indexOf(":") + 1) : allowed;
            String cleanBiomeKey = biomeKey.toLowerCase().contains(":") ? biomeKey.substring(biomeKey.indexOf(":") + 1) : biomeKey;
            if (cleanAllowed.equalsIgnoreCase(cleanBiomeKey)) return true;
        }
        return false;
    }

    public MobSquad rollSquad(EntityType type, String biome, String zoneId, List<String> allowedSquads, List<String> deniedSquads) {
        String typeName = type.name();
        for (MobSquad squad : squads.values()) {
            if (deniedSquads != null && deniedSquads.contains(squad.getId())) continue;
            if (allowedSquads != null && !allowedSquads.isEmpty() && !allowedSquads.contains(squad.getId())) continue;

            if (squad.getTriggerTypes().contains(typeName)) {
                if (!isBiomeMatch(squad.getAllowedBiomes(), biome)) continue;
                if (!squad.getAllowedZones().isEmpty() && (zoneId == null || !squad.getAllowedZones().contains(zoneId))) continue;
                
                if (random.nextDouble() < squad.getSpawnChance()) {
                    return squad;
                }
            }
        }
        return null;
    }

    public int getRandomMemberCount(MobSquad.SquadMemberRange range) {
        if (range.getMin() >= range.getMax()) return range.getMin();
        return range.getMin() + random.nextInt(range.getMax() - range.getMin() + 1);
    }

    public MobVariant cloneVariant(MobVariant original, String newId) {
        StatModifiers originalMods = original.getModifiers();
        StatModifiers newMods = new StatModifiers();
        if (originalMods != null) {
            newMods.setHealthValue(originalMods.getHealthValue());
            newMods.setDamageValue(originalMods.getDamageValue());
            newMods.setSpeedValue(originalMods.getSpeedValue());
            newMods.setFollowRangeValue(originalMods.getFollowRangeValue());
            newMods.setKnockbackValue(originalMods.getKnockbackValue());
            newMods.setRegenerationValue(originalMods.getRegenerationValue());
            newMods.setEquipmentTier(originalMods.getEquipmentTier());
            newMods.setEquipmentChance(originalMods.getEquipmentChance());
            newMods.setPassiveRegen(originalMods.getPassiveRegen());
            newMods.setExplodeOnDeath(originalMods.isExplodeOnDeath());
            newMods.setDashAttack(originalMods.isDashAttack());
            newMods.setTeleportToTarget(originalMods.isTeleportToTarget());
            newMods.setGoalWallVision(originalMods.isGoalWallVision());
            newMods.setCamouflage(originalMods.isCamouflage());
            newMods.setRangedAttack(originalMods.isRangedAttack());
            newMods.setResistanceFeu(originalMods.getResistanceFeu());
            newMods.getPotionEffects().addAll(originalMods.getPotionEffects());
            newMods.setHelmetItem(originalMods.getHelmetItem());
            newMods.setHelmetChance(originalMods.getHelmetChance());
            newMods.setChestplateItem(originalMods.getChestplateItem());
            newMods.setChestplateChance(originalMods.getChestplateChance());
            newMods.setLeggingsItem(originalMods.getLeggingsItem());
            newMods.setLeggingsChance(originalMods.getLeggingsChance());
            newMods.setBootsItem(originalMods.getBootsItem());
            newMods.setBootsChance(originalMods.getBootsChance());
            newMods.setMainHandItem(originalMods.getMainHandItem());
            newMods.setMainHandChance(originalMods.getMainHandChance());
            newMods.setOffHandItem(originalMods.getOffHandItem());
            newMods.setOffHandChance(originalMods.getOffHandChance());
            newMods.setBossBarEnabled(originalMods.isBossBarEnabled());
            newMods.setBossBarColor(originalMods.getBossBarColor());
            newMods.setBossBarStyle(originalMods.getBossBarStyle());
            newMods.setParticleAuraType(originalMods.getParticleAuraType());
            newMods.setParticleAuraColor(originalMods.getParticleAuraColor());
            newMods.setParticleSpawnType(originalMods.getParticleSpawnType());
            newMods.setParticleTrailType(originalMods.getParticleTrailType());
            newMods.setTeleportMaxUses(originalMods.getTeleportMaxUses());
            newMods.setDashMaxUses(originalMods.getDashMaxUses());
            newMods.setJumpAttack(originalMods.isJumpAttack());
            newMods.setRangedAttackType(originalMods.getRangedAttackType());
            newMods.setOnHitPotionEffect(originalMods.getOnHitPotionEffect());
            newMods.setOnHitPotionChance(originalMods.getOnHitPotionChance());
            newMods.setOnHitPotionDuration(originalMods.getOnHitPotionDuration());
            newMods.setOnHitPotionAmplifier(originalMods.getOnHitPotionAmplifier());
        }

        MobVariant clone = new MobVariant(newId, original.getType(), original.getDisplayName(), original.getWeight(), original.isIgnoreSunlight(), newMods);
        clone.setBaby(original.isBaby());
        clone.setScale(original.getScale());
        clone.setScaleVariance(original.getScaleVariance());
        clone.setAllowedBiomes(new java.util.ArrayList<>(original.getAllowedBiomes()));
        clone.setSpawnWeather(original.getSpawnWeather());
        clone.setSpawnTime(original.getSpawnTime());
        clone.setCaveSpawn(original.getCaveSpawn());
        clone.setAggroMode(original.getAggroMode());
        clone.setCustomModelData(original.getCustomModelData());
        clone.setXpOnDeath(original.getXpOnDeath());
        
        for (MobVariant.ConditionalName cn : original.getConditionalNames()) {
            clone.getConditionalNames().add(new MobVariant.ConditionalName(cn.getThreshold(), cn.getName()));
        }
        for (MobVariant.PossibleItem pi : original.getPossibleHandItems()) {
            clone.getPossibleHandItems().add(new MobVariant.PossibleItem(pi.getMaterialName(), pi.getChance()));
        }
        for (MobVariant.CustomDrop cd : original.getCustomDrops()) {
            clone.getCustomDrops().add(new MobVariant.CustomDrop(cd.getMaterialName(), cd.getChance(), cd.getMinAmount(), cd.getMaxAmount(), cd.getXp(), cd.getDeathCondition()));
        }
        for (Map.Entry<String, MobVariant.SoundConfig> entry : original.getCustomSounds().entrySet()) {
            clone.getCustomSounds().put(entry.getKey(), new MobVariant.SoundConfig(entry.getValue().getSoundKey(), entry.getValue().getVolume(), entry.getValue().getPitch()));
        }

        return clone;
    }

    // --- Custom Heads Storage ---
    public static class CustomHead {
        private final String name;
        private final String value;
        public CustomHead(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String getName() { return name; }
        public String getValue() { return value; }
    }

    private final List<CustomHead> customHeads = new ArrayList<>();

    public List<CustomHead> getCustomHeads() {
        return customHeads;
    }

    public void addCustomHead(String name, String value) {
        customHeads.add(new CustomHead(name, value));
        saveCustomHeads();
    }

    public void loadCustomHeads() {
        customHeads.clear();
        File file = new File(plugin.getDataFolder(), "custom-heads.yml");
        if (file.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection sec = cfg.getConfigurationSection("heads");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    String val = sec.getString(key);
                    if (val != null) {
                        customHeads.add(new CustomHead(key, val));
                    }
                }
            }
        }

        // Charger aussi depuis le dossier skins/
        File skinsDir = new File(plugin.getDataFolder(), "skins");
        if (skinsDir.exists() && skinsDir.isDirectory()) {
            File[] files = skinsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".txt")) {
                        String name = f.getName().substring(0, f.getName().lastIndexOf('.'));
                        try {
                            String value = java.nio.file.Files.readString(f.toPath()).trim();
                            if (!value.isEmpty()) {
                                boolean exists = false;
                                for (CustomHead ch : customHeads) {
                                    if (ch.getName().equalsIgnoreCase(name)) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    customHeads.add(new CustomHead(name, value));
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    public void saveCustomHeads() {
        File file = new File(plugin.getDataFolder(), "custom-heads.yml");
        FileConfiguration cfg = new YamlConfiguration();
        for (CustomHead ch : customHeads) {
            cfg.set("heads." + ch.getName(), ch.getValue());
        }
        try {
            cfg.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Impossible de sauvegarder custom-heads.yml: " + e.getMessage());
        }
    }

    public LivingEntity spawnVariantMob(MobVariant var, Location loc) {
        if (var == null || loc == null || loc.getWorld() == null) return null;
        Class<? extends Entity> eClass = var.getType().getEntityClass();
        if (eClass == null || !LivingEntity.class.isAssignableFrom(eClass)) return null;

        Location safeLoc = MobSpawnListener.ensureSafeLocation(loc);
        if (safeLoc == null) safeLoc = loc;

        try {
            Entity spawned = loc.getWorld().spawn(safeLoc, eClass, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                entity.getPersistentDataContainer().set(MobSpawnListener.KEY_SQUAD_SPAWNED, PersistentDataType.BYTE, (byte) 1);
                entity.getPersistentDataContainer().set(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING, var.getId());
            });
            if (spawned instanceof LivingEntity le) {
                return le;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[VariantManager] Erreur lors du spawn de " + var.getId() + " : " + t.getMessage());
        }
        return null;
    }

    public List<LivingEntity> spawnSquad(String squadId, Location loc) {
        List<LivingEntity> spawnedList = new ArrayList<>();
        MobSquad squad = getSquad(squadId);
        if (squad == null || loc == null || loc.getWorld() == null || squad.getMembers() == null) return spawnedList;

        for (Map.Entry<String, MobSquad.SquadMemberRange> entry : squad.getMembers().entrySet()) {
            String varId = entry.getKey();
            MobSquad.SquadMemberRange range = entry.getValue();
            int count = range.getMin();
            if (range.getMax() > range.getMin()) {
                count += random.nextInt(range.getMax() - range.getMin() + 1);
            }
            MobVariant var = getVariant(varId);
            if (var != null) {
                for (int i = 0; i < count; i++) {
                    Location spawnLoc = loc.clone().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
                    LivingEntity mob = spawnVariantMob(var, spawnLoc);
                    if (mob != null) {
                        mob.getPersistentDataContainer().set(MobSpawnListener.KEY_SQUAD_ID, PersistentDataType.STRING, squadId);
                        spawnedList.add(mob);
                    }
                }
            }
        }
        return spawnedList;
    }
}
