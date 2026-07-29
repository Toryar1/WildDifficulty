package fr.wilddifficulty.zone;

import java.util.UUID;

public class ZoneMember {

    public static final int LEVEL_CONTAINERS = 1;
    public static final int LEVEL_BUILDER    = 2;
    public static final int LEVEL_MANAGER    = 3;

    private final UUID playerUuid;
    private String lastKnownName;
    private int permissionLevel;

    public ZoneMember(UUID playerUuid, String lastKnownName, int permissionLevel) {
        this.playerUuid = playerUuid;
        this.lastKnownName = lastKnownName != null ? lastKnownName : "Unknown";
        this.permissionLevel = Math.max(1, Math.min(3, permissionLevel));
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = Math.max(1, Math.min(3, permissionLevel));
    }

    public String getRoleName() {
        return switch (permissionLevel) {
            case LEVEL_CONTAINERS -> "§aConteneurs (Niv. 1)";
            case LEVEL_BUILDER -> "§eBâtisseur (Niv. 2)";
            case LEVEL_MANAGER -> "§cGestionnaire (Niv. 3)";
            default -> "§7Inconnu";
        };
    }
}
