package fr.wilddifficulty.zone;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.BiomeConfigManager;
import fr.wilddifficulty.config.StatModifiers;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gestionnaire des zones de difficulté personnalisées.
 *
 * Responsabilités :
 * 1. Chargement / sauvegarde des zones depuis zones.yml
 * 2. Cache par chunk : chunkKey → liste de zones couvrant ce chunk
 *    → évite les O(n) scans à chaque événement de spawn
 * 3. Résolution de la zone prioritaire pour une position donnée
 *
 * Le cache est invalidé lors d'un reload ou d'une modification de zone.
 */
public class ZoneManager {

    private final WildDifficultyPlugin plugin;

    // Stockage principal : id → zone
    private final Map<String, DifficultyZone> zones = new LinkedHashMap<>();

    // ── Cache par chunk ───────────────────────────────────────
    // Clé : "worldName:chunkX:chunkZ"  → zones couvrant ce chunk (triées par priorité desc)
    // PERFORMANCE : évite O(n zones) à chaque spawn, réduit à O(k zones dans le chunk)
    private final Map<String, List<DifficultyZone>> chunkCache = new HashMap<>();

    // Zones en cours de création (par UUID de joueur admin)
    private final Map<UUID, DifficultyZone> pendingZones = new HashMap<>();

    public ZoneManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Chargement / Sauvegarde ──────────────────────────────

    /**
     * Charge les zones depuis zones.yml et reconstruit le cache chunk.
     */
    public void load() {
        zones.clear();
        chunkCache.clear();

        File file = new File(plugin.getDataFolder(), "zones.yml");
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection zonesSection = cfg.getConfigurationSection("zones");
        if (zonesSection == null) return;

        for (String id : zonesSection.getKeys(false)) {
            ConfigurationSection zs = zonesSection.getConfigurationSection(id);
            if (zs == null) continue;

            try {
                DifficultyZone zone = deserializeZone(id, zs);
                zones.put(id, zone);
                indexZoneInCache(zone);
            } catch (Exception e) {
                plugin.getLogger().warning("[Zones] Erreur lors du chargement de la zone '"
                        + id + "' : " + e.getMessage());
            }
        }

        plugin.getLogger().info("[Zones] " + zones.size() + " zone(s) chargée(s) depuis zones.yml.");
    }

    /**
     * Recharge les zones (appelé par /wdreload ou /mobzone reload).
     */
    public void reload() {
        save();
        load();
    }

    /**
     * Sauvegarde toutes les zones dans zones.yml.
     */
    public void save() {
        File file = new File(plugin.getDataFolder(), "zones.yml");
        FileConfiguration cfg = new YamlConfiguration();

        // En-tête commentaire
        cfg.options().setHeader(List.of(
                "============================================================",
                " WildDifficulty — zones.yml",
                " Zones de difficulté personnalisées — géré automatiquement",
                "============================================================"
        ));

        ConfigurationSection zonesSection = cfg.createSection("zones");

        for (DifficultyZone zone : zones.values()) {
            ConfigurationSection zs = zonesSection.createSection(zone.getId());
            serializeZone(zone, zs);
        }

        try {
            cfg.save(file);
            rebuildCache();
        } catch (IOException e) {
            plugin.getLogger().severe("[Zones] Impossible de sauvegarder zones.yml : " + e.getMessage());
        }
    }

    // ── Gestion des zones ────────────────────────────────────

    /**
     * Ajoute ou remplace une zone et met à jour le cache.
     */
    public void addZone(DifficultyZone zone) {
        zones.put(zone.getId(), zone);
        // Invalide les entrées du cache concernées par cette zone
        rebuildCache();
        save();
    }

    /**
     * Supprime une zone et met à jour le cache.
     */
    public boolean removeZone(String id) {
        if (zones.remove(id) != null) {
            rebuildCache();
            save();
            return true;
        }
        return false;
    }

    /**
     * Retourne une zone par son id (ou null).
     */
    public DifficultyZone getZone(String id) {
        return zones.get(id);
    }

    /**
     * Retourne toutes les zones triées par priorité décroissante.
     */
    public List<DifficultyZone> getAllZones() {
        List<DifficultyZone> list = new ArrayList<>(zones.values());
        list.sort(Comparator.comparingInt(DifficultyZone::getPriority).reversed());
        return list;
    }

