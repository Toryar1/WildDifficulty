package fr.wilddifficulty;

import fr.wilddifficulty.commands.MobZoneCommand;
import fr.wilddifficulty.commands.WDReloadCommand;
import fr.wilddifficulty.config.BiomeConfigManager;
import fr.wilddifficulty.config.LanguageSetup;
import fr.wilddifficulty.config.MainConfigManager;
import fr.wilddifficulty.config.MobConfigManager;
import fr.wilddifficulty.listener.DaytimeSpawnListener;
import fr.wilddifficulty.listener.EntityCombustListener;
import fr.wilddifficulty.listener.GuiListener;
import fr.wilddifficulty.listener.MobSpawnListener;
import fr.wilddifficulty.listener.MobBehaviorListener;
import fr.wilddifficulty.listener.ZoneToolListener;
import fr.wilddifficulty.listener.SafeZoneListener;
import fr.wilddifficulty.util.AttributeUtil;
import fr.wilddifficulty.util.MobTickScheduler;
import fr.wilddifficulty.util.UpdateChecker;
import fr.wilddifficulty.variant.VariantManager;
import fr.wilddifficulty.gui.GuiManager;
import fr.wilddifficulty.zone.ZoneManager;
import fr.wilddifficulty.commands.WDGuiCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.util.NametagUtil;

public final class WildDifficultyPlugin extends JavaPlugin {

    private static WildDifficultyPlugin instance;

    private MainConfigManager mainConfigManager;
    private BiomeConfigManager biomeConfigManager;
    private MobConfigManager mobConfigManager;
    private ZoneManager zoneManager;
    private VariantManager variantManager;
    private GuiManager guiManager;
    private fr.wilddifficulty.spawner.SpawnerManager spawnerManager;
    private fr.wilddifficulty.config.LangManager langManager;
    private UpdateChecker updateChecker;
    private LanguageSetup languageSetup;

    private fr.wilddifficulty.player.PlayerSettingsManager playerSettingsManager;

    // Hooks & Encounters v1.1
    private fr.wilddifficulty.hook.WorldGuardHook worldGuardHook;
    private fr.wilddifficulty.hook.IrisHook irisHook;
    private fr.wilddifficulty.hook.BetonQuestHook betonQuestHook;
    private fr.wilddifficulty.encounter.EncounterManager encounterManager;

    public fr.wilddifficulty.hook.WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public fr.wilddifficulty.hook.IrisHook getIrisHook() { return irisHook; }
    public fr.wilddifficulty.hook.BetonQuestHook getBetonQuestHook() { return betonQuestHook; }
    public fr.wilddifficulty.encounter.EncounterManager getEncounterManager() { return encounterManager; }

    public fr.wilddifficulty.player.PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public UpdateChecker getUpdateChecker() { return updateChecker; }

    private MobTickScheduler tickScheduler;
    private final java.util.Set<java.util.UUID> activeScoreboards = new java.util.HashSet<>();

    public java.util.Set<java.util.UUID> getActiveScoreboards() {
        return activeScoreboards;
    }

    private final java.util.Map<java.util.UUID, String> editingZoneId = new java.util.HashMap<>();
    public java.util.Map<java.util.UUID, String> getEditingZoneId() { return editingZoneId; }

