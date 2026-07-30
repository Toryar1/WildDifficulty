package fr.wilddifficulty.listener;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.MainConfigManager;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.gui.GuiManager;
import fr.wilddifficulty.gui.WDMenuHolder;
import fr.wilddifficulty.util.ChatPromptUtil;
import fr.wilddifficulty.variant.MobSquad;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.variant.VariantManager;
import fr.wilddifficulty.zone.DifficultyZone;
import fr.wilddifficulty.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GuiListener implements Listener {

    private final WildDifficultyPlugin plugin;

    public GuiListener(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;
        if (event.getView().getTopInventory().getHolder() instanceof WDMenuHolder holder) {
            event.setCancelled(true);

            String menuType = holder.getMenuType();
            String contextId = holder.getContextId();

            if (menuType.startsWith("SELECT_EQ_") && inv == event.getView().getBottomInventory()) {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != org.bukkit.Material.AIR) {
                    MobVariant var = plugin.getVariantManager().getVariant(contextId);
                    if (var != null && var.getModifiers() != null) {
                        StatModifiers m = var.getModifiers();
                        String item = event.getCurrentItem().getType().name();
                        
                        if (item.startsWith("LEATHER_")) {
                            String hexColor = "none";
                            if (event.getCurrentItem().getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta lam) {
                                int rgb = lam.getColor().asRGB();
                                hexColor = String.format("%06X", rgb);
                            }
                            
                            String slotName = switch (menuType) {
                                case "SELECT_EQ_CASQUE" -> "HELMET";
                                case "SELECT_EQ_PLASTRON" -> "CHESTPLATE";
                                case "SELECT_EQ_JAMBIÈRES" -> "LEGGINGS";
                                case "SELECT_EQ_BOTTES" -> "BOOTS";
                                default -> "CHESTPLATE";
                            };

                            if (!hexColor.equals("none")) {
                                switch (slotName) {
                                    case "HELMET" -> { m.setHelmetItem(item); m.setHelmetColor(hexColor); }
                                    case "CHESTPLATE" -> { m.setChestplateItem(item); m.setChestplateColor(hexColor); }
                                    case "LEGGINGS" -> { m.setLeggingsItem(item); m.setLeggingsColor(hexColor); }
                                    case "BOOTS" -> { m.setBootsItem(item); m.setBootsColor(hexColor); }
                                }
                                plugin.getVariantManager().save();
                                player.sendMessage("§a[WD] Équipement cuir " + item + " défini avec la couleur #" + hexColor + " depuis votre inventaire.");
                                plugin.getGuiManager().openVariantEquipmentEditor(player, contextId);
                            } else {
                                plugin.getGuiManager().openLeatherColorSelector(player, contextId, slotName, item);
                            }
                            return;
                        }

                        switch (menuType) {
                            case "SELECT_EQ_CASQUE" -> m.setHelmetItem(item);
                            case "SELECT_EQ_PLASTRON" -> m.setChestplateItem(item);
                            case "SELECT_EQ_JAMBIÈRES" -> m.setLeggingsItem(item);
                            case "SELECT_EQ_BOTTES" -> m.setBootsItem(item);
                            case "SELECT_EQ_MAIN_PRINCIPALE" -> m.setMainHandItem(item);
                            case "SELECT_EQ_MAIN_SECONDAIRE" -> m.setOffHandItem(item);
                        }
                        plugin.getVariantManager().save();
                        player.sendMessage("§a[WD] Équipement défini sur " + item + " depuis votre inventaire.");
                        plugin.getGuiManager().openVariantEquipmentEditor(player, contextId);
                    }
                }
                return;
            }

            if (event.getCurrentItem() == null) return;

            GuiManager gui = plugin.getGuiManager();
            VariantManager varManager = plugin.getVariantManager();
            ZoneManager zoneManager = plugin.getZoneManager();
            MainConfigManager mainCfg = plugin.getMainConfigManager();
            int slot = event.getRawSlot();

            // Return buttons generic handler — use PDC key so it works in all languages
            if (event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                if (event.getCurrentItem().hasItemMeta()) {
                    org.bukkit.NamespacedKey backKey = new org.bukkit.NamespacedKey(plugin, "wd_back_button");
                    String isBack = event.getCurrentItem().getItemMeta().getPersistentDataContainer()
                            .get(backKey, org.bukkit.persistence.PersistentDataType.STRING);
                    if ("true".equals(isBack)) {
                        handleGenericReturn(player, menuType, contextId, gui);
                        return;
                    }
                    // Fallback for legacy items without PDC (text-based)
                    if (event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());
                        if (plainName.contains("Retour") || plainName.contains("Annuler") || plainName.contains("Back") || plainName.contains("Cancel")
                                || plainName.contains("Close") || plainName.contains("Fermer")) {
                            handleGenericReturn(player, menuType, contextId, gui);
                            return;
                        }
                    }
                }
            }

            switch (menuType) {
                case "MAIN" -> {
                    if (slot == 10) gui.openGlobalModifiersMenu(player);
                    else if (slot == 12) gui.openVariantList(player);
                    else if (slot == 14) gui.openSquadList(player);
                    else if (slot == 16) gui.openZoneList(player);
                    else if (slot == 20) gui.openBloodMoonEditor(player);
                    else if (slot == 22) gui.openGeneralConfig(player);
                    else if (slot == 24) gui.openAdminToolsMenu(player);
                    else if (slot == 28) {
                        mainCfg.setBlockVanillaHostiles(!mainCfg.isBlockVanillaHostiles());
                        mainCfg.save();
                        gui.openMainMenu(player);
                    }
                    else if (slot == 30) {
                        mainCfg.setBlockVanillaPassives(!mainCfg.isBlockVanillaPassives());
                        mainCfg.save();
                        gui.openMainMenu(player);
                    }
                    else if (slot == 32) {
                        gui.openLanguageSelector(player);
                    }
                    else if (slot == 34) {
                        plugin.reloadAll();
                        player.sendMessage(plugin.getLangManager().get("general.config_reloaded"));
                        player.closeInventory();
                    }
                    else if (slot == 49) {
                        player.closeInventory();
                    }
                }
                case "GENERAL_CONFIG" -> {
                    if (slot == 49) {
                        gui.openMainMenu(player);
                        return;
                    }
                    if (slot == 10) {
                        mainCfg.setDebug(!mainCfg.isDebug());
                        mainCfg.save();
                        gui.openGeneralConfig(player);
                        return;
                    }
                    if (slot == 11) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.distance_de_spawn_max_par"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    mainCfg.setMaxSpawnDistance(val);
                                    mainCfg.save();
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.distance_de_spawn_maximale_définie"));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openGeneralConfig(player));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? (event.isShiftClick() ? 50 : 10) : (event.isShiftClick() ? -50 : -10);
                        int current = mainCfg.getMaxSpawnDistance();
                        if (current == -1) current = 0;
                        int next = Math.max(-1, current + delta);
                        if (next <= 0) next = -1; // -1 represents disabled
                        mainCfg.setMaxSpawnDistance(next);
                        mainCfg.save();
                        gui.openGeneralConfig(player);
                        return;
                    }
                    if (slot == 12) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.cap_maximum_de_variantes_par"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    mainCfg.setCapVariantesParJoueur(val);
                                    mainCfg.save();
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.cap_maximum_de_variantes_défini"));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openGeneralConfig(player));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? (event.isShiftClick() ? 20 : 5) : (event.isShiftClick() ? -20 : -5);
                        mainCfg.setCapVariantesParJoueur(mainCfg.getCapVariantesParJoueur() + delta);
                        mainCfg.save();
                        gui.openGeneralConfig(player);
                        return;
                    }
                    if (slot == 19) {
                        mainCfg.setDisableBurningGlobally(!mainCfg.isDisableBurningGlobally());
                        mainCfg.save();
                        gui.openGeneralConfig(player);
                        return;
                    }
                    if (slot == 20) {
                        mainCfg.setAllowDaySpawnGlobally(!mainCfg.isAllowDaySpawnGlobally());
                        mainCfg.save();
                        gui.openGeneralConfig(player);
                        return;
                    }
                    if (slot == 28) {
                        mainCfg.setNametagsEnabled(!mainCfg.isNametagsEnabled());
                        mainCfg.save();
                        gui.openGeneralConfig(player);
                        return;
                    }
                    if (slot == 29) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_format_des_nametags"), input -> {
                            mainCfg.setNametagFormat(input);
                            mainCfg.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.format_des_nametags_mis_à"));
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openGeneralConfig(player));
                        });
                        return;
                    }
                    if (slot == 30) {
                        gui.openThirstHardcoreAdminGui(player);
                        return;
                    }
                    if (slot == 31) {
                        gui.openLanguageSelector(player);
                        return;
                    }
                    if (slot == 49 || slot == 53) {
                        gui.openMainMenu(player);
                        return;
                    }
                }
                case "LANGUAGE_SELECT" -> {
                    if (slot == 49) {
                        gui.openMainMenu(player);
                        return;
                    }
                    int[] langSlots = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23};
                    int idx = -1;
                    for (int i = 0; i < langSlots.length; i++) {
                        if (langSlots[i] == slot) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        List<String> codes = new ArrayList<>(fr.wilddifficulty.config.LanguageSetup.getAvailableLanguages().keySet());
                        if (idx < codes.size()) {
                            String selectedCode = codes.get(idx);
                            plugin.getLanguageSetup().changeLanguage(selectedCode, player);
                        }
                    }
                }
                case "ADMIN_TOOLS" -> {
                    if (slot == 49) {
                        gui.openMainMenu(player);
                        return;
                    }
                    if (slot == 20) {
                        player.getInventory().addItem(fr.wilddifficulty.util.ToolsUtil.getZoneTool());
                        player.sendMessage(plugin.getLangManager().get("tools.given_zone"));
                    } else if (slot == 21) {
                        player.getInventory().addItem(fr.wilddifficulty.util.ToolsUtil.getSpawnerTool());
                        player.sendMessage(plugin.getLangManager().get("tools.given_spawner"));
                    } else if (slot == 22) {
                        player.getInventory().addItem(fr.wilddifficulty.util.ToolsUtil.getBiomeTool());
                        player.sendMessage(plugin.getLangManager().get("tools.given_biome"));
                    } else if (slot == 23) {
                        player.getInventory().addItem(fr.wilddifficulty.util.ToolsUtil.getInspectorTool());
                        player.sendMessage(plugin.getLangManager().get("tools.given_inspector"));
                    } else if (slot == 24) {
                        fr.wilddifficulty.util.ToolsUtil.giveAllTools(player);
                        player.sendMessage(plugin.getLangManager().get("tools.given_all"));
                    } else if (slot == 31) {
                        org.bukkit.Bukkit.dispatchCommand(player, "wd scoreboard");
                    }
                }
                case "SPAWNER_EDIT" -> {
                    String[] parts = contextId.split(":");
                    org.bukkit.World world = Bukkit.getWorld(parts[0]);
                    if (world == null) return;
                    Location loc = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                    fr.wilddifficulty.spawner.CustomSpawner spawner = plugin.getSpawnerManager().getSpawner(loc);
                    if (spawner == null) return;

                    if (slot == 35) {
                        player.closeInventory();
                        return;
                    }

                    if (slot == 31) {
                        plugin.getSpawnerManager().removeSpawner(loc);
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.spawner_supprimé"));
                        player.closeInventory();
                        return;
                    }

                    if (slot == 10) {
                        spawner.setActive(!spawner.isActive());
                        plugin.getSpawnerManager().save();
                    } else if (slot == 11) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_lintervalle_de_spawn_en"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    spawner.setInterval(val);
                                    plugin.getSpawnerManager().save();
                                    player.sendMessage("§aIntervalle défini à " + val + " secondes.");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSpawnerEditor(player, loc));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? (event.isShiftClick() ? 10 : 1) : (event.isShiftClick() ? -10 : -1);
                        spawner.setInterval(spawner.getInterval() + delta);
                        plugin.getSpawnerManager().save();
                    } else if (slot == 12) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_rayon_de_détection"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    spawner.setRadius(val);
                                    plugin.getSpawnerManager().save();
                                    player.sendMessage("§aRayon défini à " + val + " blocs.");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSpawnerEditor(player, loc));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? (event.isShiftClick() ? 5 : 1) : (event.isShiftClick() ? -5 : -1);
                        spawner.setRadius(spawner.getRadius() + delta);
                        plugin.getSpawnerManager().save();
                    } else if (slot == 13) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_la_limite_maximale_de"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    spawner.setMaxNearby(val);
                                    plugin.getSpawnerManager().save();
                                    player.sendMessage("§aLimite définie à " + val + " monstres.");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSpawnerEditor(player, loc));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? (event.isShiftClick() ? 5 : 1) : (event.isShiftClick() ? -5 : -1);
                        spawner.setMaxNearby(spawner.getMaxNearby() + delta);
                        plugin.getSpawnerManager().save();
                    } else if (slot == 15) {
                        gui.openSpawnerVariantsEditor(player, loc);
                        return;
                    } else if (slot == 20) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_la_variation_dintervalle_en"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    spawner.setIntervalRange(Math.max(0, val));
                                    plugin.getSpawnerManager().save();
                                    player.sendMessage("§aVariation d'intervalle définie à " + val + " secondes.");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSpawnerEditor(player, loc));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? (event.isShiftClick() ? 5 : 1) : (event.isShiftClick() ? -5 : -1);
                        spawner.setIntervalRange(Math.max(0, spawner.getIntervalRange() + delta));
                        plugin.getSpawnerManager().save();
                    } else if (slot == 21) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_rayon_de_dispersion"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    spawner.setSpawnRange(Math.max(0, val));
                                    plugin.getSpawnerManager().save();
                                    player.sendMessage("§aRayon de dispersion défini à " + val + " blocs.");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSpawnerEditor(player, loc));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? (event.isShiftClick() ? 5 : 1) : (event.isShiftClick() ? -5 : -1);
                        spawner.setSpawnRange(Math.max(0, spawner.getSpawnRange() + delta));
                        plugin.getSpawnerManager().save();
                    } else if (slot == 22) {
                        plugin.getSpawnerManager().getSpawnerClipboards().put(player.getUniqueId(), spawner.clone());
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.configuration_du_spawner_copiée"));
                    } else if (slot == 23) {
                        fr.wilddifficulty.spawner.CustomSpawner clipboard = plugin.getSpawnerManager().getSpawnerClipboards().get(player.getUniqueId());
                        if (clipboard != null) {
                            spawner.setActive(clipboard.isActive());
                            spawner.setInterval(clipboard.getInterval());
                            spawner.setIntervalRange(clipboard.getIntervalRange());
                            spawner.setRadius(clipboard.getRadius());
                            spawner.setSpawnRange(clipboard.getSpawnRange());
                            spawner.setMaxNearby(clipboard.getMaxNearby());
                            spawner.setParticleType(clipboard.getParticleType());
                            spawner.setSoundType(clipboard.getSoundType());
                            spawner.setVariantWeights(new java.util.HashMap<>(clipboard.getVariantWeights()));
                            plugin.getSpawnerManager().save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.configuration_du_spawner_collée"));
                        }
                    } else if (slot == 24) {
                        String current = spawner.getParticleType();
                        String next = switch (current.toUpperCase()) {
                            case "SMOKE" -> "FLAME";
                            case "FLAME" -> "PORTAL";
                            case "PORTAL" -> "NONE";
                            default -> "SMOKE";
                        };
                        spawner.setParticleType(next);
                        plugin.getSpawnerManager().save();
                    } else if (slot == 25) {
                        String current = spawner.getSoundType();
                        String next = switch (current.toUpperCase()) {
                            case "PLING" -> "AMBIENT";
                            case "AMBIENT" -> "EXPLODE";
                            case "EXPLODE" -> "NONE";
                            default -> "PLING";
                        };
                        spawner.setSoundType(next);
                        plugin.getSpawnerManager().save();
                    }

                    gui.openSpawnerEditor(player, loc);
                }
                case "SPAWNER_VARIANTS" -> {
                    String[] parts = contextId.split(":");
                    org.bukkit.World world = Bukkit.getWorld(parts[0]);
                    if (world == null) return;
                    Location loc = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                    fr.wilddifficulty.spawner.CustomSpawner spawner = plugin.getSpawnerManager().getSpawner(loc);
                    if (spawner == null) return;

                    if (slot == 53) {
                        gui.openSpawnerEditor(player, loc);
                        return;
                    }

                    List<MobVariant> list = new ArrayList<>(varManager.getAllVariants());
                    if (slot < list.size()) {
                        MobVariant var = list.get(slot);
                        boolean active = spawner.getVariantWeights().containsKey(var.getId());

                        if (event.getClick() == ClickType.MIDDLE && active) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_poids_précis_de") + var.getId() + " (ex: 5) :", input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    spawner.getVariantWeights().put(var.getId(), Math.max(1, val));
                                    plugin.getSpawnerManager().save();
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.poids_défini"));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSpawnerVariantsEditor(player, loc));
                            });
                            return;
                        }

                        if (active) {
                            if (event.isLeftClick()) {
                                spawner.getVariantWeights().remove(var.getId());
                                player.sendMessage("§cVariante " + var.getId() + " retirée du spawner.");
                            } else if (event.getClick() == ClickType.RIGHT) {
                                int delta = event.isShiftClick() ? 5 : 1;
                                int curW = spawner.getVariantWeights().getOrDefault(var.getId(), 1);
                                spawner.getVariantWeights().put(var.getId(), Math.max(1, curW + delta));
                            }
                        } else {
                            if (event.isLeftClick()) {
                                spawner.getVariantWeights().put(var.getId(), 1);
                                player.sendMessage("§aVariante " + var.getId() + " ajoutée au spawner (poids: 1).");
                            }
                        }
                        plugin.getSpawnerManager().save();
                        gui.openSpawnerVariantsEditor(player, loc);
                    }
                }
                case "GLOBALS" -> {
                    if (slot == 22 || slot == 26) {
                        gui.openMainMenu(player);
                        return;
                    }
                    handleModifierClick(player, event, "global", null, gui, varManager, zoneManager, mainCfg);
                }
                case "BLOODMOON_EDIT" -> {
                    if (slot == 49) {
                        gui.openMainMenu(player);
                        return;
                    }
                    if (slot == 10) {
                        mainCfg.setBloodMoonEnabled(!mainCfg.isBloodMoonEnabled());
                        mainCfg.save();
                        gui.openBloodMoonEditor(player);
                        return;
                    }
                    if (slot == 11) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.chance_lune_de_sang_00"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setBloodMoonChance(val);
                                    mainCfg.save();
                                    player.sendMessage("§aChance définie à " + String.format("%.0f%%", val * 100));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? 0.05 : -0.05;
                        mainCfg.setBloodMoonChance(mainCfg.getBloodMoonChance() + delta);
                        mainCfg.save();
                        gui.openBloodMoonEditor(player);
                    }
                    else if (slot == 12) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.multiplicateur_pv_lune_de_sang"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setBloodMoonHpMultiplier(val);
                                    mainCfg.save();
                                    player.sendMessage("§aMultiplicateur PV défini à " + val + "x");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? (event.isShiftClick() ? 0.5 : 0.1) : (event.isShiftClick() ? -0.5 : -0.1);
                        mainCfg.setBloodMoonHpMultiplier(mainCfg.getBloodMoonHpMultiplier() + delta);
                        mainCfg.save();
                        gui.openBloodMoonEditor(player);
                    }
                    else if (slot == 13) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.multiplicateur_dégâts_lune_de_sang"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setBloodMoonDamageMultiplier(val);
                                    mainCfg.save();
                                    player.sendMessage("§aMultiplicateur Dégâts défini à " + val + "x");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? (event.isShiftClick() ? 0.5 : 0.1) : (event.isShiftClick() ? -0.5 : -0.1);
                        mainCfg.setBloodMoonDamageMultiplier(mainCfg.getBloodMoonDamageMultiplier() + delta);
                        mainCfg.save();
                        gui.openBloodMoonEditor(player);
                    }
                    else if (slot == 14) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.multiplicateur_vitesse_lune_de_sang"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setBloodMoonSpeedMultiplier(val);
                                    mainCfg.save();
                                    player.sendMessage("§aMultiplicateur Vitesse défini à " + val + "x");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? (event.isShiftClick() ? 0.5 : 0.1) : (event.isShiftClick() ? -0.5 : -0.1);
                        mainCfg.setBloodMoonSpeedMultiplier(mainCfg.getBloodMoonSpeedMultiplier() + delta);
                        mainCfg.save();
                        gui.openBloodMoonEditor(player);
                    }
                    else if (slot == 15) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.multiplicateur_drops_lune_de_sang"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setBloodMoonDropsMultiplier(val);
                                    mainCfg.save();
                                    player.sendMessage("§aMultiplicateur Drops défini à " + val + "x");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? (event.isShiftClick() ? 0.5 : 0.1) : (event.isShiftClick() ? -0.5 : -0.1);
                        mainCfg.setBloodMoonDropsMultiplier(mainCfg.getBloodMoonDropsMultiplier() + delta);
                        mainCfg.save();
                        gui.openBloodMoonEditor(player);
                    }
                    else if (slot == 16) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.multiplicateur_spawn_lune_de_sang"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setBloodMoonSpawnMultiplier(val);
                                    mainCfg.save();
                                    player.sendMessage("§aMultiplicateur Spawn défini à " + val + "x");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? (event.isShiftClick() ? 0.5 : 0.1) : (event.isShiftClick() ? -0.5 : -0.1);
                        mainCfg.setBloodMoonSpawnMultiplier(mainCfg.getBloodMoonSpawnMultiplier() + delta);
                        mainCfg.save();
                        gui.openBloodMoonEditor(player);
                    } else if (slot == 18) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_message_de_début"), input -> {
                            mainCfg.setBloodMoonStartMessage(input);
                            mainCfg.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.message_de_début_défini"));
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 19) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_message_de_fin"), input -> {
                            mainCfg.setBloodMoonEndMessage(input);
                            mainCfg.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.message_de_fin_défini"));
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 20) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_type_de_son"), input -> {
                            try {
                                org.bukkit.Sound.valueOf(input.toUpperCase().trim());
                                mainCfg.setBloodMoonStartSound(input.toUpperCase().trim());
                                mainCfg.save();
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.son_de_début_défini"));
                            } catch (Exception e) {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.type_de_son_invalide_utilisez"));
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 21) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_type_de_son_1"), input -> {
                            try {
                                org.bukkit.Sound.valueOf(input.toUpperCase().trim());
                                mainCfg.setBloodMoonEndSound(input.toUpperCase().trim());
                                mainCfg.save();
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.son_de_fin_défini"));
                            } catch (Exception e) {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.type_de_son_invalide_utilisez"));
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 22) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_la_particule_de_début"), input -> {
                            try {
                                org.bukkit.Particle.valueOf(input.toUpperCase().trim());
                                mainCfg.setBloodMoonStartParticle(input.toUpperCase().trim());
                                mainCfg.save();
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.particule_de_début_définie"));
                            } catch (Exception e) {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.type_de_particule_invalide"));
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 23) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_la_particule_de_fin"), input -> {
                            try {
                                org.bukkit.Particle.valueOf(input.toUpperCase().trim());
                                mainCfg.setBloodMoonEndParticle(input.toUpperCase().trim());
                                mainCfg.save();
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.particule_de_fin_définie"));
                            } catch (Exception e) {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.type_de_particule_invalide"));
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 24) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_les_potions_de_début"), input -> {
                            java.util.List<String> list = new java.util.ArrayList<>();
                            for (String part : input.split(",")) {
                                String clean = part.trim();
                                if (!clean.isEmpty()) list.add(clean);
                            }
                            mainCfg.setBloodMoonStartPotions(list);
                            mainCfg.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.effets_de_potion_de_début"));
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 25) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_les_potions_de_fin"), input -> {
                            java.util.List<String> list = new java.util.ArrayList<>();
                            for (String part : input.split(",")) {
                                String clean = part.trim();
                                if (!clean.isEmpty()) list.add(clean);
                            }
                            mainCfg.setBloodMoonEndPotions(list);
                            mainCfg.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.effets_de_potion_de_fin"));
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openBloodMoonEditor(player));
                        });
                    } else if (slot == 31) {
                        org.bukkit.World world = player.getWorld();
                        long time = world.getTime();
                        boolean isNight = time >= 13000 && time < 23000;
                        if (isNight) {
                            if (!mainCfg.isBloodMoonActive()) {
                                mainCfg.setBloodMoonActive(true);
                                world.setMetadata("wd_bloodmoon_rolled", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                plugin.getServer().broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(mainCfg.getBloodMoonStartMessage()));
                                for (Player p : world.getPlayers()) {
                                    try {
                                        p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(mainCfg.getBloodMoonStartSound().toUpperCase()), 1.0f, 1.0f);
                                    } catch (Exception ignored) {}
                                    try {
                                        p.spawnParticle(org.bukkit.Particle.valueOf(mainCfg.getBloodMoonStartParticle().toUpperCase()), p.getLocation().add(0, 1, 0), 40, 0.6, 0.6, 0.6, 0.05);
                                    } catch (Exception ignored) {}
                                    for (String potStr : mainCfg.getBloodMoonStartPotions()) {
                                        try {
                                            String[] parts = potStr.split(":");
                                            org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(parts[0].toUpperCase());
                                            int duration = Integer.parseInt(parts[1]);
                                            int amp = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                                            if (type != null) {
                                                p.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration, amp, false, true));
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.wd_lune_de_sang_activée"));
                            } else {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.la_lune_de_sang_est"));
                            }
                        } else {
                            world.setMetadata("wd_bloodmoon_forced", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                            if (world.hasMetadata("wd_bloodmoon_rolled")) {
                                world.removeMetadata("wd_bloodmoon_rolled", plugin);
                            }
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.wd_lune_de_sang_planifiée"));
                        }
                        gui.openBloodMoonEditor(player);
                    } else if (slot == 49 || slot == 53) {
                        gui.openMainMenu(player);
                    }
                }
                case "VARIANT_LIST" -> {
                    if (slot == 49) {
                        // Click "+ Nouvelle Variante" opens base EntityType Selection first
                        gui.openEntityTypeSelection(player, "CREATE_FLOW");
                        return;
                    }
                    if (slot == 50) {
                        gui.cycleSortFilter(player.getUniqueId());
                        gui.openVariantList(player);
                        return;
                    }
                    if (slot == 53) {
                        gui.openMainMenu(player);
                        return;
                    }
                    String name = getCleanName(event.getCurrentItem()).trim();
                    if (!name.isEmpty() && varManager.getVariant(name) != null) {
                        gui.openVariantEditor(player, name);
                    }
                }
                case "CHOOSE_ENTITY_TYPE" -> {
                    String baseContext = contextId;
                    int page = 0;
                    String filter = "ALL";
                    if (contextId.contains(":")) {
                        String[] parts = contextId.split(":");
                        baseContext = parts[0];
                        if (parts.length > 2) {
                            try { page = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                            filter = parts[2];
                        } else if (parts.length > 1) {
                            try { page = Integer.parseInt(parts[1]); } catch (Exception e) { filter = parts[1]; }
                        }
                    }

                    if (slot == 45) {
                        gui.openEntityTypeSelection(player, baseContext + ":0:HOSTILES");
                        return;
                    }
                    if (slot == 46) {
                        gui.openEntityTypeSelection(player, baseContext + ":0:PASSIVES");
                        return;
                    }
                    if (slot == 47) {
                        gui.openEntityTypeSelection(player, baseContext + ":0:ALL");
                        return;
                    }
                    if (slot == 48 && page > 0) {
                        gui.openEntityTypeSelection(player, baseContext + ":" + (page - 1) + ":" + filter);
                        return;
                    }
                    if (slot == 50) {
                        gui.openEntityTypeSelection(player, baseContext + ":" + (page + 1) + ":" + filter);
                        return;
                    }
                    if (slot == 53) {
                        handleGenericReturn(player, menuType, contextId, gui);
                        return;
                    }

                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String typeName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        try {
                            EntityType type = EntityType.valueOf(typeName);
                            if (baseContext.equals("CREATE_FLOW")) {
                                // Creation Flow: Ask ID in chat
                                final String finalBaseContext = baseContext;
                                ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_lidentifiant_id_unique_pour") + type.name() + " (ex: zombie_geant) :", input -> {
                                    String id = input.toLowerCase().replace(" ", "_");
                                    if (varManager.getVariant(id) == null) {
                                        MobVariant var = new MobVariant(id, type, null, 10, false, new StatModifiers());
                                        varManager.addVariant(var);
                                        varManager.save();
                                        player.sendMessage("§aVariante " + id + " créée !");
                                        plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantEditor(player, id));
                                    } else {
                                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.cette_variante_existe_déjà"));
                                        plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantList(player));
                                    }
                                });
                            } else {
                                // Change Base Type of existing variant
                                MobVariant var = varManager.getVariant(baseContext);
                                if (var != null) {
                                    var.setType(type);
                                    varManager.save();
                                    player.sendMessage("§aType de base modifié pour " + type.name());
                                    gui.openVariantEditor(player, baseContext);
                                }
                            }
                        } catch (Exception e) {
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.type_dentité_invalide"));
                            gui.openVariantList(player);
                        }
                    }
                }
                case "VARIANT_EDIT" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    if (slot == 31) {
                        varManager.removeVariant(contextId);
                        varManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.variante_supprimée"));
                        gui.openVariantList(player);
                        return;
                    }
                    if (slot == 32) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_le_nouvel_id_unique"), input -> {
                            String newId = input.toLowerCase().replace(" ", "_");
                            if (varManager.getVariant(newId) == null) {
                                MobVariant clone = varManager.cloneVariant(var, newId);
                                varManager.addVariant(clone);
                                varManager.save();
                                player.sendMessage("§aVariante clonée avec succès sous l'ID " + newId + " !");
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantEditor(player, newId));
                            } else {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.une_variante_existe_déjà_avec"));
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantEditor(player, contextId));
                            }
                        });
                        return;
                    }
                    if (slot == 10) {
                        gui.openEntityTypeSelection(player, contextId);
                    } else if (slot == 11) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_le_nouveau_poids_ex"), input -> {
                            try {
                                var.setWeight(Integer.parseInt(input));
                                varManager.save();
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.poids_modifié"));
                            } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantEditor(player, contextId));
                        });
                    } else if (slot == 12) {
                        var.setIgnoreSunlight(!var.isIgnoreSunlight());
                        varManager.save();
                        plugin.updateVisualizedMobs(contextId);
                        gui.openVariantEditor(player, contextId);
                    } else if (slot == 13) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_la_quantité_dxp_donnée"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    var.setXpOnDeath(val);
                                    varManager.save();
                                    player.sendMessage("§a[WD] XP donnée à la mort définie sur : " + (val < 0 ? "Vanilla" : val + " XP"));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantEditor(player, contextId));
                            });
                            return;
                        }

                        int delta = 1;
                        if (event.isShiftClick()) {
                            delta = 10;
                        }
                        if (event.isRightClick()) {
                            delta = -delta;
                        }
                        int current = var.getXpOnDeath();
                        int next = Math.max(-1, current + delta);
                        var.setXpOnDeath(next);
                        varManager.save();
                        gui.openVariantEditor(player, contextId);
                    } else if (slot == 14) {
                        gui.openVariantSpawnConditions(player, contextId);
                    } else if (slot == 20) {
                        gui.openVariantModifiers(player, contextId);
                    } else if (slot == 21) {
                        gui.openVariantBehaviors(player, contextId);
                    } else if (slot == 22) {
                        gui.openVariantAesthetics(player, contextId);
                    } else if (slot == 23) {
                        gui.openVariantDropsAudio(player, contextId);
                    } else if (slot == 24) {
                        // Spawn options
                        player.closeInventory();
                        if (event.isRightClick()) {
                            player.performCommand("wd spawn " + contextId + " normal");
                        } else {
                            player.performCommand("wd spawn " + contextId + " static");
                        }
                    } else if (slot == 35) {
                        gui.openVariantList(player);
                    }
                }
                case "VARIANT_MODS" -> {
                    if (slot == 15) {
                        gui.openEquipmentTierSelector(player, contextId, "variant");
                        return;
                    }
                    handleModifierClick(player, event, "variant", contextId, gui, varManager, zoneManager, mainCfg);
                }
                case "VARIANT_BEHAVIORS" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    StatModifiers mods = var.getModifiers();

                    if (slot == 10) mods.setTeleportToTarget(!mods.isTeleportToTarget());
                    else if (slot == 11) mods.setDashAttack(!mods.isDashAttack());
                    else if (slot == 12) mods.setCamouflage(!mods.isCamouflage());
                    else if (slot == 13) mods.setGoalWallVision(!mods.isGoalWallVision());
                    else if (slot == 14) {
                        if (mods.getPassiveRegen() > 0) {
                            mods.setPassiveRegen(0.0);
                        } else {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_les_hp_par_seconde"), input -> {
                                try {
                                    mods.setPassiveRegen(Double.parseDouble(input));
                                    varManager.save();
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.régénération_modifiée"));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantBehaviors(player, contextId));
                            });
                            return;
                        }
                    }
                    else if (slot == 15) mods.setExplodeOnDeath(!mods.isExplodeOnDeath());
                    else if (slot == 16) mods.setRangedAttack(!mods.isRangedAttack());
                    else if (slot == 17) {
                        gui.openPotionSelector(player, contextId);
                        return;
                    }
                    else if (slot == 18) {
                        String current = var.getAggroMode();
                        String next = switch (current.toUpperCase()) {
                            case "PASSIVE" -> "AGGRESSIVE";
                            case "AGGRESSIVE" -> "NEUTRAL_HIT";
                            case "NEUTRAL_HIT" -> "NEUTRAL_SQUAD_HIT";
                            default -> "PASSIVE";
                        };
                        var.setAggroMode(next);
                        plugin.updateVisualizedMobs(contextId);
                    }
                    else if (slot == 19) {
                        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_seuil_de_pv"), input -> {
                                try {
                                    double val = Double.parseDouble(input) / 100.0;
                                    mods.setFleeUnderHealth(Math.max(0.0, Math.min(1.0, val)));
                                    varManager.save();
                                    player.sendMessage("§aSeuil de fuite défini à " + input + "%");
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantBehaviors(player, contextId));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? 0.05 : -0.05;
                        mods.setFleeUnderHealth(Math.max(0.0, Math.min(1.0, mods.getFleeUnderHealth() + delta)));
                    }
                    else if (slot == 20) {
                        mods.setFleeWhenSolo(!mods.isFleeWhenSolo());
                    }
                    else if (slot == 21) {
                        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                            mods.setDeathSpawnVariant("none");
                            mods.setDeathSpawnAmount(0);
                            varManager.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.spawns_de_renfort_à_la"));
                            gui.openVariantBehaviors(player, contextId);
                            return;
                        }
                        if (event.isLeftClick()) {
                            gui.openVariantReinforcementSelector(player, contextId);
                            return;
                        } else if (event.isRightClick()) {
                            int delta = event.isShiftClick() ? 5 : 1;
                            mods.setDeathSpawnAmount(Math.max(0, mods.getDeathSpawnAmount() + delta));
                        }
                    }
                    else if (slot == 22) {
                        mods.setJumpAttack(!mods.isJumpAttack());
                    }
                    else if (slot == 23) {
                        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_nombre_max_dutilisations"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    mods.setTeleportMaxUses(val);
                                    varManager.save();
                                    player.sendMessage("§aTéléportations max définies à " + val);
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantBehaviors(player, contextId));
                            });
                            return;
                        }
                        int delta = event.isShiftClick() ? 5 : 1;
                        if (event.isRightClick()) delta = -delta;
                        int current = mods.getTeleportMaxUses();
                        if (current == -1) current = 0;
                        mods.setTeleportMaxUses(Math.max(-1, current + delta));
                    }
                    else if (slot == 24) {
                        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_nombre_max_dutilisations_1"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    mods.setDashMaxUses(val);
                                    varManager.save();
                                    player.sendMessage("§aCharges max définies à " + val);
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantBehaviors(player, contextId));
                            });
                            return;
                        }
                        int delta = event.isShiftClick() ? 5 : 1;
                        if (event.isRightClick()) delta = -delta;
                        int current = mods.getDashMaxUses();
                        if (current == -1) current = 0;
                        mods.setDashMaxUses(Math.max(-1, current + delta));
                    }
                    else if (slot == 25) {
                        gui.openRangedProjectileSelector(player, contextId);
                        return;
                    }
                    else if (slot == 26) {
                        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                            mods.setOnHitPotionEffect("none");
                            mods.setOnHitPotionChance(0.0);
                            mods.setOnHitPotionAmplifier(0);
                            mods.setOnHitPotionDuration(100);
                            varManager.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.effet_onhit_réinitialisé"));
                            gui.openVariantBehaviors(player, contextId);
                            return;
                        }
                        if (event.isLeftClick()) {
                            if (event.isShiftClick()) {
                                mods.setOnHitPotionDuration(mods.getOnHitPotionDuration() + 20);
                            } else {
                                gui.openOnHitPotionSelector(player, contextId);
                                return;
                            }
                        } else if (event.isRightClick()) {
                            if (event.isShiftClick()) {
                                int amp = mods.getOnHitPotionAmplifier();
                                mods.setOnHitPotionAmplifier((amp + 1) % 5);
                            } else {
                                double ch = mods.getOnHitPotionChance() + 0.05;
                                if (ch > 1.0) ch = 0.0;
                                mods.setOnHitPotionChance(ch);
                            }
                        }
                    }
                    else if (slot == 27) {
                        mods.setSmartClimb(!mods.isSmartClimb());
                    }
                    else if (slot == 35) {
                        gui.openVariantEditor(player, contextId);
                        return;
                    }

                    varManager.save();
                    gui.openVariantBehaviors(player, contextId);
                }
                case "VARIANT_AESTHETICS" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    StatModifiers mods = var.getModifiers();

                    if (slot == 10) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_le_nouveau_nom_daffichage"), input -> {
                            var.setDisplayName(input.trim());
                            varManager.save();
                            plugin.updateVisualizedMobs(contextId);
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nom_daffichage_modifié"));
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantAesthetics(player, contextId));
                        });
                        return;
                    } else if (slot == 11) {
                        gui.openVariantConditionalNames(player, contextId);
                        return;
                    } else if (slot == 12) {
                        gui.openCustomModelDataSelector(player, contextId);
                        return;
                    } else if (slot == 13) {
                        gui.openSkinHeadBank(player, contextId);
                        return;
                    } else if (slot == 14) {
                        if (GuiManager.supportsBaby(var.getType())) {
                            var.setBaby(!var.isBaby());
                            varManager.save();
                            plugin.updateVisualizedMobs(contextId);
                        }
                    } else if (slot == 15) {
                        handleNumericScaleClick(player, event, var, false);
                        plugin.updateVisualizedMobs(contextId);
                    } else if (slot == 16) {
                        handleNumericScaleClick(player, event, var, true);
                        plugin.updateVisualizedMobs(contextId);
                    } else if (slot == 17) {
                        gui.openMobSkinSelector(player, contextId);
                        return;
                    } else if (slot == 19) {
                        gui.openParticleSelector(player, contextId, "aura");
                        return;
                    } else if (slot == 20) {
                        gui.openParticleSelector(player, contextId, "spawn");
                        return;
                    } else if (slot == 21) {
                        gui.openParticleSelector(player, contextId, "trail");
                        return;
                    } else if (slot == 22) {
                        mods.setBossBarEnabled(!mods.isBossBarEnabled());
                        varManager.save();
                    } else if (slot == 23) {
                        gui.openBossBarColorSelector(player, contextId);
                        return;
                    } else if (slot == 24) {
                        gui.openBossBarStyleSelector(player, contextId);
                        return;
                    } else if (slot == 35) {
                        gui.openVariantEditor(player, contextId);
                        return;
                    }

                    varManager.save();
                    gui.openVariantAesthetics(player, contextId);
                }
                case "VARIANT_SKIN_SELECTOR" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    StatModifiers mods = var.getModifiers();

                    if (slot == 35) {
                        gui.openVariantAesthetics(player, contextId);
                        return;
                    }

                    if (slot == 0) {
                        mods.setEntitySubtype("none");
                        varManager.save();
                        plugin.updateVisualizedMobs(contextId);
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.wilddifficulty_skinvariante_réinitialisé_par_défaut"));
                        gui.openMobSkinSelector(player, contextId);
                        return;
                    }

                    List<fr.wilddifficulty.util.EntitySubtypeUtil.SubtypeOption> options = fr.wilddifficulty.util.EntitySubtypeUtil.getAvailableSubtypes(var.getType());
                    int index = slot - 1;
                    if (index >= 0 && index < options.size()) {
                        fr.wilddifficulty.util.EntitySubtypeUtil.SubtypeOption opt = options.get(index);
                        mods.setEntitySubtype(opt.id());
                        varManager.save();
                        plugin.updateVisualizedMobs(contextId);
                        player.sendMessage("§8[§6WildDifficulty§8] §aSkin/Variante appliqué : " + opt.displayName());
                        gui.openMobSkinSelector(player, contextId);
                    }
                }
                case "SELECT_SKIN_HEAD" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    StatModifiers mods = var.getModifiers();

                    if (slot == 26) {
                        gui.openVariantAesthetics(player, contextId);
                        return;
                    } else if (slot == 22) {
                        mods.setSkullSkin("none");
                        varManager.save();
                        plugin.updateVisualizedMobs(contextId);
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.skin_de_tête_retiré"));
                        gui.openVariantAesthetics(player, contextId);
                        return;
                    } else if (slot == 20) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_le_pseudo_du_joueur"), input -> {
                            mods.setSkullSkin(input.trim());
                            varManager.save();
                            plugin.updateVisualizedMobs(contextId);
                            player.sendMessage("§aSkin défini sur le pseudo : " + input);
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantAesthetics(player, contextId));
                        });
                        return;
                    } else if (slot == 21) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_le_lien_url_minecraftnet"), input -> {
                            mods.setSkullSkin(input.trim());
                            varManager.save();
                            plugin.updateVisualizedMobs(contextId);
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.skin_défini_sur_la_texture"));
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantAesthetics(player, contextId));
                        });
                        return;
                    } else if (slot == 23) {
                        event.setCancelled(true);
                        ItemStack cursor = event.getCursor();
                        if (cursor != null && cursor.getType() == Material.PLAYER_HEAD) {
                            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) cursor.getItemMeta();
                            if (meta != null) {
                                String skinVal = null;
                                String nameVal = null;
                                com.destroystokyo.paper.profile.PlayerProfile profile = meta.getPlayerProfile();
                                if (profile != null) {
                                    if (profile.getTextures().getSkin() != null) {
                                        skinVal = profile.getTextures().getSkin().toExternalForm();
                                    }
                                    nameVal = profile.getName();
                                }
                                if (skinVal == null && meta.getOwningPlayer() != null) {
                                    nameVal = meta.getOwningPlayer().getName();
                                    skinVal = nameVal;
                                }
                                if (skinVal != null) {
                                    if (nameVal == null || nameVal.isEmpty()) {
                                        nameVal = "Tete_" + (plugin.getVariantManager().getCustomHeads().size() + 1);
                                    }
                                    plugin.getVariantManager().addCustomHead(nameVal, skinVal);
                                    mods.setSkullSkin(skinVal);
                                    varManager.save();
                                    plugin.updateVisualizedMobs(contextId);
                                    player.sendMessage("§a[WD] Tête \"" + nameVal + "\" enregistrée et appliquée avec succès !");
                                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                                } else {
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.wd_impossible_de_lire_la"));
                                }
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSkinHeadBank(player, contextId));
                        }
                        return;
                    } else if (slot >= 15 && slot <= 19) {
                        int idx = slot - 15;
                        List<fr.wilddifficulty.variant.VariantManager.CustomHead> customHeads = plugin.getVariantManager().getCustomHeads();
                        if (idx >= 0 && idx < customHeads.size()) {
                            String value = customHeads.get(idx).getValue();
                            mods.setSkullSkin(value);
                            varManager.save();
                            plugin.updateVisualizedMobs(contextId);
                            player.sendMessage("§aSkin de tête défini : " + customHeads.get(idx).getName());
                            gui.openVariantAesthetics(player, contextId);
                        }
                        return;
                    } else if (slot >= 0 && slot < 15 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                        String pseudo = "MHF_Skeleton";
                        if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasLore()) {
                            List<Component> lore = event.getCurrentItem().getItemMeta().lore();
                            if (lore != null && !lore.isEmpty()) {
                                String loreStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore.get(0));
                                if (loreStr.contains("Pseudo: ")) {
                                    pseudo = loreStr.substring(loreStr.indexOf("Pseudo: ") + 8).trim();
                                }
                            }
                        }
                        mods.setSkullSkin(pseudo);
                        varManager.save();
                        plugin.updateVisualizedMobs(contextId);
                        player.sendMessage("§aSkin de tête défini : " + pseudo);
                        gui.openVariantAesthetics(player, contextId);
                        return;
                    }
                }
                case "VARIANT_DROPS_AUDIO" -> {
                    if (slot == 10) {
                        gui.openVariantEquipmentEditor(player, contextId);
                    } else if (slot == 11) {
                        gui.openVariantDrops(player, contextId);
                    } else if (slot == 13) {
                        gui.openSoundSelector(player, contextId, "ambient");
                    } else if (slot == 14) {
                        gui.openSoundSelector(player, contextId, "aggro");
                    } else if (slot == 15) {
                        gui.openSoundSelector(player, contextId, "death");
                    }
                }
                case "VARIANT_EQUIPMENT_EDIT" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    StatModifiers m = var.getModifiers();

                    if (slot >= 10 && slot <= 15) {
                        if (event.isLeftClick()) {
                            String slotName = switch (slot) {
                                case 10 -> "casque";
                                case 11 -> "plastron";
                                case 12 -> "jambières";
                                case 13 -> "bottes";
                                case 14 -> "main_principale";
                                default -> "main_secondaire";
                            };
                            gui.openEquipmentItemSelector(player, contextId, slotName);
                            return;
                        }

                        double delta = 0.0;
                        if (event.isRightClick()) {
                            delta = event.isShiftClick() ? 0.01 : 0.1;
                        } else if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                            delta = event.isShiftClick() ? -0.01 : -0.1;
                        }

                        if (delta != 0.0) {
                            switch (slot) {
                                case 10 -> m.setHelmetChance(Math.max(0.0, Math.min(1.0, m.getHelmetChance() + delta)));
                                case 11 -> m.setChestplateChance(Math.max(0.0, Math.min(1.0, m.getChestplateChance() + delta)));
                                case 12 -> m.setLeggingsChance(Math.max(0.0, Math.min(1.0, m.getLeggingsChance() + delta)));
                                case 13 -> m.setBootsChance(Math.max(0.0, Math.min(1.0, m.getBootsChance() + delta)));
                                case 14 -> m.setMainHandChance(Math.max(0.0, Math.min(1.0, m.getMainHandChance() + delta)));
                                case 15 -> m.setOffHandChance(Math.max(0.0, Math.min(1.0, m.getOffHandChance() + delta)));
                            }
                            varManager.save();
                            gui.openVariantEquipmentEditor(player, contextId);
                        }
                    }
                }
                case "SELECT_EQ_CASQUE", "SELECT_EQ_PLASTRON", "SELECT_EQ_JAMBIÈRES", "SELECT_EQ_BOTTES", "SELECT_EQ_MAIN_PRINCIPALE", "SELECT_EQ_MAIN_SECONDAIRE" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    StatModifiers m = var.getModifiers();

                    if (slot == 26) {
                        gui.openVariantEquipmentEditor(player, contextId);
                        return;
                    }

                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String rawName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).toUpperCase();
                        String item = rawName.equalsIgnoreCase("NONE") ? "none" : rawName;
                        
                        if (item.startsWith("LEATHER_")) {
                            String slotName = switch (menuType) {
                                case "SELECT_EQ_CASQUE" -> "HELMET";
                                case "SELECT_EQ_PLASTRON" -> "CHESTPLATE";
                                case "SELECT_EQ_JAMBIÈRES" -> "LEGGINGS";
                                case "SELECT_EQ_BOTTES" -> "BOOTS";
                                default -> "CHESTPLATE";
                            };
                            gui.openLeatherColorSelector(player, contextId, slotName, item);
                            return;
                        }

                        switch (menuType) {
                            case "SELECT_EQ_CASQUE" -> m.setHelmetItem(item);
                            case "SELECT_EQ_PLASTRON" -> m.setChestplateItem(item);
                            case "SELECT_EQ_JAMBIÈRES" -> m.setLeggingsItem(item);
                            case "SELECT_EQ_BOTTES" -> m.setBootsItem(item);
                            case "SELECT_EQ_MAIN_PRINCIPALE" -> m.setMainHandItem(item);
                            case "SELECT_EQ_MAIN_SECONDAIRE" -> m.setOffHandItem(item);
                        }
                        varManager.save();
                        gui.openVariantEquipmentEditor(player, contextId);
                    }
                }
                case "SELECT_LEATHER_COLOR" -> {
                    String[] parts = contextId.split(":");
                    String variantId = parts[0];
                    String slotName = parts[1];
                    String armorItem = parts[2];

                    if (slot == 53) {
                        gui.openVariantEquipmentEditor(player, variantId);
                        return;
                    }

                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        if (name.contains("#")) {
                            String hexColor = name.split("#")[1].trim();
                            MobVariant var = varManager.getVariant(variantId);
                            if (var != null && var.getModifiers() != null) {
                                StatModifiers m = var.getModifiers();
                                switch (slotName) {
                                    case "HELMET" -> {
                                        m.setHelmetItem(armorItem);
                                        m.setHelmetColor(hexColor);
                                    }
                                    case "CHESTPLATE" -> {
                                        m.setChestplateItem(armorItem);
                                        m.setChestplateColor(hexColor);
                                    }
                                    case "LEGGINGS" -> {
                                        m.setLeggingsItem(armorItem);
                                        m.setLeggingsColor(hexColor);
                                    }
                                    case "BOOTS" -> {
                                        m.setBootsItem(armorItem);
                                        m.setBootsColor(hexColor);
                                    }
                                }
                                varManager.save();
                                player.sendMessage("§a[WD] Équipement cuir " + armorItem + " défini avec la couleur #" + hexColor);
                                gui.openVariantEquipmentEditor(player, variantId);
                            }
                        }
                    }
                }
                case "SELECT_POTIONS" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String effName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).toUpperCase();
                        List<String> active = var.getModifiers().getPotionEffects();
                        if (active.contains(effName)) {
                            active.remove(effName);
                        } else {
                            active.add(effName);
                        }
                        varManager.save();
                        gui.openPotionSelector(player, contextId);
                    }
                }
                case "SELECT_ONHIT_POTION" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    if (slot == 17) {
                        gui.openVariantBehaviors(player, contextId);
                        return;
                    }
                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        if (name.startsWith("[Actif] ")) {
                            name = name.substring(8);
                        }
                        String effName = name.trim().toLowerCase();
                        var.getModifiers().setOnHitPotionEffect(effName);
                        if (var.getModifiers().getOnHitPotionChance() <= 0.0) {
                            var.getModifiers().setOnHitPotionChance(0.2);
                        }
                        varManager.save();
                        player.sendMessage("§aEffet on-hit défini sur : " + effName);
                        gui.openOnHitPotionSelector(player, contextId);
                    }
                }
                case "CONDITIONAL_NAMES" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    if (slot == 49) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_seuil_de_vie"), input1 -> {
                            try {
                                double percent = Double.parseDouble(input1);
                                if (percent < 0 || percent > 100) {
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.le_pourcentage_doit_être_entre"));
                                    plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantConditionalNames(player, contextId));
                                    return;
                                }
                                double threshold = percent / 100.0;
                                ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_le_nom_daffichage_coloré"), input2 -> {
                                    var.getConditionalNames().add(new MobVariant.ConditionalName(threshold, input2));
                                    varManager.save();
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nom_conditionnel_ajouté"));
                                    plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantConditionalNames(player, contextId));
                                });
                            } catch (Exception e) {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.pourcentage_invalide"));
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantConditionalNames(player, contextId));
                            }
                        });
                        return;
                    }
                    // Clic sur livre pour supprimer
                    if (slot < var.getConditionalNames().size()) {
                        var.getConditionalNames().remove(slot);
                        varManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nom_conditionnel_supprimé"));
                        gui.openVariantConditionalNames(player, contextId);
                    }
                }
                case "BIOME_SPAWN_CONFIG" -> {
                    if (slot == 53) {
                        player.closeInventory();
                        return;
                    }
                    List<MobVariant> list = new ArrayList<>(varManager.getAllVariants());
                    if (slot < list.size()) {
                        MobVariant var = list.get(slot);
                        String upperBiome = contextId.toUpperCase();
                        if (var.getAllowedBiomes().contains(upperBiome)) {
                            var.getAllowedBiomes().remove(upperBiome);
                            player.sendMessage("§cVariante " + var.getId() + " n'est plus autorisée dans le biome " + upperBiome);
                        } else {
                            var.getAllowedBiomes().add(upperBiome);
                            player.sendMessage("§aVariante " + var.getId() + " est maintenant autorisée dans le biome " + upperBiome);
                        }
                        varManager.save();
                        gui.openBiomeSpawnConfig(player, contextId);
                    }
                }
                case "SPAWN_CONDITIONS" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    if (slot == 10) {
                        String current = var.getSpawnWeather();
                        String next = switch (current.toUpperCase()) {
                            case "ANY" -> "RAINY";
                            case "RAINY" -> "CLEAR";
                            default -> "ANY";
                        };
                        var.setSpawnWeather(next);
                        varManager.save();
                        gui.openVariantSpawnConditions(player, contextId);
                    } else if (slot == 11) {
                        String current = var.getCaveSpawn();
                        String next = switch (current.toUpperCase()) {
                            case "ANY" -> "ONLY_CAVES";
                            case "ONLY_CAVES" -> "NO_CAVES";
                            default -> "ANY";
                        };
                        var.setCaveSpawn(next);
                        varManager.save();
                        gui.openVariantSpawnConditions(player, contextId);
                    } else if (slot == 13) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_lid_numérique_du_custom"), input -> {
                            try {
                                int val = Integer.parseInt(input);
                                var.setCustomModelData(Math.max(0, val));
                                varManager.save();
                                plugin.updateVisualizedMobs(contextId);
                                player.sendMessage("§aCustom Model Data défini à " + val);
                            } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantSpawnConditions(player, contextId));
                        });
                        return;
                    } else if (slot == 15) {
                        String current = var.getAggroMode();
                        String next = switch (current.toUpperCase()) {
                            case "PASSIVE" -> "AGGRESSIVE";
                            case "AGGRESSIVE" -> "NEUTRAL_HIT";
                            case "NEUTRAL_HIT" -> "NEUTRAL_SQUAD_HIT";
                            default -> "PASSIVE";
                        };
                        var.setAggroMode(next);
                        varManager.save();
                        gui.openVariantSpawnConditions(player, contextId);
                    } else if (slot == 12) {
                        String current = var.getSpawnTime();
                        String next = switch (current.toUpperCase()) {
                            case "ANY" -> "DAY";
                            case "DAY" -> "NIGHT";
                            default -> "ANY";
                        };
                        var.setSpawnTime(next);
                        varManager.save();
                        gui.openVariantSpawnConditions(player, contextId);
                    } else if (slot == 14) {
                        gui.openVariantBiomesEditor(player, contextId, 0, "ALL");
                    } else if (slot >= 18 && slot < 45) {
                        int index = slot - 18;
                        if (index < var.getAllowedBiomes().size()) {
                            String removed = var.getAllowedBiomes().remove(index);
                            varManager.save();
                            player.sendMessage("§cBiome " + removed + " retiré.");
                            gui.openVariantSpawnConditions(player, contextId);
                        }
                    }
                }
                case "VARIANT_BIOMES_EDIT" -> {
                    String baseVarId = contextId;
                    int page = 0;
                    String filter = "ALL";
                    if (contextId.contains(":")) {
                        String[] parts = contextId.split(":");
                        baseVarId = parts[0];
                        if (parts.length > 2) {
                            try { page = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                            filter = parts[2];
                        } else if (parts.length > 1) {
                            try { page = Integer.parseInt(parts[1]); } catch (Exception e) { filter = parts[1]; }
                        }
                    }

                    MobVariant var = varManager.getVariant(baseVarId);
                    if (var == null) return;

                    if (slot == 45) {
                        gui.openVariantBiomesEditor(player, baseVarId, 0, "IRIS");
                        return;
                    }
                    if (slot == 46) {
                        gui.openVariantBiomesEditor(player, baseVarId, 0, "MINECRAFT");
                        return;
                    }
                    if (slot == 47) {
                        gui.openVariantBiomesEditor(player, baseVarId, 0, "ALL");
                        return;
                    }
                    if (slot == 48 && page > 0) {
                        gui.openVariantBiomesEditor(player, baseVarId, page - 1, filter);
                        return;
                    }
                    if (slot == 49) {
                        String curB = gui.getBiomeKeyString(player.getLocation());
                        if (var.getAllowedBiomes().contains(curB)) {
                            var.getAllowedBiomes().remove(curB);
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.biome_actuel_retiré_des_biomes"));
                        } else if (var.getAllowedBiomes().contains(curB.toUpperCase())) {
                            var.getAllowedBiomes().remove(curB.toUpperCase());
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.biome_actuel_retiré_des_biomes"));
                        } else {
                            var.getAllowedBiomes().add(curB);
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.biome_actuel_ajouté_aux_biomes"));
                        }
                        varManager.save();
                        gui.openVariantBiomesEditor(player, baseVarId, page, filter);
                        return;
                    }
                    if (slot == 50) {
                        gui.openVariantBiomesEditor(player, baseVarId, page + 1, filter);
                        return;
                    }
                    if (slot == 53) {
                        gui.openVariantSpawnConditions(player, baseVarId);
                        return;
                    }

                    if (slot < 45) {
                        ItemStack item = event.getCurrentItem();
                        String displayName = getCleanName(item);
                        if (!displayName.isEmpty()) {
                            String bName = displayName.trim();
                            if (var.getAllowedBiomes().contains(bName)) {
                                var.getAllowedBiomes().remove(bName);
                                player.sendMessage("§cBiome " + bName + " retiré.");
                            } else if (var.getAllowedBiomes().contains(bName.toUpperCase())) {
                                var.getAllowedBiomes().remove(bName.toUpperCase());
                                player.sendMessage("§cBiome " + bName + " retiré.");
                            } else {
                                var.getAllowedBiomes().add(bName);
                                player.sendMessage("§aBiome " + bName + " ajouté.");
                            }
                            varManager.save();
                            gui.openVariantBiomesEditor(player, baseVarId, page, filter);
                        }
                    }
                }
                case "CUSTOM_DROPS" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    if (slot == 49) {
                        gui.openDropMaterialSelector(player, contextId);
                        return;
                    }
                    if (slot < var.getCustomDrops().size()) {
                        var.getCustomDrops().remove(slot);
                        varManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.drop_supprimé"));
                        gui.openVariantDrops(player, contextId);
                    }
                }
                case "SELECT_DROP_MAT" -> {
                    MobVariant var = varManager.getVariant(contextId);
                    if (var == null) return;
                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String matName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_la_chance_de_loot"), input1 -> {
                            try {
                                double chance = Double.parseDouble(input1);
                                ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_la_quantité_minimale_et"), input2 -> {
                                    try {
                                        String[] parts = input2.split(" ");
                                        int min = Integer.parseInt(parts[0]);
                                        int max = parts.length > 1 ? Integer.parseInt(parts[1]) : min;
                                        
                                        var.getCustomDrops().add(new MobVariant.CustomDrop(matName, chance, min, max, 0, "none"));
                                        varManager.save();
                                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.loot_ajouté"));
                                    } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.quantité_invalide")); }
                                    plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantDrops(player, contextId));
                                });
                            } catch (Exception e) {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.chance_invalide"));
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantDrops(player, contextId));
                            }
                        });
                    }
                }
                case "SQUAD_LIST" -> {
                    if (slot == 53) {
                        gui.openMainMenu(player);
                        return;
                    }
                    if (slot == 49) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_lid_de_la_nouvelle"), input -> {
                            String id = input.toLowerCase().replace(" ", "_");
                            if (varManager.getSquad(id) == null) {
                                MobSquad sq = new MobSquad(id, new ArrayList<>(), 0.1, new ArrayList<>(), new ArrayList<>(), new java.util.HashMap<>());
                                varManager.addSquad(sq);
                                varManager.save();
                                player.sendMessage("§aEscouade " + id + " créée !");
                            } else {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.cette_escouade_existe_déjà"));
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openSquadEditor(player, id));
                        });
                        return;
                    }
                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        if (varManager.getSquad(name) != null) {
                            gui.openSquadEditor(player, name);
                        }
                    }
                }
                case "SQUAD_EDIT" -> {
                    MobSquad sq = varManager.getSquad(contextId);
                    if (sq == null) return;
                    if (slot == 22) {
                        varManager.removeSquad(contextId);
                        varManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.escouade_supprimée"));
                        gui.openSquadList(player);
                        return;
                    }
                    if (slot == 10) {
                        // Click +/- squad spawn chance
                        handleSquadChanceClick(player, event, sq);
                    } else if (slot == 13) {
                        gui.openSquadMembersEditor(player, contextId);
                    } else if (slot == 14) {
                        gui.openSquadBonusesEditor(player, contextId);
                    } else if (slot == 15) {
                        gui.openSquadTriggersEditor(player, contextId);
                    } else if (slot == 16) {
                        player.closeInventory();
                        player.performCommand("wd spawnsquad " + contextId);
                    }
                }
                case "SQUAD_BONUSES_EDIT" -> {
                    handleModifierClick(player, event, "squad", contextId, gui, varManager, zoneManager, mainCfg);
                }
                case "SQUAD_MEMBERS_EDIT" -> {
                    MobSquad sq = varManager.getSquad(contextId);
                    if (sq == null) return;
                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String vId = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        MobSquad.SquadMemberRange range = sq.getMembers().get(vId);

                        if (range != null) {
                            if (event.getClick() == ClickType.MIDDLE) {
                                sq.getMembers().remove(vId);
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.membre_retiré"));
                            } else {
                                int min = range.getMin();
                                int max = range.getMax();
                                if (event.isShiftClick()) {
                                    if (event.isLeftClick()) max++;
                                    else if (event.isRightClick()) max = Math.max(min, max - 1);
                                } else {
                                    if (event.isLeftClick()) min++;
                                    else if (event.isRightClick()) min = Math.max(1, min - 1);
                                }
                                sq.getMembers().put(vId, new MobSquad.SquadMemberRange(min, Math.max(min, max)));
                            }
                        } else {
                            // Ajouter comme membre
                            sq.getMembers().put(vId, new MobSquad.SquadMemberRange(1, 1));
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.variante_ajoutée_comme_membre"));
                        }
                        varManager.save();
                        gui.openSquadMembersEditor(player, contextId);
                    }
                }
                case "SQUAD_TRIGGERS_EDIT" -> {
                    MobSquad sq = varManager.getSquad(contextId);
                    if (sq == null) return;
                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String typeName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        if (sq.getTriggerTypes().contains(typeName)) {
                            sq.getTriggerTypes().remove(typeName);
                        } else {
                            sq.getTriggerTypes().add(typeName);
                        }
                        varManager.save();
                        gui.openSquadTriggersEditor(player, contextId);
                    }
                }
                case "ZONE_LIST" -> {
                    if (slot == 49) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_lid_de_la_nouvelle_1"), input -> {
                            String id = input.toLowerCase().replace(" ", "_");
                            if (zoneManager.getZone(id) == null) {
                                DifficultyZone zone = new DifficultyZone(id, DifficultyZone.ZoneType.CUBOID, player.getWorld().getName());
                                zoneManager.addZone(zone);
                                zoneManager.save();
                                player.sendMessage("§aZone " + id + " créée !");
                            } else {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.cette_zone_déjà_existante"));
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openZoneEditor(player, id));
                        });
                        return;
                    }
                    if (slot == 48) {
                        player.closeInventory();
                        player.performCommand("wd zonetool");
                        return;
                    }
                    if (slot == 51) {
                        mainCfg.setZoneBorderParticles(!mainCfg.isZoneBorderParticles());
                        mainCfg.save();
                        gui.openZoneList(player);
                        return;
                    }
                    if (slot == 53) {
                        gui.openMainMenu(player);
                        return;
                    }
                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        if (zoneManager.getZone(name) != null) {
                            gui.openZoneEditor(player, name);
                        }
                    }
                }
                case "ZONE_EDIT" -> {
                    DifficultyZone zone = zoneManager.getZone(contextId);
                    if (zone == null) return;
                    if (slot == 31) {
                        zoneManager.removeZone(contextId);
                        zoneManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.zone_supprimée"));
                        gui.openZoneList(player);
                        return;
                    }
                    if (slot == 10) {
                        zone.setSafeZone(!zone.isSafeZone());
                        zoneManager.save();
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 11) {
                        zone.setOverrideBiomeRules(!zone.isOverrideBiomeRules());
                        zoneManager.save();
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 12) {
                        handleZonePriorityClick(player, event, zone);
                    } else if (slot == 14) {
                        Location loc = player.getLocation();
                        zone.setMinMax(loc.getX(), loc.getY(), loc.getZ(), zone.getMaxX(), zone.getMaxY(), zone.getMaxZ());
                        zoneManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.position_1_définie"));
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 15) {
                        Location loc = player.getLocation();
                        zone.setMinMax(zone.getMinX(), zone.getMinY(), zone.getMinZ(), loc.getX(), loc.getY(), loc.getZ());
                        zoneManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.position_2_définie"));
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 16) {
                        Location tpLoc;
                        if (zone.hasCustomTeleport()) {
                            tpLoc = new Location(player.getWorld(), zone.getTeleportX(), zone.getTeleportY(), zone.getTeleportZ());
                        } else {
                            double cx = zone.getCenterX();
                            double cz = zone.getCenterZ();
                            double cy = zone.getCenterY();
                            // Correct cy so we don't end up under ground
                            org.bukkit.block.Block highest = player.getWorld().getHighestBlockAt((int)Math.floor(cx), (int)Math.floor(cz));
                            if (highest.getY() > cy) {
                                cy = highest.getY() + 1.0;
                            } else {
                                cy += 1.0;
                            }
                            tpLoc = new Location(player.getWorld(), cx, cy, cz);
                        }
                        player.teleport(tpLoc);
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.téléporté_à_la_zone"));
                        player.closeInventory();
                    } else if (slot == 17) {
                        zone.setMobsCanCross(!zone.isMobsCanCross());
                        zoneManager.save();
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 18) {
                        // Ouvrir le GUI de scaling extérieur per-stat
                        gui.openZoneScalingEdit(player, zone);
                    } else if (slot == 19) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_la_hauteur_de_particule"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    zone.setParticleHeightOffset(val);
                                    zoneManager.save();
                                    player.sendMessage("§aHauteur de particule définie à " + val);
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openZoneEditor(player, contextId));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? 0.05 : -0.05;
                        zone.setParticleHeightOffset(zone.getParticleHeightOffset() + delta);
                        zoneManager.save();
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 13) {
                        DifficultyZone.ZoneType nextType = switch (zone.getType()) {
                            case CUBOID -> DifficultyZone.ZoneType.RADIUS;
                            case RADIUS -> DifficultyZone.ZoneType.POLYGON;
                            case POLYGON -> DifficultyZone.ZoneType.CUBOID;
                        };
                        zone.setType(nextType);
                        zoneManager.save();
                        player.sendMessage("§a[WD] Type de forme défini sur " + nextType.name());
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 21) {
                        zone.setParticlesEnabled(!zone.isParticlesEnabled());
                        zoneManager.save();
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 24) {
                        gui.openZoneParticleSelector(player, contextId, "inside");
                    } else if (slot == 25) {
                        gui.openZoneParticleSelector(player, contextId, "outside");
                    } else if (slot == 22) {
                        Location loc = player.getLocation();
                        zone.setTeleportX(loc.getX());
                        zone.setTeleportY(loc.getY());
                        zone.setTeleportZ(loc.getZ());
                        zone.setHasCustomTeleport(true);
                        zoneManager.save();
                        player.sendMessage(plugin.getLangManager().getRaw("gui.msg.point_de_téléportation_personnalisé_défini"));
                        gui.openZoneEditor(player, contextId);
                    } else if (slot == 23) {
                        plugin.getEditingZoneId().put(player.getUniqueId(), zone.getId());
                        player.closeInventory();
                        player.performCommand("wd zone tool");
                    } else if (slot == 20) {
                        gui.openZoneModifiers(player, contextId);
                    } else if (slot == 26) {
                        gui.openZoneMembersConfig(player, contextId);
                    } else if (slot == 27) {
                        gui.openZoneBeaconEffectsConfig(player, contextId);
                    } else if (slot == 28) {
                        gui.openZoneDangerNestConfig(player, contextId);
                    } else if (slot == 29) {
                        gui.openZoneSubsectionsConfig(player, contextId);
                    } else if (slot == 35) {
                        gui.openZoneList(player);
                    }
                }
                case "ZONE_MEMBERS" -> {
                    DifficultyZone zone = zoneManager.getZone(contextId);
                    if (zone == null) return;
                    if (slot == 53) {
                        gui.openZoneEditor(player, contextId);
                        return;
                    }
                    if (slot == 0) {
                        ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_le_pseudo_du_joueur"), input -> {
                            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(input.trim());
                            if (target != null && target.getUniqueId() != null) {
                                zone.addMember(new fr.wilddifficulty.zone.ZoneMember(target.getUniqueId(), input.trim(), 1));
                                zoneManager.save();
                                player.sendMessage("§aMembre " + input.trim() + " ajouté avec succès (Niv. 1) !");
                            } else {
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.joueur_introuvable"));
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () -> gui.openZoneMembersConfig(player, contextId));
                        });
                        return;
                    }
                    if (slot >= 9 && slot < 45) {
                        ItemStack item = event.getCurrentItem();
                        if (item != null && item.getType() == Material.PLAYER_HEAD) {
                            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
                            if (meta != null && meta.getOwningPlayer() != null) {
                                java.util.UUID uuid = meta.getOwningPlayer().getUniqueId();
                                fr.wilddifficulty.zone.ZoneMember member = zone.getMember(uuid);
                                if (member != null) {
                                    if (event.isRightClick()) {
                                        zone.removeMember(uuid);
                                        zoneManager.save();
                                        player.sendMessage("§cMembre " + member.getLastKnownName() + " retiré.");
                                    } else {
                                        int nextLvl = member.getPermissionLevel() % 3 + 1;
                                        member.setPermissionLevel(nextLvl);
                                        zoneManager.save();
                                        player.sendMessage("§aNiveau de permission pour " + member.getLastKnownName() + " passé à : " + member.getRoleName());
                                    }
                                    gui.openZoneMembersConfig(player, contextId);
                                }
                            }
                        }
                    }
                }
                case "ZONE_BEACON" -> {
                    DifficultyZone zone = zoneManager.getZone(contextId);
                    if (zone == null) return;
                    if (slot == 26) {
                        gui.openZoneEditor(player, contextId);
                        return;
                    }
                    String[] effects = {"SPEED", "HASTE", "RESISTANCE", "JUMP_BOOST", "INCREASE_DAMAGE", "REGENERATION", "FIRE_RESISTANCE", "NIGHT_VISION"};
                    int idx = slot - 10;
                    if (idx >= 0 && idx < effects.length) {
                        String eff = effects[idx];
                        if (event.isRightClick()) {
                            zone.removeBeaconEffect(eff);
                            player.sendMessage("§cEffet de beacon " + eff + " désactivé.");
                        } else {
                            int currentAmp = zone.getBeaconEffects().getOrDefault(eff, -1);
                            int nextAmp = (currentAmp + 2) % 3 - 1;
                            if (nextAmp < 0) {
                                zone.removeBeaconEffect(eff);
                                player.sendMessage("§cEffet de beacon " + eff + " désactivé.");
                            } else {
                                zone.setBeaconEffect(eff, nextAmp);
                                player.sendMessage("§aEffet de beacon " + eff + " (Niv. " + (nextAmp + 1) + ") activé.");
                            }
                        }
                        zoneManager.save();
                        gui.openZoneBeaconEffectsConfig(player, contextId);
                    }
                }
                case "ZONE_DANGER_NEST" -> {
                    DifficultyZone zone = zoneManager.getZone(contextId);
                    if (zone == null) return;
                    if (slot == 26) {
                        gui.openZoneEditor(player, contextId);
                        return;
                    }
                    if (slot == 10) {
                        zone.setDangerNest(!zone.isDangerNest());
                        zoneManager.save();
                        gui.openZoneDangerNestConfig(player, contextId);
                    } else if (slot == 12) {
                        double delta = event.isLeftClick() ? 0.5 : -0.5;
                        zone.setNestSpawnBoost(Math.max(0.5, zone.getNestSpawnBoost() + delta));
                        zoneManager.save();
                        gui.openZoneDangerNestConfig(player, contextId);
                    } else if (slot == 14) {
                        gui.openZoneModifiers(player, contextId);
                    }
                }
                case "ZONE_SUBSECTIONS" -> {
                    DifficultyZone zone = zoneManager.getZone(contextId);
                    if (zone == null) return;
                    if (slot == 35) {
                        gui.openZoneEditor(player, contextId);
                        return;
                    }
                    if (slot == 0) {
                        DifficultyZone pending = zoneManager.getPendingZone(player.getUniqueId());
                        if (pending != null && pending.isCuboidComplete()) {
                            String secId = "sec_" + (zone.getSubSections().size() + 1);
                            fr.wilddifficulty.zone.ZoneSection sec = new fr.wilddifficulty.zone.ZoneSection(secId, pending.getType());
                            sec.setMinX(pending.getMinX()); sec.setMinY(pending.getMinY()); sec.setMinZ(pending.getMinZ());
                            sec.setMaxX(pending.getMaxX()); sec.setMaxY(pending.getMaxY()); sec.setMaxZ(pending.getMaxZ());
                            zone.addSubSection(sec);
                            zoneManager.clearPendingZone(player.getUniqueId());
                            zoneManager.save();
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.soussection_ajoutée_et_fusionnée_avec"));
                        } else {
                            player.sendMessage(plugin.getLangManager().getRaw("gui.msg.aucune_sélection_valide_avec_la"));
                        }
                        gui.openZoneSubsectionsConfig(player, contextId);
                        return;
                    }
                    if (slot >= 9 && slot < 27) {
                        int idx = slot - 9;
                        if (idx < zone.getSubSections().size()) {
                            fr.wilddifficulty.zone.ZoneSection sec = zone.getSubSections().get(idx);
                            zone.removeSubSection(sec.getId());
                            zoneManager.save();
                            player.sendMessage("§cSous-section " + sec.getId() + " supprimée.");
                            gui.openZoneSubsectionsConfig(player, contextId);
                        }
                    }
                }
                case "PLAYER_SETTINGS" -> {
                    fr.wilddifficulty.player.PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
                    if (slot == 26) {
                        player.closeInventory();
                        return;
                    }
                    if (slot == 11) {
                        String next = switch (pd.getDifficultyLevel().toUpperCase()) {
                            case "FACILE" -> "NORMAL";
                            case "NORMAL" -> "DIFFICILE";
                            case "DIFFICILE" -> "EXTREME";
                            default -> "FACILE";
                        };
                        pd.setDifficultyLevel(next);
                        plugin.getPlayerSettingsManager().save();
                        gui.openPlayerSettingsGui(player);
                    } else if (slot == 13) {
                        pd.setThirstEnabled(!pd.isThirstEnabled());
                        plugin.getPlayerSettingsManager().save();
                        gui.openPlayerSettingsGui(player);
                    } else if (slot == 15) {
                        gui.openPersonalHardcoreSettingsGui(player);
                    }
                }
                case "PLAYER_HARDCORE_CONFIG" -> {
                    fr.wilddifficulty.player.PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());
                    if (slot == 26) {
                        gui.openPlayerSettingsGui(player);
                        return;
                    }
                    if (slot == 10) pd.setHardcoreEnabled(!pd.isHardcoreEnabled());
                    else if (slot == 11) pd.setHardcoreNoRegen(!pd.isHardcoreNoRegen());
                    else if (slot == 12) pd.setHardcoreAllowGoldenApples(!pd.isHardcoreAllowGoldenApples());
                    else if (slot == 13) pd.setHardcoreAllowPotions(!pd.isHardcoreAllowPotions());
                    else if (slot == 14) pd.setHardcoreAllowSafezoneRegen(!pd.isHardcoreAllowSafezoneRegen());
                    else if (slot == 15) pd.setHardcoreInstantDeathDespawn(!pd.isHardcoreInstantDeathDespawn());
                    plugin.getPlayerSettingsManager().save();
                    gui.openPersonalHardcoreSettingsGui(player);
                }
                case "ADMIN_THIRST_HARDCORE" -> {
                    if (slot == 26) {
                        gui.openGeneralConfig(player);
                        return;
                    }
                    if (slot == 10) {
                        mainCfg.setThirstEnabled(!mainCfg.isThirstEnabled());
                        mainCfg.save();
                        gui.openThirstHardcoreAdminGui(player);
                    } else if (slot == 11) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_le_multiplicateur_de_dégradation"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setThirstDrainMultiplier(val);
                                    mainCfg.save();
                                    player.sendMessage("§aMultiplicateur de dégradation de soif mis à jour : " + String.format("%.2fx", mainCfg.getThirstDrainMultiplier()));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openThirstHardcoreAdminGui(player));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? 0.25 : -0.25;
                        mainCfg.setThirstDrainMultiplier(mainCfg.getThirstDrainMultiplier() + delta);
                        mainCfg.save();
                        gui.openThirstHardcoreAdminGui(player);
                    } else if (slot == 12) {
                        gui.openHardcoreConfigGui(player);
                    } else if (slot == 13) {
                        gui.openThirstSourcesAdminGui(player);
                    } else if (slot == 14) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.durée_de_despawn_du_stuff"), input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    mainCfg.setDeathItemDespawnSeconds(val);
                                    mainCfg.save();
                                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.temps_de_despawn_mis_à"));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openThirstHardcoreAdminGui(player));
                            });
                            return;
                        }
                        int delta = event.isLeftClick() ? 60 : -60;
                        mainCfg.setDeathItemDespawnSeconds(mainCfg.getDeathItemDespawnSeconds() + delta);
                        mainCfg.save();
                        gui.openThirstHardcoreAdminGui(player);
                    } else if (slot == 15) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.multiplicateur_de_déshydratation_par_chaleur"), input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    mainCfg.setThirstHeatDrainMultiplier(val);
                                    mainCfg.save();
                                    player.sendMessage("§aMultiplicateur de chaleur mis à jour : " + String.format("%.2fx", mainCfg.getThirstHeatDrainMultiplier()));
                                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openThirstHardcoreAdminGui(player));
                            });
                            return;
                        }
                        if (event.isShiftClick()) {
                            double delta = event.isLeftClick() ? 0.25 : -0.25;
                            mainCfg.setThirstHeatDrainMultiplier(mainCfg.getThirstHeatDrainMultiplier() + delta);
                        } else {
                            mainCfg.setThirstHeatDrainEnabled(!mainCfg.isThirstHeatDrainEnabled());
                        }
                        mainCfg.save();
                        gui.openThirstHardcoreAdminGui(player);
                    }
                }
                case "ADMIN_THIRST_SOURCES" -> {
                    if (slot == 35) {
                        gui.openThirstHardcoreAdminGui(player);
                        return;
                    }
                    if (slot == 10) mainCfg.setThirstRestoreWaterBucket(mainCfg.getThirstRestoreWaterBucket() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 11) mainCfg.setThirstRestoreWaterBottle(mainCfg.getThirstRestoreWaterBottle() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 12) mainCfg.setThirstRestorePotion(mainCfg.getThirstRestorePotion() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 13) mainCfg.setThirstRestoreCauldron(mainCfg.getThirstRestoreCauldron() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 14) mainCfg.setThirstRestoreWaterBlock(mainCfg.getThirstRestoreWaterBlock() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 15) mainCfg.setThirstRestoreMilkBucket(mainCfg.getThirstRestoreMilkBucket() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 16) mainCfg.setThirstRestoreHoneyBottle(mainCfg.getThirstRestoreHoneyBottle() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 19) mainCfg.setThirstRestoreMelonSlice(mainCfg.getThirstRestoreMelonSlice() + (event.isLeftClick() ? 1 : -1));
                    else if (slot == 20) mainCfg.setThirstRestoreApple(mainCfg.getThirstRestoreApple() + (event.isLeftClick() ? 1 : -1));
                    mainCfg.save();
                    gui.openThirstSourcesAdminGui(player);
                }
                case "PLAYER_EDITABLE_ZONES" -> {
                    if (slot == 53) {
                        player.closeInventory();
                        return;
                    }
                    String displayName = getCleanName(event.getCurrentItem());
                    if (displayName.startsWith("Zone: ")) {
                        String zoneId = displayName.substring("Zone: ".length()).trim();
                        gui.openZoneEditor(player, zoneId);
                    }
                }
                case "HARDCORE_CONFIG" -> {
                    if (slot == 26) {
                        gui.openThirstHardcoreAdminGui(player);
                        return;
                    }
                    if (slot == 10) {
                        mainCfg.setHardcoreEnabled(!mainCfg.isHardcoreEnabled());
                        mainCfg.save();
                        gui.openHardcoreConfigGui(player);
                    } else if (slot == 12) {
                        mainCfg.setHardcoreNoRegen(!mainCfg.isHardcoreNoRegen());
                        mainCfg.save();
                        gui.openHardcoreConfigGui(player);
                    } else if (slot == 14) {
                        double delta = event.isLeftClick() ? 0.25 : -0.25;
                        mainCfg.setHardcoreDamageTakenMult(mainCfg.getHardcoreDamageTakenMult() + delta);
                        mainCfg.save();
                        gui.openHardcoreConfigGui(player);
                    } else if (slot == 16) {
                        double delta = event.isLeftClick() ? 0.25 : -0.25;
                        mainCfg.setHardcoreHungerDrainMult(mainCfg.getHardcoreHungerDrainMult() + delta);
                        mainCfg.save();
                        gui.openHardcoreConfigGui(player);
                    }
                }
                case "ZONE_PARTICLE_INSIDE", "ZONE_PARTICLE_OUTSIDE" -> {
                    DifficultyZone zone = zoneManager.getZone(contextId);
                    if (zone == null) return;

                    if (slot == 17) {
                        gui.openZoneEditor(player, contextId);
                        return;
                    }

                    if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                        String rawName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        String pName = rawName.replace("§b", "").replace("&b", "").trim();

                        if (menuType.equals("ZONE_PARTICLE_INSIDE")) {
                            zone.setParticleInside(pName);
                        } else {
                            zone.setParticleOutside(pName);
                        }
                        zoneManager.save();
                        player.sendMessage("§a[WD] Particule de zone mise à jour : " + pName);
                        gui.openZoneEditor(player, contextId);
                    }
                }
                case "ZONE_MODS" -> {
                    if (slot == 15) {
                        gui.openEquipmentTierSelector(player, contextId, "zone");
                        return;
                    }
                    handleModifierClick(player, event, "zone", contextId, gui, varManager, zoneManager, mainCfg);
                }
                case "ZONE_SCALING_EDIT" -> {
                    DifficultyZone zone = zoneManager.getZone(contextId);
                    if (zone == null) return;

                    // Slot 53 = Retour
                    if (slot == 53) {
                        gui.openZoneEditor(player, contextId);
                        return;
                    }

                    // Slot 4 = Cap global
                    if (slot == 4) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.cap_max_multiplicateur_ex_30"), input -> {
                                try {
                                    double v = Double.parseDouble(input);
                                    zone.setExtMaxMult(Math.max(1.0, v));
                                    zoneManager.save();
                                } catch (Exception ignored) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openZoneScalingEdit(player, zone));
                            });
                            return;
                        }
                        double delta = event.isLeftClick() ? 0.5 : -0.5;
                        zone.setExtMaxMult(Math.max(1.0, zone.getExtMaxMult() + delta));
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }

                    // Slot 10 = PV bonus
                    if (slot == 10) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.pv_pas_en_blocs_ex"), input -> {
                                try {
                                    String[] parts = input.trim().split("\\s+");
                                    zone.setExtStepHp(Double.parseDouble(parts[0]));
                                    if (parts.length > 1) zone.setExtMultHp(Double.parseDouble(parts[1]));
                                    zoneManager.save();
                                } catch (Exception ignored) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.format_invalide_ex_50_020")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openZoneScalingEdit(player, zone));
                            });
                            return;
                        }
                        if (event.isShiftClick()) {
                            double delta = event.isLeftClick() ? 0.05 : -0.05;
                            zone.setExtMultHp(Math.max(0, zone.getExtMultHp() + delta));
                        } else {
                            double delta = event.isLeftClick() ? 50.0 : -50.0;
                            zone.setExtStepHp(Math.max(0, zone.getExtStepHp() + delta));
                        }
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }

                    // Slot 11 = Reset PV
                    if (slot == 11) {
                        zone.setExtStepHp(0); zone.setExtMultHp(0);
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }

                    // Slot 19 = Dégâts bonus
                    if (slot == 19) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.dégâts_pas_en_blocs_ex"), input -> {
                                try {
                                    String[] parts = input.trim().split("\\s+");
                                    zone.setExtStepDmg(Double.parseDouble(parts[0]));
                                    if (parts.length > 1) zone.setExtMultDmg(Double.parseDouble(parts[1]));
                                    zoneManager.save();
                                } catch (Exception ignored) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.format_invalide_ex_75_015")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openZoneScalingEdit(player, zone));
                            });
                            return;
                        }
                        if (event.isShiftClick()) {
                            double delta = event.isLeftClick() ? 0.05 : -0.05;
                            zone.setExtMultDmg(Math.max(0, zone.getExtMultDmg() + delta));
                        } else {
                            double delta = event.isLeftClick() ? 50.0 : -50.0;
                            zone.setExtStepDmg(Math.max(0, zone.getExtStepDmg() + delta));
                        }
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }

                    // Slot 20 = Reset Dégâts
                    if (slot == 20) {
                        zone.setExtStepDmg(0); zone.setExtMultDmg(0);
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }

                    // Slot 28 = Vitesse bonus
                    if (slot == 28) {
                        if (event.getClick() == ClickType.MIDDLE) {
                            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.vitesse_pas_en_blocs_ex"), input -> {
                                try {
                                    String[] parts = input.trim().split("\\s+");
                                    zone.setExtStepSpd(Double.parseDouble(parts[0]));
                                    if (parts.length > 1) zone.setExtMultSpd(Double.parseDouble(parts[1]));
                                    zoneManager.save();
                                } catch (Exception ignored) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.format_invalide_ex_100_010")); }
                                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openZoneScalingEdit(player, zone));
                            });
                            return;
                        }
                        if (event.isShiftClick()) {
                            double delta = event.isLeftClick() ? 0.05 : -0.05;
                            zone.setExtMultSpd(Math.max(0, zone.getExtMultSpd() + delta));
                        } else {
                            double delta = event.isLeftClick() ? 50.0 : -50.0;
                            zone.setExtStepSpd(Math.max(0, zone.getExtStepSpd() + delta));
                        }
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }

                    // Slot 29 = Reset Vitesse
                    if (slot == 29) {
                        zone.setExtStepSpd(0); zone.setExtMultSpd(0);
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }

                    // Slot 40 = Global fallback
                    if (slot == 40) {
                        if (event.isShiftClick()) {
                            double delta = event.isLeftClick() ? 0.05 : -0.05;
                            zone.setExtMultPerStep(Math.max(0, zone.getExtMultPerStep() + delta));
                        } else {
                            double delta = event.isLeftClick() ? 50.0 : -50.0;
                            zone.setExtStep(Math.max(0, zone.getExtStep() + delta));
                        }
                        zoneManager.save();
                        gui.openZoneScalingEdit(player, zone);
                        return;
                    }
                }
                default -> {

                    // Submenus choice dispatches
                    if (menuType.startsWith("SELECT_PARTICLE_")) {
                        String field = menuType.substring(16).toLowerCase();
                        MobVariant var = varManager.getVariant(contextId);
                        if (var != null && var.getModifiers() != null) {
                            String part = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                            if (field.equals("aura")) var.getModifiers().setParticleAuraType(part);
                            else if (field.equals("spawn")) var.getModifiers().setParticleSpawnType(part);
                            else if (field.equals("trail")) var.getModifiers().setParticleTrailType(part);
                            varManager.save();
                            player.sendMessage("§aParticule définie sur " + part);
                            gui.openVariantAesthetics(player, contextId);
                        }
                    } else if (menuType.equals("SELECT_BB_COLOR")) {
                        MobVariant var = varManager.getVariant(contextId);
                        if (var != null && var.getModifiers() != null) {
                            String color = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                            var.getModifiers().setBossBarColor(color);
                            varManager.save();
                            player.sendMessage("§aCouleur BossBar définie sur " + color);
                            gui.openVariantAesthetics(player, contextId);
                        }
                    } else if (menuType.equals("SELECT_BB_STYLE")) {
                        MobVariant var = varManager.getVariant(contextId);
                        if (var != null && var.getModifiers() != null) {
                            String style = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                            var.getModifiers().setBossBarStyle(style);
                            varManager.save();
                            player.sendMessage("§aStyle BossBar défini sur " + style);
                            gui.openVariantAesthetics(player, contextId);
                        }
                    } else if (menuType.startsWith("SELECT_SOUND_")) {
                        String sType = menuType.substring(13).toLowerCase();
                        MobVariant var = varManager.getVariant(contextId);
                        if (var != null) {
                            String soundKey = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                            if (soundKey.equals("none")) {
                                var.getCustomSounds().remove(sType);
                            } else {
                                var.getCustomSounds().put(sType, new MobVariant.SoundConfig(soundKey, 1.0f, 1.0f));
                            }
                            varManager.save();
                            player.sendMessage("§aSon défini sur : " + soundKey);
                            gui.openVariantDropsAudio(player, contextId);
                        }
                    } else if (menuType.startsWith("SELECT_EQUIP_")) {
                        String cType = menuType.substring(13).toLowerCase();
                        String tier = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                        if (cType.equals("variant")) {
                            MobVariant var = varManager.getVariant(contextId);
                            if (var != null && var.getModifiers() != null) {
                                var.getModifiers().setEquipmentTier(tier);
                                varManager.save();
                                player.sendMessage("§aTier d'équipement défini sur " + tier);
                                gui.openVariantModifiers(player, contextId);
                            }
                        } else {
                            DifficultyZone zone = zoneManager.getZone(contextId);
                            if (zone != null && zone.getModifiers() != null) {
                                zone.getModifiers().setEquipmentTier(tier);
                                zoneManager.save();
                                player.sendMessage("§aTier d'équipement défini sur " + tier);
                                gui.openZoneModifiers(player, contextId);
                            }
                        }
                    }
                    if (menuType.equals("SELECT_REINFORCEMENT")) {
                        MobVariant var = varManager.getVariant(contextId);
                        if (var != null && var.getModifiers() != null) {
                            if (slot == 53) {
                                gui.openVariantBehaviors(player, contextId);
                                return;
                            }
                            if (slot == 49) {
                                var.getModifiers().setDeathSpawnVariant("none");
                                var.getModifiers().setDeathSpawnAmount(0);
                                varManager.save();
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.sbire_de_renfort_retiré"));
                                gui.openVariantBehaviors(player, contextId);
                                return;
                            }
                            List<MobVariant> list = new ArrayList<>(varManager.getAllVariants());
                            list.removeIf(v -> v.getId().equals(contextId));
                            if (slot < list.size()) {
                                MobVariant choice = list.get(slot);
                                var.getModifiers().setDeathSpawnVariant(choice.getId());
                                if (var.getModifiers().getDeathSpawnAmount() <= 0) {
                                    var.getModifiers().setDeathSpawnAmount(1);
                                }
                                varManager.save();
                                player.sendMessage("§aSbire de renfort défini sur : " + choice.getId());
                                gui.openVariantBehaviors(player, contextId);
                            }
                        }
                        return;
                    }
                    if (menuType.equals("SELECT_CMD")) {
                        MobVariant var = varManager.getVariant(contextId);
                        if (var != null) {
                            if (slot == 26) {
                                gui.openVariantAesthetics(player, contextId);
                                return;
                            }
                            if (slot == 22) {
                                var.setCustomModelData(0);
                                varManager.save();
                                plugin.updateVisualizedMobs(contextId);
                                player.sendMessage(plugin.getLangManager().getRaw("gui.msg.custom_model_data_réinitialisé"));
                                gui.openVariantAesthetics(player, contextId);
                                return;
                            }
                            if (slot == 18) {
                                ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.saisissez_lid_numérique_du_custom_1"), input -> {
                                    try {
                                        int val = Integer.parseInt(input);
                                        var.setCustomModelData(Math.max(0, val));
                                        varManager.save();
                                        plugin.updateVisualizedMobs(contextId);
                                        player.sendMessage("§aCustom Model Data défini à " + val);
                                    } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                                    plugin.getServer().getScheduler().runTask(plugin, () -> gui.openVariantAesthetics(player, contextId));
                                });
                                return;
                            }
                            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.GOLD_INGOT) {
                                String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                                if (name.contains("Modèle : ")) {
                                    try {
                                        int val = Integer.parseInt(name.split("Modèle : ")[1].trim());
                                        var.setCustomModelData(val);
                                        varManager.save();
                                        plugin.updateVisualizedMobs(contextId);
                                        player.sendMessage("§aCustom Model Data défini à " + val);
                                        gui.openVariantAesthetics(player, contextId);
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                        return;
                    }
                    if (menuType.equals("SELECT_RANGED_TYPE")) {
                        MobVariant var = varManager.getVariant(contextId);
                        if (var != null && var.getModifiers() != null) {
                            if (slot == 17) {
                                gui.openVariantBehaviors(player, contextId);
                                return;
                            }
                            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.BARRIER) {
                                String typeName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                                var.getModifiers().setRangedAttackType(typeName.toUpperCase());
                                varManager.save();
                                player.sendMessage("§aProjectile à distance défini sur : " + typeName);
                                gui.openVariantBehaviors(player, contextId);
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    private void handleGenericReturn(Player player, String menuType, String contextId, GuiManager gui) {
        if (menuType.equals("GLOBALS") || menuType.equals("VARIANT_LIST") || menuType.equals("SQUAD_LIST") || menuType.equals("ZONE_LIST") || menuType.equals("BLOODMOON_EDIT") || menuType.equals("BIOME_SPAWN_CONFIG") || menuType.equals("ADMIN_TOOLS") || menuType.equals("GENERAL_CONFIG")) {
            gui.openMainMenu(player);
        } else if (menuType.equals("VARIANT_EDIT")) {
            gui.openVariantList(player);
        } else if (menuType.equals("CHOOSE_ENTITY_TYPE")) {
            String baseContext = contextId;
            if (contextId.contains(":")) {
                baseContext = contextId.split(":")[0];
            }
            if (baseContext.equals("CREATE_FLOW")) gui.openVariantList(player);
            else gui.openVariantEditor(player, baseContext);
        } else if (menuType.equals("VARIANT_MODS") || menuType.equals("VARIANT_BEHAVIORS") || menuType.equals("VARIANT_AESTHETICS") || menuType.equals("VARIANT_DROPS_AUDIO")) {
            gui.openVariantEditor(player, contextId);
        } else if (menuType.startsWith("SELECT_PARTICLE_") || menuType.equals("SELECT_BB_COLOR") || menuType.equals("SELECT_BB_STYLE") || menuType.equals("SELECT_SKIN_HEAD") || menuType.equals("SELECT_CMD") || menuType.equals("SKIN_HEAD_BANK") || menuType.equals("VARIANT_SKIN_SELECTOR")) {
            gui.openVariantAesthetics(player, contextId);
        } else if (menuType.equals("SELECT_POTIONS") || menuType.equals("SELECT_REINFORCEMENT") || menuType.equals("SELECT_RANGED_TYPE") || menuType.equals("SELECT_ONHIT_POTION")) {
            gui.openVariantBehaviors(player, contextId);
        } else if (menuType.startsWith("SELECT_SOUND_") || menuType.equals("CUSTOM_DROPS")) {
            gui.openVariantDropsAudio(player, contextId);
        } else if (menuType.equals("CONDITIONAL_NAMES")) {
            gui.openVariantEditor(player, contextId);
        } else if (menuType.equals("SELECT_DROP_MAT")) {
            gui.openVariantDrops(player, contextId);
        } else if (menuType.equals("SELECT_EQUIP_VARIANT")) {
            gui.openVariantModifiers(player, contextId);
        } else if (menuType.equals("SELECT_EQUIP_ZONE")) {
            gui.openZoneModifiers(player, contextId);
        } else if (menuType.equals("SQUAD_EDIT")) {
            gui.openSquadList(player);
        } else if (menuType.equals("SQUAD_MEMBERS_EDIT") || menuType.equals("SQUAD_TRIGGERS_EDIT") || menuType.equals("SQUAD_BONUSES_EDIT")) {
            gui.openSquadEditor(player, contextId);
        } else if (menuType.equals("ZONE_EDIT")) {
            if (player.hasPermission("wilddifficulty.admin")) {
                gui.openZoneList(player);
            } else {
                gui.openPlayerEditableZonesGui(player);
            }
        } else if (menuType.equals("ZONE_MODS")) {
            gui.openZoneEditor(player, contextId);
        } else if (menuType.equals("ZONE_SCALING_EDIT")) {
            gui.openZoneEditor(player, contextId);
        } else if (menuType.equals("ZONE_MEMBERS") || menuType.equals("ZONE_BEACON") || menuType.equals("ZONE_DANGER_NEST") || menuType.equals("ZONE_SUBSECTIONS")) {
            gui.openZoneEditor(player, contextId);
        } else if (menuType.equals("LANGUAGE_SELECT") || menuType.equals("ADMIN_TOOLS") || menuType.equals("GENERAL_CONFIG") || menuType.equals("BLOODMOON_EDIT")) {
            gui.openMainMenu(player);
        } else if (menuType.equals("MAIN")) {
            player.closeInventory();
        } else if (menuType.equals("ADMIN_THIRST_HARDCORE")) {
            gui.openGeneralConfig(player);
        } else if (menuType.equals("ADMIN_THIRST_SOURCES")) {
            gui.openThirstHardcoreAdminGui(player);
        } else if (menuType.equals("HARDCORE_CONFIG")) {
            gui.openThirstHardcoreAdminGui(player);
        } else if (menuType.equals("SPAWNER_VARIANTS")) {
            String[] parts = contextId.split(":");
            org.bukkit.World world = Bukkit.getWorld(parts[0]);
            if (world != null) {
                Location loc = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                gui.openSpawnerEditor(player, loc);
            }
        } else if (menuType.equals("VARIANT_EQUIPMENT_EDIT")) {
            gui.openVariantDropsAudio(player, contextId);
        } else if (menuType.startsWith("SELECT_EQ_")) {
            gui.openVariantEquipmentEditor(player, contextId);
        } else if (menuType.equals("SPAWN_CONDITIONS")) {
            gui.openVariantEditor(player, contextId);
        } else if (menuType.equals("VARIANT_BIOMES_EDIT")) {
            String baseVarId = contextId.contains(":") ? contextId.split(":")[0] : contextId;
            gui.openVariantSpawnConditions(player, baseVarId);
        } else if (menuType.startsWith("ZONE_PARTICLE_")) {
            gui.openZoneEditor(player, contextId);
        } else if (menuType.equals("SELECT_LEATHER_COLOR")) {
            String[] parts = contextId.split(":");
            gui.openVariantEquipmentEditor(player, parts[0]);
        }
    }

    private void handleNumericScaleClick(Player player, InventoryClickEvent event, MobVariant var, boolean isVariance) {
        if (event.getClick() == ClickType.MIDDLE) {
            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_la_valeur_précise_ex"), input -> {
                try {
                    double val = Double.parseDouble(input);
                    if (isVariance) var.setScaleVariance(Math.max(0.0, val));
                    else var.setScale(Math.max(0.1, val));
                    plugin.getVariantManager().save();
                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.valeur_définie"));
                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openVariantAesthetics(player, var.getId()));
            });
            return;
        }

        double delta = 0.0;
        boolean shift = event.isShiftClick();
        if (event.isLeftClick()) {
            delta = shift ? (isVariance ? 0.5 : 1.0) : 0.1;
        } else if (event.isRightClick()) {
            delta = shift ? (isVariance ? -0.5 : -1.0) : -0.1;
        }

        if (delta != 0.0) {
            if (isVariance) var.setScaleVariance(Math.round(Math.max(0.0, var.getScaleVariance() + delta) * 100.0) / 100.0);
            else var.setScale(Math.round(Math.max(0.1, var.getScale() + delta) * 100.0) / 100.0);
            plugin.getVariantManager().save();
            plugin.getGuiManager().openVariantAesthetics(player, var.getId());
        }
    }

    private void handleSquadChanceClick(Player player, InventoryClickEvent event, MobSquad sq) {
        if (event.getClick() == ClickType.MIDDLE) {
            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_la_chance_sous_forme"), input -> {
                try {
                    double val = Double.parseDouble(input) / 100.0;
                    sq.setSpawnChance(Math.max(0.0, val));
                    plugin.getVariantManager().save();
                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.chance_définie"));
                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openSquadEditor(player, sq.getId()));
            });
            return;
        }

        double delta = 0.0;
        boolean shift = event.isShiftClick();
        if (event.isLeftClick()) {
            delta = shift ? 0.1 : 0.01;
        } else if (event.isRightClick()) {
            delta = shift ? -0.1 : -0.01;
        }

        if (delta != 0.0) {
            sq.setSpawnChance(Math.max(0.0, sq.getSpawnChance() + delta));
            plugin.getVariantManager().save();
            plugin.getGuiManager().openSquadEditor(player, sq.getId());
        }
    }

    private void handleZonePriorityClick(Player player, InventoryClickEvent event, DifficultyZone zone) {
        if (event.getClick() == ClickType.MIDDLE) {
            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_la_priorité_précise_ex"), input -> {
                try {
                    int val = Integer.parseInt(input);
                    zone.setPriority(val);
                    plugin.getZoneManager().save();
                    player.sendMessage(plugin.getLangManager().getRaw("gui.msg.priorité_définie"));
                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openZoneEditor(player, zone.getId()));
            });
            return;
        }

        int delta = 0;
        boolean shift = event.isShiftClick();
        if (event.isLeftClick()) {
            delta = shift ? 5 : 1;
        } else if (event.isRightClick()) {
            delta = shift ? -5 : -1;
        }

        if (delta != 0) {
            zone.setPriority(zone.getPriority() + delta);
            plugin.getZoneManager().save();
            plugin.getGuiManager().openZoneEditor(player, zone.getId());
        }
    }

    private void handleModifierClick(Player player, InventoryClickEvent event, String type, String contextId, GuiManager gui, VariantManager varManager, ZoneManager zoneManager, MainConfigManager mainCfg) {
        int slot = event.getRawSlot();
        if (type.equals("squad")) {
            if (slot < 10 || slot > 13) return;
        } else {
            if ((slot < 10 || slot > 14) && slot != 16) return;
        }

        if (event.getClick() == ClickType.MIDDLE) {
            ChatPromptUtil.prompt(plugin, player, plugin.getLangManager().getRaw("gui.msg.entrez_la_nouvelle_valeur_précise"), input -> {
                try {
                    double val = Double.parseDouble(input);
                    applyModifier(type, contextId, slot, val, true, varManager, zoneManager, mainCfg);
                    player.sendMessage("§aValeur définie sur " + val);
                } catch (Exception e) { player.sendMessage(plugin.getLangManager().getRaw("gui.msg.nombre_invalide")); }
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (type.equals("global")) gui.openGlobalModifiersMenu(player);
                    else if (type.equals("variant")) gui.openVariantModifiers(player, contextId);
                    else if (type.equals("zone")) gui.openZoneModifiers(player, contextId);
                    else if (type.equals("squad")) gui.openSquadBonusesEditor(player, contextId);
                });
            });
            return;
        }

        double delta = 0.0;
        boolean shift = event.isShiftClick();
        if (event.isLeftClick()) {
            delta = shift ? 10.0 : 1.0;
        } else if (event.isRightClick()) {
            delta = shift ? -10.0 : -1.0;
        }

        if (delta != 0) {
            applyModifier(type, contextId, slot, delta, false, varManager, zoneManager, mainCfg);
            if (type.equals("global")) gui.openGlobalModifiersMenu(player);
            else if (type.equals("variant")) gui.openVariantModifiers(player, contextId);
            else if (type.equals("zone")) gui.openZoneModifiers(player, contextId);
            else if (type.equals("squad")) gui.openSquadBonusesEditor(player, contextId);
        }
    }

    private void applyModifier(String type, String contextId, int slot, double valOrDelta, boolean isSet, VariantManager varManager, ZoneManager zoneManager, MainConfigManager mainCfg) {
        if (type.equals("global")) {
            switch (slot) {
                case 10 -> mainCfg.setGlobalHealthMult(isSet ? valOrDelta : mainCfg.getGlobalHealthMult() + valOrDelta);
                case 11 -> mainCfg.setGlobalDamageMult(isSet ? valOrDelta : mainCfg.getGlobalDamageMult() + valOrDelta);
                case 12 -> mainCfg.setGlobalSpeedMult(isSet ? valOrDelta : mainCfg.getGlobalSpeedMult() + valOrDelta);
                case 13 -> mainCfg.setGlobalFollowRangeMult(isSet ? valOrDelta : mainCfg.getGlobalFollowRangeMult() + valOrDelta);
                case 14 -> mainCfg.setGlobalKnockbackMult(isSet ? valOrDelta : mainCfg.getGlobalKnockbackMult() + valOrDelta);
            }
            mainCfg.save();
        } else if (type.equals("variant")) {
            MobVariant var = varManager.getVariant(contextId);
            if (var != null && var.getModifiers() != null) {
                StatModifiers m = var.getModifiers();
                switch (slot) {
                    case 10 -> m.setHealthValue(isSet ? valOrDelta : m.getHealthValue() + valOrDelta);
                    case 11 -> m.setDamageValue(isSet ? valOrDelta : m.getDamageValue() + valOrDelta);
                    case 12 -> m.setSpeedValue(isSet ? valOrDelta : m.getSpeedValue() + valOrDelta);
                    case 13 -> m.setFollowRangeValue(isSet ? valOrDelta : m.getFollowRangeValue() + valOrDelta);
                    case 14 -> m.setKnockbackValue(isSet ? valOrDelta : m.getKnockbackValue() + valOrDelta);
                    case 16 -> m.setRegenerationValue(isSet ? valOrDelta : m.getRegenerationValue() + valOrDelta);
                }
                varManager.save();
            }
        } else if (type.equals("zone")) {
            DifficultyZone zone = zoneManager.getZone(contextId);
            if (zone != null && zone.getModifiers() != null) {
                StatModifiers m = zone.getModifiers();
                switch (slot) {
                    case 10 -> m.setHealthValue(isSet ? valOrDelta : m.getHealthValue() + valOrDelta);
                    case 11 -> m.setDamageValue(isSet ? valOrDelta : m.getDamageValue() + valOrDelta);
                    case 12 -> m.setSpeedValue(isSet ? valOrDelta : m.getSpeedValue() + valOrDelta);
                    case 13 -> m.setFollowRangeValue(isSet ? valOrDelta : m.getFollowRangeValue() + valOrDelta);
                    case 14 -> m.setKnockbackValue(isSet ? valOrDelta : m.getKnockbackValue() + valOrDelta);
                    case 16 -> m.setRegenerationValue(isSet ? valOrDelta : m.getRegenerationValue() + valOrDelta);
                }
                zoneManager.save();
            }
        } else if (type.equals("squad")) {
            MobSquad sq = varManager.getSquad(contextId);
            if (sq != null) {
                switch (slot) {
                    case 10 -> sq.setBonusHealth(Math.max(0.0, isSet ? valOrDelta : sq.getBonusHealth() + valOrDelta));
                    case 11 -> sq.setBonusDamage(Math.max(0.0, isSet ? valOrDelta : sq.getBonusDamage() + valOrDelta));
                    case 12 -> sq.setBonusSpeed(Math.max(0.0, isSet ? valOrDelta : sq.getBonusSpeed() + valOrDelta));
                    case 13 -> sq.setBonusRegen(Math.max(0.0, isSet ? valOrDelta : sq.getBonusRegen() + valOrDelta));
                }
                varManager.save();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        org.bukkit.inventory.ItemStack item = event.getItem();
        if (item == null || item.getType() != org.bukkit.Material.COMPASS) return;
        if (!item.hasItemMeta()) return;

        org.bukkit.NamespacedKey toolKey = new org.bukkit.NamespacedKey(plugin, "wd_tool_type");
        String toolType = item.getItemMeta().getPersistentDataContainer().get(toolKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (!"biome".equals(toolType)) {
            String plainName = getCleanName(item);
            if (!plainName.contains("Configure Spawns de Biome") && !plainName.contains("Biome")) return;
        }

        event.setCancelled(true);
        String bKey = plugin.getGuiManager().getBiomeKeyString(player.getLocation());
        plugin.getGuiManager().openBiomeSpawnConfig(player, bKey);
    }

    private String getCleanName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        net.kyori.adventure.text.Component comp = item.getItemMeta().displayName();
        if (comp == null) return "";
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
        return org.bukkit.ChatColor.stripColor(plain);
    }
}
