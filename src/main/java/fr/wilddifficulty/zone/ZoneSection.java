package fr.wilddifficulty.zone;

import java.util.ArrayList;
import java.util.List;

public class ZoneSection {

    private String id;
    private DifficultyZone.ZoneType type;
    
    // CUBOID
    private double minX, minY = -64, minZ;
    private double maxX, maxY = 320, maxZ;

    // RADIUS
    private double centerX, centerY, centerZ;
    private double radius;

    // POLYGON
    private List<double[]> points = new ArrayList<>();

    public ZoneSection(String id, DifficultyZone.ZoneType type) {
        this.id = id;
        this.type = type;
    }

    public boolean contains(double x, double y, double z) {
        return switch (type) {
            case CUBOID -> x >= minX && x <= maxX && z >= minZ && z <= maxZ;
            case RADIUS -> {
                double dx = x - centerX;
                double dz = z - centerZ;
                yield (dx * dx + dz * dz) <= (radius * radius);
            }
            case POLYGON -> isPointInPolygon(x, z, points);
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

    // Getters and Setters
    public String getId() { return id; }
    public DifficultyZone.ZoneType getType() { return type; }
    public void setType(DifficultyZone.ZoneType type) { this.type = type; }

    public double getMinX() { return minX; }
    public void setMinX(double minX) { this.minX = minX; }
    public double getMinY() { return minY; }
    public void setMinY(double minY) { this.minY = minY; }
    public double getMinZ() { return minZ; }
    public void setMinZ(double minZ) { this.minZ = minZ; }

    public double getMaxX() { return maxX; }
    public void setMaxX(double maxX) { this.maxX = maxX; }
    public double getMaxY() { return maxY; }
    public void setMaxY(double maxY) { this.maxY = maxY; }
    public double getMaxZ() { return maxZ; }
    public void setMaxZ(double maxZ) { this.maxZ = maxZ; }

    public double getCenterX() { return centerX; }
    public void setCenterX(double centerX) { this.centerX = centerX; }
    public double getCenterY() { return centerY; }
    public void setCenterY(double centerY) { this.centerY = centerY; }
    public double getCenterZ() { return centerZ; }
    public void setCenterZ(double centerZ) { this.centerZ = centerZ; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public List<double[]> getPoints() { return points; }
    public void setPoints(List<double[]> points) { this.points = points; }
}