    @Override
    public void onEnable() {
        instance = this;

        // Sauvegarde des configs par défaut si elles n'existent pas
        saveDefaultConfig();
        saveResourceIfNotExists("biomes.yml");
        saveResourceIfNotExists("mobs.yml");
        saveResourceIfNotExists("mob-variants.yml");
        saveResourceIfNotExists("zones.yml");

        // Dossier skins local
        java.io.File skinsDir = new java.io.File(getDataFolder(), "skins");
        if (!skinsDir.exists()) {
            skinsDir.mkdirs();
            try {
                java.io.File readme = new java.io.File(skinsDir, "LISEZ_MOI.txt");
                java.nio.file.Files.writeString(readme.toPath(),
                    "Glissez-deposez des fichiers .txt contenant l'URL de texture ou le code Base64 dans ce dossier.\n" +
                    "Exemple de contenu d'un fichier 'vache_sang.txt' :\n" +
                    "http://textures.minecraft.net/texture/3f1a2b...\n" +
                    "Cette texture sera alors disponible dans la banque de tetes/skins sous le nom 'vache_sang'.");
            } catch (Exception ignored) {}
        }

        // ─── Sélection de la langue au premier démarrage ───
        languageSetup = new LanguageSetup(this);
        languageSetup.setupIfNeeded();

        // Initialisation des managers
        langManager = new fr.wilddifficulty.config.LangManager(this);
        langManager.load();
        mainConfigManager = new MainConfigManager(this);
        biomeConfigManager = new BiomeConfigManager(this);
        mobConfigManager = new MobConfigManager(this);
        zoneManager = new ZoneManager(this);
        variantManager = new VariantManager(this);
        guiManager = new GuiManager(this);
        spawnerManager = new fr.wilddifficulty.spawner.SpawnerManager(this);
        playerSettingsManager = new fr.wilddifficulty.player.PlayerSettingsManager(this);

        mainConfigManager.load();
        biomeConfigManager.load();
        mobConfigManager.load();
        zoneManager.load();
        variantManager.load();
        spawnerManager.load();

        // Initialisation des Hooks & Encounters v1.1
        worldGuardHook = new fr.wilddifficulty.hook.WorldGuardHook(this);
        irisHook = new fr.wilddifficulty.hook.IrisHook(this);
        betonQuestHook = new fr.wilddifficulty.hook.BetonQuestHook(this);
        encounterManager = new fr.wilddifficulty.encounter.EncounterManager(this);

        // Initialisation des clés d'attributs
        AttributeUtil.init(this);

        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityCombustListener(this), this);
        getServer().getPluginManager().registerEvents(new DaytimeSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new MobBehaviorListener(this), this);
        getServer().getPluginManager().registerEvents(new ZoneToolListener(this), this);
        getServer().getPluginManager().registerEvents(new SafeZoneListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.wilddifficulty.listener.SpawnerToolListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.wilddifficulty.listener.InspectorToolListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.wilddifficulty.listener.ZoneProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.wilddifficulty.listener.ThirstListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.wilddifficulty.listener.HardcoreDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.wilddifficulty.listener.EncounterListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.wilddifficulty.listener.SpawnMarkerToolListener(this), this);

        // Enregistrement des commandes
        WDGuiCommand wdCmd = new WDGuiCommand(this);
        getCommand("wd").setExecutor(wdCmd);
        getCommand("wd").setTabCompleter(wdCmd);

        // Démarrage du MobTickScheduler (toutes les 1 tick = 0.05s pour une dégradation d'apnée fluide frame-par-frame)
        tickScheduler = new MobTickScheduler(this);
        tickScheduler.runTaskTimer(this, 1L, 1L);

        // Génération automatique du resource pack
        fr.wilddifficulty.util.ResourcePackGenerator.generate(this);

        // ─── Update Checker (asynchrone) ───
        updateChecker = new UpdateChecker(this);
        updateChecker.checkAsync();