    /**
     * Retourne la zone active à priorité la plus élevée pour une position donnée,
     * ou null si aucune zone ne couvre cette position.
     *
     * PERFORMANCE : utilise le cache chunk pour ne chercher que dans les zones pertinentes.
     *
     * @param worldName nom du monde
     * @param x         coordonnée X
     * @param y         coordonnée Y
     * @param z         coordonnée Z
     */
    public DifficultyZone getZoneAt(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return getZoneAt(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    public DifficultyZone getZoneAt(String worldName, double x, double y, double z) {
        int chunkX = (int) Math.floor(x / 16);
        int chunkZ = (int) Math.floor(z / 16);
        String cacheKey = worldName + ":" + chunkX + ":" + chunkZ;

        List<DifficultyZone> candidates = chunkCache.get(cacheKey);
        if (candidates == null || candidates.isEmpty()) return null;

        // Les candidates sont déjà triées par priorité décroissante
        for (DifficultyZone zone : candidates) {
            if (zone.contains(worldName, x, y, z)) return zone;
        }
        return null;
    }

    /**
     * Trouve la zone la plus proche avec scaling extérieur actif dans ce monde.
     */
    public DifficultyZone getExternalScalingZone(String worldName, double x, double z) {
        DifficultyZone closest = null;
        double minDistance = Double.MAX_VALUE;
        for (DifficultyZone zone : zones.values()) {
            if (zone.getWorld().equals(worldName) && zone.hasExtScaling()) {
                double dx = x - zone.getCenterX();
                double dz = z - zone.getCenterZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = zone;
                }
            }
        }
        return closest;
    }

    // ── Zones en cours de construction ───────────────────────

    public void setPendingZone(UUID playerId, DifficultyZone zone) {
        pendingZones.put(playerId, zone);
    }

    public DifficultyZone getPendingZone(UUID playerId) {
        return pendingZones.get(playerId);
    }

    public void clearPendingZone(UUID playerId) {
        pendingZones.remove(playerId);
    }

    // ── Cache par chunk ───────────────────────────────────────

    /**
     * Reconstruit intégralement le cache chunk.
     * Appelé après tout changement de zones.
     */
    private void rebuildCache() {
        chunkCache.clear();
        for (DifficultyZone zone : zones.values()) {
            indexZoneInCache(zone);
        }
    }

    /**
     * Indexe une zone dans le cache chunk pour tous les chunks qu'elle couvre.
     */
    private void indexZoneInCache(DifficultyZone zone) {
        List<long[]> chunks = zone.getCoveredChunkKeys();
        for (long[] chunkCoords : chunks) {
            String key = zone.getWorld() + ":" + chunkCoords[0] + ":" + chunkCoords[1];
            chunkCache.computeIfAbsent(key, k -> new ArrayList<>()).add(zone);
        }
        // Re-trier chaque liste par priorité décroissante
        for (List<DifficultyZone> list : chunkCache.values()) {
            list.sort(Comparator.comparingInt(DifficultyZone::getPriority).reversed());
        }
    }

    // ── Sérialisation ────────────────────────────────────────

    private void serializeZone(DifficultyZone zone, ConfigurationSection section) {
        section.set("type", zone.getType().name());
        section.set("monde", zone.getWorld());
        section.set("priorite", zone.getPriority());
        section.set("zone-sure", zone.isSafeZone());
        section.set("ignorer-regles-biome", zone.isOverrideBiomeRules());

        // Modificateurs
        ConfigurationSection mod = section.createSection("modificateurs");
        StatModifiers m = zone.getModifiers();
        if (m.getHealthValue() != -1.0) mod.set("valeur-pv", m.getHealthValue());
        if (m.getDamageValue() != -1.0) mod.set("valeur-degats", m.getDamageValue());
        if (m.getSpeedValue() != -1.0) mod.set("valeur-vitesse", m.getSpeedValue());
        if (m.getFollowRangeValue() != -1.0) mod.set("valeur-portee-detection", m.getFollowRangeValue());
        if (m.getKnockbackValue() != -1.0) mod.set("valeur-resistance-knockback", m.getKnockbackValue());
        if (m.getRegenerationValue() != -1.0) mod.set("valeur-regen", m.getRegenerationValue());
        if (m.getResistanceFeu() != 1.0) mod.set("resistance-feu", m.getResistanceFeu());
        mod.set("tier-equipement", m.getEquipmentTier());
        mod.set("chance-equipement", m.getEquipmentChance());

        if (!m.getHelmetItem().equals("none")) mod.set("casque-item", m.getHelmetItem());
        if (m.getHelmetChance() > 0) mod.set("casque-chance", m.getHelmetChance());
        if (!m.getHelmetColor().equals("none")) mod.set("casque-couleur", m.getHelmetColor());
        if (!m.getChestplateItem().equals("none")) mod.set("plastron-item", m.getChestplateItem());
        if (m.getChestplateChance() > 0) mod.set("plastron-chance", m.getChestplateChance());
        if (!m.getChestplateColor().equals("none")) mod.set("plastron-couleur", m.getChestplateColor());
        if (!m.getLeggingsItem().equals("none")) mod.set("jambieres-item", m.getLeggingsItem());
        if (m.getLeggingsChance() > 0) mod.set("jambieres-chance", m.getLeggingsChance());
        if (!m.getLeggingsColor().equals("none")) mod.set("jambieres-couleur", m.getLeggingsColor());
        if (!m.getBootsItem().equals("none")) mod.set("bottes-item", m.getBootsItem());
        if (m.getBootsChance() > 0) mod.set("bottes-chance", m.getBootsChance());
        if (!m.getBootsColor().equals("none")) mod.set("bottes-couleur", m.getBootsColor());
        if (!m.getMainHandItem().equals("none")) mod.set("main-principale-item", m.getMainHandItem());
        if (m.getMainHandChance() > 0) mod.set("main-principale-chance", m.getMainHandChance());
        if (!m.getOffHandItem().equals("none")) mod.set("main-secondaire-item", m.getOffHandItem());
        if (m.getOffHandChance() > 0) mod.set("main-secondaire-chance", m.getOffHandChance());

        // Filtres mobs
        section.set("mobs-autorises", new ArrayList<>(zone.getAllowedMobs()));
        section.set("mobs-interdits", new ArrayList<>(zone.getDeniedMobs()));
        section.set("variantes-autorisees", zone.getAllowedVariants());
        section.set("variantes-interdites", zone.getDeniedVariants());
        section.set("escouades-autorisees", zone.getAllowedSquads());
        section.set("escouades-interdites", zone.getDeniedSquads());

        section.set("franchissable", zone.isMobsCanCross());
        section.set("particle-hauteur-offset", zone.getParticleHeightOffset());
        section.set("particles-active", zone.isParticlesEnabled());
        section.set("particle-dedans", zone.getParticleInside());
        section.set("particle-dehors", zone.getParticleOutside());
        section.set("a-tp-perso", zone.hasCustomTeleport());
        if (zone.hasCustomTeleport()) {
            section.set("tp-x", zone.getTeleportX());
            section.set("tp-y", zone.getTeleportY());
            section.set("tp-z", zone.getTeleportZ());
        }
        if (zone.getExtStep() > 0 || zone.getExtStepHp() > 0 || zone.getExtStepDmg() > 0 || zone.getExtStepSpd() > 0) {
            if (zone.getExtStep() > 0) {
                section.set("ext-pas", zone.getExtStep());
                section.set("ext-multiplicateur-par-pas", zone.getExtMultPerStep());
            }
            section.set("ext-max-multiplicateur", zone.getExtMaxMult());
            if (zone.getExtStepHp() > 0) {
                section.set("ext-pas-pv", zone.getExtStepHp());
                section.set("ext-mult-pv", zone.getExtMultHp());
            }
            if (zone.getExtStepDmg() > 0) {
                section.set("ext-pas-degats", zone.getExtStepDmg());
                section.set("ext-mult-degats", zone.getExtMultDmg());
            }
            if (zone.getExtStepSpd() > 0) {
                section.set("ext-pas-vitesse", zone.getExtStepSpd());
                section.set("ext-mult-vitesse", zone.getExtMultSpd());
            }
        }

        // Membres de zone
        if (!zone.getMembers().isEmpty()) {
            ConfigurationSection memSec = section.createSection("membres");
            for (ZoneMember mem : zone.getMembers().values()) {
                ConfigurationSection memConf = memSec.createSection(mem.getPlayerUuid().toString());
                memConf.set("nom", mem.getLastKnownName());
                memConf.set("niveau", mem.getPermissionLevel());
            }
        }

        // Sous-sections (multi-sections)
        if (!zone.getSubSections().isEmpty()) {
            ConfigurationSection subSecs = section.createSection("sub-sections");
            for (ZoneSection sub : zone.getSubSections()) {
                ConfigurationSection s = subSecs.createSection(sub.getId());
                s.set("type", sub.getType().name());
                if (sub.getType() == DifficultyZone.ZoneType.CUBOID) {
                    s.set("min-x", sub.getMinX()); s.set("min-y", sub.getMinY()); s.set("min-z", sub.getMinZ());
                    s.set("max-x", sub.getMaxX()); s.set("max-y", sub.getMaxY()); s.set("max-z", sub.getMaxZ());
                } else if (sub.getType() == DifficultyZone.ZoneType.RADIUS) {
                    s.set("center-x", sub.getCenterX()); s.set("center-y", sub.getCenterY()); s.set("center-z", sub.getCenterZ());
                    s.set("radius", sub.getRadius());
                } else if (sub.getType() == DifficultyZone.ZoneType.POLYGON) {
                    List<List<Double>> ptsList = new ArrayList<>();
                    for (double[] pt : sub.getPoints()) ptsList.add(List.of(pt[0], pt[1]));
                    s.set("points", ptsList);
                }
            }
        }

        // Effets de Beacon
        if (!zone.getBeaconEffects().isEmpty()) {
            ConfigurationSection beacSec = section.createSection("beacon-effects");
            for (Map.Entry<String, Integer> entry : zone.getBeaconEffects().entrySet()) {
                beacSec.set(entry.getKey(), entry.getValue());
            }
        }

        // Nid d'ennemis
        section.set("danger-nest", zone.isDangerNest());
        section.set("nest-spawn-boost", zone.getNestSpawnBoost());
        if (zone.isDangerNest() && zone.getNestModifiers() != null) {
            ConfigurationSection nestMod = section.createSection("nest-modificateurs");
            StatModifiers nm = zone.getNestModifiers();
            if (nm.getHealthValue() > 0) nestMod.set("pv", nm.getHealthValue());
            if (nm.getDamageValue() > 0) nestMod.set("degats", nm.getDamageValue());
            if (nm.getSpeedValue() > 0) nestMod.set("vitesse", nm.getSpeedValue());
        }

        // WorldGuard region link & Encounters v1.1
        if (zone.getWorldGuardRegion() != null && !zone.getWorldGuardRegion().isEmpty()) {
            section.set("worldguard-region", zone.getWorldGuardRegion());
        }

        if (zone.getEncounterConfig() != null && zone.getEncounterConfig().getType() != fr.wilddifficulty.encounter.EncounterType.NONE) {
            ConfigurationSection encSec = section.createSection("encounter");
            fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();
            encSec.set("type", enc.getType().name());
            encSec.set("enabled", enc.isEnabled());
            encSec.set("cooldown", enc.getCooldownSeconds());
            encSec.set("min-players", enc.getMinPlayers());
            encSec.set("max-players", enc.getMaxPlayers());
            encSec.set("raid-mode", enc.getRaidMode());
            encSec.set("raid-waves-count", enc.getRaidWaveCount());
            encSec.set("protect-npc", enc.isProtectNpc());
            encSec.set("protect-npc-name", enc.getProtectNpcName());
            encSec.set("trial-mode", enc.getTrialMode());
            encSec.set("trial-activation-radius", enc.getTrialActivationRadius());
            encSec.set("trial-mob-cap", enc.getTrialMobCap());
            encSec.set("trial-total-mobs", enc.getTrialTotalMobs());
            encSec.set("trial-scaling-player", enc.getTrialScalingPerPlayer());
            encSec.set("outpost-patrol-interval", enc.getOutpostPatrolIntervalSeconds());
            encSec.set("outpost-lingering-time", enc.getOutpostLingeringInvasionTimeSeconds());
            encSec.set("outpost-captain-chance", enc.getOutpostCaptainChance());
            encSec.set("ruins-trigger-radius", enc.getRuinsTriggerRadius());
            encSec.set("ruins-trigger-chest", enc.isRuinsTriggerOnChestOpen());
            encSec.set("bossbar-enabled", enc.isBossBarEnabled());
            encSec.set("bossbar-color", enc.getBossBarColor());
            encSec.set("bossbar-style", enc.getBossBarStyle());
            encSec.set("defeat-condition", enc.getDefeatCondition());
            encSec.set("grace-period", enc.getPlayerLeaveGracePeriodSeconds());
            encSec.set("mob-objective", enc.getMobObjective());

            if (!enc.getWaves().isEmpty()) {
                ConfigurationSection wavesSec = encSec.createSection("waves");
                for (int i = 0; i < enc.getWaves().size(); i++) {
                    fr.wilddifficulty.encounter.EncounterWave w = enc.getWaves().get(i);
                    ConfigurationSection wSec = wavesSec.createSection("wave_" + (i + 1));
                    wSec.set("delay", w.getDelaySeconds());
                    wSec.set("sound", w.getSoundEffect());
                    wSec.set("particle", w.getParticleEffect());
                    wSec.set("spawn-distrib", w.getSpawnDistribution());
                    if (!w.getVariantSpawns().isEmpty()) {
                        wSec.set("variants", w.getVariantSpawns());
                    }
                    if (!w.getSquadSpawns().isEmpty()) {
                        wSec.set("squads", w.getSquadSpawns());
                    }
                }
            }

            if (enc.getRewards() != null) {
                ConfigurationSection rewSec = encSec.createSection("rewards");
                rewSec.set("xp", enc.getRewards().getXpAmount());
                rewSec.set("money", enc.getRewards().getMoneyAmount());
                if (!enc.getRewards().getConsoleCommands().isEmpty()) {
                    rewSec.set("commands", enc.getRewards().getConsoleCommands());
                }
                if (!enc.getRewards().getItems().isEmpty()) {
                    List<Map<String, Object>> itemsList = new ArrayList<>();
                    for (fr.wilddifficulty.encounter.EncounterReward.RewardItem ri : enc.getRewards().getItems()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("material", ri.getMaterialName());
                        map.put("amount", ri.getAmount());
                        map.put("chance", ri.getChance());
                        if (ri.getCustomName() != null) map.put("name", ri.getCustomName());
                        itemsList.add(map);
                    }
                    rewSec.set("items", itemsList);
                }
            }

            if (!enc.getSpawnMarkers().isEmpty()) {
                List<String> markersList = new ArrayList<>();
                for (double[] sm : enc.getSpawnMarkers()) {
                    markersList.add(sm[0] + "," + sm[1] + "," + sm[2]);
                }
                encSec.set("spawn-markers", markersList);
            }
        }

        // Géométrie
        if (zone.getType() == DifficultyZone.ZoneType.CUBOID) {
            ConfigurationSection pos1 = section.createSection("pos1");
            pos1.set("x", zone.getMinX()); pos1.set("y", zone.getMinY()); pos1.set("z", zone.getMinZ());
            ConfigurationSection pos2 = section.createSection("pos2");
            pos2.set("x", zone.getMaxX()); pos2.set("y", zone.getMaxY()); pos2.set("z", zone.getMaxZ());
        } else if (zone.getType() == DifficultyZone.ZoneType.RADIUS) {
            ConfigurationSection centre = section.createSection("centre");
            centre.set("x", zone.getCenterX()); centre.set("y", zone.getCenterY()); centre.set("z", zone.getCenterZ());
            section.set("rayon", zone.getRadius());
        } else {
            // POLYGON
            List<List<Double>> ptsList = new ArrayList<>();
            for (double[] pt : zone.getPoints()) {
                ptsList.add(List.of(pt[0], pt[1]));
            }
            section.set("points", ptsList);
            section.set("min-y", zone.getMinY());
            section.set("max-y", zone.getMaxY());
        }
    }

    private DifficultyZone deserializeZone(String id, ConfigurationSection section) {
        String typeStr = section.getString("type", "CUBOID");
        DifficultyZone.ZoneType type = DifficultyZone.ZoneType.valueOf(typeStr.toUpperCase());
        String world = section.getString("monde", "world");

        DifficultyZone zone = new DifficultyZone(id, type, world);
        zone.setPriority(section.getInt("priorite", 0));
        zone.setSafeZone(section.getBoolean("zone-sure", false));
        zone.setOverrideBiomeRules(section.getBoolean("ignorer-regles-biome", false));
        zone.setMobsCanCross(section.getBoolean("franchissable", true));
        zone.setExtStep(section.getDouble("ext-pas", 0.0));
        zone.setExtMultPerStep(section.getDouble("ext-multiplicateur-par-pas", 0.0));
        zone.setExtMaxMult(section.getDouble("ext-max-multiplicateur", 5.0));
        zone.setExtStepHp(section.getDouble("ext-pas-pv", 0.0));
        zone.setExtMultHp(section.getDouble("ext-mult-pv", 0.0));
        zone.setExtStepDmg(section.getDouble("ext-pas-degats", 0.0));
        zone.setExtMultDmg(section.getDouble("ext-mult-degats", 0.0));
        zone.setExtStepSpd(section.getDouble("ext-pas-vitesse", 0.0));
        zone.setExtMultSpd(section.getDouble("ext-mult-vitesse", 0.0));
        zone.setParticleHeightOffset(section.getDouble("particle-hauteur-offset", 0.1));
        zone.setParticlesEnabled(section.getBoolean("particles-active", true));
        zone.setParticleInside(section.getString("particle-dedans", "HAPPY_VILLAGER"));
        zone.setParticleOutside(section.getString("particle-dehors", "FLAME"));
        zone.setHasCustomTeleport(section.getBoolean("a-tp-perso", false));
        if (zone.hasCustomTeleport()) {
            zone.setTeleportX(section.getDouble("tp-x"));
            zone.setTeleportY(section.getDouble("tp-y"));
            zone.setTeleportZ(section.getDouble("tp-z"));
        }

        // Modificateurs
        ConfigurationSection modSection = section.getConfigurationSection("modificateurs");
        if (modSection != null) {
            zone.setModifiers(BiomeConfigManager.readModifiers(modSection));
        }

        // Membres
        ConfigurationSection memSec = section.getConfigurationSection("membres");
        if (memSec != null) {
            for (String uuidStr : memSec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = memSec.getString(uuidStr + ".nom", "Unknown");
                    int lvl = memSec.getInt(uuidStr + ".niveau", 1);
                    zone.addMember(new ZoneMember(uuid, name, lvl));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Sous-sections
        ConfigurationSection subSecs = section.getConfigurationSection("sub-sections");
        if (subSecs != null) {
            for (String secId : subSecs.getKeys(false)) {
                ConfigurationSection s = subSecs.getConfigurationSection(secId);
                if (s == null) continue;
                String subTypeStr = s.getString("type", "CUBOID");
                DifficultyZone.ZoneType subType = DifficultyZone.ZoneType.valueOf(subTypeStr.toUpperCase());
                ZoneSection sub = new ZoneSection(secId, subType);
                if (subType == DifficultyZone.ZoneType.CUBOID) {
                    sub.setMinX(s.getDouble("min-x")); sub.setMinY(s.getDouble("min-y")); sub.setMinZ(s.getDouble("min-z"));
                    sub.setMaxX(s.getDouble("max-x")); sub.setMaxY(s.getDouble("max-y")); sub.setMaxZ(s.getDouble("max-z"));
                } else if (subType == DifficultyZone.ZoneType.RADIUS) {
                    sub.setCenterX(s.getDouble("center-x")); sub.setCenterY(s.getDouble("center-y")); sub.setCenterZ(s.getDouble("center-z"));
                    sub.setRadius(s.getDouble("radius"));
                } else if (subType == DifficultyZone.ZoneType.POLYGON) {
                    List<double[]> points = new ArrayList<>();
                    if (s.isList("points")) {
                        for (Object item : s.getList("points")) {
                            if (item instanceof List<?> list && list.size() >= 2) {
                                double px = ((Number) list.get(0)).doubleValue();
                                double pz = ((Number) list.get(1)).doubleValue();
                                points.add(new double[]{px, pz});
                            }
                        }
                    }
                    sub.setPoints(points);
                }
                zone.addSubSection(sub);
            }
        }

        // Effets de Beacon
        ConfigurationSection beacSec = section.getConfigurationSection("beacon-effects");
        if (beacSec != null) {
            for (String effectKey : beacSec.getKeys(false)) {
                zone.setBeaconEffect(effectKey.toUpperCase(), beacSec.getInt(effectKey, 0));
            }
        }

        // Nid d'ennemis
        zone.setDangerNest(section.getBoolean("danger-nest", false));
        zone.setNestSpawnBoost(section.getDouble("nest-spawn-boost", 1.0));
        ConfigurationSection nestModSection = section.getConfigurationSection("nest-modificateurs");
        if (nestModSection != null) {
            zone.setNestModifiers(BiomeConfigManager.readModifiers(nestModSection));
        }

        // Filtres mobs
        zone.setAllowedMobs(new HashSet<>(section.getStringList("mobs-autorises").stream().map(String::toUpperCase).toList()));
        zone.setDeniedMobs(new HashSet<>(section.getStringList("mobs-interdits").stream().map(String::toUpperCase).toList()));
        zone.setAllowedVariants(section.getStringList("variantes-autorisees"));
        zone.setDeniedVariants(section.getStringList("variantes-interdites"));
        zone.setAllowedSquads(section.getStringList("escouades-autorisees"));
        zone.setDeniedSquads(section.getStringList("escouades-interdites"));

        // Géométrie
        if (type == DifficultyZone.ZoneType.CUBOID) {
            ConfigurationSection pos1 = section.getConfigurationSection("pos1");
            ConfigurationSection pos2 = section.getConfigurationSection("pos2");
            if (pos1 != null && pos2 != null) {
                zone.setMinMax(
                        pos1.getDouble("x"), pos1.getDouble("y"), pos1.getDouble("z"),
                        pos2.getDouble("x"), pos2.getDouble("y"), pos2.getDouble("z")
                );
                zone.finalizeCuboid();
            }
        } else if (type == DifficultyZone.ZoneType.RADIUS) {
            ConfigurationSection centre = section.getConfigurationSection("centre");
            if (centre != null) {
                zone.setCenter(centre.getDouble("x"), centre.getDouble("y"), centre.getDouble("z"));
            }
            zone.setRadius(section.getDouble("rayon", 0.0));
        } else {
            // POLYGON
            List<double[]> points = new ArrayList<>();
            if (section.isList("points")) {
                for (Object item : section.getList("points")) {
                    if (item instanceof List<?> list && list.size() >= 2) {
                        double x = ((Number) list.get(0)).doubleValue();
                        double z = ((Number) list.get(1)).doubleValue();
                        points.add(new double[]{x, z});
                    }
                }
            }
            zone.setPoints(points);
            zone.setMinY(section.getDouble("min-y", -64.0));
            zone.setMaxY(section.getDouble("max-y", 320.0));
            zone.finalizePolygon();
        }

        // WorldGuard region link & Encounters v1.1
        zone.setWorldGuardRegion(section.getString("worldguard-region", null));

        ConfigurationSection encSec = section.getConfigurationSection("encounter");
        if (encSec != null) {
            String encTypeStr = encSec.getString("type", "NONE");
            fr.wilddifficulty.encounter.EncounterType encType = fr.wilddifficulty.encounter.EncounterType.fromString(encTypeStr);
            fr.wilddifficulty.encounter.EncounterConfig enc = new fr.wilddifficulty.encounter.EncounterConfig(encType);
            enc.setEnabled(encSec.getBoolean("enabled", true));
            enc.setCooldownSeconds(encSec.getInt("cooldown", 1800));
            enc.setMinPlayers(encSec.getInt("min-players", 1));
            enc.setMaxPlayers(encSec.getInt("max-players", 10));
            enc.setRaidMode(encSec.getString("raid-mode", "CUSTOM_RAID_MODE"));
            enc.setRaidWaveCount(encSec.getInt("raid-waves-count", 5));
            enc.setProtectNpc(encSec.getBoolean("protect-npc", false));
            enc.setProtectNpcName(encSec.getString("protect-npc-name", "Village Elder"));
            enc.setTrialMode(encSec.getString("trial-mode", "INTERNAL_SIMULATION"));
            enc.setTrialActivationRadius(encSec.getDouble("trial-activation-radius", 14.0));
            enc.setTrialMobCap(encSec.getInt("trial-mob-cap", 8));
            enc.setTrialTotalMobs(encSec.getInt("trial-total-mobs", 20));
            enc.setTrialScalingPerPlayer(encSec.getDouble("trial-scaling-player", 0.5));
            enc.setOutpostPatrolIntervalSeconds(encSec.getDouble("outpost-patrol-interval", 60.0));
            enc.setOutpostLingeringInvasionTimeSeconds(encSec.getDouble("outpost-lingering-time", 120.0));
            enc.setOutpostCaptainChance(encSec.getDouble("outpost-captain-chance", 0.50));
            enc.setRuinsTriggerRadius(encSec.getDouble("ruins-trigger-radius", 12.0));
            enc.setRuinsTriggerOnChestOpen(encSec.getBoolean("ruins-trigger-chest", true));
            enc.setBossBarEnabled(encSec.getBoolean("bossbar-enabled", true));
            enc.setBossBarColor(encSec.getString("bossbar-color", "RED"));
            enc.setBossBarStyle(encSec.getString("bossbar-style", "NOTCHED_10"));
            enc.setDefeatCondition(encSec.getString("defeat-condition", "TIMEOUT_OUTSIDE_ZONE"));
            enc.setPlayerLeaveGracePeriodSeconds(encSec.getInt("grace-period", 30));
            enc.setMobObjective(encSec.getString("mob-objective", "TARGET_PLAYERS"));

            ConfigurationSection wavesSec = encSec.getConfigurationSection("waves");
            if (wavesSec != null) {
                List<fr.wilddifficulty.encounter.EncounterWave> waves = new ArrayList<>();
                for (String wKey : wavesSec.getKeys(false)) {
                    ConfigurationSection wSec = wavesSec.getConfigurationSection(wKey);
                    if (wSec == null) continue;
                    fr.wilddifficulty.encounter.EncounterWave wave = new fr.wilddifficulty.encounter.EncounterWave();
                    wave.setDelaySeconds(wSec.getInt("delay", 5));
                    wave.setSoundEffect(wSec.getString("sound", "entity.wither.spawn"));
                    wave.setParticleEffect(wSec.getString("particle", "FLAME"));
                    wave.setSpawnDistribution(wSec.getString("spawn-distrib", "AROUND_CENTER"));

                    ConfigurationSection varSec = wSec.getConfigurationSection("variants");
                    if (varSec != null) {
                        for (String vId : varSec.getKeys(false)) {
                            wave.addVariant(vId, varSec.getInt(vId, 1));
                        }
                    }

                    ConfigurationSection sqSec = wSec.getConfigurationSection("squads");
                    if (sqSec != null) {
                        for (String sqId : sqSec.getKeys(false)) {
                            wave.addSquad(sqId, sqSec.getInt(sqId, 1));
                        }
                    }
                    waves.add(wave);
                }
                enc.setWaves(waves);
            }

            ConfigurationSection rewSec = encSec.getConfigurationSection("rewards");
            if (rewSec != null) {
                fr.wilddifficulty.encounter.EncounterReward rew = new fr.wilddifficulty.encounter.EncounterReward();
                rew.setXpAmount(rewSec.getInt("xp", 50));
                rew.setMoneyAmount(rewSec.getDouble("money", 0.0));
                rew.setConsoleCommands(rewSec.getStringList("commands"));

                if (rewSec.isList("items")) {
                    for (Map<?, ?> itemMap : rewSec.getMapList("items")) {
                        String mat = (String) itemMap.get("material");
                        int amt = itemMap.containsKey("amount") ? ((Number) itemMap.get("amount")).intValue() : 1;
                        double chance = itemMap.containsKey("chance") ? ((Number) itemMap.get("chance")).doubleValue() : 1.0;
                        String customName = (String) itemMap.get("name");
                        rew.addItem(mat, amt, chance, customName);
                    }
                }
                enc.setRewards(rew);
            }

            List<String> markersList = encSec.getStringList("spawn-markers");
            if (markersList != null) {
                for (String s : markersList) {
                    String[] p = s.split(",");
                    if (p.length >= 3) {
                        try {
                            enc.addSpawnMarker(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]));
                        } catch (Exception ignored) {}
                    }
                }
            }

            zone.setEncounterConfig(enc);
        }

        return zone;
    }
}
