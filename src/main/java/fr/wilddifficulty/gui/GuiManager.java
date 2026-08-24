package fr.wilddifficulty.gui;

import fr.wilddifficulty.WildDifficultyPlugin;
import fr.wilddifficulty.config.MainConfigManager;
import fr.wilddifficulty.config.StatModifiers;
import fr.wilddifficulty.variant.MobSquad;
import fr.wilddifficulty.variant.MobVariant;
import fr.wilddifficulty.zone.DifficultyZone;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiManager {

    private final WildDifficultyPlugin plugin;

    public GuiManager(WildDifficultyPlugin plugin) {
        this.plugin = plugin;
    }

    private void fillBorders(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        int size = inv.getSize();
        int rows = size / 9;
        for (int col = 0; col < 9; col++) {
            if (inv.getItem(col) == null) inv.setItem(col, glass);
            int bot = (rows - 1) * 9 + col;
            if (inv.getItem(bot) == null) inv.setItem(bot, glass);
        }
        for (int row = 0; row < rows; row++) {
            int left = row * 9;
            if (inv.getItem(left) == null) inv.setItem(left, glass);
            int right = row * 9 + 8;
            if (inv.getItem(right) == null) inv.setItem(right, glass);
        }
    }

    /** Creates a Retour/Back barrier button tagged with PDC so all languages work */
    private ItemStack createBackItem() {
        ItemStack item = createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.button_back"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "wd_back_button"),
                org.bukkit.persistence.PersistentDataType.STRING, "true");
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Creates a Fermer/Close barrier button tagged with PDC so all languages work */
    private ItemStack createCloseItem() {
        ItemStack item = createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.button_close"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "wd_back_button"),
                org.bukkit.persistence.PersistentDataType.STRING, "true");
            item.setItemMeta(meta);
        }
        return item;
    }


    // ================= MAIN MENU =================
    public void openMainMenu(Player player) {
        WDMenuHolder holder = new WDMenuHolder("MAIN", null);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_main")));
        holder.setInventory(inv);

        fillBorders(inv);

        // Core Management (Row 2)
        inv.setItem(10, createItem(Material.ZOMBIE_HEAD, plugin.getLangManager().getRaw("gui.item.variantes"), plugin.getLangManager().getRaw("gui.item.gérer_les_variantes_de_mobs")));
        inv.setItem(11, createItem(Material.SKELETON_SKULL, plugin.getLangManager().getRaw("gui.item.escouades"), plugin.getLangManager().getRaw("gui.item.gérer_les_escouades")));
        inv.setItem(12, createItem(Material.MAP, plugin.getLangManager().getRaw("gui.item.zones"), plugin.getLangManager().getRaw("gui.item.gérer_les_zones_de_difficulté")));

        inv.setItem(14, createItem(Material.REDSTONE, plugin.getLangManager().getRaw("gui.item.configuration_lune_de_sang"), 
                plugin.getLangManager().getRaw("gui.item.gérer_le_taux_dapparition_multiplicateurs"), 
                plugin.getLangManager().getRaw("gui.item.et_bonus_lors_des_nuits"), 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_pour_configurer")));
        
        inv.setItem(15, createItem(Material.COMPARATOR, plugin.getLangManager().getRaw("gui.item.configuration_générale"),
                plugin.getLangManager().getRaw("gui.item.configurer_le_cap_de_mobs"),
                plugin.getLangManager().getRaw("gui.item.de_spawn_max_la_langue"),
                plugin.getLangManager().getRaw("gui.item.les_nametags_et_paramètres_généraux"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_configurer")));

        inv.setItem(16, createItem(Material.CHEST_MINECART, plugin.getLangManager().getRaw("gui.item.outils_dadministration"), 
                plugin.getLangManager().getRaw("gui.item.obtenir_individuellement_ou_tous_les"), 
                plugin.getLangManager().getRaw("gui.item.de_configuration_et_danalyse"), 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_pour_ouvrir")));

        inv.setItem(22, createCloseItem());

        player.openInventory(inv);
    }

    public void openGeneralConfig(Player player) {
        WDMenuHolder holder = new WDMenuHolder("GENERAL_CONFIG", null);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_general_config")));
        holder.setInventory(inv);
        MainConfigManager cfg = plugin.getMainConfigManager();

        fillBorders(inv);

        // Modificateurs globaux (Slot 10)
        inv.setItem(10, createItem(Material.DIAMOND_SWORD, plugin.getLangManager().getRaw("gui.item.modificateurs_globaux"), plugin.getLangManager().getRaw("gui.item.ajustez_la_difficulté_générale")));

        // Bannissement d'entités naturelles (Slot 11)
        int bannedCount = cfg.getBannedNaturalEntities().size();
        String bannedCountBadge = bannedCount > 0 ? "§c" + bannedCount + " banni(s)" : "§aAucun (Vanilla actif)";
        inv.setItem(11, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.bannir_entites_naturelles"),
                plugin.getLangManager().getRaw("gui.item.bannir_entites_naturelles_desc1"),
                plugin.getLangManager().getRaw("gui.item.bannir_entites_naturelles_desc2"),
                "&7Statut : " + bannedCountBadge,
                "",
                plugin.getLangManager().getRaw("gui.item.bannir_entites_naturelles_desc3")));

        // Soif, Hardcore & Mort (Slot 12)
        String thirstStatus = cfg.isThirstEnabled() ? "§a✔ Actif" : "§c✖ Inactif";
        String hardcoreStatus = cfg.isHardcoreEnabled() ? "§c✔ Actif" : "§7✖ Inactif";
        inv.setItem(12, createItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.soif_hardcore_mort"),
                "&7Soif : " + thirstStatus,
                "&7Hardcore : " + hardcoreStatus,
                "&7Despawn mort : &e" + cfg.getDeathItemDespawnSeconds() + "s",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_configurer_1")));

        // Max spawn distance (Slot 14)
        inv.setItem(14, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.item.distance_de_spawn_max"),
                plugin.getLangManager().getRaw("gui.item.distance_actuelle") + (cfg.getMaxSpawnDistance() == -1 ? plugin.getLangManager().getRaw("gui.item.désactivé") : cfg.getMaxSpawnDistance() + plugin.getLangManager().getRaw("gui.item.blocs")),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_10_blocs_clic"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_50_blocs"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_50_blocs"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis")));

        // Cap of variants per player (Slot 15)
        inv.setItem(15, createItem(Material.COMPARATOR, plugin.getLangManager().getRaw("gui.item.cap_de_variantes_joueur"),
                plugin.getLangManager().getRaw("gui.item.limite_actuelle") + cfg.getCapVariantesParJoueur() + plugin.getLangManager().getRaw("gui.item.variantes_1"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_5_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_20"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_20"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis")));

        // Disable burning globally (Slot 16)
        inv.setItem(16, createToggleItem(Material.FLINT_AND_STEEL, plugin.getLangManager().getRaw("gui.item.pas_de_combustion_au_soleil"), cfg.isDisableBurningGlobally()));

        // Allow day spawn globally (Slot 19)
        inv.setItem(19, createToggleItem(Material.SUNFLOWER, plugin.getLangManager().getRaw("gui.item.spawns_de_jour_globaux"), cfg.isAllowDaySpawnGlobally()));

        // Nametags status (Slot 20)
        inv.setItem(20, createToggleItem(Material.NAME_TAG, plugin.getLangManager().getRaw("gui.item.activer_nametags"), cfg.isNametagsEnabled()));

        // Nametag format (Slot 21)
        inv.setItem(21, createItem(Material.WRITABLE_BOOK, plugin.getLangManager().getRaw("gui.item.format_des_nametags"),
                plugin.getLangManager().getRaw("gui.item.format_actuel") + cfg.getNametagFormat(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_définir_via_le")));

        // Langue du plugin (Slot 23)
        String currentLangCode = plugin.getConfig().getString("plugin.language", "fr");
        String currentLangName = fr.wilddifficulty.config.LanguageSetup.getAvailableLanguages().getOrDefault(currentLangCode, "Français");
        inv.setItem(23, createItem(Material.BOOK, plugin.getLangManager().getRaw("gui.item.langue_du_plugin"),
                plugin.getLangManager().getRaw("gui.item.langue_actuelle") + currentLangName + " (" + currentLangCode + ")",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_choisir_la_langue")));

        // Recharger config (Slot 24)
        inv.setItem(24, createItem(Material.COMMAND_BLOCK, plugin.getLangManager().getRaw("gui.item.recharger_config"), plugin.getLangManager().getRaw("gui.item.recharge_les_fichiers_yaml"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_recharger")));

        // Mode Debug (Slot 25)
        inv.setItem(25, createToggleItem(Material.REDSTONE_TORCH, plugin.getLangManager().getRaw("gui.item.mode_debug"), cfg.isDebug()));

        inv.setItem(31, createBackItem());
        player.openInventory(inv);
    }

    public void openBannedNaturalEntitiesMenu(Player player, int page, String filter) {
        if (filter == null || filter.isEmpty()) filter = "ALL";
        WDMenuHolder holder = new WDMenuHolder("BANNED_NATURAL_ENTITIES", page + ":" + filter);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_banned_natural_entities") + " (" + filter + ")"));
        holder.setInventory(inv);

        List<EntityType> choicesList = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && type != EntityType.ARMOR_STAND && type != EntityType.PLAYER) {
                boolean hostile = isHostileType(type);
                if (filter.equals("HOSTILES") && !hostile) continue;
                if (filter.equals("PASSIVES") && hostile) continue;
                choicesList.add(type);
            }
        }
        choicesList.sort(java.util.Comparator.comparing(Enum::name));

        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) choicesList.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, choicesList.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            EntityType type = choicesList.get(i);
            Material mat = getVariantHeadMaterial(type);
            boolean isBanned = plugin.getMainConfigManager().isNaturalEntityBanned(type.name());
            String statusTitle = isBanned ? "&c✖ &l" + type.name() : "&a✔ &l" + type.name();
            String statusLore = isBanned ? plugin.getLangManager().getRaw("gui.banned_entities.status_banned") : plugin.getLangManager().getRaw("gui.banned_entities.status_allowed");

            inv.setItem(slot++, createItem(mat, statusTitle,
                    statusLore,
                    "",
                    plugin.getLangManager().getRaw("gui.banned_entities.click_toggle")));
        }

        // Filtres (slots 45, 46, 47)
        inv.setItem(45, createItem(Material.NETHERITE_SWORD, plugin.getLangManager().getRaw("gui.item.filtrer_hostiles"), plugin.getLangManager().getRaw("gui.item.afficher_uniquement_les_monstres")));
        inv.setItem(46, createItem(Material.WHEAT, plugin.getLangManager().getRaw("gui.item.filtrer_passifs_neutres"), plugin.getLangManager().getRaw("gui.item.afficher_les_animaux_et_créatures")));
        inv.setItem(47, createItem(Material.BOOK, plugin.getLangManager().getRaw("gui.item.filtrer_tous"), plugin.getLangManager().getRaw("gui.item.afficher_toutes_les_créatures_de")));

        // Pagination
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_précédente") + page + ")"));
        }
        if (page < maxPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_suivante") + (page + 2) + ")"));
        }

        // Tête d'information '?' (slot 49)
        ItemStack infoHead = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) infoHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("MHF_Question"));
            infoHead.setItemMeta(skullMeta);
        }
        inv.setItem(49, createItem(infoHead, plugin.getLangManager().getRaw("gui.banned_entities.info_title"),
                plugin.getLangManager().getRaw("gui.banned_entities.info_line1"),
                plugin.getLangManager().getRaw("gui.banned_entities.info_line2"),
                "",
                plugin.getLangManager().getRaw("gui.banned_entities.info_line3"),
                plugin.getLangManager().getRaw("gui.banned_entities.info_line4")));

        // Bouton Retour (slot 53)
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openLanguageSelector(Player player) {
        WDMenuHolder holder = new WDMenuHolder("LANGUAGE_SELECT", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_language_select")));
        holder.setInventory(inv);

        fillBorders(inv);

        String currentLang = plugin.getConfig().getString("plugin.language", "fr");

        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        int idx = 0;

        for (Map.Entry<String, String> entry : fr.wilddifficulty.config.LanguageSetup.getAvailableLanguages().entrySet()) {
            if (idx >= slots.length) break;
            String code = entry.getKey();
            String name = entry.getValue();
            boolean isCurrent = code.equalsIgnoreCase(currentLang);

            Material mat = isCurrent ? Material.EMERALD_BLOCK : Material.PAPER;
            String statusLore = isCurrent ? "&a✔ Langue Actuelle" : "&eClic pour choisir";

            ItemStack item = createItem(mat, "&6&l" + name + " &7(" + code + ")",
                plugin.getLangManager().getRaw("gui.item.code") + code,
                "",
                statusLore);

            if (isCurrent) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setEnchantmentGlintOverride(true);
                    item.setItemMeta(meta);
                }
            }

            inv.setItem(slots[idx], item);
            idx++;
        }

        inv.setItem(49, createBackItem());
        player.openInventory(inv);
    }

    public void openAdminToolsMenu(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ADMIN_TOOLS", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_admin_tools")));
        holder.setInventory(inv);

        fillBorders(inv);

        inv.setItem(20, createItem(Material.GOLDEN_HOE, plugin.getLangManager().getRaw("gui.item.outil_de_zone"), plugin.getLangManager().getRaw("gui.item.dessine_les_points_du_polygone"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_obtenir")));
        inv.setItem(21, createItem(Material.NETHERITE_SHOVEL, plugin.getLangManager().getRaw("gui.item.outil_de_spawner"), plugin.getLangManager().getRaw("gui.item.configure_les_spawners_personnalisés"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_obtenir")));
        inv.setItem(22, createItem(Material.COMPASS, plugin.getLangManager().getRaw("gui.item.outil_de_biome"), plugin.getLangManager().getRaw("gui.item.configure_les_spawns_par_biome"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_obtenir")));
        inv.setItem(23, createItem(Material.STICK, plugin.getLangManager().getRaw("gui.item.inspecteur_de_mobs"), plugin.getLangManager().getRaw("gui.item.affiche_les_détails_complets_dune"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_obtenir")));
        
        inv.setItem(24, createItem(Material.CHEST, plugin.getLangManager().getRaw("gui.item.tous_les_outils"), plugin.getLangManager().getRaw("gui.item.obtenir_tous_les_outils_dun"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_obtenir")));
        inv.setItem(31, createItem(Material.FILLED_MAP, plugin.getLangManager().getRaw("gui.item.activerdésactiver_scoreboard_debug"), plugin.getLangManager().getRaw("gui.item.affiche_le_scoreboard_latéral_danalyse"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer")));

        inv.setItem(49, createBackItem());
        player.openInventory(inv);
    }

    public void openBloodMoonEditor(Player player) {
        WDMenuHolder holder = new WDMenuHolder("BLOODMOON_EDIT", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_bloodmoon")));
        holder.setInventory(inv);
        MainConfigManager cfg = plugin.getMainConfigManager();

        inv.setItem(10, createToggleItem(Material.REDSTONE, plugin.getLangManager().getRaw("gui.item.activer_lune_de_sang"), cfg.isBloodMoonEnabled()));
        
        inv.setItem(11, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.item.chance_dapparition"), 
                plugin.getLangManager().getRaw("gui.item.chance_actuelle") + String.format(plugin.getLangManager().getRaw("gui.item.0f"), cfg.getBloodMoonChance() * 100),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_5_clic_droit_1"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat")));

        inv.setItem(12, createItem(Material.RED_DYE, plugin.getLangManager().getRaw("gui.item.multiplicateur_pv"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + String.format(plugin.getLangManager().getRaw("gui.item.1fx"), cfg.getBloodMoonHpMultiplier()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_01_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_05_shiftclic_05"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat")));

        inv.setItem(13, createItem(Material.NETHERITE_SWORD, plugin.getLangManager().getRaw("gui.item.multiplicateur_dégâts"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + String.format(plugin.getLangManager().getRaw("gui.item.1fx"), cfg.getBloodMoonDamageMultiplier()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_01_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_05_shiftclic_05"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat")));

        inv.setItem(14, createItem(Material.FEATHER, plugin.getLangManager().getRaw("gui.item.multiplicateur_vitesse"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + String.format(plugin.getLangManager().getRaw("gui.item.1fx"), cfg.getBloodMoonSpeedMultiplier()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_01_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_05_shiftclic_05"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat")));

        inv.setItem(15, createItem(Material.CHEST, plugin.getLangManager().getRaw("gui.item.multiplicateur_drops"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + String.format(plugin.getLangManager().getRaw("gui.item.1fx"), cfg.getBloodMoonDropsMultiplier()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_01_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_05_shiftclic_05"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat")));

        inv.setItem(16, createItem(Material.SPAWNER, plugin.getLangManager().getRaw("gui.item.multiplicateur_spawn"),
                plugin.getLangManager().getRaw("gui.item.actuel") + String.format(plugin.getLangManager().getRaw("gui.item.1fx"), cfg.getBloodMoonSpawnMultiplier()),
                plugin.getLangManager().getRaw("gui.item.nombre_supplémentaire_de_mobs"),
                plugin.getLangManager().getRaw("gui.item.qui_spawneront_pendant_la_blood"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_01_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_05_shiftclic_05"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat")));

        inv.setItem(18, createItem(Material.WRITABLE_BOOK, plugin.getLangManager().getRaw("gui.item.message_de_début"), 
                plugin.getLangManager().getRaw("gui.item.actuel_1") + cfg.getBloodMoonStartMessage(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_définir_via_le")));

        inv.setItem(19, createItem(Material.WRITTEN_BOOK, plugin.getLangManager().getRaw("gui.item.message_de_fin"), 
                plugin.getLangManager().getRaw("gui.item.actuel_1") + cfg.getBloodMoonEndMessage(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_définir_via_le")));

        inv.setItem(20, createItem(Material.JUKEBOX, plugin.getLangManager().getRaw("gui.item.son_de_début"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + cfg.getBloodMoonStartSound(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_modifier_via_le")));

        inv.setItem(21, createItem(Material.JUKEBOX, plugin.getLangManager().getRaw("gui.item.son_de_fin"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + cfg.getBloodMoonEndSound(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_modifier_via_le")));

        inv.setItem(22, createItem(Material.REDSTONE, plugin.getLangManager().getRaw("gui.item.particule_de_début"), 
                plugin.getLangManager().getRaw("gui.item.actuelle") + cfg.getBloodMoonStartParticle(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_modifier_via_le")));

        inv.setItem(23, createItem(Material.EMERALD, plugin.getLangManager().getRaw("gui.item.particule_de_fin"), 
                plugin.getLangManager().getRaw("gui.item.actuelle") + cfg.getBloodMoonEndParticle(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_modifier_via_le")));

        java.util.List<String> startPots = cfg.getBloodMoonStartPotions();
        String startPotsStr = startPots.isEmpty() ? "&7Aucun" : String.join(", ", startPots);
        inv.setItem(24, createItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.effets_potion_de_début"), 
                plugin.getLangManager().getRaw("gui.item.actuels") + startPotsStr,
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_définir_format_typeduréeampli"),
                plugin.getLangManager().getRaw("gui.item.séparez_par_des_virgules_pour")));

        java.util.List<String> endPots = cfg.getBloodMoonEndPotions();
        String endPotsStr = endPots.isEmpty() ? "&7Aucun" : String.join(", ", endPots);
        inv.setItem(25, createItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.effets_potion_de_fin"), 
                plugin.getLangManager().getRaw("gui.item.actuels") + endPotsStr,
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_définir_format_typeduréeampli_1"),
                plugin.getLangManager().getRaw("gui.item.séparez_par_des_virgules_pour")));

        inv.setItem(31, createItem(Material.NETHER_STAR, plugin.getLangManager().getRaw("gui.item.forcer_la_lune_de_sang"),
                plugin.getLangManager().getRaw("gui.item.active_la_lune_de_sang"),
                plugin.getLangManager().getRaw("gui.item.si_cest_le_jour_planifiée"),
                plugin.getLangManager().getRaw("gui.item.si_cest_la_nuit_démarre"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_lactiver")));

        inv.setItem(49, createBackItem());
        player.openInventory(inv);
    }

    public void openGlobalModifiersMenu(Player player) {
        WDMenuHolder holder = new WDMenuHolder("GLOBALS", null);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_global_modifiers")));
        holder.setInventory(inv);
        MainConfigManager cfg = plugin.getMainConfigManager();

        inv.setItem(11, createModifierItem(Material.APPLE, plugin.getLangManager().getRaw("gui.item.pv_sante"), cfg.getGlobalHealthMult()));
        inv.setItem(12, createModifierItem(Material.IRON_SWORD, plugin.getLangManager().getRaw("gui.item.degats"), cfg.getGlobalDamageMult()));
        inv.setItem(13, createModifierItem(Material.FEATHER, plugin.getLangManager().getRaw("gui.item.vitesse"), cfg.getGlobalSpeedMult()));
        inv.setItem(14, createModifierItem(Material.ENDER_EYE, plugin.getLangManager().getRaw("gui.item.detection"), cfg.getGlobalFollowRangeMult()));
        inv.setItem(15, createModifierItem(Material.SHIELD, plugin.getLangManager().getRaw("gui.item.knockback"), cfg.getGlobalKnockbackMult()));

        inv.setItem(22, createBackItem());
        player.openInventory(inv);
    }

    private final Map<java.util.UUID, String> variantSortFilters = new java.util.HashMap<>();

    public String getSortFilter(java.util.UUID uuid) {
        return variantSortFilters.getOrDefault(uuid, "DEFAULT");
    }

    public void cycleSortFilter(java.util.UUID uuid) {
        String current = variantSortFilters.getOrDefault(uuid, "DEFAULT");
        String next = switch (current) {
            case "DEFAULT" -> "HP";
            case "HP" -> "DAMAGE";
            case "DAMAGE" -> "SPEED";
            default -> "DEFAULT";
        };
        variantSortFilters.put(uuid, next);
    }

    // ================= VARIANTS =================
    public void openVariantList(Player player) {
        WDMenuHolder holder = new WDMenuHolder("VARIANT_LIST", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_variants")));
        holder.setInventory(inv);

        String filter = getSortFilter(player.getUniqueId());
        List<MobVariant> list = new ArrayList<>(plugin.getVariantManager().getAllVariants());
        switch (filter) {
            case "HP" -> list.sort((a, b) -> {
                double v1 = a.getModifiers() != null ? a.getModifiers().getHealthValue() : -1;
                double v2 = b.getModifiers() != null ? b.getModifiers().getHealthValue() : -1;
                return Double.compare(v2, v1);
            });
            case "DAMAGE" -> list.sort((a, b) -> {
                double v1 = a.getModifiers() != null ? a.getModifiers().getDamageValue() : -1;
                double v2 = b.getModifiers() != null ? b.getModifiers().getDamageValue() : -1;
                return Double.compare(v2, v1);
            });
            case "SPEED" -> list.sort((a, b) -> {
                double v1 = a.getModifiers() != null ? a.getModifiers().getSpeedValue() : -1;
                double v2 = b.getModifiers() != null ? b.getModifiers().getSpeedValue() : -1;
                return Double.compare(v2, v1);
            });
            default -> list.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        }

        int slot = 0;
        for (MobVariant var : list) {
            if (slot >= 45) break;
            Material mat = getVariantHeadMaterial(var.getType());
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + var.getId(), 
                    plugin.getLangManager().getRaw("gui.item.type") + var.getType().name(), 
                    plugin.getLangManager().getRaw("gui.item.poids") + var.getWeight(),
                    plugin.getLangManager().getRaw("gui.item.taille") + String.format(plugin.getLangManager().getRaw("gui.item.2f"), var.getScale()) + " (&7+-" + String.format(plugin.getLangManager().getRaw("gui.item.2f"), var.getScaleVariance()) + ")",
                    plugin.getLangManager().getRaw("gui.item.bébé") + (var.isBaby() ? plugin.getLangManager().getRaw("gui.item.oui") : plugin.getLangManager().getRaw("gui.item.non")),
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_éditer")));
        }
        
        inv.setItem(49, createItem(Material.EMERALD, plugin.getLangManager().getRaw("gui.item.nouvelle_variante"), plugin.getLangManager().getRaw("gui.item.créer_une_nouvelle_variante")));
        inv.setItem(50, createItem(Material.HOPPER, plugin.getLangManager().getRaw("gui.item.tri_actuel") + filter, 
                plugin.getLangManager().getRaw("gui.item.changer_le_tri_des_variantes"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_basculer_1"),
                plugin.getLangManager().getRaw("gui.item.id_par_défaut"),
                plugin.getLangManager().getRaw("gui.item.hp_points_de_vie"),
                plugin.getLangManager().getRaw("gui.item.dégâts_points_dattaque"),
                plugin.getLangManager().getRaw("gui.item.vitesse_rapidité")));
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    private boolean isHostileType(EntityType type) {
        String name = type.name();
        return name.equals("ZOMBIE") || name.equals("SKELETON") || name.equals("CREEPER") || name.equals("SPIDER")
            || name.equals("CAVE_SPIDER") || name.equals("WITHER_SKELETON") || name.equals("PIGLIN") || name.equals("HUSK")
            || name.equals("DROWNED") || name.equals("STRAY") || name.equals("WITCH") || name.equals("EVOKER")
            || name.equals("VINDICATOR") || name.equals("PILLAGER") || name.equals("RAVAGER") || name.equals("BLAZE")
            || name.equals("GHAST") || name.equals("MAGMA_CUBE") || name.equals("SLIME") || name.equals("ENDERMAN")
            || name.equals("PHANTOM") || name.equals("GUARDIAN") || name.equals("ELDER_GUARDIAN") || name.equals("SHULKER")
            || name.equals("SILVERFISH") || name.equals("ENDERMITE") || name.equals("PIGLIN_BRUTE") || name.equals("HOGLIN")
            || name.equals("ZOGLIN") || name.equals("BREEZE") || name.equals("BOGGED") || name.equals("ZOMBIE_VILLAGER")
            || name.equals("ZOMBIFIED_PIGLIN") || name.equals("VEX") || name.equals("GIANT") || name.equals("ILLUSIONER")
            || name.equals("WITHER") || name.equals("ENDER_DRAGON") || name.equals("WARDEN");
    }

    public void openEntityTypeSelection(Player player, String context) {
        String baseContext = context;
        int page = 0;
        String filter = "ALL";
        if (context.contains(":")) {
            String[] parts = context.split(":");
            baseContext = parts[0];
            if (parts.length > 2) {
                try {
                    page = Integer.parseInt(parts[1]);
                } catch (Exception ignored) {}
                filter = parts[2];
            } else if (parts.length > 1) {
                try {
                    page = Integer.parseInt(parts[1]);
                } catch (Exception e) {
                    filter = parts[1];
                }
            }
        }

        WDMenuHolder holder = new WDMenuHolder("CHOOSE_ENTITY_TYPE", baseContext + ":" + page + ":" + filter);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_choose_base_type") + " (" + filter + ")"));
        holder.setInventory(inv);

        List<EntityType> choicesList = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && type != EntityType.ARMOR_STAND) {
                boolean hostile = isHostileType(type);
                if (filter.equals("HOSTILES") && !hostile) continue;
                if (filter.equals("PASSIVES") && hostile) continue;
                choicesList.add(type);
            }
        }
        // Always ensure PLAYER is included
        if (filter.equals("ALL") || filter.equals("PASSIVES")) {
            if (!choicesList.contains(EntityType.PLAYER)) {
                choicesList.add(EntityType.PLAYER);
            }
        }

        // Sort alphabetically
        choicesList.sort(java.util.Comparator.comparing(Enum::name));

        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) choicesList.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, choicesList.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            EntityType type = choicesList.get(i);
            Material mat = getVariantHeadMaterial(type);
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + type.name(), plugin.getLangManager().getRaw("gui.item.sélectionner_ce_type_de_base")));
        }

        // Filtres
        inv.setItem(45, createItem(Material.NETHERITE_SWORD, plugin.getLangManager().getRaw("gui.item.filtrer_hostiles"), plugin.getLangManager().getRaw("gui.item.afficher_uniquement_les_monstres")));
        inv.setItem(46, createItem(Material.WHEAT, plugin.getLangManager().getRaw("gui.item.filtrer_passifs_neutres"), plugin.getLangManager().getRaw("gui.item.afficher_les_animaux_et_créatures")));
        inv.setItem(47, createItem(Material.BOOK, plugin.getLangManager().getRaw("gui.item.filtrer_tous"), plugin.getLangManager().getRaw("gui.item.afficher_toutes_les_créatures_de")));

        // Pagination
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_précédente") + page + ")"));
        }
        if (page < maxPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_suivante") + (page + 2) + ")"));
        }

        inv.setItem(53, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.annuler")));
        player.openInventory(inv);
    }

    public void openVariantEditor(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("VARIANT_EDIT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_edit") + ": " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createItem(Material.NAME_TAG, plugin.getLangManager().getRaw("gui.item.type_de_base"), plugin.getLangManager().getRaw("gui.item.actuel_2") + var.getType().name(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(11, createItem(Material.ANVIL, plugin.getLangManager().getRaw("gui.item.poids_dapparition"), plugin.getLangManager().getRaw("gui.item.actuel_3") + var.getWeight(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(12, createItem(Material.DAYLIGHT_DETECTOR, plugin.getLangManager().getRaw("gui.item.immunité_solaire"), plugin.getLangManager().getRaw("gui.item.actuel_2") + (var.isIgnoreSunlight() ? plugin.getLangManager().getRaw("gui.item.oui") : plugin.getLangManager().getRaw("gui.item.non")), "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer")));
        inv.setItem(13, createItem(Material.EXPERIENCE_BOTTLE, plugin.getLangManager().getRaw("gui.item.xp_donnée_à_la_mort"),
                plugin.getLangManager().getRaw("gui.item.actuel") + (var.getXpOnDeath() < 0 ? plugin.getLangManager().getRaw("gui.item.vanilla") : var.getXpOnDeath() + plugin.getLangManager().getRaw("gui.item.xp")),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_10_shiftclic_droit"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_saisir_dans_le")));
        inv.setItem(14, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.item.conditions_de_spawn"), plugin.getLangManager().getRaw("gui.item.configurer_la_météo_le_journuit"), plugin.getLangManager().getRaw("gui.item.et_les_biomes_autorisés"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_configurer")));
        
        inv.setItem(20, createItem(Material.DIAMOND_SWORD, plugin.getLangManager().getRaw("gui.item.modifier_stats_absolues"), plugin.getLangManager().getRaw("gui.item.pv_dégâts_vitesse"), plugin.getLangManager().getRaw("gui.item.knockback_et_portée")));
        inv.setItem(21, createItem(Material.NETHER_STAR, plugin.getLangManager().getRaw("gui.item.comportements_ai"), plugin.getLangManager().getRaw("gui.item.agressivité_fuite_charge"), plugin.getLangManager().getRaw("gui.item.vision_murs_et_renforts_mort")));
        inv.setItem(22, createItem(Material.GLOWSTONE_DUST, plugin.getLangManager().getRaw("gui.item.esthétique_cosmétique"), plugin.getLangManager().getRaw("gui.item.skins_taille_auras_noms"), plugin.getLangManager().getRaw("gui.item.bossbar_et_bébés")));
        
        String eqWarning = "";
        if (!supportsEquipment(var.getType())) {
            eqWarning = "\n&cAttention: Ce type de mob ne peut pas\n&cporter d'équipement en Vanilla (visuellement).";
        }
        inv.setItem(23, createItem(Material.CHEST, plugin.getLangManager().getRaw("gui.item.drops_audio_équipement"), plugin.getLangManager().getRaw("gui.item.chance_déquipements"), plugin.getLangManager().getRaw("gui.item.sons_et_drops_personnalisés") + eqWarning));
        
        // Bouton spawn avec description claire des modes
        inv.setItem(24, createItem(Material.ZOMBIE_SPAWN_EGG, plugin.getLangManager().getRaw("gui.item.faire_apparaître"), 
                plugin.getLangManager().getRaw("gui.item.invoque_ce_monstre_2_blocs"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_visualiser_férustatique"),
                plugin.getLangManager().getRaw("gui.item.clic_droit_normal_ia_activée")));

        inv.setItem(31, createItem(Material.LAVA_BUCKET, plugin.getLangManager().getRaw("gui.item.supprimer_la_variante"), plugin.getLangManager().getRaw("gui.item.attention_action_immédiate")));
        inv.setItem(32, createItem(Material.PAPER, plugin.getLangManager().getRaw("gui.item.copier_la_variante"), plugin.getLangManager().getRaw("gui.item.cloner_cette_variante_avec"), plugin.getLangManager().getRaw("gui.item.tous_ses_paramètres_sous"), plugin.getLangManager().getRaw("gui.item.un_nouvel_id"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_cloner")));
        inv.setItem(35, createBackItem());
        player.openInventory(inv);
    }

    public void openVariantModifiers(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        StatModifiers mods = var.getModifiers();
        if (mods == null) {
            mods = new StatModifiers();
            var.setModifiers(mods);
        }

        WDMenuHolder holder = new WDMenuHolder("VARIANT_MODS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_stats") + ": " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createModifierItem(Material.APPLE, plugin.getLangManager().getRaw("gui.item.pv_sante"), mods.getHealthValue()));
        inv.setItem(11, createModifierItem(Material.IRON_SWORD, plugin.getLangManager().getRaw("gui.item.degats"), mods.getDamageValue()));
        inv.setItem(12, createModifierItem(Material.FEATHER, plugin.getLangManager().getRaw("gui.item.vitesse"), mods.getSpeedValue()));
        inv.setItem(13, createModifierItem(Material.ENDER_EYE, plugin.getLangManager().getRaw("gui.item.detection"), mods.getFollowRangeValue()));
        inv.setItem(14, createModifierItem(Material.SHIELD, plugin.getLangManager().getRaw("gui.item.knockback"), mods.getKnockbackValue()));

        inv.setItem(15, createItem(Material.IRON_CHESTPLATE, plugin.getLangManager().getRaw("gui.item.équipement_armure"), plugin.getLangManager().getRaw("gui.item.tier") + mods.getEquipmentTier(), plugin.getLangManager().getRaw("gui.item.chance") + (mods.getEquipmentChance()*100) + "%", "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(16, createModifierItem(Material.GOLDEN_APPLE, plugin.getLangManager().getRaw("gui.item.regen"), mods.getRegenerationValue()));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openVariantBehaviors(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        StatModifiers mods = var.getModifiers();

        WDMenuHolder holder = new WDMenuHolder("VARIANT_BEHAVIORS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_ai_behavior") + ": " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.ENDER_PEARL, plugin.getLangManager().getRaw("gui.item.téléportation_cible"), mods.isTeleportToTarget()));
        inv.setItem(11, createToggleItem(Material.SADDLE, plugin.getLangManager().getRaw("gui.item.chargedash"), mods.isDashAttack()));
        inv.setItem(12, createToggleItem(Material.GLASS_BOTTLE, plugin.getLangManager().getRaw("gui.item.invisibilité_camouflage"), mods.isCamouflage()));
        inv.setItem(13, createToggleItem(Material.REDSTONE_TORCH, plugin.getLangManager().getRaw("gui.item.vision_à_travers_murs"), mods.isGoalWallVision()));
        inv.setItem(14, createToggleItem(Material.GOLDEN_APPLE, plugin.getLangManager().getRaw("gui.item.régénération_passive"), mods.getPassiveRegen() > 0));
        inv.setItem(15, createToggleItem(Material.FIRE_CHARGE, plugin.getLangManager().getRaw("gui.item.explosion_à_la_mort"), mods.isExplodeOnDeath()));
        inv.setItem(16, createToggleItem(Material.BOW, plugin.getLangManager().getRaw("gui.item.attaque_à_distance"), mods.isRangedAttack()));
        inv.setItem(17, createItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.effets_de_potion"), plugin.getLangManager().getRaw("gui.item.gérer_les_effets_de_potion"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_gérer")));

        // Mode d'agressivité
        String agg = var.getAggroMode();
        Material aggMat = agg.equalsIgnoreCase("AGGRESSIVE") ? Material.REDSTONE_BLOCK : (agg.equalsIgnoreCase("PASSIVE") ? Material.SLIME_BLOCK : Material.GOLD_BLOCK);
        inv.setItem(18, createItem(aggMat, plugin.getLangManager().getRaw("gui.item.agressivité_ia"), plugin.getLangManager().getRaw("gui.item.actuel") + agg, "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer"), plugin.getLangManager().getRaw("gui.item.passive_aggressive_neutral_hit_neutral")));

        // Fuite
        inv.setItem(19, createItem(Material.FEATHER, plugin.getLangManager().getRaw("gui.item.fuite_seuil_pv"), 
                plugin.getLangManager().getRaw("gui.item.seuil_de_vie_actuel") + String.format(plugin.getLangManager().getRaw("gui.item.0f"), mods.getFleeUnderHealth() * 100), 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_gauche_5_clic_droit_1"), 
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat")));

        inv.setItem(20, createToggleItem(Material.RABBIT_FOOT, plugin.getLangManager().getRaw("gui.item.fuite_si_solo_isolé"), mods.isFleeWhenSolo()));

        // Spawn renforts à la mort
        inv.setItem(21, createItem(Material.SPIDER_EYE, plugin.getLangManager().getRaw("gui.item.spawns_renforts_à_la_mort"), 
                plugin.getLangManager().getRaw("gui.item.variante") + mods.getDeathSpawnVariant(),
                plugin.getLangManager().getRaw("gui.item.quantité") + mods.getDeathSpawnAmount(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_définir_variante"),
                plugin.getLangManager().getRaw("gui.item.clic_droit_changer_quantité"),
                plugin.getLangManager().getRaw("gui.item.shift_5_simple_1"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_réinitialiser_none0")));

        // Jump AI
        inv.setItem(22, createToggleItem(Material.LEATHER_BOOTS, plugin.getLangManager().getRaw("gui.item.ia_de_saut"), mods.isJumpAttack()));

        // Max usages limits
        inv.setItem(23, createItem(Material.ENDER_PEARL, plugin.getLangManager().getRaw("gui.item.téléportation_max_usages"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + (mods.getTeleportMaxUses() == -1 ? plugin.getLangManager().getRaw("gui.item.infini") : mods.getTeleportMaxUses()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_5_shiftclic_5"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis")));

        inv.setItem(24, createItem(Material.SADDLE, plugin.getLangManager().getRaw("gui.item.charge_max_usages"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + (mods.getDashMaxUses() == -1 ? plugin.getLangManager().getRaw("gui.item.infini") : mods.getDashMaxUses()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_5_shiftclic_5"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis")));

        // Ranged attack projectile type
        inv.setItem(25, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.projectile_à_distance"), 
                plugin.getLangManager().getRaw("gui.item.type_actuel") + mods.getRangedAttackType(),
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));

        // On Hit potion effect
        inv.setItem(26, createItem(Material.DRAGON_BREATH, plugin.getLangManager().getRaw("gui.item.effet_sur_coup_donné_on"), 
                plugin.getLangManager().getRaw("gui.item.effet") + mods.getOnHitPotionEffect(),
                plugin.getLangManager().getRaw("gui.item.chance") + String.format(plugin.getLangManager().getRaw("gui.item.0f"), mods.getOnHitPotionChance()*100),
                plugin.getLangManager().getRaw("gui.item.niveaudurée_level") + (mods.getOnHitPotionAmplifier()+1) + " / " + (mods.getOnHitPotionDuration()/20) + "s",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_changer_effet"),
                plugin.getLangManager().getRaw("gui.item.clic_droit_chance_5"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_durée_1s"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_niveau_1"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_réinitialiser")));

        inv.setItem(27, createToggleItem(Material.SCAFFOLDING, plugin.getLangManager().getRaw("gui.item.escalade_intelligente"), mods.isSmartClimb()));

        inv.setItem(35, createBackItem());
        player.openInventory(inv);
    }

    public void openVariantAesthetics(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        StatModifiers mods = var.getModifiers();
        if (mods == null) {
            mods = new StatModifiers();
            var.setModifiers(mods);
        }

        WDMenuHolder holder = new WDMenuHolder("VARIANT_AESTHETICS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_aesthetics") + ": " + variantId));
        holder.setInventory(inv);

        // Noms
        inv.setItem(10, createItem(Material.NAME_TAG, plugin.getLangManager().getRaw("gui.item.nom_daffichage"), plugin.getLangManager().getRaw("gui.item.actuel_2") + (var.getDisplayName() != null ? var.getDisplayName() : plugin.getLangManager().getRaw("gui.item.par_défaut")), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer_tchat")));
        inv.setItem(11, createItem(Material.WRITABLE_BOOK, plugin.getLangManager().getRaw("gui.item.noms_conditionnels"), plugin.getLangManager().getRaw("gui.item.gérer_les_noms_selon_les"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_gérer")));

        // Skin & Model
        int cmd = var.getCustomModelData();
        inv.setItem(12, createItem(Material.ITEM_FRAME, plugin.getLangManager().getRaw("gui.item.skin_custom_model_data"), 
                plugin.getLangManager().getRaw("gui.item.actuel_2") + (cmd > 0 ? cmd : plugin.getLangManager().getRaw("gui.item.aucun")),
                plugin.getLangManager().getRaw("gui.item.permet_dassocier_un_modèle_3d"),
                plugin.getLangManager().getRaw("gui.item.personnalisé_de_resource_pack"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_ouvrir_le_sélecteur")));
        
        String headSkin = mods.getSkullSkin();
        inv.setItem(13, createItem(Material.PLAYER_HEAD, plugin.getLangManager().getRaw("gui.item.banque_de_skins_têtes"), 
                plugin.getLangManager().getRaw("gui.item.actuel_2") + (plugin.getLangManager().getRaw("gui.item.none").equals(headSkin) ? plugin.getLangManager().getRaw("gui.item.par_défaut") : (headSkin.length() > 20 ? plugin.getLangManager().getRaw("gui.item.skin_custom") : headSkin)),
                plugin.getLangManager().getRaw("gui.item.appliquez_des_skins_personnalisés"),
                plugin.getLangManager().getRaw("gui.item.sur_la_tête_du_monstre"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_ouvrir_la_banque")));

        // Bébé
        if (supportsBaby(var.getType())) {
            inv.setItem(14, createToggleItem(Material.EGG, plugin.getLangManager().getRaw("gui.item.toggle_bébé"), var.isBaby()));
        } else {
            inv.setItem(14, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.bébé_non_supporté"), plugin.getLangManager().getRaw("gui.item.ce_type_de_mob_de"), plugin.getLangManager().getRaw("gui.item.ne_supporte_pas_de_forme")));
        }

        // Scale (Taille)
        inv.setItem(15, createItem(Material.SLIME_BLOCK, plugin.getLangManager().getRaw("gui.item.taille_scale"), 
                plugin.getLangManager().getRaw("gui.item.actuelle_1") + String.format(plugin.getLangManager().getRaw("gui.item.2f"), var.getScale()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_01_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_10"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_10"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis")));

        inv.setItem(16, createItem(Material.HONEY_BLOCK, plugin.getLangManager().getRaw("gui.item.variance_de_taille"), 
                plugin.getLangManager().getRaw("gui.item.actuelle") + String.format(plugin.getLangManager().getRaw("gui.item.2f"), var.getScaleVariance()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_01_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_05"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_05"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis")));

        // Skin & Trait Spécial Vanilla (Fusionné)
        String curSub = mods.getEntitySubtype();
        inv.setItem(17, createItem(Material.PAINTING, plugin.getLangManager().getRaw("gui.item.skin_variante_trait_spécial_vanilla"),
                plugin.getLangManager().getRaw("gui.item.configuration_actuelle") + (plugin.getLangManager().getRaw("gui.item.none").equalsIgnoreCase(curSub) ? plugin.getLangManager().getRaw("gui.item.par_défaut") : curSub),
                plugin.getLangManager().getRaw("gui.item.choisissez_la_sousvariante_skin"),
                plugin.getLangManager().getRaw("gui.item.ou_le_trait_esthétique_creeper"),
                plugin.getLangManager().getRaw("gui.item.loup_chat_poule_vache_villager"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_ouvrir_le_sélecteur_1")));

        // Particules
        inv.setItem(19, createItem(Material.DRAGON_BREATH, plugin.getLangManager().getRaw("gui.item.particules_aura"), plugin.getLangManager().getRaw("gui.item.type") + mods.getParticleAuraType(), plugin.getLangManager().getRaw("gui.item.couleur") + mods.getParticleAuraColor(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(20, createItem(Material.FIREWORK_ROCKET, plugin.getLangManager().getRaw("gui.item.particule_spawn"), plugin.getLangManager().getRaw("gui.item.type") + mods.getParticleSpawnType(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(21, createItem(Material.BONE_MEAL, plugin.getLangManager().getRaw("gui.item.particule_trail"), plugin.getLangManager().getRaw("gui.item.type") + mods.getParticleTrailType(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));

        // BossBar
        inv.setItem(22, createToggleItem(Material.WRITABLE_BOOK, plugin.getLangManager().getRaw("gui.item.bossbar_active"), mods.isBossBarEnabled()));
        inv.setItem(23, createItem(Material.RED_DYE, plugin.getLangManager().getRaw("gui.item.bossbar_couleur"), plugin.getLangManager().getRaw("gui.item.couleur_1") + mods.getBossBarColor(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(24, createItem(Material.ANVIL, plugin.getLangManager().getRaw("gui.item.bossbar_style"), plugin.getLangManager().getRaw("gui.item.style") + mods.getBossBarStyle(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));

        inv.setItem(35, createBackItem());
        player.openInventory(inv);
    }

    public void openMobSkinSelector(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        StatModifiers mods = var.getModifiers();
        if (mods == null) {
            mods = new StatModifiers();
            var.setModifiers(mods);
        }

        WDMenuHolder holder = new WDMenuHolder("VARIANT_SKIN_SELECTOR", variantId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_skin_selector") + ": " + variantId));
        holder.setInventory(inv);

        List<fr.wilddifficulty.util.EntitySubtypeUtil.SubtypeOption> options = fr.wilddifficulty.util.EntitySubtypeUtil.getAvailableSubtypes(var.getType());
        String currentSubtype = mods.getEntitySubtype();

        // Option 0: Par défaut / Reset
        inv.setItem(0, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.par_défaut_aucun_skin_spécifique"),
                plugin.getLangManager().getRaw("gui.item.skinvariante_de_base_du_jeu"),
                plugin.getLangManager().getRaw("gui.item.none").equalsIgnoreCase(currentSubtype) ? plugin.getLangManager().getRaw("gui.item.sélectionné") : "",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_réinitialiser")));

        if (options.isEmpty()) {
            inv.setItem(13, createItem(Material.PAPER, plugin.getLangManager().getRaw("gui.item.aucune_variante_prédéfinie_pour") + var.getType().name(),
                    plugin.getLangManager().getRaw("gui.item.ce_type_de_mob_na"),
                    plugin.getLangManager().getRaw("gui.item.vanilla_intégrée_dans_minecraft"),
                    plugin.getLangManager().getRaw("gui.item.utilisez_la_banque_de_skins"),
                    plugin.getLangManager().getRaw("gui.item.custom_model_data_pour_le")));
        } else {
            int slot = 1;
            for (fr.wilddifficulty.util.EntitySubtypeUtil.SubtypeOption opt : options) {
                if (slot >= 35) break;
                Material mat = Material.matchMaterial(opt.iconMaterial());
                if (mat == null) mat = Material.NAME_TAG;

                boolean selected = opt.id().equalsIgnoreCase(currentSubtype);
                inv.setItem(slot, createItem(mat, plugin.getLangManager().getRaw("empty_5") + opt.displayName(),
                        plugin.getLangManager().getRaw("gui.item.id_interne") + opt.id(),
                        selected ? plugin.getLangManager().getRaw("gui.item.sélectionné") : plugin.getLangManager().getRaw("gui.item.non_sélectionné"),
                        "",
                        plugin.getLangManager().getRaw("gui.item.clic_pour_choisir_cette_variante")));
                slot++;
            }
        }

        inv.setItem(35, createBackItem());
        player.openInventory(inv);
    }

    public void openSkinHeadBank(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_SKIN_HEAD", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_heads_bank")));
        holder.setInventory(inv);

        inv.setItem(0, createSkullItem("MHF_Skeleton", "&bSquelette", "&7Pseudo: MHF_Skeleton", "", "&eClic pour appliquer"));
        inv.setItem(1, createSkullItem("MHF_Blaze", "&bBlaze", "&7Pseudo: MHF_Blaze", "", "&eClic pour appliquer"));
        inv.setItem(2, createSkullItem("MHF_Golem", "&bGolem de Fer", "&7Pseudo: MHF_Golem", "", "&eClic pour appliquer"));
        inv.setItem(3, createSkullItem("MHF_Spider", "&bSpider", "&7Pseudo: MHF_Spider", "", "&eClic pour appliquer"));
        inv.setItem(4, createSkullItem("MHF_Creeper", "&bCreeper", "&7Pseudo: MHF_Creeper", "", "&eClic pour appliquer"));
        inv.setItem(5, createSkullItem("MHF_Enderman", "&bEnderman", "&7Pseudo: MHF_Enderman", "", "&eClic pour appliquer"));
        
        inv.setItem(9, createSkullItem("Demon", "&cDemon", "&7Pseudo: Demon", "", "&eClic pour appliquer"));
        inv.setItem(10, createSkullItem("Orc", "&aOrc", "&7Pseudo: Orc", "", "&eClic pour appliquer"));
        inv.setItem(11, createSkullItem("Ninja", "&eNinja", "&7Pseudo: Ninja", "", "&eClic pour appliquer"));
        inv.setItem(12, createSkullItem("Mummy", "&7Mummy", "&7Pseudo: Mummy", "", "&eClic pour appliquer"));
        inv.setItem(13, createSkullItem("Vampire", "&bVampire", "&7Pseudo: Vampire", "", "&eClic pour appliquer"));
        inv.setItem(14, createSkullItem("Knight", "&fKnight", "&7Pseudo: Knight", "", "&eClic pour appliquer"));

        int customSlot = 15;
        for (fr.wilddifficulty.variant.VariantManager.CustomHead ch : plugin.getVariantManager().getCustomHeads()) {
            if (customSlot > 19) break;
            inv.setItem(customSlot++, createSkullItem(ch.getValue(), "&b" + ch.getName(), "&7Custom Head", "", "&eClic pour appliquer"));
        }

        inv.setItem(20, createItem(Material.NAME_TAG, plugin.getLangManager().getRaw("gui.item.pseudo_personnalisé_tchat"), plugin.getLangManager().getRaw("gui.item.entrer_le_pseudo_dun_joueur"), plugin.getLangManager().getRaw("gui.item.pour_récupérer_son_skin"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_entrer")));
        inv.setItem(21, createItem(Material.PAPER, plugin.getLangManager().getRaw("gui.item.lien_url_texture_base64_tchat"), plugin.getLangManager().getRaw("gui.item.entrer_une_url_minecraftnet"), plugin.getLangManager().getRaw("gui.item.ou_une_texture_base64"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_entrer")));
        inv.setItem(22, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.retirer_le_skin_de_tête"), plugin.getLangManager().getRaw("gui.item.remet_la_tête_par_défaut"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_réinitialiser_1")));
        inv.setItem(23, createItem(Material.CHEST, plugin.getLangManager().getRaw("gui.item.glisserdéposer_une_tête"), plugin.getLangManager().getRaw("gui.item.déposez_un_item_de_tête"), plugin.getLangManager().getRaw("gui.item.pour_copier_sa_texture_et"), plugin.getLangManager().getRaw("gui.item.à_cette_variante_elle_sera"), plugin.getLangManager().getRaw("gui.item.à_la_banque")));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openVariantDropsAudio(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("VARIANT_DROPS_AUDIO", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_drops_sounds") + ": " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createItem(Material.DIAMOND_CHESTPLATE, plugin.getLangManager().getRaw("gui.item.équipement_spécifique"), plugin.getLangManager().getRaw("gui.item.gérer_casque_plastron_jambières_bottes"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_configurer")));
        inv.setItem(11, createItem(Material.CHEST, plugin.getLangManager().getRaw("gui.item.tables_de_drops"), plugin.getLangManager().getRaw("gui.item.gérer_les_loots_personnalisés"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_gérer")));
        
        // Sounds buttons
        inv.setItem(13, createItem(Material.JUKEBOX, plugin.getLangManager().getRaw("gui.item.son_dambiance"), plugin.getLangManager().getRaw("gui.item.actuel_2") + getSoundKey(var, plugin.getLangManager().getRaw("gui.item.ambient")), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(14, createItem(Material.JUKEBOX, plugin.getLangManager().getRaw("gui.item.son_de_dégâtsaggro"), plugin.getLangManager().getRaw("gui.item.actuel_2") + getSoundKey(var, plugin.getLangManager().getRaw("gui.item.aggro")), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(15, createItem(Material.JUKEBOX, plugin.getLangManager().getRaw("gui.item.son_de_mort"), plugin.getLangManager().getRaw("gui.item.actuel_2") + getSoundKey(var, plugin.getLangManager().getRaw("gui.item.death")), "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openVariantEquipmentEditor(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        StatModifiers mods = var.getModifiers();

        WDMenuHolder holder = new WDMenuHolder("VARIANT_EQUIPMENT_EDIT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_equipment") + ": " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createEquipmentSlotItem(mods.getHelmetItem(), mods.getHelmetChance(), "Casque", Material.LEATHER_HELMET));
        inv.setItem(11, createEquipmentSlotItem(mods.getChestplateItem(), mods.getChestplateChance(), "Plastron", Material.LEATHER_CHESTPLATE));
        inv.setItem(12, createEquipmentSlotItem(mods.getLeggingsItem(), mods.getLeggingsChance(), "Jambières", Material.LEATHER_LEGGINGS));
        inv.setItem(13, createEquipmentSlotItem(mods.getBootsItem(), mods.getBootsChance(), "Bottes", Material.LEATHER_BOOTS));
        inv.setItem(14, createEquipmentSlotItem(mods.getMainHandItem(), mods.getMainHandChance(), "Main Principale", Material.IRON_SWORD));
        inv.setItem(15, createEquipmentSlotItem(mods.getOffHandItem(), mods.getOffHandChance(), "Main Secondaire", Material.SHIELD));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    private ItemStack createEquipmentSlotItem(String item, double chance, String label, Material def) {
        Material mat = Material.matchMaterial(item.toUpperCase());
        if (mat == null || mat == Material.AIR) mat = def;
        return createItem(mat, plugin.getLangManager().getRaw("empty_6") + label + plugin.getLangManager().getRaw("empty_7") + item,
                plugin.getLangManager().getRaw("gui.item.chance_dobtention") + String.format(plugin.getLangManager().getRaw("gui.item.2f"), chance),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_choisir_lobjet"),
                plugin.getLangManager().getRaw("gui.item.clic_droit_chance_01_shiftclic"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_chance_01_shiftclic"));
    }

    public void openEquipmentItemSelector(Player player, String variantId, String slotName) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_EQ_" + slotName.toUpperCase(), variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_choose") + ": " + slotName));
        holder.setInventory(inv);

        String[] choices = switch (slotName.toLowerCase()) {
            case "casque" -> new String[]{"LEATHER_HELMET", "CHAINMAIL_HELMET", "IRON_HELMET", "GOLDEN_HELMET", "DIAMOND_HELMET", "NETHERITE_HELMET", "TURTLE_HELMET", "NONE"};
            case "plastron" -> new String[]{"LEATHER_CHESTPLATE", "CHAINMAIL_CHESTPLATE", "IRON_CHESTPLATE", "GOLDEN_CHESTPLATE", "DIAMOND_CHESTPLATE", "NETHERITE_CHESTPLATE", "NONE"};
            case "jambières" -> new String[]{"LEATHER_LEGGINGS", "CHAINMAIL_LEGGINGS", "IRON_LEGGINGS", "GOLDEN_LEGGINGS", "DIAMOND_LEGGINGS", "NETHERITE_LEGGINGS", "NONE"};
            case "bottes" -> new String[]{"LEATHER_BOOTS", "CHAINMAIL_BOOTS", "IRON_BOOTS", "GOLDEN_BOOTS", "DIAMOND_BOOTS", "NETHERITE_BOOTS", "NONE"};
            default -> new String[]{"WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "DIAMOND_SWORD", "NETHERITE_SWORD", "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLDEN_AXE", "DIAMOND_AXE", "NETHERITE_AXE", "BOW", "CROSSBOW", "SHIELD", "TRIDENT", "MACE", "WIND_CHARGE", "NONE"};
        };

        int slot = 0;
        for (String c : choices) {
            Material mat = c.equals("NONE") ? Material.BARRIER : Material.matchMaterial(c);
            if (mat == null) continue;
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + c, plugin.getLangManager().getRaw("gui.item.sélectionner_cet_objet"), plugin.getLangManager().getRaw("gui.item.ou_cliquez_sur_un_objet"), plugin.getLangManager().getRaw("gui.item.inventaire_pour_le_choisir")));
        }

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    private String getSoundKey(MobVariant var, String type) {
        MobVariant.SoundConfig sc = var.getCustomSounds().get(type);
        return (sc != null) ? sc.getSoundKey() : "none";
    }

    // ================= SELECTORS (100% Cliquable sans tchat) =================
    
    // 1. Particle selector
    public void openParticleSelector(Player player, String variantId, String fieldName) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_PARTICLE_" + fieldName.toUpperCase(), variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text(plugin.getLangManager().getRaw("gui.title_particle") + ": " + fieldName));
        holder.setInventory(inv);

        String[] particles = {
            "FLAME", "SOUL_FIRE_FLAME", "HEART", "WITCH", "PORTAL", 
            "HAPPY_VILLAGER", "CLOUD", "SMOKE", "CRIT", "REVERSE_PORTAL", 
            "SOUL", "NONE"
        };

        int slot = 0;
        for (String p : particles) {
            Material mat = p.equals("NONE") ? Material.BARRIER : Material.GLOWSTONE_DUST;
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_6") + p, plugin.getLangManager().getRaw("gui.item.sélectionner_cette_particule")));
        }
        inv.setItem(17, createBackItem());
        player.openInventory(inv);
    }

    // 2. BossBar Color selector
    public void openBossBarColorSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_BB_COLOR", variantId);
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text(plugin.getLangManager().getRaw("gui.title_bossbar_color")));
        holder.setInventory(inv);

        String[] colors = { "BLUE", "GREEN", "PINK", "PURPLE", "RED", "WHITE", "YELLOW" };
        int slot = 0;
        for (String c : colors) {
            Material mat = switch (c) {
                case "BLUE" -> Material.BLUE_DYE;
                case "GREEN" -> Material.GREEN_DYE;
                case "PINK" -> Material.PINK_DYE;
                case "PURPLE" -> Material.PURPLE_DYE;
                case "RED" -> Material.RED_DYE;
                case "WHITE" -> Material.WHITE_DYE;
                default -> Material.YELLOW_DYE;
            };
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + c, plugin.getLangManager().getRaw("gui.item.sélectionner_cette_couleur")));
        }
        inv.setItem(8, createBackItem());
        player.openInventory(inv);
    }

    // 3. BossBar Style selector
    public void openBossBarStyleSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_BB_STYLE", variantId);
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text(plugin.getLangManager().getRaw("gui.title_bossbar_style")));
        holder.setInventory(inv);

        String[] styles = { "PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20" };
        int slot = 0;
        for (String s : styles) {
            inv.setItem(slot++, createItem(Material.ANVIL, plugin.getLangManager().getRaw("empty_4") + s, plugin.getLangManager().getRaw("gui.item.sélectionner_ce_style")));
        }
        inv.setItem(8, createBackItem());
        player.openInventory(inv);
    }

    // 4. Potion Effects Selector
    public void openPotionSelector(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        List<String> activeEffects = var.getModifiers().getPotionEffects();

        WDMenuHolder holder = new WDMenuHolder("SELECT_POTIONS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text(plugin.getLangManager().getRaw("gui.title_potion_effects")));
        holder.setInventory(inv);

        String[] effects = { "SPEED", "INCREASE_DAMAGE", "DAMAGE_RESISTANCE", "FIRE_RESISTANCE", "REGENERATION", "INVISIBILITY", "GLOWING" };
        int slot = 0;
        for (String eff : effects) {
            boolean active = activeEffects.contains(eff.toUpperCase());
            inv.setItem(slot++, createItem(active ? Material.GLOWSTONE_DUST : Material.REDSTONE, 
                    plugin.getLangManager().getRaw("empty_4") + eff, 
                    plugin.getLangManager().getRaw("gui.item.statut") + (active ? plugin.getLangManager().getRaw("gui.item.activé") : plugin.getLangManager().getRaw("gui.item.désactivé_1")),
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer")));
        }
        inv.setItem(17, createBackItem());
        player.openInventory(inv);
    }

    public void openOnHitPotionSelector(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        String activeEffect = var.getModifiers().getOnHitPotionEffect();

        WDMenuHolder holder = new WDMenuHolder("SELECT_ONHIT_POTION", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text(plugin.getLangManager().getRaw("gui.title_onhit_effect")));
        holder.setInventory(inv);

        String[] effects = { "POISON", "WITHER", "BLINDNESS", "SLOW", "WEAKNESS", "CONFUSION", "LEVITATION", "none" };
        int slot = 0;
        for (String eff : effects) {
            boolean active = activeEffect != null && activeEffect.equalsIgnoreCase(eff);
            Material mat = eff.equals("none") ? Material.BARRIER : Material.GLOWSTONE_DUST;
            inv.setItem(slot++, createItem(mat, 
                    active ? plugin.getLangManager().getRaw("gui.item.actif") + eff : "&7" + eff, 
                    plugin.getLangManager().getRaw("gui.item.statut") + (active ? plugin.getLangManager().getRaw("gui.item.sélectionné_1") : plugin.getLangManager().getRaw("gui.item.désactivé_1")),
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_choisir")));
        }
        inv.setItem(17, createBackItem());
        player.openInventory(inv);
    }

    // 5. Sound Selector
    public void openSoundSelector(Player player, String variantId, String soundType) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_SOUND_" + soundType.toUpperCase(), variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text(plugin.getLangManager().getRaw("gui.title_sound") + ": " + soundType));
        holder.setInventory(inv);

        String[] sounds = {
            "entity.zombie.ambient", "entity.zombie.hurt", "entity.zombie.death",
            "entity.skeleton.ambient", "entity.skeleton.hurt", "entity.skeleton.death",
            "entity.creeper.primed", "entity.creeper.hurt", "entity.creeper.death",
            "entity.enderman.ambient", "entity.enderman.teleport",
            "entity.phantom.ambient", "entity.blaze.ambient",
            "entity.ender_dragon.growl", "none"
        };

        int slot = 0;
        for (String s : sounds) {
            Material mat = s.equals("none") ? Material.BARRIER : Material.JUKEBOX;
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_8") + s, plugin.getLangManager().getRaw("gui.item.sélectionner_ce_son")));
        }
        inv.setItem(17, createBackItem());
        player.openInventory(inv);
    }

    // 6. Equipment Tier Selector
    public void openEquipmentTierSelector(Player player, String contextId, String contextType) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_EQUIP_" + contextType.toUpperCase(), contextId);
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text(plugin.getLangManager().getRaw("gui.title_equipment_tier")));
        holder.setInventory(inv);

        String[] tiers = { "none", "leather", "chainmail", "iron", "gold", "diamond", "netherite" };
        int slot = 0;
        for (String t : tiers) {
            Material mat = switch (t) {
                case "leather" -> Material.LEATHER_CHESTPLATE;
                case "chainmail" -> Material.CHAINMAIL_CHESTPLATE;
                case "iron" -> Material.IRON_CHESTPLATE;
                case "gold" -> Material.GOLDEN_CHESTPLATE;
                case "diamond" -> Material.DIAMOND_CHESTPLATE;
                case "netherite" -> Material.NETHERITE_CHESTPLATE;
                default -> Material.BARRIER;
            };
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + t, plugin.getLangManager().getRaw("gui.item.sélectionner_ce_tier")));
        }
        inv.setItem(8, createBackItem());
        player.openInventory(inv);
    }

    // 7. Conditional Names List Editor
    public void openVariantConditionalNames(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("CONDITIONAL_NAMES", variantId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_conditional_names")));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant.ConditionalName cn : var.getConditionalNames()) {
            if (slot >= 45) break;
            int pct = (int) Math.round(cn.getThreshold() * 100);
            inv.setItem(slot++, createItem(Material.WRITABLE_BOOK, plugin.getLangManager().getRaw("gui.item.sous") + pct + plugin.getLangManager().getRaw("gui.item.de_vie"), 
                    plugin.getLangManager().getRaw("gui.item.nom") + cn.getName(), 
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_supprimer")));
        }

        inv.setItem(49, createItem(Material.EMERALD, plugin.getLangManager().getRaw("gui.item.ajouter_un_nom"), plugin.getLangManager().getRaw("gui.item.ajouter_un_nom_conditionnel")));
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    // 8. Custom Drops List Editor
    public void openVariantDrops(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("CUSTOM_DROPS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_custom_drops")));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant.CustomDrop cd : var.getCustomDrops()) {
            if (slot >= 45) break;
            Material mat = Material.matchMaterial(cd.getMaterialName());
            if (mat == null) mat = Material.PAPER;
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_8") + cd.getMaterialName(), 
                    plugin.getLangManager().getRaw("gui.item.chance") + (cd.getChance()*100) + "%", 
                    plugin.getLangManager().getRaw("gui.item.quantité_1") + cd.getMinAmount() + " - " + cd.getMaxAmount(), 
                    plugin.getLangManager().getRaw("gui.item.xp_1") + cd.getXp(),
                    plugin.getLangManager().getRaw("gui.item.condition") + cd.getDeathCondition(),
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_supprimer")));
        }

        inv.setItem(49, createItem(Material.EMERALD, plugin.getLangManager().getRaw("gui.item.ajouter_un_drop"), plugin.getLangManager().getRaw("gui.item.ajouter_un_drop_par_liste")));
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    // 9. Choice of item to drop
    public void openDropMaterialSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_DROP_MAT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text(plugin.getLangManager().getRaw("gui.title_choose_loot_item")));
        holder.setInventory(inv);

        Material[] drops = {
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT, 
            Material.NETHERITE_SCRAP, Material.ARROW, Material.COAL, Material.BONE, 
            Material.ROTTEN_FLESH, Material.STRING, Material.GUNPOWDER, Material.ENDER_PEARL
        };

        int slot = 0;
        for (Material m : drops) {
            inv.setItem(slot++, createItem(m, plugin.getLangManager().getRaw("empty_8") + m.name(), plugin.getLangManager().getRaw("gui.item.sélectionner_cet_objet")));
        }
        inv.setItem(17, createBackItem());
        player.openInventory(inv);
    }

    public Material getSquadMemberMaterial(String memberKey) {
        MobVariant var = plugin.getVariantManager().getVariant(memberKey);
        if (var != null) {
            return getVariantHeadMaterial(var.getType());
        }
        try {
            EntityType type = EntityType.valueOf(memberKey.toUpperCase());
            return getVariantHeadMaterial(type);
        } catch (Exception ignored) {}
        return Material.SKELETON_SKULL;
    }

    // ================= SQUADS =================
    public void openSquadList(Player player) {
        WDMenuHolder holder = new WDMenuHolder("SQUAD_LIST", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_squads")));
        holder.setInventory(inv);

        int slot = 0;
        for (MobSquad sq : plugin.getVariantManager().getAllSquads()) {
            if (slot >= 45) break;

            // Trouver le membre majoritaire
            String majorityMember = null;
            double maxAvg = -1.0;
            for (Map.Entry<String, MobSquad.SquadMemberRange> entry : sq.getMembers().entrySet()) {
                double avg = (entry.getValue().getMin() + entry.getValue().getMax()) / 2.0;
                if (avg > maxAvg) {
                    maxAvg = avg;
                    majorityMember = entry.getKey();
                }
            }
            Material mat = (majorityMember != null) ? getSquadMemberMaterial(majorityMember) : Material.SKELETON_SKULL;

            // Construire la description avec les principaux membres
            List<String> lore = new ArrayList<>();
            lore.add("&7Chance: &a" + String.format(java.util.Locale.US, "%.1f", sq.getSpawnChance() * 100) + "%");
            lore.add("&7Membres (" + sq.getMembers().size() + ") :");
            for (Map.Entry<String, MobSquad.SquadMemberRange> entry : sq.getMembers().entrySet()) {
                lore.add("  &e- " + entry.getKey() + " &7(Min: " + entry.getValue().getMin() + ", Max: " + entry.getValue().getMax() + ")");
            }
            lore.add("");
            lore.add("&eClic pour éditer");

            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + sq.getId(), lore.toArray(new String[0])));
        }
        
        inv.setItem(49, createItem(Material.EMERALD, plugin.getLangManager().getRaw("gui.item.nouvelle_escouade"), plugin.getLangManager().getRaw("gui.item.créer_une_nouvelle_escouade")));
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openSquadEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_edit") + ": " + squadId));
        holder.setInventory(inv);

        // Chance de spawn avec +/- clics
        inv.setItem(10, createItem(Material.GOLD_NUGGET, plugin.getLangManager().getRaw("gui.item.chance_de_spawn"), 
                plugin.getLangManager().getRaw("gui.item.actuelle_2") + String.format(plugin.getLangManager().getRaw("gui.item.1f"), sq.getSpawnChance() * 100) + "%",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_10_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_100"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_100"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis")));

        inv.setItem(13, createItem(Material.ZOMBIE_HEAD, plugin.getLangManager().getRaw("gui.item.membres_de_lescouade"), plugin.getLangManager().getRaw("gui.item.gérer_les_variantes_membres"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_gérer")));
        inv.setItem(14, createItem(Material.NETHER_STAR, plugin.getLangManager().getRaw("gui.item.bonus_descouade"), 
                plugin.getLangManager().getRaw("gui.item.gérer_les_multiplicateurs_de_pv"),
                plugin.getLangManager().getRaw("gui.item.dégâts_vitesse_et_regen"),
                plugin.getLangManager().getRaw("gui.item.pour_tous_les_membres"),
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_pour_gérer")));
        inv.setItem(15, createItem(Material.PAPER, plugin.getLangManager().getRaw("gui.item.types_déclencheurs"), plugin.getLangManager().getRaw("gui.item.gérer_les_entités_déclencheurs"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_gérer")));
        inv.setItem(16, createItem(Material.SKELETON_SPAWN_EGG, plugin.getLangManager().getRaw("gui.item.faire_apparaître"), plugin.getLangManager().getRaw("gui.item.invoque_cette_escouade_devant_vous")));

        inv.setItem(22, createItem(Material.LAVA_BUCKET, plugin.getLangManager().getRaw("gui.item.supprimer_lescouade"), plugin.getLangManager().getRaw("gui.item.attention_action_immédiate")));
        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openSquadBonusesEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_BONUSES_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_bonus") + ": " + squadId));
        holder.setInventory(inv);

        inv.setItem(10, createModifierItem(Material.APPLE, plugin.getLangManager().getRaw("gui.item.pv_sante"), sq.getBonusHealth()));
        inv.setItem(11, createModifierItem(Material.IRON_SWORD, plugin.getLangManager().getRaw("gui.item.degats"), sq.getBonusDamage()));
        inv.setItem(12, createModifierItem(Material.FEATHER, plugin.getLangManager().getRaw("gui.item.vitesse"), sq.getBonusSpeed()));
        inv.setItem(13, createModifierItem(Material.GOLDEN_APPLE, plugin.getLangManager().getRaw("gui.item.regen"), sq.getBonusRegen()));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    // New Menu to Edit Squad Members in GUI
    public void openSquadMembersEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_MEMBERS_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_members") + ": " + squadId));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            Material mat = getVariantHeadMaterial(var.getType());
            MobSquad.SquadMemberRange range = sq.getMembers().get(var.getId());
            if (range != null) {
                inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_9") + var.getId(), 
                        plugin.getLangManager().getRaw("gui.item.statut_membre"),
                        plugin.getLangManager().getRaw("gui.item.min") + range.getMin() + plugin.getLangManager().getRaw("gui.item.max") + range.getMax(),
                        "",
                        plugin.getLangManager().getRaw("gui.item.clic_gauche_min_1_clic"),
                        plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_max_1"),
                        plugin.getLangManager().getRaw("gui.item.shiftclic_droit_max_1"),
                        plugin.getLangManager().getRaw("gui.item.clic_milieu_retirer_de_lescouade")));
            } else {
                inv.setItem(slot++, createItem(Material.BARRIER, plugin.getLangManager().getRaw("empty_10") + var.getId(), 
                        plugin.getLangManager().getRaw("gui.item.statut_non_membre"),
                        "",
                        plugin.getLangManager().getRaw("gui.item.clic_pour_ajouter_à_lescouade")));
            }
        }

        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    // New Menu to Edit Squad Trigger EntityTypes in GUI
    public void openSquadTriggersEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_TRIGGERS_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_triggers") + ": " + squadId));
        holder.setInventory(inv);

        EntityType[] triggerChoices = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
            EntityType.CAVE_SPIDER, EntityType.WITHER_SKELETON, EntityType.PIGLIN, EntityType.HUSK,
            EntityType.DROWNED, EntityType.STRAY, EntityType.WITCH, EntityType.EVOKER
        };

        int slot = 0;
        for (EntityType type : triggerChoices) {
            boolean active = sq.getTriggerTypes().contains(type.name());
            Material mat = getVariantHeadMaterial(type);
            inv.setItem(slot++, createItem(active ? mat : Material.REDSTONE, 
                    plugin.getLangManager().getRaw("empty_4") + type.name(), 
                    plugin.getLangManager().getRaw("gui.item.statut") + (active ? plugin.getLangManager().getRaw("gui.item.activé") : plugin.getLangManager().getRaw("gui.item.désactivé_1")),
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer")));
        }

        inv.setItem(35, createBackItem());
        player.openInventory(inv);
    }

    // ================= ZONES =================
    public void openZoneList(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ZONE_LIST", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_zones")));
        holder.setInventory(inv);

        int slot = 0;
        for (DifficultyZone zone : plugin.getZoneManager().getAllZones()) {
            if (slot >= 45) break;
            inv.setItem(slot++, createItem(Material.MAP, plugin.getLangManager().getRaw("empty_4") + zone.getId(),
                    plugin.getLangManager().getRaw("gui.item.type") + zone.getType().name(),
                    plugin.getLangManager().getRaw("gui.item.priorité") + zone.getPriority(),
                    plugin.getLangManager().getRaw("gui.item.monde") + zone.getWorld(),
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_éditer")));
        }

        inv.setItem(48, createItem(Material.GOLDEN_HOE, plugin.getLangManager().getRaw("gui.item.ouvrir_loutil_de_création_houe"), plugin.getLangManager().getRaw("gui.item.obtenir_loutil_de_zone_en"), plugin.getLangManager().getRaw("gui.item.dessiner_les_points_du_polygone"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_obtenir")));
        inv.setItem(49, createItem(Material.EMERALD, plugin.getLangManager().getRaw("gui.item.nouvelle_zone"), plugin.getLangManager().getRaw("gui.item.créer_une_nouvelle_zone_de")));
        inv.setItem(51, createToggleItem(Material.GLOWSTONE_DUST, plugin.getLangManager().getRaw("gui.item.particules_bordure_zone"), plugin.getMainConfigManager().isZoneBorderParticles()));
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneEditor(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_EDIT", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 45, Component.text(plugin.getLangManager().getRaw("gui.title_zone") + ": " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.SHIELD, plugin.getLangManager().getRaw("gui.item.zone_sûre_safe_zone"), zone.isSafeZone()));
        inv.setItem(11, createToggleItem(Material.DAYLIGHT_DETECTOR, plugin.getLangManager().getRaw("gui.item.ignorer_règles_biome"), zone.isOverrideBiomeRules()));
        
        // Priorité avec +/- clics
        inv.setItem(12, createItem(Material.ANVIL, plugin.getLangManager().getRaw("gui.item.priorité_1"), 
                plugin.getLangManager().getRaw("gui.item.actuelle_2") + zone.getPriority(),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_5"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_5"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis")));

        // Type de Forme (slot 13)
        inv.setItem(13, createItem(Material.MAP, plugin.getLangManager().getRaw("gui.item.type_de_forme"), 
                plugin.getLangManager().getRaw("gui.item.type_actuel") + zone.getType().name(), 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_pour_changer_cuboid_radius")));

        inv.setItem(14, createItem(Material.COMPASS, plugin.getLangManager().getRaw("gui.item.définir_pos_1_ici"), plugin.getLangManager().getRaw("gui.item.assigne_la_position_1_de"), plugin.getLangManager().getRaw("gui.item.à_votre_position_actuelle"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_définir")));
        inv.setItem(15, createItem(Material.COMPASS, plugin.getLangManager().getRaw("gui.item.définir_pos_2_ici"), plugin.getLangManager().getRaw("gui.item.assigne_la_position_2_de"), plugin.getLangManager().getRaw("gui.item.à_votre_position_actuelle"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_définir")));

        String tpType = zone.hasCustomTeleport() ? "point de TP personnalisé" : "point central calculé";
        inv.setItem(16, createItem(Material.ENDER_PEARL, plugin.getLangManager().getRaw("gui.item.se_téléporter_à_la_zone"), 
                plugin.getLangManager().getRaw("gui.item.téléporte_instantanément_au"), 
                "&7" + tpType + plugin.getLangManager().getRaw("gui.item.de_la_zone"), 
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_se_téléporter")));
        inv.setItem(17, createToggleItem(Material.IRON_DOOR, plugin.getLangManager().getRaw("gui.item.traverser_zone_mobs"), zone.isMobsCanCross()));
        
        // Scaling extérieur par stat (slot 18) — ouvre une page dédiée
        boolean hasAnyScaling = zone.hasExtScaling();
        String scalingBadge = hasAnyScaling ? "§a✔ Configuré" : "§7✖ Non configuré";
        inv.setItem(18, createItem(Material.COMPARATOR, plugin.getLangManager().getRaw("gui.item.scaling_extérieur_par_stat"),
                plugin.getLangManager().getRaw("gui.item.augmentation_de_difficulté_par_tranche"),
                plugin.getLangManager().getRaw("gui.item.de_blocs_audelà_des_limites"),
                "",
                plugin.getLangManager().getRaw("gui.item.statut_1") + scalingBadge,
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_configurer_1")));

        inv.setItem(19, createItem(Material.GLOWSTONE_DUST, plugin.getLangManager().getRaw("gui.item.hauteur_particules_bordure"), 
                plugin.getLangManager().getRaw("gui.item.hauteur_relative_au_sol") + String.format(plugin.getLangManager().getRaw("gui.item.2f"), zone.getParticleHeightOffset()) + plugin.getLangManager().getRaw("gui.item.blocs"), 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_gauche_005_clic_droit"), 
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis")));
                
        inv.setItem(22, createItem(Material.COMPASS, plugin.getLangManager().getRaw("gui.item.définir_point_de_tp_ici"), 
                plugin.getLangManager().getRaw("gui.item.assigne_le_point_de_téléportation"), 
                plugin.getLangManager().getRaw("gui.item.de_cette_zone_à_vos"), 
                plugin.getLangManager().getRaw("gui.item.actuel_4") + (zone.hasCustomTeleport() ? String.format(plugin.getLangManager().getRaw("gui.item.1f_1f_1f"), zone.getTeleportX(), zone.getTeleportY(), zone.getTeleportZ()) : plugin.getLangManager().getRaw("gui.item.calculé_centre")), 
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_définir")));

        inv.setItem(23, createItem(Material.GOLDEN_HOE, plugin.getLangManager().getRaw("gui.item.ouvrir_loutil_de_création_houe"), 
                plugin.getLangManager().getRaw("gui.item.obtenir_loutil_de_zone_en"), 
                plugin.getLangManager().getRaw("gui.item.dessiner_la_forme_en_jeu"),
                plugin.getLangManager().getRaw("gui.item.cuboid_clic_droit_pos_1"),
                plugin.getLangManager().getRaw("gui.item.radius_clic_gauche_centre_clic"),
                plugin.getLangManager().getRaw("gui.item.polygon_clic_droit_ajouter_point"),
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_obtenir")));

        inv.setItem(20, createItem(Material.DIAMOND_SWORD, plugin.getLangManager().getRaw("gui.item.modifier_stats_absolues"), plugin.getLangManager().getRaw("gui.item.pv_dégâts_vitesse_knockback_portée")));

        // Particules de zone (slots 21, 24, 25)
        inv.setItem(21, createToggleItem(Material.GLOWSTONE_DUST, plugin.getLangManager().getRaw("gui.item.activer_particules_bordure"), zone.isParticlesEnabled()));
        inv.setItem(24, createItem(Material.WRITABLE_BOOK, plugin.getLangManager().getRaw("gui.item.particule_quand_on_rentre"), 
                plugin.getLangManager().getRaw("gui.item.actuelle_3") + zone.getParticleInside(), 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_pour_la_modifier")));
        inv.setItem(25, createItem(Material.WRITABLE_BOOK, plugin.getLangManager().getRaw("gui.item.particule_quand_on_sort"), 
                plugin.getLangManager().getRaw("gui.item.actuelle_3") + zone.getParticleOutside(), 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_pour_la_modifier")));
        
        // Nouveaux boutons : Membres, Beacon Effects, Nid d'ennemis, Sous-sections (slots 26, 27, 28, 29)
        inv.setItem(26, createItem(Material.PLAYER_HEAD, plugin.getLangManager().getRaw("gui.item.gestion_des_membres"), plugin.getLangManager().getRaw("gui.item.membres_actuels") + zone.getMembers().size(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_gérer_les_membres")));
        inv.setItem(27, createItem(Material.BEACON, plugin.getLangManager().getRaw("gui.item.effets_de_beacon"), plugin.getLangManager().getRaw("gui.item.effets_actifs") + zone.getBeaconEffects().size(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_configurer_les_effets")));
        
        String nestBadge = zone.isDangerNest() ? "§c✔ Actif" : "§7✖ Inactif";
        inv.setItem(28, createItem(Material.NETHER_STAR, plugin.getLangManager().getRaw("gui.item.nid_dennemis_danger_nest"), plugin.getLangManager().getRaw("gui.item.statut_1") + nestBadge, "", plugin.getLangManager().getRaw("gui.item.clic_pour_configurer_le_mode")));
        inv.setItem(29, createItem(Material.MAP, plugin.getLangManager().getRaw("gui.item.soussections_multisections"), plugin.getLangManager().getRaw("gui.item.sections_additionnelles") + zone.getSubSections().size(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_fusionner_et_étendre")));

        // Nouveaux boutons v1.1 Encounter Engine, WorldGuard & Iris (slots 30, 31, 32)
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();
        String encStatus = (enc != null && enc.getType() != fr.wilddifficulty.encounter.EncounterType.NONE && enc.isEnabled()) ? "§a✔ " + enc.getType().getDisplayName() : "§7✖ Aucun";
        inv.setItem(30, createItem(Material.TARGET, "&6⚔ Configuration d'Encounter (v1.1)",
                "&7Type actuel : " + encStatus,
                "&7Gère les raids, trial bunkers, avant-postes et ruines.",
                "",
                "&e➜ Clic pour ouvrir l'éditeur d'Encounter"));

        String wgStatus = zone.isWorldGuardLinked() ? "§a✔ " + zone.getWorldGuardRegion() : "§7✖ Géométrie interne";
        inv.setItem(31, createItem(Material.SHIELD, "&9🛡 Liaison Région WorldGuard",
                "&7Région liée : " + wgStatus,
                "&7Utilise les limites d'une région WorldGuard",
                "&7pour éviter les calculs de zone en doublon.",
                "",
                "&e➜ Clic pour sélectionner une région WorldGuard"));

        String irisStatus = plugin.getIrisHook().isAvailable() ? "§a✔ Prêt" : "§7✖ Non détecté";
        inv.setItem(32, createItem(Material.GRASS_BLOCK, "&2🌲 Import Structure Iris",
                "&7Statut Iris : " + irisStatus,
                "&7Détecte la structure ou le biome Iris à votre position",
                "&7pour centrer la zone d'Encounter.",
                "",
                "&e➜ Clic pour importer depuis votre position"));

        inv.setItem(40, createItem(Material.LAVA_BUCKET, plugin.getLangManager().getRaw("gui.item.supprimer_la_zone"), plugin.getLangManager().getRaw("gui.item.attention_action_immédiate")));
        inv.setItem(44, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneEncounterMenu(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();
        if (enc == null) {
            enc = new fr.wilddifficulty.encounter.EncounterConfig();
            zone.setEncounterConfig(enc);
        }

        WDMenuHolder holder = new WDMenuHolder("ZONE_ENCOUNTER_EDIT", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("⚔ Encounter: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.REDSTONE_TORCH, "&eActiver l'Encounter", enc.isEnabled()));
        inv.setItem(11, createItem(Material.BEACON, "&6Type d'Encounter",
                "&7Type actuel : &e" + enc.getType().getDisplayName(),
                "",
                "&7Types disponibles :",
                "&8- &fBASE_RAID &7(Invasion de camp / Raid)",
                "&8- &fTRIAL_BUNKER &7(Trial Chamber 14 blocs)",
                "&8- &fOUTPOST &7(Avant-poste & patrouilles)",
                "&8- &fRUINS &7(Ruines & gardiens anciens)",
                "",
                "&e➜ Clic pour changer de type"));

        inv.setItem(12, createItem(Material.CLOCK, "&eTemps de Recharge (Cooldown)",
                "&7Durée actuelle : &a" + (enc.getCooldownSeconds() / 60) + " min &7(" + enc.getCooldownSeconds() + "s)",
                "",
                "&7Clic gauche : &a+1 min",
                "&7Clic droit : &c-1 min",
                "&7Shift-Clic : &a±5 min",
                "&7Clic milieu : Définir via le tchat"));

        inv.setItem(13, createItem(Material.PLAYER_HEAD, "&bJoueurs Requis",
                "&7Min joueurs : &e" + enc.getMinPlayers(),
                "&7Max joueurs : &e" + enc.getMaxPlayers(),
                "",
                "&7Clic gauche : &a+1 Min",
                "&7Clic droit : &c-1 Min",
                "&7Shift-Clic gauche : &a+1 Max",
                "&7Shift-Clic droit : &c-1 Max"));

        inv.setItem(14, createItem(Material.ZOMBIE_HEAD, "&c⚔ Configuration des Vagues",
                "&7Nombre de vagues configurées : &e" + enc.getWaves().size(),
                "&7Personnalisez les variantes et escouades de chaque vague.",
                "",
                "&e➜ Clic pour éditer les vagues"));

        inv.setItem(15, createItem(Material.CHEST, "&6🎁 Récompenses de Victoire",
                "&7XP : &e" + enc.getRewards().getXpAmount() + " XP",
                "&7Items configurés : &e" + enc.getRewards().getItems().size(),
                "&7Commandes : &e" + enc.getRewards().getConsoleCommands().size(),
                "",
                "&e➜ Clic pour configurer les récompenses"));

        // Options spécifiques selon le type d'Encounter
        if (enc.getType() == fr.wilddifficulty.encounter.EncounterType.BASE_RAID) {
            inv.setItem(16, createItem(Material.CROSSBOW, "&c⚔ Mode de Raid",
                    "&7Mode actuel : &e" + enc.getRaidMode(),
                    "&8- &fCUSTOM_RAID_MODE &7: Vagues simulées",
                    "&8- &fVANILLA_RAID_MODE &7: Bad Omen & Raid naturel",
                    "",
                    "&e➜ Clic pour basculer le mode"));
        } else if (enc.getType() == fr.wilddifficulty.encounter.EncounterType.TRIAL_BUNKER) {
            inv.setItem(16, createItem(Material.TRIAL_KEY, "&6⚔ Paramètres Trial Bunker",
                    "&7Rayon d'activation : &e" + enc.getTrialActivationRadius() + " blocs",
                    "&7Cap mobs simultanés : &e" + enc.getTrialMobCap(),
                    "&7Total mobs : &e" + enc.getTrialTotalMobs(),
                    "&7Scaling par joueur : &e+" + (int)(enc.getTrialScalingPerPlayer() * 100) + "%",
                    "",
                    "&e➜ Clic pour ajuster"));
        } else if (enc.getType() == fr.wilddifficulty.encounter.EncounterType.OUTPOST) {
            inv.setItem(16, createItem(Material.IRON_SWORD, "&4⚔ Paramètres Avant-Poste",
                    "&7Temps de présence avant invasion : &e" + (int) enc.getOutpostLingeringInvasionTimeSeconds() + "s",
                    "&7Chance de Capitaine : &e" + (int)(enc.getOutpostCaptainChance() * 100) + "%",
                    "",
                    "&e➜ Clic pour ajuster"));
        } else if (enc.getType() == fr.wilddifficulty.encounter.EncounterType.RUINS) {
            inv.setItem(16, createItem(Material.SCULK_SENSOR, "&3🏛 Paramètres Ruines Anciennes",
                    "&7Rayon de déclenchement : &e" + enc.getRuinsTriggerRadius() + " blocs",
                    "&7Déclenchement à l'ouverture de coffre : " + (enc.isRuinsTriggerOnChestOpen() ? "§aOui" : "§cNon"),
                    "",
                    "&e➜ Clic pour basculer"));
        }

        inv.setItem(19, createToggleItem(Material.DRAGON_HEAD, "&dBossBar de Progression", enc.isBossBarEnabled()));

        // Bouton de Test / Démarrage Forcé
        boolean isActive = plugin.getEncounterManager().isEncounterActive(zoneId);
        long cd = plugin.getEncounterManager().getRemainingCooldownSeconds(zoneId);
        String triggerLore = isActive ? "&cEncounter déjà en cours !" : (cd > 0 ? "&cEn recharge (" + cd + "s)" : "&aPrêt à démarrer");
        inv.setItem(22, createItem(Material.NETHER_STAR, "&a⚡ Déclencher l'Encounter (Test)",
                "&7Statut : " + triggerLore,
                "",
                "&e➜ Clic pour forcer le démarrage"));

        inv.setItem(31, createBackItem());
        player.openInventory(inv);
    }

    public void openEncounterWavesMenu(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();

        WDMenuHolder holder = new WDMenuHolder("ENCOUNTER_WAVES_EDIT", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("⚔ Vagues: " + zoneId));
        holder.setInventory(inv);

        List<fr.wilddifficulty.encounter.EncounterWave> waves = enc.getWaves();
        for (int i = 0; i < Math.min(waves.size(), 18); i++) {
            fr.wilddifficulty.encounter.EncounterWave w = waves.get(i);
            inv.setItem(i, createItem(Material.ZOMBIE_HEAD, "&c⚔ Vague #" + (i + 1),
                    "&7Délai : &e" + w.getDelaySeconds() + "s",
                    "&7Variantes : &f" + w.getVariantSpawns().size(),
                    "&7Escouades : &f" + w.getSquadSpawns().size(),
                    "&7Distribution : &e" + w.getSpawnDistribution(),
                    "",
                    "&e➜ Clic pour éditer cette vague",
                    "&c➜ Clic droit pour supprimer"));
        }

        // Arrêter l'encounter en cours (Slot 18)
        boolean isActive = plugin.getEncounterManager().isEncounterActive(zoneId);
        if (isActive) {
            inv.setItem(18, createItem(Material.RED_CONCRETE, "&c⏹ &lArrêter l'Encounter en cours",
                    "&7Un événement est actif dans cette zone.",
                    "&7Arrête le raid, nettoie les monstres restants",
                    "&7et réinitialise la BossBar.",
                    "",
                    "&c➜ Clic pour forcer l'arrêt"));
        } else {
            inv.setItem(18, createItem(Material.GRAY_DYE, "&7⏹ Arrêter l'Encounter",
                    "&7Aucun événement en cours dans cette zone."));
        }

        // Marqueurs de spawn (Slot 20)
        inv.setItem(20, createItem(Material.TARGET, "&e🎯 Marqueurs de Spawn",
                "&7Marqueurs définis : &e" + enc.getSpawnMarkers().size(),
                "&7Permet de définir des positions de spawn",
                "&7fixes pour le mode de distribution MARKERS.",
                "",
                "&e➜ Clic pour gérer les marqueurs"));

        // Ajouter une vague (Slot 22)
        inv.setItem(22, createItem(Material.EMERALD, "&a+ Ajouter une Vague", "&7Ajoute une nouvelle vague successive."));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openEncounterSpawnMarkersMenu(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();

        WDMenuHolder holder = new WDMenuHolder("ENCOUNTER_SPAWN_MARKERS", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("🎯 Marqueurs: " + zoneId));
        holder.setInventory(inv);

        fillBorders(inv);

        List<double[]> markers = enc.getSpawnMarkers();
        int[] availableSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(markers.size(), availableSlots.length); i++) {
            double[] m = markers.get(i);
            inv.setItem(availableSlots[i], createItem(Material.LODESTONE, "&e🎯 Marqueur #" + (i + 1),
                    "&7X: &f" + String.format("%.1f", m[0]) + " &7Y: &f" + String.format("%.1f", m[1]) + " &7Z: &f" + String.format("%.1f", m[2]),
                    "",
                    "&a➜ Clic gauche : Se téléporter",
                    "&c➜ Clic droit : Supprimer"));
        }

        // Ajouter ma position (Slot 29)
        inv.setItem(29, createItem(Material.EMERALD, "&a+ Ajouter ma position",
                "&7Enregistre vos coordonnées actuelles",
                "&7comme point d'apparition (Marqueur).",
                "",
                "&a➜ Clic pour ajouter"));

        // Retour (Slot 31)
        inv.setItem(31, createBackItem());

        // Tout supprimer (Slot 33)
        if (!markers.isEmpty()) {
            inv.setItem(33, createItem(Material.BARRIER, "&c✖ Tout supprimer",
                    "&7Supprime tous les marqueurs de spawn",
                    "&7enregistrés pour cette zone.",
                    "",
                    "&c➜ Clic pour tout effacer"));
        }

        player.openInventory(inv);
    }

    public void openConfirmDeleteMenu(Player player, String type, String id) {
        WDMenuHolder holder = new WDMenuHolder("CONFIRM_DELETE", type + ":" + id);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_confirm_delete")));
        holder.setInventory(inv);

        fillBorders(inv);

        // Confirmer (Slot 11)
        inv.setItem(11, createItem(Material.LIME_CONCRETE, plugin.getLangManager().getRaw("gui.confirm_delete.btn_confirm"),
                plugin.getLangManager().getRaw("gui.confirm_delete.btn_confirm_lore") + " &e" + id,
                "",
                plugin.getLangManager().getRaw("gui.confirm_delete.click_to_confirm")));

        // Info Avertissement (Slot 13)
        inv.setItem(13, createItem(Material.TNT, plugin.getLangManager().getRaw("gui.confirm_delete.info_title"),
                "&7Type : &f" + type,
                "&7Cible : &e" + id,
                "",
                plugin.getLangManager().getRaw("gui.confirm_delete.info_warning")));

        // Annuler (Slot 15)
        inv.setItem(15, createItem(Material.RED_CONCRETE, plugin.getLangManager().getRaw("gui.confirm_delete.btn_cancel"),
                plugin.getLangManager().getRaw("gui.confirm_delete.btn_cancel_lore"),
                "",
                plugin.getLangManager().getRaw("gui.confirm_delete.click_to_cancel")));

        player.openInventory(inv);
    }

    public void openEncounterSingleWaveMenu(Player player, String zoneId, int waveIndex) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();
        if (enc == null || waveIndex < 0 || waveIndex >= enc.getWaves().size()) return;

        fr.wilddifficulty.encounter.EncounterWave wave = enc.getWaves().get(waveIndex);
        WDMenuHolder holder = new WDMenuHolder("ENCOUNTER_SINGLE_WAVE_EDIT", zoneId + ":" + waveIndex);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("⚔ Vague #" + (waveIndex + 1) + " - " + zoneId));
        holder.setInventory(inv);

        fillBorders(inv);

        // Délai de vague (Slot 10)
        inv.setItem(10, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.wave.delay_title"),
                plugin.getLangManager().getRaw("gui.wave.delay_current") + " &e" + wave.getDelaySeconds() + "s",
                "",
                plugin.getLangManager().getRaw("gui.wave.delay_lore1"),
                plugin.getLangManager().getRaw("gui.wave.delay_lore2"),
                plugin.getLangManager().getRaw("gui.wave.delay_lore3")));

        // Variantes configurées (Slot 12)
        int totalVarCount = wave.getVariantSpawns().values().stream().mapToInt(Integer::intValue).sum();
        inv.setItem(12, createItem(Material.ZOMBIE_HEAD, plugin.getLangManager().getRaw("gui.wave.variants_title"),
                "&7Types configurés : &e" + wave.getVariantSpawns().size(),
                "&7Total monstres : &a" + totalVarCount,
                "",
                plugin.getLangManager().getRaw("gui.wave.variants_click_manage")));

        // Escouades configurées (Slot 14)
        int totalSquadCount = wave.getSquadSpawns().values().stream().mapToInt(Integer::intValue).sum();
        inv.setItem(14, createItem(Material.SKELETON_SKULL, plugin.getLangManager().getRaw("gui.wave.squads_title"),
                "&7Types configurés : &e" + wave.getSquadSpawns().size(),
                "&7Total escouades : &a" + totalSquadCount,
                "",
                plugin.getLangManager().getRaw("gui.wave.squads_click_manage")));

        // Distribution du spawn (Slot 16)
        inv.setItem(16, createItem(Material.COMPASS, plugin.getLangManager().getRaw("gui.wave.distribution_title"),
                "&7Mode actuel : &e" + wave.getSpawnDistribution(),
                "&8- &fAROUND_CENTER &7: Autour du centre",
                "&8- &fRANDOM_ZONE &7: Aléatoire dans la zone",
                "&8- &fMARKERS &7: Marqueurs fixes (" + enc.getSpawnMarkers().size() + " définis)",
                "",
                plugin.getLangManager().getRaw("gui.wave.distribution_click_cycle")));

        // Marqueurs de spawn raccourci (Slot 17)
        inv.setItem(17, createItem(Material.TARGET, "&e🎯 Marqueurs de Spawn",
                "&7Marqueurs définis : &e" + enc.getSpawnMarkers().size(),
                "",
                "&e➜ Clic pour gérer les marqueurs"));

        // Supprimer cette vague (Slot 22)
        inv.setItem(22, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.wave.delete_title"),
                plugin.getLangManager().getRaw("gui.wave.delete_lore"),
                "",
                plugin.getLangManager().getRaw("gui.wave.delete_click")));

        // Bouton Retour (Slot 31)
        inv.setItem(31, createBackItem());
        player.openInventory(inv);
    }

    public void openEncounterWaveVariantsMenu(Player player, String zoneId, int waveIndex, int page) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();
        if (enc == null || waveIndex < 0 || waveIndex >= enc.getWaves().size()) return;
        fr.wilddifficulty.encounter.EncounterWave wave = enc.getWaves().get(waveIndex);

        WDMenuHolder holder = new WDMenuHolder("ENCOUNTER_WAVE_VARIANTS", zoneId + ":" + waveIndex + ":" + page);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("⚔ Variantes: Vague #" + (waveIndex + 1)));
        holder.setInventory(inv);

        List<MobVariant> allVariants = new ArrayList<>(plugin.getVariantManager().getAllVariants());
        allVariants.sort(java.util.Comparator.comparing(MobVariant::getId));

        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) allVariants.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allVariants.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            MobVariant var = allVariants.get(i);
            int count = wave.getVariantSpawns().getOrDefault(var.getId(), 0);
            Material mat = getVariantHeadMaterial(var.getType());
            String title = (count > 0 ? "&a✔ &l" : "&7") + var.getId() + " &8(" + var.getType().name() + ")";
            String countStatus = count > 0 ? "&aQuantité : &e" + count : "&7Non inclus dans cette vague";

            inv.setItem(slot++, createItem(mat, title,
                    countStatus,
                    "",
                    "&7Clic gauche : &a+1",
                    "&7Shift-clic gauche : &a+5",
                    "&7Clic droit : &c-1",
                    "&7Shift-clic droit : &c-5",
                    "&7Clic milieu : Définir via le tchat"));
        }

        // Pagination
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_précédente") + page + ")"));
        }
        if (page < maxPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_suivante") + (page + 2) + ")"));
        }

        // Bouton Retour (Slot 49)
        inv.setItem(49, createBackItem());
        player.openInventory(inv);
    }

    public void openEncounterWaveSquadsMenu(Player player, String zoneId, int waveIndex, int page) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();
        if (enc == null || waveIndex < 0 || waveIndex >= enc.getWaves().size()) return;
        fr.wilddifficulty.encounter.EncounterWave wave = enc.getWaves().get(waveIndex);

        WDMenuHolder holder = new WDMenuHolder("ENCOUNTER_WAVE_SQUADS", zoneId + ":" + waveIndex + ":" + page);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("⚔ Escouades: Vague #" + (waveIndex + 1)));
        holder.setInventory(inv);

        List<MobSquad> allSquads = new ArrayList<>(plugin.getVariantManager().getAllSquads());
        allSquads.sort(java.util.Comparator.comparing(MobSquad::getId));

        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) allSquads.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allSquads.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            MobSquad sq = allSquads.get(i);
            int count = wave.getSquadSpawns().getOrDefault(sq.getId(), 0);
            String title = (count > 0 ? "&a✔ &l" : "&7") + sq.getId();
            String countStatus = count > 0 ? "&aNombre d'escouades : &e" + count : "&7Non inclus dans cette vague";

            inv.setItem(slot++, createItem(Material.SKELETON_SKULL, title,
                    countStatus,
                    "&7Membres par escouade : &f" + sq.getMembers().size(),
                    "",
                    "&7Clic gauche : &a+1",
                    "&7Shift-clic gauche : &a+5",
                    "&7Clic droit : &c-1",
                    "&7Shift-clic droit : &c-5",
                    "&7Clic milieu : Définir via le tchat"));
        }

        // Pagination
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_précédente") + page + ")"));
        }
        if (page < maxPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_suivante") + (page + 2) + ")"));
        }

        // Bouton Retour (Slot 49)
        inv.setItem(49, createBackItem());
        player.openInventory(inv);
    }

    public void openEncounterRewardsMenu(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        fr.wilddifficulty.encounter.EncounterConfig enc = zone.getEncounterConfig();
        fr.wilddifficulty.encounter.EncounterReward reward = enc.getRewards();

        WDMenuHolder holder = new WDMenuHolder("ENCOUNTER_REWARDS_EDIT", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("🎁 Récompenses: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createItem(Material.EXPERIENCE_BOTTLE, "&aExpérience (XP)",
                "&7Quantité : &e" + reward.getXpAmount() + " XP",
                "",
                "&7Clic gauche : &a+10 XP",
                "&7Clic droit : &c-10 XP",
                "&7Shift-Clic : &a±50 XP",
                "&7Clic milieu : Définir via le tchat"));

        inv.setItem(12, createItem(Material.CHEST, "&6Items de Récompense",
                "&7Nombre d'items configurés : &e" + reward.getItems().size(),
                "",
                "&e➜ Clic avec un item en main pour l'ajouter"));

        inv.setItem(14, createItem(Material.COMMAND_BLOCK, "&bCommandes Console",
                "&7Commandes exécutées : &e" + reward.getConsoleCommands().size(),
                "&7Placeholders : &f%player%",
                "",
                "&e➜ Clic pour ajouter une commande"));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openWorldGuardRegionSelect(Player player, String zoneId, int page) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        World world = Bukkit.getWorld(zone.getWorld());
        List<String> regions = plugin.getWorldGuardHook().getRegionsInWorld(world);

        WDMenuHolder holder = new WDMenuHolder("WG_REGION_SELECT", zoneId + ":" + page);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("🛡 Régions WorldGuard (" + (world != null ? world.getName() : "Monde") + ")"));
        holder.setInventory(inv);

        int perPage = 45;
        int startIndex = (page - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, regions.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String rName = regions.get(i);
            boolean isLinked = rName.equalsIgnoreCase(zone.getWorldGuardRegion());
            Material mat = isLinked ? Material.EMERALD_BLOCK : Material.IRON_BLOCK;
            String prefix = isLinked ? "&a✔ " : "&7";
            inv.setItem(slot++, createItem(mat, prefix + rName,
                    "&7Monde : &f" + (world != null ? world.getName() : ""),
                    isLinked ? "&aRégion actuellement liée à cette zone" : "&7Clic pour lier cette région",
                    "",
                    "&e➜ Clic pour sélectionner"));
        }

        if (zone.isWorldGuardLinked()) {
            inv.setItem(48, createItem(Material.BARRIER, "&cDissocier la région WorldGuard", "&7Revient au calcul géométrique interne."));
        }

        if (page > 1) {
            inv.setItem(45, createItem(Material.ARROW, "&e◀ Page Précédente"));
        }
        if (endIndex < regions.size()) {
            inv.setItem(53, createItem(Material.ARROW, "&ePage Suivante ▶"));
        }

        inv.setItem(49, createBackItem());
        player.openInventory(inv);
    }

    public void openIrisStructureImport(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        Location loc = player.getLocation();
        String biomeOrStruct = plugin.getIrisHook().getIrisBiomeOrStructure(loc);
        boolean isIris = plugin.getIrisHook().isIrisWorld(loc.getWorld());

        WDMenuHolder holder = new WDMenuHolder("IRIS_IMPORT", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("🌲 Import Iris: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(11, createItem(Material.COMPASS, "&2Position Actuelle",
                "&7Monde : &f" + loc.getWorld().getName(),
                "&7Générateur Iris : " + (isIris ? "§aOui" : "§eStandard/Inconnu"),
                "&7Biome/Structure : &b" + biomeOrStruct,
                "&7Coordonnées : &e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));

        inv.setItem(15, createItem(Material.EMERALD, "&aCentrer la Zone sur cette Structure",
                "&7Définit le centre de la zone d'Encounter",
                "&7sur votre position actuelle.",
                "",
                "&e➜ Clic pour appliquer"));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneParticleSelector(Player player, String zoneId, String type) {
        WDMenuHolder holder = new WDMenuHolder("ZONE_PARTICLE_" + type.toUpperCase(), zoneId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text(plugin.getLangManager().getRaw("gui.title_zone_particle") + " (" + type + ")"));
        holder.setInventory(inv);

        String[] particles = {
            "FLAME", "SOUL_FIRE_FLAME", "HEART", "WITCH", "PORTAL", 
            "HAPPY_VILLAGER", "CLOUD", "SMOKE", "CRIT", "REVERSE_PORTAL", 
            "SOUL", "COMPOSTER", "NONE"
        };

        int slot = 0;
        for (String p : particles) {
            Material mat = p.equals("NONE") ? Material.BARRIER : Material.GLOWSTONE_DUST;
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_6") + p, plugin.getLangManager().getRaw("gui.item.sélectionner_cette_particule")));
        }
        inv.setItem(17, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneModifiers(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;
        StatModifiers mods = zone.getModifiers();
        if (mods == null) {
            mods = new StatModifiers();
            zone.setModifiers(mods);
        }

        WDMenuHolder holder = new WDMenuHolder("ZONE_MODS", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_zone_stats") + ": " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createModifierItem(Material.APPLE, plugin.getLangManager().getRaw("gui.item.pv_sante"), mods.getHealthValue()));
        inv.setItem(11, createModifierItem(Material.IRON_SWORD, plugin.getLangManager().getRaw("gui.item.degats"), mods.getDamageValue()));
        inv.setItem(12, createModifierItem(Material.FEATHER, plugin.getLangManager().getRaw("gui.item.vitesse"), mods.getSpeedValue()));
        inv.setItem(13, createModifierItem(Material.ENDER_EYE, plugin.getLangManager().getRaw("gui.item.detection"), mods.getFollowRangeValue()));
        inv.setItem(14, createModifierItem(Material.SHIELD, plugin.getLangManager().getRaw("gui.item.knockback"), mods.getKnockbackValue()));

        inv.setItem(15, createItem(Material.IRON_CHESTPLATE, plugin.getLangManager().getRaw("gui.item.équipement_armure"), plugin.getLangManager().getRaw("gui.item.tier") + mods.getEquipmentTier(), plugin.getLangManager().getRaw("gui.item.chance") + (mods.getEquipmentChance()*100) + "%", "", plugin.getLangManager().getRaw("gui.item.clic_pour_changer")));
        inv.setItem(16, createModifierItem(Material.GOLDEN_APPLE, plugin.getLangManager().getRaw("gui.item.regen"), mods.getRegenerationValue()));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }


    private Material getVariantHeadMaterial(EntityType type) {
        try {
            Material egg = Material.valueOf(type.name() + "_SPAWN_EGG");
            if (egg != null) return egg;
        } catch (Exception ignored) {}

        return switch (type) {
            case ENDER_DRAGON -> Material.DRAGON_HEAD;
            case PLAYER -> Material.PLAYER_HEAD;
            case IRON_GOLEM -> Material.IRON_BLOCK;
            case SNOW_GOLEM -> Material.SNOW_BLOCK;
            case GIANT -> Material.ZOMBIE_HEAD;
            case ILLUSIONER -> Material.BOW;
            default -> Material.EGG;
        };
    }

    public ItemStack createItem(ItemStack item, String name, String... loreLines) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line != null) {
                    for (String subLine : line.split("\n")) {
                        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(subLine));
                    }
                }
            }
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line != null) {
                    for (String subLine : line.split("\n")) {
                        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(subLine));
                    }
                }
            }
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createModifierItem(Material mat, String name, double value) {
        String valStr = (value == -1.0) ? "&7(Non défini / Vanilla)" : "&e" + String.format("%.2f", value);
        return createItem(mat, name, 
                plugin.getLangManager().getRaw("gui.item.valeur_actuelle") + valStr,
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_10_clic_droit_1"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_100_1"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_100_1"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis"));
    }

    private ItemStack createToggleItem(Material mat, String name, boolean active) {
        return createItem(mat, name, 
                plugin.getLangManager().getRaw("gui.item.statut") + (active ? plugin.getLangManager().getRaw("gui.item.activé") : plugin.getLangManager().getRaw("gui.item.désactivé_1")),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_basculer"));
    }

    public void openBiomeSpawnConfig(Player player, String biomeName) {
        WDMenuHolder holder = new WDMenuHolder("BIOME_SPAWN_CONFIG", biomeName);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_spawns") + ": " + biomeName));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            boolean active = var.getAllowedBiomes().contains(biomeName.toUpperCase());
            Material mat = getVariantHeadMaterial(var.getType());
            inv.setItem(slot++, createItem(mat, 
                    active ? plugin.getLangManager().getRaw("gui.item.actif") + var.getId() : plugin.getLangManager().getRaw("gui.item.inactif") + var.getId(),
                    plugin.getLangManager().getRaw("gui.item.type_1") + var.getType().name(),
                    plugin.getLangManager().getRaw("gui.item.statut_2") + (active ? plugin.getLangManager().getRaw("gui.item.autorisé_dans_ce_biome") : plugin.getLangManager().getRaw("gui.item.spawns_standards_non_autorisé")),
                    "",
                    plugin.getLangManager().getRaw("gui.item.clic_pour_basculer_lautorisation")));
        }

        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public String getBiomeKeyString(Location loc) {
        try {
            org.bukkit.block.Biome b = loc.getBlock().getBiome();
            org.bukkit.NamespacedKey key = org.bukkit.Registry.BIOME.getKey(b);
            if (key != null) return key.toString();
            return b.name();
        } catch (Throwable t) {
            return loc.getBlock().getBiome().name();
        }
    }

    public void openVariantSpawnConditions(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("SPAWN_CONDITIONS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_conditions") + ": " + variantId));
        holder.setInventory(inv);

        // Météo (slot 10)
        String w = var.getSpawnWeather();
        Material wMat = w.equalsIgnoreCase("RAINY") ? Material.WATER_BUCKET : (w.equalsIgnoreCase("CLEAR") ? Material.SUNFLOWER : Material.GLASS_BOTTLE);
        inv.setItem(10, createItem(wMat, plugin.getLangManager().getRaw("gui.item.météo_requise"), plugin.getLangManager().getRaw("gui.item.actuel") + w, "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer_any_rainy")));

        // Moment journée (slot 12)
        String t = var.getSpawnTime();
        Material tMat = t.equalsIgnoreCase("DAY") ? Material.CAMPFIRE : (t.equalsIgnoreCase("NIGHT") ? Material.SOUL_CAMPFIRE : Material.CLOCK);
        inv.setItem(12, createItem(tMat, plugin.getLangManager().getRaw("gui.item.moment_requis"), plugin.getLangManager().getRaw("gui.item.actuel") + t, "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer_any_day")));

        // Cave Spawn (slot 11)
        String c = var.getCaveSpawn();
        Material cMat = c.equalsIgnoreCase("ONLY_CAVES") ? Material.COBBLESTONE : (c.equalsIgnoreCase("NO_CAVES") ? Material.GRASS_BLOCK : Material.COAL_ORE);
        inv.setItem(11, createItem(cMat, plugin.getLangManager().getRaw("gui.item.spawn_dans_caves"), plugin.getLangManager().getRaw("gui.item.actuel") + c, "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer_any_only")));

        // Biome actuel (slot 14)
        inv.setItem(14, createItem(Material.GRASS_BLOCK, plugin.getLangManager().getRaw("gui.item.gérer_les_biomes_autorisés"), 
                plugin.getLangManager().getRaw("gui.item.actuellement") + (var.getAllowedBiomes().isEmpty() ? plugin.getLangManager().getRaw("gui.item.tous_par_défaut") : var.getAllowedBiomes().size() + plugin.getLangManager().getRaw("gui.item.biomes_spécifiés")), 
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_ouvrir_la_configuration")));

        // Biomes autorisés (slots 18-44)
        int slot = 18;
        for (String biome : var.getAllowedBiomes()) {
            if (slot >= 45) break;
            inv.setItem(slot++, createItem(Material.PAPER, plugin.getLangManager().getRaw("empty_4") + biome, "", plugin.getLangManager().getRaw("gui.item.clic_pour_retirer_ce_biome")));
        }

        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openVariantBiomesEditor(Player player, String variantId, int page, String filter) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("VARIANT_BIOMES_EDIT", variantId + ":" + page + ":" + filter);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_biomes_of") + " " + variantId));
        holder.setInventory(inv);

        // Gather all registry biomes
        List<String> allBiomes = new ArrayList<>();
        try {
            for (org.bukkit.block.Biome b : org.bukkit.Registry.BIOME) {
                org.bukkit.NamespacedKey key = org.bukkit.Registry.BIOME.getKey(b);
                if (key != null) {
                    allBiomes.add(key.toString());
                }
            }
        } catch (Throwable t) {
            for (org.bukkit.block.Biome b : org.bukkit.block.Biome.values()) {
                allBiomes.add("minecraft:" + b.name().toLowerCase());
            }
        }
        allBiomes = allBiomes.stream().distinct().sorted().collect(java.util.stream.Collectors.toList());

        // Apply filters
        List<String> filteredBiomes = new ArrayList<>();
        for (String b : allBiomes) {
            if (filter.equals("IRIS") && !b.toLowerCase().contains("iris")) continue;
            if (filter.equals("MINECRAFT") && !b.toLowerCase().contains("minecraft")) continue;
            filteredBiomes.add(b);
        }

        // Pagination
        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) filteredBiomes.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredBiomes.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String biome = filteredBiomes.get(i);
            boolean active = var.getAllowedBiomes().contains(biome.toUpperCase()) || var.getAllowedBiomes().contains(biome);
            Material mat = active ? Material.MAP : Material.PAPER;
            inv.setItem(slot++, createItem(mat, 
                    active ? plugin.getLangManager().getRaw("empty_9") + biome : "&7" + biome, 
                    "", 
                    active ? plugin.getLangManager().getRaw("gui.item.clic_retirer") : plugin.getLangManager().getRaw("gui.item.clic_ajouter")));
        }

        // Filtres
        inv.setItem(45, createItem(Material.LIME_DYE, plugin.getLangManager().getRaw("gui.item.catégorie_iris"), plugin.getLangManager().getRaw("gui.item.afficher_les_biomes_iris")));
        inv.setItem(46, createItem(Material.BLUE_DYE, plugin.getLangManager().getRaw("gui.item.catégorie_minecraftvanilla"), plugin.getLangManager().getRaw("gui.item.afficher_les_biomes_minecraft")));
        inv.setItem(47, createItem(Material.BOOK, plugin.getLangManager().getRaw("gui.item.catégorie_tous"), plugin.getLangManager().getRaw("gui.item.afficher_tous_les_biomes")));

        // Navigation
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_précédente") + page + ")"));
        }
        
        String curB = getBiomeKeyString(player.getLocation());
        boolean hasCur = var.getAllowedBiomes().contains(curB.toUpperCase()) || var.getAllowedBiomes().contains(curB);
        inv.setItem(49, createItem(Material.COMPASS, plugin.getLangManager().getRaw("gui.item.biome_actuel") + (hasCur ? plugin.getLangManager().getRaw("gui.item.inclus") : plugin.getLangManager().getRaw("gui.item.exclu")), 
                plugin.getLangManager().getRaw("gui.item.votre_biome") + curB, 
                "", 
                plugin.getLangManager().getRaw("gui.item.clic_pour_lajouterretirer")));
                
        if (page < maxPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, plugin.getLangManager().getRaw("gui.item.page_suivante") + (page + 2) + ")"));
        }

        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openLeatherColorSelector(Player player, String variantId, String slotName, String armorItem) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_LEATHER_COLOR", variantId + ":" + slotName + ":" + armorItem);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_leather_color") + ": " + slotName));
        holder.setInventory(inv);

        String[] colors = new String[] {
            "FF0000", "E60000", "CC0000", "990000", "FF3333", "FF6666", "FF9999", "FFC0CB", "FF69B4",
            "FFA500", "FF8C00", "FF4500", "FFD700", "FFFF00", "FFFFE0", "F0E68C", "BDB76B", "E67E22",
            "00FF00", "00CC00", "009900", "006600", "32CD32", "98FB98", "8FBC8F", "2E8B57", "16A085",
            "00FFFF", "00E5E5", "00B2B2", "008080", "0000FF", "1E90FF", "87CEEB", "ADD8E6", "2980B9",
            "800080", "9B59B6", "8E44AD", "FF00FF", "DA70D6", "2C3E50", "34495E", "7F8C8D", "95A5A6",
            "8B4513", "A0522D", "CD853F", "D2B48C", "FFFFFF", "D3D3D3", "808080", "555555"
        };

        Material mat = Material.matchMaterial(armorItem.toUpperCase());
        if (mat == null) mat = Material.LEATHER_CHESTPLATE;

        for (int i = 0; i < colors.length && i < 53; i++) {
            ItemStack item = new ItemStack(mat);
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
                try {
                    int rgb = Integer.parseInt(colors[i], 16);
                    meta.setColor(org.bukkit.Color.fromRGB(rgb));
                    item.setItemMeta(meta);
                } catch (NumberFormatException ignored) {}
            }
            inv.setItem(i, createItem(item, plugin.getLangManager().getRaw("gui.item.couleur_2") + colors[i], "", plugin.getLangManager().getRaw("gui.item.clic_pour_sélectionner_cette_couleur")));
        }

        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public static boolean supportsBaby(EntityType type) {
        String name = type.name();
        return name.equals("ZOMBIE") || name.equals("PIGLIN") || name.equals("HUSK") || name.equals("DROWNED")
            || name.equals("ZOMBIE_VILLAGER") || name.equals("COW") || name.equals("SHEEP") || name.equals("PIG")
            || name.equals("CHICKEN") || name.equals("FOX") || name.equals("WOLF") || name.equals("CAT")
            || name.equals("RABBIT") || name.equals("HORSE") || name.equals("DONKEY") || name.equals("MULE")
            || name.equals("LLAMA") || name.equals("PANDA") || name.equals("POLAR_BEAR") || name.equals("HOGLIN")
            || name.equals("ZOGLIN") || name.equals("STRIDER") || name.equals("GOAT") || name.equals("AXOLOTL")
            || name.equals("FROG") || name.equals("CAMEL");
    }

    public static boolean supportsEquipment(EntityType type) {
        String name = type.name();
        return name.contains("ZOMBIE") || name.contains("SKELETON") || name.contains("PIGLIN") || name.equals("DROWNED")
            || name.equals("STRAY") || name.equals("HUSK") || name.equals("GIANT") || name.equals("ARMOR_STAND") || name.equals("WITHER_SKELETON");
    }

    public void openSpawnerEditor(Player player, Location loc) {
        String contextId = fr.wilddifficulty.spawner.SpawnerManager.toKey(loc);
        fr.wilddifficulty.spawner.CustomSpawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        if (spawner == null) return;

        WDMenuHolder holder = new WDMenuHolder("SPAWNER_EDIT", contextId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_spawner_config")));
        holder.setInventory(inv);

        // Status active
        inv.setItem(10, createToggleItem(Material.LEVER, plugin.getLangManager().getRaw("gui.item.spawner_actif"), spawner.isActive()));

        // Interval
        inv.setItem(11, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.item.intervalle_dapparition"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + spawner.getInterval() + plugin.getLangManager().getRaw("gui.item.secondes"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1s_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_10s"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_10s"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis")));

        // Rayon
        inv.setItem(12, createItem(Material.BEACON, plugin.getLangManager().getRaw("gui.item.rayon_de_détection_spawn"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + spawner.getRadius() + plugin.getLangManager().getRaw("gui.item.blocs"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_5"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_5"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis")));

        // Max entités proches
        inv.setItem(13, createItem(Material.IRON_BARS, plugin.getLangManager().getRaw("gui.item.max_monstres_proches"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + spawner.getMaxNearby() + plugin.getLangManager().getRaw("gui.item.max_1"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_5"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_droit_5"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis")));

        // Variants weights list manager
        inv.setItem(15, createItem(Material.CHEST, plugin.getLangManager().getRaw("gui.item.monstres_poids"), 
                plugin.getLangManager().getRaw("gui.item.configurer_les_variantes_de_monstres"),
                plugin.getLangManager().getRaw("gui.item.qui_peuvent_apparaître_ici_et"),
                plugin.getLangManager().getRaw("gui.item.leurs_chances_relatives_poids"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_gérer")));

        // Variation Interval (+/- seconds)
        inv.setItem(20, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.item.variation_dintervalle_sec"), 
                plugin.getLangManager().getRaw("gui.item.actuel_5") + spawner.getIntervalRange() + plugin.getLangManager().getRaw("gui.item.secondes"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1s_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_5s_shiftclic_droit"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis")));

        // Dispersion range (spawn range)
        inv.setItem(21, createItem(Material.COMPASS, plugin.getLangManager().getRaw("gui.item.rayon_de_dispersion_spawn"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + spawner.getSpawnRange() + plugin.getLangManager().getRaw("gui.item.blocs"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_1_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_5_shiftclic_droit"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis")));

        // Copy spawner
        inv.setItem(22, createItem(Material.PAPER, plugin.getLangManager().getRaw("gui.item.copier_ce_spawner"), 
                plugin.getLangManager().getRaw("gui.item.enregistre_la_configuration_de_ce"), 
                plugin.getLangManager().getRaw("gui.item.pour_pouvoir_la_coller_sur"), 
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_copier")));

        // Paste spawner (only if clipboard has one)
        fr.wilddifficulty.spawner.CustomSpawner clipboard = plugin.getSpawnerManager().getSpawnerClipboards().get(player.getUniqueId());
        if (clipboard != null) {
            inv.setItem(23, createItem(Material.SHEARS, plugin.getLangManager().getRaw("gui.item.coller_la_configuration"), 
                    plugin.getLangManager().getRaw("gui.item.applique_les_réglages_copiés_sur"), 
                    "", plugin.getLangManager().getRaw("gui.item.clic_pour_coller")));
        }

        // Particle spawner
        inv.setItem(24, createItem(Material.GLOWSTONE_DUST, plugin.getLangManager().getRaw("gui.item.particules_du_spawner"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + spawner.getParticleType(), 
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer"), plugin.getLangManager().getRaw("gui.item.smoke_flame_portal_none")));

        // Sound spawner
        inv.setItem(25, createItem(Material.JUKEBOX, plugin.getLangManager().getRaw("gui.item.sons_du_spawner"), 
                plugin.getLangManager().getRaw("gui.item.actuel") + spawner.getSoundType(), 
                "", plugin.getLangManager().getRaw("gui.item.clic_pour_basculer"), plugin.getLangManager().getRaw("gui.item.pling_ambient_explode_none")));

        inv.setItem(31, createItem(Material.LAVA_BUCKET, plugin.getLangManager().getRaw("gui.item.supprimer_ce_spawner"), plugin.getLangManager().getRaw("gui.item.attention_supprime_le_bloc"), plugin.getLangManager().getRaw("gui.item.spawner_de_la_configuration")));
        inv.setItem(35, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.quitter")));
        player.openInventory(inv);
    }

    public void openSpawnerVariantsEditor(Player player, Location loc) {
        String contextId = fr.wilddifficulty.spawner.SpawnerManager.toKey(loc);
        fr.wilddifficulty.spawner.CustomSpawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        if (spawner == null) return;

        WDMenuHolder holder = new WDMenuHolder("SPAWNER_VARIANTS", contextId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_spawner_mobs")));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            boolean active = spawner.getVariantWeights().containsKey(var.getId());
            int weight = spawner.getVariantWeights().getOrDefault(var.getId(), 0);

            Material mat = getVariantHeadMaterial(var.getType());
            
            if (active) {
                inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("gui.item.inclus_1") + var.getId(),
                        plugin.getLangManager().getRaw("gui.item.type_1") + var.getType().name(),
                        plugin.getLangManager().getRaw("gui.item.poids_actuel") + weight,
                        "",
                        plugin.getLangManager().getRaw("gui.item.clic_gauche_retirer_du_spawner"),
                        plugin.getLangManager().getRaw("gui.item.clic_droit_changer_poids_shift"),
                        plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_poids_précis")));
            } else {
                inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("gui.item.exclus") + var.getId(),
                        plugin.getLangManager().getRaw("gui.item.type_1") + var.getType().name(),
                        "",
                        plugin.getLangManager().getRaw("gui.item.clic_gauche_ajouter_au_spawner")));
            }
        }

        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openVariantReinforcementSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_REINFORCEMENT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_death_reinforcement")));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            if (var.getId().equals(variantId)) continue; // Can't reinforce with itself directly
            Material mat = getVariantHeadMaterial(var.getType());
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + var.getId(), plugin.getLangManager().getRaw("gui.item.sélectionner_comme_renfort")));
        }

        inv.setItem(49, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.retirer_le_renfort"), plugin.getLangManager().getRaw("gui.item.aucun_monstre_ne_spawneront_à")));
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openCustomModelDataSelector(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("SELECT_CMD", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_custom_model_data")));
        holder.setInventory(inv);

        // Predefined choices based on mob type
        int[] presets = switch (var.getType()) {
            case ZOMBIE -> new int[]{10001, 10002, 10003, 10004};
            case SKELETON -> new int[]{20001, 20002, 20003};
            case PIGLIN -> new int[]{30001, 30002};
            case COW -> new int[]{40001};
            case SHEEP -> new int[]{50001};
            case CHICKEN -> new int[]{60001};
            default -> new int[]{10001, 10002, 10003};
        };

        int slot = 0;
        for (int p : presets) {
            inv.setItem(slot++, createItem(Material.GOLD_INGOT, plugin.getLangManager().getRaw("gui.item.modèle") + p, plugin.getLangManager().getRaw("gui.item.appliquer_le_custommodeldata") + p));
        }

        inv.setItem(18, createItem(Material.EMERALD, plugin.getLangManager().getRaw("gui.item.valeur_personnalisée_tchat"), plugin.getLangManager().getRaw("gui.item.saisir_un_identifiant_cmd_précis")));
        inv.setItem(22, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.réinitialiser_0"), plugin.getLangManager().getRaw("gui.item.retirer_le_custommodeldata")));
        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openRangedProjectileSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_RANGED_TYPE", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text(plugin.getLangManager().getRaw("gui.title_ranged_projectile")));
        holder.setInventory(inv);

        String[] types = { "ARROW", "SNOWBALL", "FIREBALL", "SMALL_FIREBALL", "WITHER_SKULL", "EGG", "DRAGON_FIREBALL" };
        int slot = 0;
        for (String t : types) {
            Material mat = switch (t) {
                case "ARROW" -> Material.ARROW;
                case "SNOWBALL" -> Material.SNOWBALL;
                case "FIREBALL" -> Material.FIRE_CHARGE;
                case "SMALL_FIREBALL" -> Material.MAGMA_CREAM;
                case "WITHER_SKULL" -> Material.WITHER_SKELETON_SKULL;
                case "EGG" -> Material.EGG;
                default -> Material.DRAGON_BREATH;
            };
            inv.setItem(slot++, createItem(mat, plugin.getLangManager().getRaw("empty_4") + t, plugin.getLangManager().getRaw("gui.item.sélectionner_comme_projectile")));
        }

        inv.setItem(17, createBackItem());
        player.openInventory(inv);
    }

    public ItemStack createSkullItem(String skin, String name, String... loreLines) {
        ItemStack item = fr.wilddifficulty.util.EquipmentUtil.createSkull(skin);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line != null) {
                    for (String subLine : line.split("\n")) {
                        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(subLine));
                    }
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Opens the dedicated per-stat external scaling editor for a zone.
     * Title: "ZONE_SCALING_EDIT:<zoneId>"
     */
    public void openZoneScalingEdit(Player player, DifficultyZone zone) {
        WDMenuHolder holder = new WDMenuHolder("ZONE_SCALING_EDIT", zone.getId());
        Inventory inv = plugin.getServer().createInventory(holder, 54,
                net.kyori.adventure.text.Component.text(plugin.getLangManager().getRaw("gui.title_outer_scaling") + " — " + zone.getId()));
        holder.setInventory(inv);

        // === CAP GLOBAL (slot 4) ===
        inv.setItem(4, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.multiplicateur_maximum_cap"),
                plugin.getLangManager().getRaw("gui.item.limite_le_bonus_maximum_toutes"),
                plugin.getLangManager().getRaw("gui.item.actuel_6") + zone.getExtMaxMult() + "x",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_05x_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat_1")));

        // === PV / HP (slots 10–12) ===
        inv.setItem(10, createItem(Material.HEART_OF_THE_SEA, plugin.getLangManager().getRaw("gui.item.bonus_pv"),
                plugin.getLangManager().getRaw("gui.item.augmente_les_pv_des_mobs"),
                plugin.getLangManager().getRaw("gui.item.pas") + (zone.getExtStepHp() > 0 ? zone.getExtStepHp() : (zone.getExtStep() > 0 ? zone.getExtStep() + plugin.getLangManager().getRaw("gui.item.global") : plugin.getLangManager().getRaw("gui.item.non_défini"))),
                plugin.getLangManager().getRaw("gui.item.bonuspas") + String.format(plugin.getLangManager().getRaw("gui.item.1f"), (zone.getExtMultHp() > 0 ? zone.getExtMultHp() : zone.getExtMultPerStep()) * 100) + "%",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_pas_50_clic"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_g_bonus_5_shiftclic"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat_1")));

        inv.setItem(11, createItem(Material.RED_DYE, plugin.getLangManager().getRaw("gui.item.désactiver_bonus_pv"),
                plugin.getLangManager().getRaw("gui.item.remet_le_bonus_pv_à")));

        // === DÉGÂTS / DMG (slots 19–21) ===
        inv.setItem(19, createItem(Material.DIAMOND_SWORD, plugin.getLangManager().getRaw("gui.item.bonus_dégâts"),
                plugin.getLangManager().getRaw("gui.item.augmente_les_dégâts_des_mobs"),
                plugin.getLangManager().getRaw("gui.item.pas") + (zone.getExtStepDmg() > 0 ? zone.getExtStepDmg() : (zone.getExtStep() > 0 ? zone.getExtStep() + plugin.getLangManager().getRaw("gui.item.global") : plugin.getLangManager().getRaw("gui.item.non_défini"))),
                plugin.getLangManager().getRaw("gui.item.bonuspas") + String.format(plugin.getLangManager().getRaw("gui.item.1f"), (zone.getExtMultDmg() > 0 ? zone.getExtMultDmg() : zone.getExtMultPerStep()) * 100) + "%",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_pas_50_clic"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_g_bonus_5_shiftclic"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat_1")));

        inv.setItem(20, createItem(Material.ORANGE_DYE, plugin.getLangManager().getRaw("gui.item.désactiver_bonus_dégâts"),
                plugin.getLangManager().getRaw("gui.item.remet_le_bonus_dégâts_à")));

        // === VITESSE / SPD (slots 28–30) ===
        inv.setItem(28, createItem(Material.FEATHER, plugin.getLangManager().getRaw("gui.item.bonus_vitesse"),
                plugin.getLangManager().getRaw("gui.item.augmente_la_vitesse_des_mobs"),
                plugin.getLangManager().getRaw("gui.item.pas") + (zone.getExtStepSpd() > 0 ? zone.getExtStepSpd() : (zone.getExtStep() > 0 ? zone.getExtStep() + plugin.getLangManager().getRaw("gui.item.global") : plugin.getLangManager().getRaw("gui.item.non_défini"))),
                plugin.getLangManager().getRaw("gui.item.bonuspas") + String.format(plugin.getLangManager().getRaw("gui.item.1f"), (zone.getExtMultSpd() > 0 ? zone.getExtMultSpd() : zone.getExtMultPerStep()) * 100) + "%",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_pas_50_clic"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_g_bonus_5_shiftclic"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_définir_précis_tchat_1")));

        inv.setItem(29, createItem(Material.LIGHT_BLUE_DYE, plugin.getLangManager().getRaw("gui.item.désactiver_bonus_vitesse"),
                plugin.getLangManager().getRaw("gui.item.remet_le_bonus_vitesse_à")));
        // === GLOBAL FALLBACK (slot 40) ===
        inv.setItem(40, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.item.bonus_global_fallback"),
                plugin.getLangManager().getRaw("gui.item.utilisé_si_une_stat_na"),
                plugin.getLangManager().getRaw("gui.item.pas") + zone.getExtStep() + plugin.getLangManager().getRaw("gui.item.blocs"),
                plugin.getLangManager().getRaw("gui.item.bonuspas") + String.format(plugin.getLangManager().getRaw("gui.item.1f"), zone.getExtMultPerStep() * 100) + "%",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_pas_50_clic"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_g_bonus_5_shiftclic")));

        // === APERÇU (slot 49) ===
        double simulatedDist = zone.getRadius() + 200;
        double hpPct   = (zone.computeHpExtMult(simulatedDist)  - 1) * 100;
        double dmgPct  = (zone.computeDmgExtMult(simulatedDist) - 1) * 100;
        double spdPct  = (zone.computeSpdExtMult(simulatedDist) - 1) * 100;
        inv.setItem(49, createItem(Material.SPYGLASS, plugin.getLangManager().getRaw("gui.item.aperçu_à") + (int) simulatedDist + plugin.getLangManager().getRaw("gui.item.blocs_du_centre"),
                plugin.getLangManager().getRaw("gui.item.pv") + String.format(plugin.getLangManager().getRaw("gui.item.0f_1"), hpPct)  + "%",
                plugin.getLangManager().getRaw("gui.item.dégâts") + String.format(plugin.getLangManager().getRaw("gui.item.0f_1"), dmgPct) + "%",
                plugin.getLangManager().getRaw("gui.item.vitesse") + String.format(plugin.getLangManager().getRaw("gui.item.0f_1"), spdPct) + "%"));
        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneMembersConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_MEMBERS", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(plugin.getLangManager().getRaw("gui.title_members") + ": " + zoneId));
        holder.setInventory(inv);

        inv.setItem(0, createItem(Material.NAME_TAG, plugin.getLangManager().getRaw("gui.item.ajouter_un_membre"), plugin.getLangManager().getRaw("gui.item.ajouter_un_joueur_via_son"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_saisir_le_pseudo")));

        int slot = 9;
        for (fr.wilddifficulty.zone.ZoneMember member : zone.getMembers().values()) {
            if (slot >= 45) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getPlayerUuid()));
            meta.displayName(Component.text("§e" + member.getLastKnownName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7UUID: " + member.getPlayerUuid()));
            lore.add(Component.text("§7Rôle: " + member.getRoleName()));
            lore.add(Component.text(""));
            lore.add(Component.text("§aClic Gauche: §fChanger le niveau de permission"));
            lore.add(Component.text("§cClic Droit: §fRetirer le membre"));
            meta.lore(lore);
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        inv.setItem(53, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneBeaconEffectsConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_BEACON", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_beacon_effects") + ": " + zoneId));
        holder.setInventory(inv);

        String[] effects = {"SPEED", "HASTE", "RESISTANCE", "JUMP_BOOST", "INCREASE_DAMAGE", "REGENERATION", "FIRE_RESISTANCE", "NIGHT_VISION"};
        Material[] icons = {Material.SUGAR, Material.GOLDEN_PICKAXE, Material.SHIELD, Material.RABBIT_FOOT, Material.DIAMOND_SWORD, Material.GHAST_TEAR, Material.MAGMA_CREAM, Material.GOLDEN_CARROT};

        for (int i = 0; i < effects.length; i++) {
            String eff = effects[i];
            boolean active = zone.getBeaconEffects().containsKey(eff);
            int amp = zone.getBeaconEffects().getOrDefault(eff, 0);
            inv.setItem(10 + i, createItem(icons[i], (active ? plugin.getLangManager().getRaw("empty_11") : "§7✖ ") + eff,
                    plugin.getLangManager().getRaw("gui.item.statut_1") + (active ? plugin.getLangManager().getRaw("gui.item.actif_niv") + (amp + 1) + ")" : plugin.getLangManager().getRaw("gui.item.inactif_1")),
                    "",
                    plugin.getLangManager().getRaw("gui.item.clic_gauche_basculeraugmenter_le_niveau"),
                    plugin.getLangManager().getRaw("gui.item.clic_droit_désactiver_leffet")));
        }

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneDangerNestConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_DANGER_NEST", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_danger_nest") + ": " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.NETHER_STAR, plugin.getLangManager().getRaw("gui.item.nid_dennemis_danger_nest"), zone.isDangerNest()));
        inv.setItem(12, createItem(Material.REDSTONE, plugin.getLangManager().getRaw("gui.item.boost_spawn_rate"), plugin.getLangManager().getRaw("gui.item.multiplicateur") + zone.getNestSpawnBoost() + "x", "", plugin.getLangManager().getRaw("gui.item.clic_gauche_05_clic_droit")));
        inv.setItem(14, createItem(Material.DIAMOND_SWORD, plugin.getLangManager().getRaw("gui.item.modifier_stats_nest_pv_dégâts"), plugin.getLangManager().getRaw("gui.item.stats_additionnelles_appliquées_aux_entités")));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openZoneSubsectionsConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_SUBSECTIONS", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Sub-sections: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(0, createItem(Material.GOLDEN_HOE, plugin.getLangManager().getRaw("gui.item.ajouter_la_sélection_actuelle"), plugin.getLangManager().getRaw("gui.item.ajoute_la_formesélection_de_votre"), "", plugin.getLangManager().getRaw("gui.item.clic_pour_fusionner")));

        int slot = 9;
        for (fr.wilddifficulty.zone.ZoneSection sec : zone.getSubSections()) {
            if (slot >= 27) break;
            inv.setItem(slot++, createItem(Material.MAP, plugin.getLangManager().getRaw("gui.item.soussection") + sec.getId(), plugin.getLangManager().getRaw("gui.item.type_2") + sec.getType().name(), "", plugin.getLangManager().getRaw("gui.item.clic_pour_supprimer_cette_soussection")));
        }

        inv.setItem(35, createBackItem());
        player.openInventory(inv);
    }

    public void openPlayerSettingsGui(Player player) {
        fr.wilddifficulty.player.PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());

        WDMenuHolder holder = new WDMenuHolder("PLAYER_SETTINGS", player.getUniqueId().toString());
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Paramètres de Jeu"));
        holder.setInventory(inv);

        fillBorders(inv);

        inv.setItem(11, createItem(Material.DIAMOND_SWORD, plugin.getLangManager().getRaw("gui.item.difficulté_personnelle"),
                plugin.getLangManager().getRaw("gui.item.niveau_actuel") + pd.getDifficultyLevel(),
                plugin.getLangManager().getRaw("gui.item.multiplicateur_dégâts_pris") + String.format(plugin.getLangManager().getRaw("gui.item.2f"), pd.getDamageMultiplier()) + "x",
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_basculer_le_niveau"),
                plugin.getLangManager().getRaw("gui.item.facile_normal_difficile_extreme")));

        inv.setItem(13, createToggleItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.barre_de_soif"), pd.isThirstEnabled()));
        inv.setItem(15, createItem(Material.WITHER_SKELETON_SKULL, plugin.getLangManager().getRaw("gui.item.mode_hardcore_personnel"),
                plugin.getLangManager().getRaw("gui.item.statut_1") + (pd.isHardcoreEnabled() ? plugin.getLangManager().getRaw("gui.item.actif_1") : plugin.getLangManager().getRaw("gui.item.inactif_2")),
                plugin.getLangManager().getRaw("gui.item.régén_naturelle") + (pd.isHardcoreNoRegen() ? plugin.getLangManager().getRaw("gui.item.désactivée") : plugin.getLangManager().getRaw("gui.item.activée")),
                plugin.getLangManager().getRaw("gui.item.régén_pommes_dorées") + (pd.isHardcoreAllowGoldenApples() ? plugin.getLangManager().getRaw("gui.item.autorisée") : plugin.getLangManager().getRaw("gui.item.bloquée")),
                plugin.getLangManager().getRaw("gui.item.régén_potions") + (pd.isHardcoreAllowPotions() ? plugin.getLangManager().getRaw("gui.item.autorisée") : plugin.getLangManager().getRaw("gui.item.bloquée")),
                plugin.getLangManager().getRaw("gui.item.stuff_mort") + (pd.isHardcoreInstantDeathDespawn() ? plugin.getLangManager().getRaw("gui.item.despawn_immédiat") : plugin.getLangManager().getRaw("gui.item.timer_normal")),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_configurer_votre_hardcore")));

        inv.setItem(26, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.fermer")));
        player.openInventory(inv);
    }

    public void openPersonalHardcoreSettingsGui(Player player) {
        fr.wilddifficulty.player.PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());

        WDMenuHolder holder = new WDMenuHolder("PLAYER_HARDCORE_CONFIG", player.getUniqueId().toString());
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Hardcore Personnel"));
        holder.setInventory(inv);

        fillBorders(inv);

        inv.setItem(10, createToggleItem(Material.WITHER_SKELETON_SKULL, plugin.getLangManager().getRaw("gui.item.activer_mode_hardcore"), pd.isHardcoreEnabled()));
        inv.setItem(11, createToggleItem(Material.APPLE, plugin.getLangManager().getRaw("gui.item.désactiver_régénération_naturelle"), pd.isHardcoreNoRegen()));
        inv.setItem(12, createToggleItem(Material.GOLDEN_APPLE, plugin.getLangManager().getRaw("gui.item.autoriser_régén_pommes_dorées"), pd.isHardcoreAllowGoldenApples()));
        inv.setItem(13, createToggleItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.autoriser_régén_potions"), pd.isHardcoreAllowPotions()));
        inv.setItem(14, createToggleItem(Material.BEACON, plugin.getLangManager().getRaw("gui.item.autoriser_régén_zones_sûres"), pd.isHardcoreAllowSafezoneRegen()));
        inv.setItem(15, createToggleItem(Material.LAVA_BUCKET, plugin.getLangManager().getRaw("gui.item.despawn_immédiat_du_stuff_mort"), pd.isHardcoreInstantDeathDespawn()));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openThirstHardcoreAdminGui(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ADMIN_THIRST_HARDCORE", "admin");
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(plugin.getLangManager().getRaw("gui.title_thirst_hardcore")));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.activer_système_de_soif_globalement"), plugin.getMainConfigManager().isThirstEnabled()));
        inv.setItem(11, createItem(Material.CLOCK, plugin.getLangManager().getRaw("gui.item.multiplicateur_vitesse_dégradation_soif"),
                plugin.getLangManager().getRaw("gui.item.multiplicateur_actuel") + String.format(plugin.getLangManager().getRaw("gui.item.2fx"), plugin.getMainConfigManager().getThirstDrainMultiplier()),
                plugin.getLangManager().getRaw("gui.item.immobile_pas_de_perte_de"),
                plugin.getLangManager().getRaw("gui.item.marche_course_épuisement_dynamique_style"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_025x_vider_plus"),
                plugin.getLangManager().getRaw("gui.item.clic_droit_025x_vider_moins"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_multiplicateur")));
        inv.setItem(12, createItem(Material.WITHER_SKELETON_SKULL, plugin.getLangManager().getRaw("gui.item.configurer_le_mode_hardcore"),
                plugin.getLangManager().getRaw("gui.item.statut_global") + (plugin.getMainConfigManager().isHardcoreEnabled() ? plugin.getLangManager().getRaw("gui.item.actif_1") : plugin.getLangManager().getRaw("gui.item.inactif_2")),
                plugin.getLangManager().getRaw("gui.item.pas_de_régén") + (plugin.getMainConfigManager().isHardcoreNoRegen() ? plugin.getLangManager().getRaw("gui.item.active") : plugin.getLangManager().getRaw("gui.item.inactive")),
                plugin.getLangManager().getRaw("gui.item.multiplicateur_dégâts_pris_1") + String.format(plugin.getLangManager().getRaw("gui.item.2fx"), plugin.getMainConfigManager().getHardcoreDamageTakenMult()),
                plugin.getLangManager().getRaw("gui.item.multiplicateur_faim") + String.format(plugin.getLangManager().getRaw("gui.item.2fx"), plugin.getMainConfigManager().getHardcoreHungerDrainMult()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_configurer_le_mode_1")));

        inv.setItem(13, createItem(Material.WATER_BUCKET, plugin.getLangManager().getRaw("gui.item.configurer_les_sources_deau"),
                plugin.getLangManager().getRaw("gui.item.définir_les_points_dhydratation_apportés"),
                plugin.getLangManager().getRaw("gui.item.par_chaque_seau_bouteille_chaudron"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_pour_configurer_les_valeurs")));

        inv.setItem(14, createItem(Material.CHEST, plugin.getLangManager().getRaw("gui.item.temps_despawn_stuff_mort"),
                plugin.getLangManager().getRaw("gui.item.temps_avant_disparition") + plugin.getMainConfigManager().getDeathItemDespawnSeconds() + plugin.getLangManager().getRaw("gui.item.sec"),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_60s_clic_droit"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(15, createItem(Material.LAVA_BUCKET, plugin.getLangManager().getRaw("gui.item.déshydratation_par_chaleur_lave"),
                plugin.getLangManager().getRaw("gui.item.augmente_la_déshydratation_près_de"),
                plugin.getLangManager().getRaw("gui.item.statut_1") + (plugin.getMainConfigManager().isThirstHeatDrainEnabled() ? plugin.getLangManager().getRaw("gui.item.actif_1") : plugin.getLangManager().getRaw("gui.item.inactif_2")),
                plugin.getLangManager().getRaw("gui.item.multiplicateur_chaleur") + String.format(plugin.getLangManager().getRaw("gui.item.2fx"), plugin.getMainConfigManager().getThirstHeatDrainMultiplier()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_basculer_actifinactif"),
                plugin.getLangManager().getRaw("gui.item.shiftclic_gauche_025x_multiplicateur"),
                plugin.getLangManager().getRaw("gui.item.clic_droit_025x_multiplicateur"),
                plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }

    public void openThirstSourcesAdminGui(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ADMIN_THIRST_SOURCES", "admin");
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text(plugin.getLangManager().getRaw("gui.title_thirst_sources")));
        holder.setInventory(inv);

        fillBorders(inv);
        MainConfigManager mainCfg = plugin.getMainConfigManager();

        inv.setItem(10, createItem(Material.WATER_BUCKET, plugin.getLangManager().getRaw("gui.item.seau_deau"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreWaterBucket() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(11, createItem(Material.POTION, plugin.getLangManager().getRaw("gui.item.bouteille_deau"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreWaterBottle() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(12, createItem(Material.GLASS_BOTTLE, plugin.getLangManager().getRaw("gui.item.autres_potions"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestorePotion() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(13, createItem(Material.CAULDRON, plugin.getLangManager().getRaw("gui.item.chaudron_deau"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreCauldron() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(14, createItem(Material.WATER_BUCKET, plugin.getLangManager().getRaw("gui.item.source_deau_bloc"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreWaterBlock() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(15, createItem(Material.MILK_BUCKET, plugin.getLangManager().getRaw("gui.item.seau_de_lait"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreMilkBucket() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(16, createItem(Material.HONEY_BOTTLE, plugin.getLangManager().getRaw("gui.item.fiole_de_miel"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreHoneyBottle() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(19, createItem(Material.MELON_SLICE, plugin.getLangManager().getRaw("gui.item.tranche_de_melon"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreMelonSlice() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(20, createItem(Material.APPLE, plugin.getLangManager().getRaw("gui.item.pomme_pomme_dorée"),
                plugin.getLangManager().getRaw("gui.item.restauration") + mainCfg.getThirstRestoreApple() + plugin.getLangManager().getRaw("gui.item.pts"),
                "", plugin.getLangManager().getRaw("gui.item.clic_gauche_1_pt_clic"), plugin.getLangManager().getRaw("gui.item.clic_milieu_tchat_définir_précis_1")));

        inv.setItem(35, createBackItem());
        player.openInventory(inv);
    }

    public void openPlayerEditableZonesGui(Player player) {
        WDMenuHolder holder = new WDMenuHolder("PLAYER_EDITABLE_ZONES", player.getUniqueId().toString());
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Vos Zones Éditables"));
        holder.setInventory(inv);

        fillBorders(inv);
        boolean isAdmin = player.hasPermission("wilddifficulty.admin") || player.hasPermission("wilddifficulty.zone.manage");

        List<DifficultyZone> editableZones = new ArrayList<>();
        for (DifficultyZone zone : plugin.getZoneManager().getAllZones()) {
            if (isAdmin || zone.getMemberLevel(player.getUniqueId()) >= 3) {
                editableZones.add(zone);
            }
        }

        if (editableZones.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.aucune_zone_éditable"), plugin.getLangManager().getRaw("gui.item.vous_navez_le_rôle_gestionnaire"), plugin.getLangManager().getRaw("gui.item.sur_aucune_zone_actuellement")));
        } else {
            int slot = 10;
            for (DifficultyZone zone : editableZones) {
                if (slot == 17 || slot == 26 || slot == 35 || slot == 44) slot += 2;
                if (slot >= 44) break;
                String role = isAdmin ? "Administrateur" : "Gestionnaire (Niveau 3)";
                inv.setItem(slot++, createItem(Material.MAP, plugin.getLangManager().getRaw("gui.item.zone") + zone.getId(),
                        plugin.getLangManager().getRaw("gui.item.type_3") + zone.getType().name(),
                        plugin.getLangManager().getRaw("gui.item.monde_1") + zone.getWorld(),
                        plugin.getLangManager().getRaw("gui.item.votre_rôle") + role,
                        "",
                        plugin.getLangManager().getRaw("gui.item.clic_pour_ouvrir_léditeur_de")));
            }
        }

        inv.setItem(53, createItem(Material.BARRIER, plugin.getLangManager().getRaw("gui.item.fermer")));
        player.openInventory(inv);
    }

    public void openHardcoreConfigGui(Player player) {
        WDMenuHolder holder = new WDMenuHolder("HARDCORE_CONFIG", "admin");
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Configuration Hardcore"));
        holder.setInventory(inv);
        MainConfigManager mainCfg = plugin.getMainConfigManager();

        inv.setItem(10, createToggleItem(Material.WITHER_SKELETON_SKULL, plugin.getLangManager().getRaw("gui.item.activer_mode_hardcore_globalement"), mainCfg.isHardcoreEnabled()));
        inv.setItem(12, createToggleItem(Material.GOLDEN_APPLE, plugin.getLangManager().getRaw("gui.item.désactiver_régénération_naturelle"), mainCfg.isHardcoreNoRegen()));

        inv.setItem(14, createItem(Material.NETHERITE_SWORD, plugin.getLangManager().getRaw("gui.item.multiplicateur_dégâts_subis"),
                plugin.getLangManager().getRaw("gui.item.actuel_7") + String.format(plugin.getLangManager().getRaw("gui.item.2fx"), mainCfg.getHardcoreDamageTakenMult()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_025x_clic_droit")));

        inv.setItem(16, createItem(Material.ROTTEN_FLESH, plugin.getLangManager().getRaw("gui.item.multiplicateur_épuisement_faim"),
                plugin.getLangManager().getRaw("gui.item.actuel_7") + String.format(plugin.getLangManager().getRaw("gui.item.2fx"), mainCfg.getHardcoreHungerDrainMult()),
                "",
                plugin.getLangManager().getRaw("gui.item.clic_gauche_025x_clic_droit")));

        inv.setItem(26, createBackItem());
        player.openInventory(inv);
    }
}
