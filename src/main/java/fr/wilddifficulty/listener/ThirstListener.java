package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public class ThirstListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public ThirstListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAirChange(org.bukkit.event.entity.EntityAirChangeEvent event) {
        if (!plugin.getMainConfigManager().isThirstEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
        if (!pd.isThirstEnabled()) return;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;

        boolean underwater = player.getEyeLocation().getBlock().getType() == Material.WATER
                || player.getLocation().getBlock().getType() == Material.WATER;
        if (!underwater) {
            // On dry land: map the bubble bar to thirst level. Prevent vanilla from modifying it.
            int thirstAir = (int) Math.round((pd.getThirstLevel() / 20.0) * player.getMaximumAir());
            event.setAmount(thirstAir);
        }
        // Underwater: let vanilla manage air freely (apnée). No override.
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getMainConfigManager().isThirstEnabled()) return;

        Player player = event.getPlayer();
        PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
        if (!pd.isThirstEnabled()) return;

        ItemStack item = event.getItem();
        int restoreAmount = 0;

        fr.wilddifficulty.config.MainConfigManager mainCfg = plugin.getMainConfigManager();
        if (item.getType() == Material.POTION) {
            if (item.getItemMeta() instanceof PotionMeta meta) {
                if (meta.getBasePotionType() == PotionType.WATER || meta.getBasePotionType() == PotionType.AWKWARD) {
                    restoreAmount = mainCfg.getThirstRestoreWaterBottle();
                } else {
                    restoreAmount = mainCfg.getThirstRestorePotion();
                }
            } else {
                restoreAmount = mainCfg.getThirstRestorePotion();
            }
        } else if (item.getType() == Material.MILK_BUCKET) {
            restoreAmount = mainCfg.getThirstRestoreMilkBucket();
        } else if (item.getType() == Material.HONEY_BOTTLE) {
            restoreAmount = mainCfg.getThirstRestoreHoneyBottle();
        } else if (item.getType() == Material.MELON_SLICE) {
            restoreAmount = mainCfg.getThirstRestoreMelonSlice();
        } else if (item.getType() == Material.APPLE || item.getType() == Material.GOLDEN_APPLE) {
            restoreAmount = mainCfg.getThirstRestoreApple();
        }

        if (restoreAmount > 0) {
            int old = pd.getThirstLevel();
            pd.setThirstLevel(old + restoreAmount);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!plugin.getMainConfigManager().isThirstEnabled()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
        if (!pd.isThirstEnabled()) return;

        fr.wilddifficulty.config.MainConfigManager mainCfg = plugin.getMainConfigManager();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 1. Drinking Water Bucket
        if (item.getType() == Material.WATER_BUCKET) {
            int restore = mainCfg.getThirstRestoreWaterBucket();
            if (pd.getThirstLevel() < 20) {
                pd.setThirstLevel(pd.getThirstLevel() + restore);
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
                }
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
                event.setCancelled(true);
                return;
            }
        }

        // 2. Right click Cauldron or Water Block
        if (event.getClickedBlock() != null) {
            org.bukkit.block.Block block = event.getClickedBlock();
            if (block.getType() == Material.CAULDRON || block.getType().name().contains("CAULDRON")) {
                if (block.getBlockData() instanceof org.bukkit.block.data.Levelled levelled) {
                    if (levelled.getLevel() > 0) {
                        int restore = mainCfg.getThirstRestoreCauldron();
                        if (pd.getThirstLevel() < 20) {
                            pd.setThirstLevel(pd.getThirstLevel() + restore);
                            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                                levelled.setLevel(levelled.getLevel() - 1);
                                block.setBlockData(levelled);
                            }
                            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            } else if (block.getType() == Material.WATER) {
                if (item.getType() == Material.AIR || item.getType() == Material.GLASS_BOTTLE) {
                    int restore = mainCfg.getThirstRestoreWaterBlock();
                    if (pd.getThirstLevel() < 20) {
                        pd.setThirstLevel(pd.getThirstLevel() + restore);
                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        if (!plugin.getMainConfigManager().isThirstEnabled()) return;
        String msg = event.getMessage().toLowerCase();
        String cmd = msg.split(" ")[0];
        if (cmd.equals("/heal") || cmd.equals("/essentials:heal") || cmd.equals("/eheal") || cmd.equals("/wdheal")) {
            Player player = event.getPlayer();
            PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
            if (pd.isThirstEnabled()) {
                pd.setThirstLevel(20);
                plugin.getPlayerSettingsManager().save();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(org.bukkit.event.entity.EntityRegainHealthEvent event) {
        if (!plugin.getMainConfigManager().isThirstEnabled()) return;
        if (event.getEntity() instanceof Player player) {
            if (event.getRegainReason() == org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.CUSTOM) {
                PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
                if (pd.isThirstEnabled()) {
                    pd.setThirstLevel(20);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
        pd.setThirstLevel(20);
        plugin.getPlayerSettingsManager().save();
    }
}