        // ─── PlaceholderAPI ───
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new fr.wilddifficulty.hook.PlaceholderAPIHook(this).register();
            getLogger().info("[WildDifficulty] PlaceholderAPI détecté — placeholders enregistrés.");
        }

        // ─── Listener pour notifier les admins des mises à jour ───
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                if (updateChecker != null) {
                    getServer().getScheduler().runTaskLater(WildDifficultyPlugin.this,
                        () -> updateChecker.notifyPlayer(e.getPlayer()), 40L);
                }
            }
        }, this);

        // ─── bStats Métriques ───
        fr.wilddifficulty.util.BStatsMetrics.register(this);

        getLogger().info("WildDifficulty v1.1 activé avec succès !");
    }

    @Override
    public void onDisable() {
        if (tickScheduler != null) {
            tickScheduler.cancel();
        }
        if (encounterManager != null) {
            encounterManager.shutdown();
        }
        if (spawnerManager != null) {
            spawnerManager.save();
        }
        getLogger().info("WildDifficulty désactivé.");
    }

    public void reloadAll() {
        reloadConfig();
        if (langManager != null) {
            langManager.load();
        }
        mainConfigManager.load();
        biomeConfigManager.load();
        mobConfigManager.load();
        zoneManager.reload();
        variantManager.load();
        if (spawnerManager != null) {
            spawnerManager.load();
        }
        if (worldGuardHook != null) {
            worldGuardHook.checkAvailability();
        }
        if (irisHook != null) {
            irisHook.checkAvailability();
        }
        if (betonQuestHook != null) {
            betonQuestHook.checkAvailability();
        }
        // Régénérer le resource pack au reload
        fr.wilddifficulty.util.ResourcePackGenerator.generate(this);
        getLogger().info("WildDifficulty : configuration rechargée.");
    }

    public NamespacedKey getNamespacedKey(String key) {
        return new NamespacedKey(this, key);
    }

    public static WildDifficultyPlugin getInstance() { return instance; }
    public fr.wilddifficulty.config.LangManager getLangManager() { return langManager; }
    public LanguageSetup getLanguageSetup() { return languageSetup; }
    public MainConfigManager getMainConfigManager() { return mainConfigManager; }
    public BiomeConfigManager getBiomeConfigManager() { return biomeConfigManager; }
    public MobConfigManager getMobConfigManager() { return mobConfigManager; }
    public ZoneManager getZoneManager() { return zoneManager; }
    public VariantManager getVariantManager() { return variantManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public fr.wilddifficulty.spawner.SpawnerManager getSpawnerManager() { return spawnerManager; }

    public void updateVisualizedMobs(String variantId) {
        MobVariant var = getVariantManager().getVariant(variantId);
        if (var == null) return;

        for (org.bukkit.World world : getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity le) {
                    if (le.getPersistentDataContainer().has(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING)) {
                        String varId = le.getPersistentDataContainer().get(MobSpawnListener.KEY_VARIANT_ID, PersistentDataType.STRING);
                        if (variantId.equalsIgnoreCase(varId) && le.getPersistentDataContainer().has(new NamespacedKey(this, "wd_visualize"), PersistentDataType.BYTE)) {
                            // Update scale
                            le.getPersistentDataContainer().set(new NamespacedKey(this, "wd_scale"), PersistentDataType.DOUBLE, var.getScale());
                            le.getPersistentDataContainer().set(new NamespacedKey(this, "wd_scale_variance"), PersistentDataType.DOUBLE, var.getScaleVariance());
                            AttributeUtil.applyScale(le, var.getScale());

                            if (le instanceof org.bukkit.entity.Ageable ageable) {
                                if (var.isBaby()) {
                                    ageable.setBaby();
                                } else {
                                    ageable.setAdult();
                                }
                            }
                            
                            // Apply modifiers
                            StatModifiers finalMod = new StatModifiers();
                            if (var.getModifiers() != null) {
                                finalMod.stackWith(var.getModifiers());
                            }
                            double hp = finalMod.getHealthValue() > 0 ? finalMod.getHealthValue() : getBaseValue(le, org.bukkit.attribute.Attribute.MAX_HEALTH);
                            double dmg = finalMod.getDamageValue() > 0 ? finalMod.getDamageValue() : getBaseValue(le, org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
                            double fRange = finalMod.getFollowRangeValue() > 0 ? finalMod.getFollowRangeValue() : getBaseValue(le, org.bukkit.attribute.Attribute.FOLLOW_RANGE);
                            double kb = finalMod.getKnockbackValue() >= 0 ? finalMod.getKnockbackValue() : getBaseValue(le, org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
                            
                            AttributeUtil.applyHealth(le, hp);
                            AttributeUtil.applyDamage(le, dmg);
                            AttributeUtil.applyFollowRange(le, fRange);
                            AttributeUtil.applyKnockbackResistance(le, kb);
                            
                            // Keep speed 0 for static
                            org.bukkit.attribute.AttributeInstance speedAttr = le.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                            if (speedAttr != null) {
                                speedAttr.setBaseValue(0.0);
                            }
                            
                            // Equipment
                            fr.wilddifficulty.util.EquipmentUtil.applyEquipment(le, finalMod, var.getCustomModelData());
                            
                            // Nametag
                            NametagUtil.applyNametag(le, getMainConfigManager().getNametagFormat(), 0, var);
                        }
                    }
                }
            }
        }
    }

    private double getBaseValue(LivingEntity entity, org.bukkit.attribute.Attribute attr) {
        org.bukkit.attribute.AttributeInstance instance = entity.getAttribute(attr);
        return instance != null ? instance.getBaseValue() : 0.0;
    }

    private void saveResourceIfNotExists(String resourcePath) {
        java.io.File file = new java.io.File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            try {
                saveResource(resourcePath, false);
            } catch (Exception ignored) {}
        }
    }
}
