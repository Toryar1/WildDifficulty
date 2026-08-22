package fr.wilddifficulty.encounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Récompenses distribuées aux participants à la réussite d'un Encounter.
 */
public class EncounterReward {

    private int xpAmount = 50;
    private double moneyAmount = 0.0;
    private List<RewardItem> items = new ArrayList<>();
    private List<String> consoleCommands = new ArrayList<>();

    public static class RewardItem {
        private String materialName;
        private int amount;
        private double chance; // 0.0 à 1.0
        private String customName;

        public RewardItem(String materialName, int amount, double chance, String customName) {
            this.materialName = materialName;
            this.amount = amount;
            this.chance = chance;
            this.customName = customName;
        }

        public String getMaterialName() { return materialName; }
        public int getAmount() { return amount; }
        public double getChance() { return chance; }
        public String getCustomName() { return customName; }
    }

    public int getXpAmount() { return xpAmount; }
    public void setXpAmount(int xpAmount) { this.xpAmount = xpAmount; }

    public double getMoneyAmount() { return moneyAmount; }
    public void setMoneyAmount(double moneyAmount) { this.moneyAmount = moneyAmount; }

    public List<RewardItem> getItems() { return items; }
    public void setItems(List<RewardItem> items) { this.items = items; }

    public List<String> getConsoleCommands() { return consoleCommands; }
    public void setConsoleCommands(List<String> consoleCommands) { this.consoleCommands = consoleCommands; }

    public void addItem(String materialName, int amount, double chance, String customName) {
        this.items.add(new RewardItem(materialName, amount, chance, customName));
    }
}
