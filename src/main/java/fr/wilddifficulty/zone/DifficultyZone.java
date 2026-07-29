package fr.wilddifficulty.zone;

import fr.wilddifficulty.config.StatModifiers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DifficultyZone {

    public enum ZoneType { CUBOID, RADIUS, POLYGON }

    private final String id;
    private ZoneType type;
    private String world;
    private int priority;

    private boolean safeZone;
    private boolean overrideBiomeRules;
    private boolean mobsCanCross = true; // Mobs ne peuvent pas franchir la zone si faux
    
    // Scaling extérieur par rayon — par statistique
    private double extStep = 0.0;         // Pas global (rétrocompatibilité)
    private double extMultPerStep = 0.0;  // Bonus global (rétrocompatibilité)
    private double extMaxMult = 5.0;      // Cap global

    // Per-stat overrides (0 = use global)
    private double extStepHp = 0.0;
    private double extMultHp = 0.0;
    private double extStepDmg = 0.0;
    private double extMultDmg = 0.0;
    private double extStepSpd = 0.0;
    private double extMultSpd = 0.0;

    // Config de hauteur de particules et TP
    private double particleHeightOffset = 0.1;
    private boolean particlesEnabled = true;
    private String particleInside = "HAPPY_VILLAGER";
    private String particleOutside = "FLAME";
    private double teleportX;
    private double teleportY;
    private double teleportZ;
    private boolean hasCustomTeleport = false;
    
    private StatModifiers modifiers;

    // Nouveaux champs pour protection, membres, multi-sections, beacon et nid d'ennemis
    private final java.util.Map<java.util.UUID, ZoneMember> members = new java.util.HashMap<>();
    private final List<ZoneSection> subSections = new ArrayList<>();
    private final java.util.Map<String, Integer> beaconEffects = new java.util.HashMap<>();
    private boolean dangerNest = false;
    private StatModifiers nestModifiers = new StatModifiers();
    private double nestSpawnBoost = 1.0;

    private Set<String> allowedMobs = new HashSet<>(), deniedMobs = new HashSet<>();
    private List<String> allowedVariants = new ArrayList<>(), deniedVariants = new ArrayList<>();
    private List<String> allowedSquads = new ArrayList<>(), deniedSquads = new ArrayList<>();

    // CUBOID
    private double minX, minY = -64, minZ;
    private double maxX, maxY = 320, maxZ;

    // RADIUS
    private double centerX, centerY, centerZ;
    private double radius;

    // POLYGON
    private List<double[]> points = new ArrayList<>(); // Couple [x, z]

    // Construction temp
    private double pos1X, pos1Y, pos1Z;
    private double pos2X, pos2Y, pos2Z;
    private boolean pos1Set = false;
    private boolean pos2Set = false;

    public DifficultyZone(String id, ZoneType type, String world) {
        this.id        = id;
        this.type      = type;
        this.world     = world;
        this.priority  = 0;
        this.modifiers = new StatModifiers();
    }

    public void finalizeCuboid() {
        minX = Math.min(pos1X, pos2X);
        minY = Math.min(pos1Y, pos2Y);
        minZ = Math.min(pos1Z, pos2Z);
        maxX = Math.max(pos1X, pos2X);
        maxY = Math.max(pos1Y, pos2Y);
        maxZ = Math.max(pos1Z, pos2Z);
        centerX = (minX + maxX) / 2.0;
        centerY = (minY + maxY) / 2.0;
        centerZ = (minZ + maxZ) / 2.0;
        double dx = maxX - minX;
        double dz = maxZ - minZ;
        radius = Math.sqrt(dx * dx + dz * dz) / 2.0;
    }

    public void finalizePolygon() {
        if (points.isEmpty()) return;
        double pMinX = Double.MAX_VALUE; double pMaxX = -Double.MAX_VALUE;
        double pMinZ = Double.MAX_VALUE; double pMaxZ = -Double.MAX_VALUE;
        for (double[] pt : points) {
            if (pt[0] < pMinX) pMinX = pt[0];
            if (pt[0] > pMaxX) pMaxX = pt[0];
            if (pt[1] < pMinZ) pMinZ = pt[1];
            if (pt[1] > pMaxZ) pMaxZ = pt[1];
        }
        centerX = (pMinX + pMaxX) / 2.0;
        centerZ = (pMinZ + pMaxZ) / 2.0;
        centerY = 64;
        double dx = pMaxX - pMinX;
        double dz = pMaxZ - pMinZ;
        radius = Math.sqrt(dx * dx + dz * dz) / 2.0;
    }

    public boolean contains(String worldName, double x, double y, double z) {
        if (!this.world.equals(worldName)) return false;

        boolean insideMain = switch (type) {
            case CUBOID -> x >= minX && x <= maxX
                    && z >= minZ && z <= maxZ;
            case RADIUS -> {
                double dx = x - centerX;
                double dz = z - centerZ;
                yield (dx * dx + dz * dz) <= (radius * radius);
            }
            case POLYGON -> isPointInPolygon(x, z, points);
        };

        if (insideMain) return true;

        for (ZoneSection sec : subSections) {
            if (sec.contains(x, y, z)) return true;
        }

        return false;
    }

    public boolean overlapsChunk(String worldName, int chunkX, int chunkZ) {
        if (!this.world.equals(worldName)) return false;

        double chunkBlockMinX = chunkX * 16.0;
        double chunkBlockMinZ = chunkZ * 16.0;
        double chunkBlockMaxX = chunkBlockMinX + 15.0;
        double chunkBlockMaxZ = chunkBlockMinZ + 15.0;

        return switch (type) {
            case CUBOID -> chunkBlockMaxX >= minX && chunkBlockMinX <= maxX
                    && chunkBlockMaxZ >= minZ && chunkBlockMinZ <= maxZ;
            case RADIUS -> {
                double nearestX = Math.max(chunkBlockMinX, Math.min(centerX, chunkBlockMaxX));
                double nearestZ = Math.max(chunkBlockMinZ, Math.min(centerZ, chunkBlockMaxZ));
                double dx = centerX - nearestX;
                double dz = centerZ - nearestZ;
                yield (dx * dx + dz * dz) <= (radius * radius);
            }
            case POLYGON -> {
                if (points.isEmpty()) yield false;
                // Calculer la Bounding Box du polygone
                double pMinX = Double.MAX_VALUE; double pMaxX = -Double.MAX_VALUE;
                double pMinZ = Double.MAX_VALUE; double pMaxZ = -Double.MAX_VALUE;
                for (double[] pt : points) {
                    if (pt[0] < pMinX) pMinX = pt[0];
                    if (pt[0] > pMaxX) pMaxX = pt[0];
                    if (pt[1] < pMinZ) pMinZ = pt[1];
                    if (pt[1] > pMaxZ) pMaxZ = pt[1];
                }
                yield chunkBlockMaxX >= pMinX && chunkBlockMinX <= pMaxX
                        && chunkBlockMaxZ >= pMinZ && chunkBlockMinZ <= pMaxZ;
            }
        };
    }

    private boolean isPointInPolygon(double x, double z, List<double[]> polyPoints) {
        if (polyPoints.size() < 3) return false;
        int i;
        int j = polyPoints.size() - 1;
        boolean inPoly = false;
        for (i = 0; i < polyPoints.size(); i++) {
            double[] pi = polyPoints.get(i);
            double[] pj = polyPoints.get(j);
            if (pi[1] < z && pj[1] >= z || pj[1] < z && pi[1] >= z) {
                if (pi[0] + (z - pi[1]) / (pj[1] - pi[1]) * (pj[0] - pi[0]) < x) {
                    inPoly = !inPoly;
                }
            }
            j = i;
        }
        return inPoly;
    }

    public boolean isCuboidComplete() {
        return pos1Set && pos2Set;
    }

    public boolean isRadiusComplete() {
        return radius > 0;
    }

    public boolean isMobAllowed(String mobKey) {
        String upper = mobKey.toUpperCase();
        if (deniedMobs.contains(upper)) return false;
        if (!allowedMobs.isEmpty()) return allowedMobs.contains(upper);
        return true;
    }

    // Getters / Setters
    public String getId()            { return id; }
    public ZoneType getType()        { return type; }
    public void setType(ZoneType t)  { this.type = t; }
    public String getWorld()         { return world; }
    public void setWorld(String w)   { this.world = w; }
    public int getPriority()         { return priority; }
    public void setPriority(int p)   { this.priority = p; }
    public boolean isSafeZone()      { return safeZone; }
    public void setSafeZone(boolean v){ this.safeZone = v; }
    public boolean isOverrideBiomeRules()       { return overrideBiomeRules; }
    public void setOverrideBiomeRules(boolean v){ this.overrideBiomeRules = v; }
    public boolean isMobsCanCross() { return mobsCanCross; }
    public void setMobsCanCross(boolean v) { this.mobsCanCross = v; }
    public double getExtStep() { return extStep; }
    public void setExtStep(double v) { this.extStep = v; }
    public double getExtMultPerStep() { return extMultPerStep; }
    public void setExtMultPerStep(double v) { this.extMultPerStep = v; }
    public double getExtMaxMult() { return extMaxMult; }
    public void setExtMaxMult(double v) { this.extMaxMult = v; }

    // Per-stat HP
    public double getExtStepHp() { return extStepHp; }
    public void setExtStepHp(double v) { this.extStepHp = v; }
    public double getExtMultHp() { return extMultHp; }
    public void setExtMultHp(double v) { this.extMultHp = v; }

    // Per-stat Damage
    public double getExtStepDmg() { return extStepDmg; }
    public void setExtStepDmg(double v) { this.extStepDmg = v; }
    public double getExtMultDmg() { return extMultDmg; }
    public void setExtMultDmg(double v) { this.extMultDmg = v; }

    // Per-stat Speed
    public double getExtStepSpd() { return extStepSpd; }
    public void setExtStepSpd(double v) { this.extStepSpd = v; }
    public double getExtMultSpd() { return extMultSpd; }
    public void setExtMultSpd(double v) { this.extMultSpd = v; }

    /** Compute the external multiplier for a given stat at the specified distance from center. */
    public double computeExtMult(double distance, double step, double multPerStep) {
        if (step <= 0.0 || multPerStep <= 0.0) return 1.0;
        if (distance <= getRadius()) return 1.0;
        double excess = distance - getRadius();
        long steps = (long) Math.floor(excess / step);
        double mult = 1.0 + steps * multPerStep;
        return Math.min(extMaxMult, mult);
    }

    /** Effective step/mult for HP (falls back to global if per-stat not configured). */
    public double computeHpExtMult(double distance) {
        double step = extStepHp > 0 ? extStepHp : extStep;
        double mult = extMultHp > 0 ? extMultHp : extMultPerStep;
        return computeExtMult(distance, step, mult);
    }

    /** Effective step/mult for Damage. */
    public double computeDmgExtMult(double distance) {
        double step = extStepDmg > 0 ? extStepDmg : extStep;
        double mult = extMultDmg > 0 ? extMultDmg : extMultPerStep;
        return computeExtMult(distance, step, mult);
    }

    /** Effective step/mult for Speed. */
    public double computeSpdExtMult(double distance) {
        double step = extStepSpd > 0 ? extStepSpd : extStep;
        double mult = extMultSpd > 0 ? extMultSpd : extMultPerStep;
        return computeExtMult(distance, step, mult);
    }

    /** True if any external scaling is configured. */
    public boolean hasExtScaling() {
        return extStep > 0.0 || extStepHp > 0.0 || extStepDmg > 0.0 || extStepSpd > 0.0;
    }
    public double getParticleHeightOffset() { return particleHeightOffset; }
    public void setParticleHeightOffset(double v) { this.particleHeightOffset = v; }
    public boolean isParticlesEnabled() { return particlesEnabled; }
    public void setParticlesEnabled(boolean v) { this.particlesEnabled = v; }
    public String getParticleInside() { return particleInside; }
    public void setParticleInside(String p) { this.particleInside = p; }
    public String getParticleOutside() { return particleOutside; }
    public void setParticleOutside(String p) { this.particleOutside = p; }
    public double getTeleportX() { return teleportX; }
    public void setTeleportX(double v) { this.teleportX = v; }
    public double getTeleportY() { return teleportY; }
    public void setTeleportY(double v) { this.teleportY = v; }
    public double getTeleportZ() { return teleportZ; }
    public void setTeleportZ(double v) { this.teleportZ = v; }
    public boolean hasCustomTeleport() { return hasCustomTeleport; }
    public void setHasCustomTeleport(boolean v) { this.hasCustomTeleport = v; }
    public StatModifiers getModifiers()         { return modifiers; }
    public void setModifiers(StatModifiers m)   { this.modifiers = m; }
    public Set<String> getAllowedMobs() { return allowedMobs; }
    public void setAllowedMobs(Set<String> s) { this.allowedMobs = s; }
    public Set<String> getDeniedMobs() { return deniedMobs; }
    public void setDeniedMobs(Set<String> s) { this.deniedMobs = s; }
    public List<String> getAllowedVariants() { return allowedVariants; }
    public void setAllowedVariants(List<String> l) { this.allowedVariants = l; }
    public List<String> getDeniedVariants() { return deniedVariants; }
    public void setDeniedVariants(List<String> l) { this.deniedVariants = l; }
    public List<String> getAllowedSquads() { return allowedSquads; }
    public void setAllowedSquads(List<String> l) { this.allowedSquads = l; }
    public List<String> getDeniedSquads() { return deniedSquads; }
    public void setDeniedSquads(List<String> l) { this.deniedSquads = l; }

    // CUBOID
    public double getMinX() { return minX; } public double getMinY() { return minY; } public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
    public void setMinY(double minY) { this.minY = minY; }
    public void setMaxY(double maxY) { this.maxY = maxY; }
    public void setMinMax(double minX, double minY, double minZ,
                          double maxX, double maxY, double maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }

    // RADIUS
    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getCenterZ() { return centerZ; }
    public double getRadius()  { return radius; }
    public void setCenter(double x, double y, double z) {
        this.centerX = x; this.centerY = y; this.centerZ = z;
    }
    public void setRadius(double r) { this.radius = r; }

    // POLYGON
    public List<double[]> getPoints() { return points; }
    public void setPoints(List<double[]> p) { this.points = p; }

    // Construction temp
    public double getPos1X() { return pos1X; }
    public double getPos1Y() { return pos1Y; }
    public double getPos1Z() { return pos1Z; }
    public boolean isPos1Set() { return pos1Set; }
    public void setPos1(double x, double y, double z) {
        this.pos1X = x; this.pos1Y = y; this.pos1Z = z; this.pos1Set = true;
    }
    public double getPos2X() { return pos2X; }
    public double getPos2Y() { return pos2Y; }
    public double getPos2Z() { return pos2Z; }
    public boolean isPos2Set() { return pos2Set; }
    public void setPos2(double x, double y, double z) {
        this.pos2X = x; this.pos2Y = y; this.pos2Z = z; this.pos2Set = true;
    }

    public String getGeometryDescription() {
        return switch (type) {
            case CUBOID -> String.format("Cuboid [%.0f,%.0f,%.0f] → [%.0f,%.0f,%.0f]",
                    minX, minY, minZ, maxX, maxY, maxZ);
            case RADIUS -> String.format("Rayon %.0f autour de [%.0f,%.0f,%.0f]",
                    radius, centerX, centerY, centerZ);
            case POLYGON -> String.format("Polygone (%d points)", points.size());
        };
    }

    public List<long[]> getCoveredChunkKeys() {
        List<long[]> chunks = new ArrayList<>();
        if (type == ZoneType.CUBOID) {
            int cMinX = (int) Math.floor(minX / 16);
            int cMinZ = (int) Math.floor(minZ / 16);
            int cMaxX = (int) Math.floor(maxX / 16);
            int cMaxZ = (int) Math.floor(maxZ / 16);
            for (int cx = cMinX; cx <= cMaxX; cx++) {
                for (int cz = cMinZ; cz <= cMaxZ; cz++) {
                    chunks.add(new long[]{cx, cz});
                }
            }
        } else if (type == ZoneType.RADIUS) {
            int cMinX = (int) Math.floor((centerX - radius) / 16);
            int cMinZ = (int) Math.floor((centerZ - radius) / 16);
            int cMaxX = (int) Math.floor((centerX + radius) / 16);
            int cMaxZ = (int) Math.floor((centerZ + radius) / 16);
            for (int cx = cMinX; cx <= cMaxX; cx++) {
                for (int cz = cMinZ; cz <= cMaxZ; cz++) {
                    chunks.add(new long[]{cx, cz});
                }
            }
        } else {
            if (!points.isEmpty()) {
                double pMinX = Double.MAX_VALUE; double pMaxX = -Double.MAX_VALUE;
                double pMinZ = Double.MAX_VALUE; double pMaxZ = -Double.MAX_VALUE;
                for (double[] pt : points) {
                    if (pt[0] < pMinX) pMinX = pt[0];
                    if (pt[0] > pMaxX) pMaxX = pt[0];
                    if (pt[1] < pMinZ) pMinZ = pt[1];
                    if (pt[1] > pMaxZ) pMaxZ = pt[1];
                }
                int cMinX = (int) Math.floor(pMinX / 16);
                int cMinZ = (int) Math.floor(pMinZ / 16);
                int cMaxX = (int) Math.floor(pMaxX / 16);
                int cMaxZ = (int) Math.floor(pMaxZ / 16);
                for (int cx = cMinX; cx <= cMaxX; cx++) {
                    for (int cz = cMinZ; cz <= cMaxZ; cz++) {
                        chunks.add(new long[]{cx, cz});
                    }
                }
            }
        }
        return chunks;
    }

    // Members management
    public java.util.Map<java.util.UUID, ZoneMember> getMembers() { return members; }
    public void addMember(ZoneMember member) { members.put(member.getPlayerUuid(), member); }
    public void removeMember(java.util.UUID uuid) { members.remove(uuid); }
    public ZoneMember getMember(java.util.UUID uuid) { return members.get(uuid); }
    public int getMemberLevel(java.util.UUID uuid) {
        ZoneMember m = members.get(uuid);
        return m != null ? m.getPermissionLevel() : 0;
    }

    // SubSections management
    public List<ZoneSection> getSubSections() { return subSections; }
    public void addSubSection(ZoneSection sec) { subSections.add(sec); }
    public void removeSubSection(String secId) { subSections.removeIf(s -> s.getId().equalsIgnoreCase(secId)); }

    // Beacon Effects management
    public java.util.Map<String, Integer> getBeaconEffects() { return beaconEffects; }
    public void setBeaconEffect(String effect, int amp) { beaconEffects.put(effect, amp); }
    public void removeBeaconEffect(String effect) { beaconEffects.remove(effect); }

    // Danger Nest management
    public boolean isDangerNest() { return dangerNest; }
    public void setDangerNest(boolean dangerNest) { this.dangerNest = dangerNest; }
    public StatModifiers getNestModifiers() { return nestModifiers; }
    public void setNestModifiers(StatModifiers nestModifiers) { this.nestModifiers = nestModifiers; }
    public double getNestSpawnBoost() { return nestSpawnBoost; }
    public void setNestSpawnBoost(double nestSpawnBoost) { this.nestSpawnBoost = nestSpawnBoost; }
}
