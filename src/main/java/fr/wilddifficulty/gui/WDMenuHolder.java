package fr.wilddifficulty.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class WDMenuHolder implements InventoryHolder {

    private final String menuType;
    private final String contextId; // ex: ID de la variante ou de l'escouade
    private Inventory inventory;

    public WDMenuHolder(String menuType, String contextId) {
        this.menuType = menuType;
        this.contextId = contextId;
    }

    public String getMenuType() { return menuType; }
    public String getContextId() { return contextId; }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
