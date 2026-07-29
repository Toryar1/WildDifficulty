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

    // ================= MAIN MENU =================
    public void openMainMenu(Player player) {
        WDMenuHolder holder = new WDMenuHolder("MAIN", null);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("WildDifficulty - Menu"));
        holder.setInventory(inv);
        MainConfigManager cfg = plugin.getMainConfigManager();

        fillBorders(inv);

        inv.setItem(10, createItem(Material.DIAMOND_SWORD, "&cModificateurs Globaux", "&7Ajustez la difficulté générale."));
        inv.setItem(12, createItem(Material.ZOMBIE_HEAD, "&aVariantes", "&7Gérer les variantes de mobs."));
        inv.setItem(14, createItem(Material.SKELETON_SKULL, "&eEscouades", "&7Gérer les escouades."));
        inv.setItem(16, createItem(Material.MAP, "&dZones", "&7Gérer les zones de difficulté."));
        inv.setItem(18, createToggleItem(Material.BARRIER, "&cBloquer Hostiles Vanilla", cfg.isBlockVanillaHostiles()));
        inv.setItem(19, createToggleItem(Material.PIG_SPAWN_EGG, "&aBloquer Passifs Vanilla", cfg.isBlockVanillaPassives()));
        
        inv.setItem(20, createItem(Material.REDSTONE, "&cConfiguration Lune de Sang", 
                "&7Gérer le taux d'apparition, multiplicateurs", 
                "&7et bonus lors des nuits rouges.", 
                "", 
                "&eClic pour configurer"));
        
        inv.setItem(22, createItem(Material.CHEST_MINECART, "&6Outils d'Administration", 
                "&7Obtenir individuellement ou tous les outils", 
                "&7de configuration et d'analyse.", 
                "", 
                "&eClic pour ouvrir"));

        inv.setItem(24, createItem(Material.COMPARATOR, "&bConfiguration Générale",
                "&7Configurer le cap de mobs, la distance",
                "&7de spawn max, les soleils, les nametags",
                "&7et autres paramètres généraux.",
                "",
                "&eClic pour configurer"));

        inv.setItem(26, createItem(Material.COMMAND_BLOCK, "&dRecharger Config"));

        player.openInventory(inv);
    }

    public void openGeneralConfig(Player player) {
        WDMenuHolder holder = new WDMenuHolder("GENERAL_CONFIG", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("WD - Config Générale"));
        holder.setInventory(inv);
        MainConfigManager cfg = plugin.getMainConfigManager();

        // Debug mode
        inv.setItem(10, createToggleItem(Material.REDSTONE_TORCH, "&cMode Debug", cfg.isDebug()));

        // Max spawn distance
        inv.setItem(11, createItem(Material.CLOCK, "&bDistance de Spawn Max",
                "&7Distance actuelle : &e" + (cfg.getMaxSpawnDistance() == -1 ? "Désactivé" : cfg.getMaxSpawnDistance() + " blocs"),
                "",
                "&aClic Gauche: &f+10 blocs   &cClic Droit: &f-10 blocs",
                "&aShift+Clic Gauche: &f+50 blocs",
                "&cShift+Clic Droit: &f-50 blocs",
                "&eClic Milieu (Tchat): &fDéfinir précis"));

        // Cap of variants per player
        inv.setItem(12, createItem(Material.COMPARATOR, "&bCap de Variantes / Joueur",
                "&7Limite actuelle : &e" + cfg.getCapVariantesParJoueur() + " variantes",
                "",
                "&aClic Gauche: &f+5   &cClic Droit: &f-5",
                "&aShift+Clic Gauche: &f+20",
                "&cShift+Clic Droit: &f-20",
                "&eClic Milieu (Tchat): &fDéfinir précis"));

        // Disable burning globally
        inv.setItem(19, createToggleItem(Material.FLINT_AND_STEEL, "&cPas de Combustion au Soleil", cfg.isDisableBurningGlobally()));

        // Allow day spawn globally
        inv.setItem(20, createToggleItem(Material.SUNFLOWER, "&eSpawns de Jour Globaux", cfg.isAllowDaySpawnGlobally()));

        // Nametags status
        inv.setItem(28, createToggleItem(Material.NAME_TAG, "&aActiver Nametags", cfg.isNametagsEnabled()));

        // Nametag format
        inv.setItem(29, createItem(Material.WRITABLE_BOOK, "&bFormat des Nametags",
                "&7Format actuel : &f" + cfg.getNametagFormat(),
                "",
                "&eClic pour définir via le tchat"));

        // Soif, Hardcore & Mort
        inv.setItem(30, createItem(Material.POTION, "§bSoif, Hardcore & Mort",
                "§7Soif globale: " + (cfg.isThirstEnabled() ? "§a✔" : "§c✖"),
                "§7Hardcore global: " + (cfg.isHardcoreEnabled() ? "§c✔" : "§7✖"),
                "§7Despawn mort: §e" + cfg.getDeathItemDespawnSeconds() + "s",
                "",
                "§eClic pour configurer"));

        inv.setItem(49, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openAdminToolsMenu(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ADMIN_TOOLS", null);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("WD - Outils d'Administration"));
        holder.setInventory(inv);

        inv.setItem(10, createItem(Material.GOLDEN_HOE, "&eOutil de Zone", "&7Dessine les points du polygone.", "", "&eClic pour obtenir"));
        inv.setItem(11, createItem(Material.NETHERITE_SHOVEL, "&eOutil de Spawner", "&7Configure les spawners personnalisés.", "", "&eClic pour obtenir"));
        inv.setItem(12, createItem(Material.COMPASS, "&dOutil de Biome", "&7Configure les spawns par biome.", "", "&eClic pour obtenir"));
        inv.setItem(13, createItem(Material.STICK, "&bInspecteur de Mobs", "&7Affiche les détails complets d'une entité.", "", "&eClic pour obtenir"));
        
        inv.setItem(15, createItem(Material.CHEST, "&aTous les Outils", "&7Obtenir tous les outils d'un coup.", "", "&eClic pour obtenir"));
        inv.setItem(16, createItem(Material.FILLED_MAP, "&6Activer/Désactiver Scoreboard Debug", "&7Affiche le scoreboard latéral d'analyse.", "", "&eClic pour basculer"));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openBloodMoonEditor(Player player) {
        WDMenuHolder holder = new WDMenuHolder("BLOODMOON_EDIT", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("WD - Lune de Sang"));
        holder.setInventory(inv);
        MainConfigManager cfg = plugin.getMainConfigManager();

        inv.setItem(10, createToggleItem(Material.REDSTONE, "&cActiver Lune de Sang", cfg.isBloodMoonEnabled()));
        
        inv.setItem(11, createItem(Material.CLOCK, "&bChance d'apparition", 
                "&7Chance actuelle : &e" + String.format("%.0f%%", cfg.getBloodMoonChance() * 100),
                "",
                "&aClic Gauche: &f+5%   &cClic Droit: &f-5%",
                "&eClic Milieu: &fDéfinir précis (Tchat)"));

        inv.setItem(12, createItem(Material.RED_DYE, "&cMultiplicateur PV", 
                "&7Actuel : &e" + String.format("%.1fx", cfg.getBloodMoonHpMultiplier()),
                "",
                "&aClic Gauche: &f+0.1   &cClic Droit: &f-0.1",
                "&aShift+Clic: &f+0.5   &cShift+Clic: &f-0.5",
                "&eClic Milieu: &fDéfinir précis (Tchat)"));

        inv.setItem(13, createItem(Material.NETHERITE_SWORD, "&cMultiplicateur Dégâts", 
                "&7Actuel : &e" + String.format("%.1fx", cfg.getBloodMoonDamageMultiplier()),
                "",
                "&aClic Gauche: &f+0.1   &cClic Droit: &f-0.1",
                "&aShift+Clic: &f+0.5   &cShift+Clic: &f-0.5",
                "&eClic Milieu: &fDéfinir précis (Tchat)"));

        inv.setItem(14, createItem(Material.FEATHER, "&bMultiplicateur Vitesse", 
                "&7Actuel : &e" + String.format("%.1fx", cfg.getBloodMoonSpeedMultiplier()),
                "",
                "&aClic Gauche: &f+0.1   &cClic Droit: &f-0.1",
                "&aShift+Clic: &f+0.5   &cShift+Clic: &f-0.5",
                "&eClic Milieu: &fDéfinir précis (Tchat)"));

        inv.setItem(15, createItem(Material.CHEST, "&6Multiplicateur Drops", 
                "&7Actuel : &e" + String.format("%.1fx", cfg.getBloodMoonDropsMultiplier()),
                "",
                "&aClic Gauche: &f+0.1   &cClic Droit: &f-0.1",
                "&aShift+Clic: &f+0.5   &cShift+Clic: &f-0.5",
                "&eClic Milieu: &fDéfinir précis (Tchat)"));

        inv.setItem(16, createItem(Material.SPAWNER, "&5Multiplicateur Spawn",
                "&7Actuel : &e" + String.format("%.1fx", cfg.getBloodMoonSpawnMultiplier()),
                "&7Nombre supplémentaire de mobs",
                "&7qui spawneront pendant la Blood Moon.",
                "",
                "&aClic Gauche: &f+0.1   &cClic Droit: &f-0.1",
                "&aShift+Clic: &f+0.5   &cShift+Clic: &f-0.5",
                "&eClic Milieu: &fDéfinir précis (Tchat)"));

        inv.setItem(18, createItem(Material.WRITABLE_BOOK, "&bMessage de Début", 
                "&7Actuel : &f" + cfg.getBloodMoonStartMessage(),
                "",
                "&eClic pour définir via le tchat"));

        inv.setItem(19, createItem(Material.WRITTEN_BOOK, "&bMessage de Fin", 
                "&7Actuel : &f" + cfg.getBloodMoonEndMessage(),
                "",
                "&eClic pour définir via le tchat"));

        inv.setItem(20, createItem(Material.JUKEBOX, "&bSon de Début", 
                "&7Actuel : &e" + cfg.getBloodMoonStartSound(),
                "",
                "&eClic pour modifier via le tchat"));

        inv.setItem(21, createItem(Material.JUKEBOX, "&bSon de Fin", 
                "&7Actuel : &e" + cfg.getBloodMoonEndSound(),
                "",
                "&eClic pour modifier via le tchat"));

        inv.setItem(22, createItem(Material.REDSTONE, "&bParticule de Début", 
                "&7Actuelle : &e" + cfg.getBloodMoonStartParticle(),
                "",
                "&eClic pour modifier via le tchat"));

        inv.setItem(23, createItem(Material.EMERALD, "&bParticule de Fin", 
                "&7Actuelle : &e" + cfg.getBloodMoonEndParticle(),
                "",
                "&eClic pour modifier via le tchat"));

        java.util.List<String> startPots = cfg.getBloodMoonStartPotions();
        String startPotsStr = startPots.isEmpty() ? "&7Aucun" : String.join(", ", startPots);
        inv.setItem(24, createItem(Material.POTION, "&bEffets Potion de Début", 
                "&7Actuels : &f" + startPotsStr,
                "",
                "&eClic pour définir (Format: TYPE:durée:ampli, ex: DARKNESS:100:0)",
                "&7Séparez par des virgules pour en ajouter plusieurs"));

        java.util.List<String> endPots = cfg.getBloodMoonEndPotions();
        String endPotsStr = endPots.isEmpty() ? "&7Aucun" : String.join(", ", endPots);
        inv.setItem(25, createItem(Material.POTION, "&bEffets Potion de Fin", 
                "&7Actuels : &f" + endPotsStr,
                "",
                "&eClic pour définir (Format: TYPE:durée:ampli, ex: SPEED:100:0)",
                "&7Séparez par des virgules pour en ajouter plusieurs"));

        inv.setItem(31, createItem(Material.NETHER_STAR, "&dForcer la Lune de Sang",
                "&7Active la Lune de Sang.",
                "&7- Si c'est le jour: planifiée pour la prochaine nuit.",
                "&7- Si c'est la nuit: démarre immédiatement.",
                "",
                "&eClic pour l'activer"));

        inv.setItem(49, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openGlobalModifiersMenu(Player player) {
        WDMenuHolder holder = new WDMenuHolder("GLOBALS", null);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("WD - Globaux"));
        holder.setInventory(inv);
        MainConfigManager cfg = plugin.getMainConfigManager();

        inv.setItem(10, createModifierItem(Material.APPLE, "&cPV (Santé)", cfg.getGlobalHealthMult()));
        inv.setItem(11, createModifierItem(Material.IRON_SWORD, "&cDégâts", cfg.getGlobalDamageMult()));
        inv.setItem(12, createModifierItem(Material.FEATHER, "&bVitesse", cfg.getGlobalSpeedMult()));
        inv.setItem(13, createModifierItem(Material.ENDER_EYE, "&eDétection", cfg.getGlobalFollowRangeMult()));
        inv.setItem(14, createModifierItem(Material.SHIELD, "&7Knockback", cfg.getGlobalKnockbackMult()));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("WD - Variantes"));
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
            inv.setItem(slot++, createItem(mat, "&f" + var.getId(), 
                    "&7Type: &e" + var.getType().name(), 
                    "&7Poids: &a" + var.getWeight(),
                    "&7Taille: &b" + String.format("%.2f", var.getScale()) + " (&7+-" + String.format("%.2f", var.getScaleVariance()) + ")",
                    "&7Bébé: &e" + (var.isBaby() ? "Oui" : "Non"),
                    "", "&eClic pour éditer"));
        }
        
        inv.setItem(49, createItem(Material.EMERALD, "&a+ Nouvelle Variante", "&7Créer une nouvelle variante"));
        inv.setItem(50, createItem(Material.HOPPER, "&bTri actuel : &e" + filter, 
                "&7Changer le tri des variantes.",
                "",
                "&eClic pour basculer :",
                "&7- ID (Par défaut)",
                "&7- HP (Points de vie)",
                "&7- Dégâts (Points d'attaque)",
                "&7- Vitesse (Rapidité)"));
        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Choisir Type de Base (" + filter + ")"));
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
            inv.setItem(slot++, createItem(mat, "&f" + type.name(), "&7Sélectionner ce type de base"));
        }

        // Filtres
        inv.setItem(45, createItem(Material.NETHERITE_SWORD, "&cFiltrer : Hostiles", "&7Afficher uniquement les monstres."));
        inv.setItem(46, createItem(Material.WHEAT, "&aFiltrer : Passifs & Neutres", "&7Afficher les animaux et créatures pacifiques."));
        inv.setItem(47, createItem(Material.BOOK, "&fFiltrer : Tous", "&7Afficher toutes les créatures de base."));

        // Pagination
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "&ePage Précédente (" + page + ")"));
        }
        if (page < maxPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, "&ePage Suivante (" + (page + 2) + ")"));
        }

        inv.setItem(53, createItem(Material.BARRIER, "&cAnnuler"));
        player.openInventory(inv);
    }

    public void openVariantEditor(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("VARIANT_EDIT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Édition: " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createItem(Material.NAME_TAG, "&aType de base", "&7Actuel: &e" + var.getType().name(), "", "&eClic pour changer"));
        inv.setItem(11, createItem(Material.ANVIL, "&aPoids d'apparition", "&7Actuel: &a" + var.getWeight(), "", "&eClic pour changer"));
        inv.setItem(12, createItem(Material.DAYLIGHT_DETECTOR, "&aImmunité Solaire", "&7Actuel: &e" + (var.isIgnoreSunlight() ? "Oui" : "Non"), "", "&eClic pour basculer"));
        inv.setItem(13, createItem(Material.EXPERIENCE_BOTTLE, "&aXP Donnée à la mort",
                "&7Actuel : &e" + (var.getXpOnDeath() < 0 ? "Vanilla" : var.getXpOnDeath() + " XP"),
                "",
                "&aClic Gauche: &f+1   &cClic Droit: &f-1",
                "&aShift+Clic Gauche: &f+10  &cShift+Clic Droit: &f-10",
                "&eClic Milieu: &fSaisir dans le tchat"));
        inv.setItem(14, createItem(Material.CLOCK, "&bConditions de Spawn", "&7Configurer la météo, le jour/nuit", "&7et les biomes autorisés.", "", "&eClic pour configurer"));
        
        inv.setItem(20, createItem(Material.DIAMOND_SWORD, "&cModifier Stats (Absolues)", "&7PV, Dégâts, Vitesse,", "&7Knockback et Portée."));
        inv.setItem(21, createItem(Material.NETHER_STAR, "&bComportements (AI)", "&7Agressivité, fuite, charge,", "&7vision murs et renforts mort."));
        inv.setItem(22, createItem(Material.GLOWSTONE_DUST, "&dEsthétique & Cosmétique", "&7Skins, taille, auras, noms,", "&7BossBar et bébés."));
        
        String eqWarning = "";
        if (!supportsEquipment(var.getType())) {
            eqWarning = "\n&cAttention: Ce type de mob ne peut pas\n&cporter d'équipement en Vanilla (visuellement).";
        }
        inv.setItem(23, createItem(Material.CHEST, "&6Drops, Audio & Équipement", "&7Chance d'équipements,", "&7sons et drops personnalisés." + eqWarning));
        
        // Bouton spawn avec description claire des modes
        inv.setItem(24, createItem(Material.ZOMBIE_SPAWN_EGG, "&bFaire apparaître", 
                "&7Invoque ce monstre 2 blocs devant vous.",
                "",
                "&aClic Gauche: &fVisualiser (Féru/Statique)",
                "&cClic Droit: &fNormal (IA activée)"));

        inv.setItem(31, createItem(Material.LAVA_BUCKET, "&cSupprimer la variante", "&4Attention: Action immédiate"));
        inv.setItem(32, createItem(Material.PAPER, "&bCopier la variante", "&7Cloner cette variante avec", "&7tous ses paramètres sous", "&7un nouvel ID.", "", "&eClic pour cloner"));
        inv.setItem(35, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Stats: " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createModifierItem(Material.APPLE, "&cPV (Santé Absolue)", mods.getHealthValue()));
        inv.setItem(11, createModifierItem(Material.IRON_SWORD, "&cDégâts Absolus", mods.getDamageValue()));
        inv.setItem(12, createModifierItem(Material.FEATHER, "&bVitesse Absolue", mods.getSpeedValue()));
        inv.setItem(13, createModifierItem(Material.ENDER_EYE, "&eDétection Absolue", mods.getFollowRangeValue()));
        inv.setItem(14, createModifierItem(Material.SHIELD, "&7Knockback Absolu", mods.getKnockbackValue()));

        inv.setItem(15, createItem(Material.IRON_CHESTPLATE, "&aÉquipement (Armure)", "&7Tier: &e" + mods.getEquipmentTier(), "&7Chance: &a" + (mods.getEquipmentChance()*100) + "%", "", "&eClic pour changer"));
        inv.setItem(16, createModifierItem(Material.GOLDEN_APPLE, "&aRégénération Absolue", mods.getRegenerationValue()));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openVariantBehaviors(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        StatModifiers mods = var.getModifiers();

        WDMenuHolder holder = new WDMenuHolder("VARIANT_BEHAVIORS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("IA & Comportement: " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.ENDER_PEARL, "&eTéléportation cible", mods.isTeleportToTarget()));
        inv.setItem(11, createToggleItem(Material.SADDLE, "&bCharge/Dash", mods.isDashAttack()));
        inv.setItem(12, createToggleItem(Material.GLASS_BOTTLE, "&dInvisibilité (Camouflage)", mods.isCamouflage()));
        inv.setItem(13, createToggleItem(Material.REDSTONE_TORCH, "&cVision à travers murs", mods.isGoalWallVision()));
        inv.setItem(14, createToggleItem(Material.GOLDEN_APPLE, "&aRégénération Passive", mods.getPassiveRegen() > 0));
        inv.setItem(15, createToggleItem(Material.FIRE_CHARGE, "&6Explosion à la mort", mods.isExplodeOnDeath()));
        inv.setItem(16, createToggleItem(Material.BOW, "&7Attaque à distance", mods.isRangedAttack()));
        inv.setItem(17, createItem(Material.POTION, "&dEffets de Potion", "&7Gérer les effets de potion passifs", "", "&eClic pour gérer"));

        // Mode d'agressivité
        String agg = var.getAggroMode();
        Material aggMat = agg.equalsIgnoreCase("AGGRESSIVE") ? Material.REDSTONE_BLOCK : (agg.equalsIgnoreCase("PASSIVE") ? Material.SLIME_BLOCK : Material.GOLD_BLOCK);
        inv.setItem(18, createItem(aggMat, "&bAgressivité IA", "&7Actuel : &e" + agg, "", "&eClic pour basculer", "&7(PASSIVE -> AGGRESSIVE -> NEUTRAL_HIT -> NEUTRAL_SQUAD_HIT)"));

        // Fuite
        inv.setItem(19, createItem(Material.FEATHER, "&cFuite Seuil PV", 
                "&7Seuil de vie actuel : &e" + String.format("%.0f%%", mods.getFleeUnderHealth() * 100), 
                "", 
                "&aClic Gauche: &f+5%   &cClic Droit: &f-5%", 
                "&eClic Milieu: &fDéfinir précis (Tchat)"));

        inv.setItem(20, createToggleItem(Material.RABBIT_FOOT, "&aFuite si Solo (Isolé)", mods.isFleeWhenSolo()));

        // Spawn renforts à la mort
        inv.setItem(21, createItem(Material.SPIDER_EYE, "&dSpawns Renforts à la Mort", 
                "&7Variante: &e" + mods.getDeathSpawnVariant(),
                "&7Quantité: &a" + mods.getDeathSpawnAmount(),
                "",
                "&aClic Gauche: &fDéfinir Variante",
                "&cClic Droit: &fChanger Quantité",
                "&7(Shift: +5, Simple: +1)",
                "&eClic Milieu: &fRéinitialiser (none/0)"));

        // Jump AI
        inv.setItem(22, createToggleItem(Material.LEATHER_BOOTS, "&aIA de Saut", mods.isJumpAttack()));

        // Max usages limits
        inv.setItem(23, createItem(Material.ENDER_PEARL, "&eTéléportation Max Usages", 
                "&7Actuel : &e" + (mods.getTeleportMaxUses() == -1 ? "Infini" : mods.getTeleportMaxUses()),
                "",
                "&aClic Gauche: &f+1   &cClic Droit: &f-1",
                "&aShift+Clic: &f+5   &cShift+Clic: &f-5",
                "&eClic Milieu: &fDéfinir précis"));

        inv.setItem(24, createItem(Material.SADDLE, "&bCharge Max Usages", 
                "&7Actuel : &e" + (mods.getDashMaxUses() == -1 ? "Infini" : mods.getDashMaxUses()),
                "",
                "&aClic Gauche: &f+1   &cClic Droit: &f-1",
                "&aShift+Clic: &f+5   &cShift+Clic: &f-5",
                "&eClic Milieu: &fDéfinir précis"));

        // Ranged attack projectile type
        inv.setItem(25, createItem(Material.ARROW, "&7Projectile à distance", 
                "&7Type actuel : &e" + mods.getRangedAttackType(),
                "", "&eClic pour changer"));

        // On Hit potion effect
        inv.setItem(26, createItem(Material.DRAGON_BREATH, "&dEffet sur coup donné (On Hit)", 
                "&7Effet: &e" + mods.getOnHitPotionEffect(),
                "&7Chance: &a" + String.format("%.0f%%", mods.getOnHitPotionChance()*100),
                "&7Niveau/Durée: &dLevel " + (mods.getOnHitPotionAmplifier()+1) + " / " + (mods.getOnHitPotionDuration()/20) + "s",
                "",
                "&aClic Gauche: &fChanger Effet",
                "&cClic Droit: &fChance +5%",
                "&aShift+Clic Gauche: &fDurée +1s",
                "&cShift+Clic Droit: &fNiveau +1",
                "&eClic Milieu: &fRéinitialiser"));

        inv.setItem(27, createToggleItem(Material.SCAFFOLDING, "&aEscalade Intelligente", mods.isSmartClimb()));

        inv.setItem(35, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Esthétique: " + variantId));
        holder.setInventory(inv);

        // Noms
        inv.setItem(10, createItem(Material.NAME_TAG, "&aNom d'affichage", "&7Actuel: &e" + (var.getDisplayName() != null ? var.getDisplayName() : "Par défaut"), "", "&eClic pour changer (Tchat)"));
        inv.setItem(11, createItem(Material.WRITABLE_BOOK, "&aNoms Conditionnels", "&7Gérer les noms selon les PV", "", "&eClic pour gérer"));

        // Skin & Model
        int cmd = var.getCustomModelData();
        inv.setItem(12, createItem(Material.ITEM_FRAME, "&bSkin (Custom Model Data)", 
                "&7Actuel: &e" + (cmd > 0 ? cmd : "Aucun"),
                "&7Permet d'associer un modèle 3D",
                "&7personnalisé de Resource Pack.",
                "",
                "&eClic pour ouvrir le sélecteur"));
        
        String headSkin = mods.getSkullSkin();
        inv.setItem(13, createItem(Material.PLAYER_HEAD, "&bBanque de Skins / Têtes", 
                "&7Actuel: &e" + ("none".equals(headSkin) ? "Par défaut" : (headSkin.length() > 20 ? "Skin Custom" : headSkin)),
                "&7Appliquez des skins personnalisés",
                "&7sur la tête du monstre.",
                "",
                "&eClic pour ouvrir la banque"));

        // Bébé
        if (supportsBaby(var.getType())) {
            inv.setItem(14, createToggleItem(Material.EGG, "&aToggle Bébé", var.isBaby()));
        } else {
            inv.setItem(14, createItem(Material.BARRIER, "&cBébé non supporté", "&7Ce type de mob de base", "&7ne supporte pas de forme bébé."));
        }

        // Scale (Taille)
        inv.setItem(15, createItem(Material.SLIME_BLOCK, "&aTaille (Scale)", 
                "&7Actuelle : &b" + String.format("%.2f", var.getScale()),
                "",
                "&aClic Gauche: &f+0.1   &cClic Droit: &f-0.1",
                "&aShift+Clic Gauche: &f+1.0",
                "&cShift+Clic Droit: &f-1.0",
                "&eClic Milieu (Tchat): &fDéfinir précis"));

        inv.setItem(16, createItem(Material.HONEY_BLOCK, "&aVariance de Taille", 
                "&7Actuelle : &e" + String.format("%.2f", var.getScaleVariance()),
                "",
                "&aClic Gauche: &f+0.1   &cClic Droit: &f-0.1",
                "&aShift+Clic Gauche: &f+0.5",
                "&cShift+Clic Droit: &f-0.5",
                "&eClic Milieu (Tchat): &fDéfinir précis"));

        // Skin & Trait Spécial Vanilla (Fusionné)
        String curSub = mods.getEntitySubtype();
        inv.setItem(17, createItem(Material.PAINTING, "§bSkin / Variante & Trait Spécial Vanilla",
                "§7Configuration actuelle : §e" + ("none".equalsIgnoreCase(curSub) ? "Par défaut" : curSub),
                "§7Choisissez la sous-variante / skin",
                "§7ou le trait esthétique (Creeper chargé, Golem,",
                "§7Loup, Chat, Poule, Vache, Villager, etc.)",
                "",
                "§eClic pour ouvrir le sélecteur de skin"));

        // Particules
        inv.setItem(19, createItem(Material.DRAGON_BREATH, "&dParticules Aura", "&7Type: &e" + mods.getParticleAuraType(), "&7Couleur: &b" + mods.getParticleAuraColor(), "", "&eClic pour changer"));
        inv.setItem(20, createItem(Material.FIREWORK_ROCKET, "&aParticule Spawn", "&7Type: &e" + mods.getParticleSpawnType(), "", "&eClic pour changer"));
        inv.setItem(21, createItem(Material.BONE_MEAL, "&cParticule Trail", "&7Type: &e" + mods.getParticleTrailType(), "", "&eClic pour changer"));

        // BossBar
        inv.setItem(22, createToggleItem(Material.WRITABLE_BOOK, "&bBossBar Active", mods.isBossBarEnabled()));
        inv.setItem(23, createItem(Material.RED_DYE, "&cBossBar Couleur", "&7Couleur: &e" + mods.getBossBarColor(), "", "&eClic pour changer"));
        inv.setItem(24, createItem(Material.ANVIL, "&7BossBar Style", "&7Style: &e" + mods.getBossBarStyle(), "", "&eClic pour changer"));

        inv.setItem(35, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Sélecteur de Skin: " + variantId));
        holder.setInventory(inv);

        List<fr.wilddifficulty.util.EntitySubtypeUtil.SubtypeOption> options = fr.wilddifficulty.util.EntitySubtypeUtil.getAvailableSubtypes(var.getType());
        String currentSubtype = mods.getEntitySubtype();

        // Option 0: Par défaut / Reset
        inv.setItem(0, createItem(Material.BARRIER, "§cPar Défaut (Aucun Skin Spécifique)",
                "§7Skin/Variante de base du jeu",
                "none".equalsIgnoreCase(currentSubtype) ? "§a✔ Sélectionné" : "",
                "",
                "§eClic pour réinitialiser"));

        if (options.isEmpty()) {
            inv.setItem(13, createItem(Material.PAPER, "§eAucune variante prédéfinie pour " + var.getType().name(),
                    "§7Ce type de mob n'a pas de variante",
                    "§7vanilla intégrée dans Minecraft.",
                    "§7Utilisez la Banque de Skins / Têtes ou les",
                    "§7Custom Model Data pour le personnaliser !"));
        } else {
            int slot = 1;
            for (fr.wilddifficulty.util.EntitySubtypeUtil.SubtypeOption opt : options) {
                if (slot >= 35) break;
                Material mat = Material.matchMaterial(opt.iconMaterial());
                if (mat == null) mat = Material.NAME_TAG;

                boolean selected = opt.id().equalsIgnoreCase(currentSubtype);
                inv.setItem(slot, createItem(mat, "§b" + opt.displayName(),
                        "§7ID interne : §e" + opt.id(),
                        selected ? "§a✔ Sélectionné" : "§7Non sélectionné",
                        "",
                        "§eClic pour choisir cette variante"));
                slot++;
            }
        }

        inv.setItem(35, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openSkinHeadBank(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_SKIN_HEAD", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("WD - Têtes / Skins"));
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

        inv.setItem(20, createItem(Material.NAME_TAG, "&aPseudo personnalisé (Tchat)", "&7Entrer le pseudo d'un joueur", "&7pour récupérer son skin.", "", "&eClic pour entrer"));
        inv.setItem(21, createItem(Material.PAPER, "&bLien URL / Texture Base64 (Tchat)", "&7Entrer une URL minecraft.net", "&7ou une texture base64.", "", "&eClic pour entrer"));
        inv.setItem(22, createItem(Material.BARRIER, "&cRetirer le Skin de Tête", "&7Remet la tête par défaut.", "", "&eClic pour réinitialiser"));
        inv.setItem(23, createItem(Material.CHEST, "&aGlisser/Déposer une Tête", "&7Déposez un item de tête (Skull) ici", "&7pour copier sa texture et l'appliquer", "&7à cette variante. Elle sera ajoutée", "&7à la banque."));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openVariantDropsAudio(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("VARIANT_DROPS_AUDIO", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Drops & Sons: " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createItem(Material.DIAMOND_CHESTPLATE, "&bÉquipement Spécifique", "&7Gérer casque, plastron, jambières, bottes, main principale/secondaire.", "", "&eClic pour configurer"));
        inv.setItem(11, createItem(Material.CHEST, "&6Tables de Drops", "&7Gérer les loots personnalisés", "", "&eClic pour gérer"));
        
        // Sounds buttons
        inv.setItem(13, createItem(Material.JUKEBOX, "&bSon d'Ambiance", "&7Actuel: &e" + getSoundKey(var, "ambient"), "", "&eClic pour changer"));
        inv.setItem(14, createItem(Material.JUKEBOX, "&bSon de Dégâts/Aggro", "&7Actuel: &e" + getSoundKey(var, "aggro"), "", "&eClic pour changer"));
        inv.setItem(15, createItem(Material.JUKEBOX, "&bSon de Mort", "&7Actuel: &e" + getSoundKey(var, "death"), "", "&eClic pour changer"));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openVariantEquipmentEditor(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        StatModifiers mods = var.getModifiers();

        WDMenuHolder holder = new WDMenuHolder("VARIANT_EQUIPMENT_EDIT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Équipement: " + variantId));
        holder.setInventory(inv);

        inv.setItem(10, createEquipmentSlotItem(mods.getHelmetItem(), mods.getHelmetChance(), "Casque", Material.LEATHER_HELMET));
        inv.setItem(11, createEquipmentSlotItem(mods.getChestplateItem(), mods.getChestplateChance(), "Plastron", Material.LEATHER_CHESTPLATE));
        inv.setItem(12, createEquipmentSlotItem(mods.getLeggingsItem(), mods.getLeggingsChance(), "Jambières", Material.LEATHER_LEGGINGS));
        inv.setItem(13, createEquipmentSlotItem(mods.getBootsItem(), mods.getBootsChance(), "Bottes", Material.LEATHER_BOOTS));
        inv.setItem(14, createEquipmentSlotItem(mods.getMainHandItem(), mods.getMainHandChance(), "Main Principale", Material.IRON_SWORD));
        inv.setItem(15, createEquipmentSlotItem(mods.getOffHandItem(), mods.getOffHandChance(), "Main Secondaire", Material.SHIELD));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    private ItemStack createEquipmentSlotItem(String item, double chance, String label, Material def) {
        Material mat = Material.matchMaterial(item.toUpperCase());
        if (mat == null || mat == Material.AIR) mat = def;
        return createItem(mat, "&b" + label + " : &e" + item,
                "&7Chance d'obtention: &a" + String.format("%.2f", chance),
                "",
                "&aClic Gauche: &fChoisir l'objet",
                "&cClic Droit: &fChance +0.1   &cShift+Clic Droit: &fChance +0.01",
                "&cClic Milieu: &fChance -0.1   &cShift+Clic Milieu: &fChance -0.01");
    }

    public void openEquipmentItemSelector(Player player, String variantId, String slotName) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_EQ_" + slotName.toUpperCase(), variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Choisir : " + slotName));
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
            inv.setItem(slot++, createItem(mat, "&f" + c, "&7Sélectionner cet objet", "&7ou cliquez sur un objet de votre", "&7inventaire pour le choisir."));
        }

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text("Particule : " + fieldName));
        holder.setInventory(inv);

        String[] particles = {
            "FLAME", "SOUL_FIRE_FLAME", "HEART", "WITCH", "PORTAL", 
            "HAPPY_VILLAGER", "CLOUD", "SMOKE", "CRIT", "REVERSE_PORTAL", 
            "SOUL", "NONE"
        };

        int slot = 0;
        for (String p : particles) {
            Material mat = p.equals("NONE") ? Material.BARRIER : Material.GLOWSTONE_DUST;
            inv.setItem(slot++, createItem(mat, "&b" + p, "&7Sélectionner cette particule"));
        }
        inv.setItem(17, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 2. BossBar Color selector
    public void openBossBarColorSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_BB_COLOR", variantId);
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Couleur BossBar"));
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
            inv.setItem(slot++, createItem(mat, "&f" + c, "&7Sélectionner cette couleur"));
        }
        inv.setItem(8, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 3. BossBar Style selector
    public void openBossBarStyleSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_BB_STYLE", variantId);
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Style BossBar"));
        holder.setInventory(inv);

        String[] styles = { "PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20" };
        int slot = 0;
        for (String s : styles) {
            inv.setItem(slot++, createItem(Material.ANVIL, "&f" + s, "&7Sélectionner ce style"));
        }
        inv.setItem(8, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 4. Potion Effects Selector
    public void openPotionSelector(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        List<String> activeEffects = var.getModifiers().getPotionEffects();

        WDMenuHolder holder = new WDMenuHolder("SELECT_POTIONS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text("Effets de potion"));
        holder.setInventory(inv);

        String[] effects = { "SPEED", "INCREASE_DAMAGE", "DAMAGE_RESISTANCE", "FIRE_RESISTANCE", "REGENERATION", "INVISIBILITY", "GLOWING" };
        int slot = 0;
        for (String eff : effects) {
            boolean active = activeEffects.contains(eff.toUpperCase());
            inv.setItem(slot++, createItem(active ? Material.GLOWSTONE_DUST : Material.REDSTONE, 
                    "&f" + eff, 
                    "&7Statut : " + (active ? "&aActivé" : "&cDésactivé"),
                    "", "&eClic pour basculer"));
        }
        inv.setItem(17, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openOnHitPotionSelector(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;
        String activeEffect = var.getModifiers().getOnHitPotionEffect();

        WDMenuHolder holder = new WDMenuHolder("SELECT_ONHIT_POTION", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text("Effet On-Hit"));
        holder.setInventory(inv);

        String[] effects = { "POISON", "WITHER", "BLINDNESS", "SLOW", "WEAKNESS", "CONFUSION", "LEVITATION", "none" };
        int slot = 0;
        for (String eff : effects) {
            boolean active = activeEffect != null && activeEffect.equalsIgnoreCase(eff);
            Material mat = eff.equals("none") ? Material.BARRIER : Material.GLOWSTONE_DUST;
            inv.setItem(slot++, createItem(mat, 
                    active ? "&a[Actif] " + eff : "&7" + eff, 
                    "&7Statut : " + (active ? "&aSélectionné" : "&cDésactivé"),
                    "", "&eClic pour choisir"));
        }
        inv.setItem(17, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 5. Sound Selector
    public void openSoundSelector(Player player, String variantId, String soundType) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_SOUND_" + soundType.toUpperCase(), variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text("Son : " + soundType));
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
            inv.setItem(slot++, createItem(mat, "&e" + s, "&7Sélectionner ce son"));
        }
        inv.setItem(17, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 6. Equipment Tier Selector
    public void openEquipmentTierSelector(Player player, String contextId, String contextType) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_EQUIP_" + contextType.toUpperCase(), contextId);
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Tier d'équipement"));
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
            inv.setItem(slot++, createItem(mat, "&f" + t, "&7Sélectionner ce tier"));
        }
        inv.setItem(8, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 7. Conditional Names List Editor
    public void openVariantConditionalNames(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("CONDITIONAL_NAMES", variantId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Noms Conditionnels"));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant.ConditionalName cn : var.getConditionalNames()) {
            if (slot >= 45) break;
            int pct = (int) Math.round(cn.getThreshold() * 100);
            inv.setItem(slot++, createItem(Material.WRITABLE_BOOK, "&fSous &e" + pct + "% &fde vie", 
                    "&7Nom: " + cn.getName(), 
                    "", "&cClic pour supprimer"));
        }

        inv.setItem(49, createItem(Material.EMERALD, "&a+ Ajouter un nom", "&7Ajouter un nom conditionnel"));
        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 8. Custom Drops List Editor
    public void openVariantDrops(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("CUSTOM_DROPS", variantId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Drops Personnalisés"));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant.CustomDrop cd : var.getCustomDrops()) {
            if (slot >= 45) break;
            Material mat = Material.matchMaterial(cd.getMaterialName());
            if (mat == null) mat = Material.PAPER;
            inv.setItem(slot++, createItem(mat, "&e" + cd.getMaterialName(), 
                    "&7Chance: &a" + (cd.getChance()*100) + "%", 
                    "&7Quantité: &b" + cd.getMinAmount() + " - " + cd.getMaxAmount(), 
                    "&7XP: &d" + cd.getXp(),
                    "&7Condition: &c" + cd.getDeathCondition(),
                    "", "&cClic pour supprimer"));
        }

        inv.setItem(49, createItem(Material.EMERALD, "&a+ Ajouter un drop", "&7Ajouter un drop par liste cliquable"));
        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // 9. Choice of item to drop
    public void openDropMaterialSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_DROP_MAT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text("Choisir Objet à Looter"));
        holder.setInventory(inv);

        Material[] drops = {
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT, 
            Material.NETHERITE_SCRAP, Material.ARROW, Material.COAL, Material.BONE, 
            Material.ROTTEN_FLESH, Material.STRING, Material.GUNPOWDER, Material.ENDER_PEARL
        };

        int slot = 0;
        for (Material m : drops) {
            inv.setItem(slot++, createItem(m, "&e" + m.name(), "&7Sélectionner cet objet"));
        }
        inv.setItem(17, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("WD - Escouades"));
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

            inv.setItem(slot++, createItem(mat, "&f" + sq.getId(), lore.toArray(new String[0])));
        }
        
        inv.setItem(49, createItem(Material.EMERALD, "&a+ Nouvelle Escouade", "&7Créer une nouvelle escouade"));
        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openSquadEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Édition: " + squadId));
        holder.setInventory(inv);

        // Chance de spawn avec +/- clics
        inv.setItem(10, createItem(Material.GOLD_NUGGET, "&aChance de spawn", 
                "&7Actuelle : &a" + String.format("%.1f", sq.getSpawnChance() * 100) + "%",
                "",
                "&aClic Gauche: &f+1.0%   &cClic Droit: &f-1.0%",
                "&aShift+Clic Gauche: &f+10.0%",
                "&cShift+Clic Droit: &f-10.0%",
                "&eClic Milieu (Tchat): &fDéfinir précis"));

        inv.setItem(13, createItem(Material.ZOMBIE_HEAD, "&aMembres de l'Escouade", "&7Gérer les variantes membres", "", "&eClic pour gérer"));
        inv.setItem(14, createItem(Material.NETHER_STAR, "&dBonus d'Escouade", 
                "&7Gérer les multiplicateurs de PV,",
                "&7Dégâts, Vitesse et Regen",
                "&7pour tous les membres.",
                "", 
                "&eClic pour gérer"));
        inv.setItem(15, createItem(Material.PAPER, "&aTypes Déclencheurs", "&7Gérer les entités déclencheurs", "", "&eClic pour gérer"));
        inv.setItem(16, createItem(Material.SKELETON_SPAWN_EGG, "&bFaire apparaître", "&7Invoque cette escouade devant vous"));

        inv.setItem(22, createItem(Material.LAVA_BUCKET, "&cSupprimer l'escouade", "&4Attention: Action immédiate"));
        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openSquadBonusesEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_BONUSES_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Bonus : " + squadId));
        holder.setInventory(inv);

        inv.setItem(10, createModifierItem(Material.APPLE, "&cBonus PV", sq.getBonusHealth()));
        inv.setItem(11, createModifierItem(Material.IRON_SWORD, "&cBonus Dégâts", sq.getBonusDamage()));
        inv.setItem(12, createModifierItem(Material.FEATHER, "&bBonus Vitesse", sq.getBonusSpeed()));
        inv.setItem(13, createModifierItem(Material.GOLDEN_APPLE, "&aBonus Regen (HP/s)", sq.getBonusRegen()));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // New Menu to Edit Squad Members in GUI
    public void openSquadMembersEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_MEMBERS_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Membres : " + squadId));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            Material mat = getVariantHeadMaterial(var.getType());
            MobSquad.SquadMemberRange range = sq.getMembers().get(var.getId());
            if (range != null) {
                inv.setItem(slot++, createItem(mat, "&a" + var.getId(), 
                        "&7Statut: &aMembre",
                        "&7Min: &e" + range.getMin() + "   &7Max: &e" + range.getMax(),
                        "",
                        "&aClic Gauche: &fMin +1   &cClic Droit: &fMin -1",
                        "&aShift+Clic Gauche: &fMax +1",
                        "&cShift+Clic Droit: &fMax -1",
                        "&eClic Milieu: &cRetirer de l'escouade"));
            } else {
                inv.setItem(slot++, createItem(Material.BARRIER, "&c" + var.getId(), 
                        "&7Statut: &7Non membre",
                        "",
                        "&eClic pour ajouter à l'escouade"));
            }
        }

        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // New Menu to Edit Squad Trigger EntityTypes in GUI
    public void openSquadTriggersEditor(Player player, String squadId) {
        MobSquad sq = plugin.getVariantManager().getSquad(squadId);
        if (sq == null) return;

        WDMenuHolder holder = new WDMenuHolder("SQUAD_TRIGGERS_EDIT", squadId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Déclencheurs : " + squadId));
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
                    "&f" + type.name(), 
                    "&7Statut : " + (active ? "&aActivé" : "&cDésactivé"),
                    "", "&eClic pour basculer"));
        }

        inv.setItem(35, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    // ================= ZONES =================
    public void openZoneList(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ZONE_LIST", null);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("WD - Zones"));
        holder.setInventory(inv);

        int slot = 0;
        for (DifficultyZone zone : plugin.getZoneManager().getAllZones()) {
            if (slot >= 45) break;
            inv.setItem(slot++, createItem(Material.MAP, "&f" + zone.getId(),
                    "&7Type: &e" + zone.getType().name(),
                    "&7Priorité: &a" + zone.getPriority(),
                    "&7Monde: &b" + zone.getWorld(),
                    "", "&eClic pour éditer"));
        }

        inv.setItem(48, createItem(Material.GOLDEN_HOE, "&eOuvrir l'outil de création (Houe)", "&7Obtenir l'outil de zone en or pour", "&7dessiner les points du polygone.", "", "&eClic pour obtenir"));
        inv.setItem(49, createItem(Material.EMERALD, "&a+ Nouvelle Zone", "&7Créer une nouvelle zone de difficulté"));
        inv.setItem(51, createToggleItem(Material.GLOWSTONE_DUST, "&eParticules Bordure Zone", plugin.getMainConfigManager().isZoneBorderParticles()));
        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openZoneEditor(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_EDIT", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Zone: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.SHIELD, "&aZone Sûre (Safe Zone)", zone.isSafeZone()));
        inv.setItem(11, createToggleItem(Material.DAYLIGHT_DETECTOR, "&aIgnorer Règles Biome", zone.isOverrideBiomeRules()));
        
        // Priorité avec +/- clics
        inv.setItem(12, createItem(Material.ANVIL, "&aPriorité", 
                "&7Actuelle : &a" + zone.getPriority(),
                "",
                "&aClic Gauche: &f+1   &cClic Droit: &f-1",
                "&aShift+Clic Gauche: &f+5",
                "&cShift+Clic Droit: &f-5",
                "&eClic Milieu (Tchat): &fDéfinir précis"));

        // Type de Forme (slot 13)
        inv.setItem(13, createItem(Material.MAP, "&bType de Forme", 
                "&7Type actuel : &e" + zone.getType().name(), 
                "", 
                "&eClic pour changer (CUBOID -> RADIUS -> POLYGON)"));

        inv.setItem(14, createItem(Material.COMPASS, "&bDéfinir Pos 1 (Ici)", "&7Assigne la Position 1 de la zone", "&7à votre position actuelle.", "", "&eClic pour définir"));
        inv.setItem(15, createItem(Material.COMPASS, "&bDéfinir Pos 2 (Ici)", "&7Assigne la Position 2 de la zone", "&7à votre position actuelle.", "", "&eClic pour définir"));

        String tpType = zone.hasCustomTeleport() ? "point de TP personnalisé" : "point central calculé";
        inv.setItem(16, createItem(Material.ENDER_PEARL, "&aSe téléporter à la zone", 
                "&7Téléporte instantanément au", 
                "&7" + tpType + " de la zone.", 
                "", "&eClic pour se téléporter"));
        inv.setItem(17, createToggleItem(Material.IRON_DOOR, "&aTraverser Zone (Mobs)", zone.isMobsCanCross()));
        
        // Scaling extérieur par stat (slot 18) — ouvre une page dédiée
        boolean hasAnyScaling = zone.hasExtScaling();
        String scalingBadge = hasAnyScaling ? "§a✔ Configuré" : "§7✖ Non configuré";
        inv.setItem(18, createItem(Material.COMPARATOR, "§bScaling Extérieur par Stat",
                "§7Augmentation de difficulté par tranche",
                "§7de blocs au-delà des limites de la zone.",
                "",
                "§7Statut: " + scalingBadge,
                "",
                "§eClic pour configurer"));

        inv.setItem(19, createItem(Material.GLOWSTONE_DUST, "&aHauteur Particules Bordure", 
                "&7Hauteur relative au sol: &e" + String.format("%.2f", zone.getParticleHeightOffset()) + " blocs", 
                "", 
                "&aClic Gauche: &f+0.05   &cClic Droit: &f-0.05", 
                "&eClic Milieu (Tchat): &fDéfinir précis"));
                
        inv.setItem(22, createItem(Material.COMPASS, "&bDéfinir point de TP (Ici)", 
                "&7Assigne le point de téléportation", 
                "&7de cette zone à vos coordonnées actuelles.", 
                "&7Actuel : " + (zone.hasCustomTeleport() ? String.format("%.1f, %.1f, %.1f", zone.getTeleportX(), zone.getTeleportY(), zone.getTeleportZ()) : "&eCalculé (Centre)"), 
                "", "&eClic pour définir"));

        inv.setItem(23, createItem(Material.GOLDEN_HOE, "&eOuvrir l'outil de création (Houe)", 
                "&7Obtenir l'outil de zone en or pour", 
                "&7dessiner la forme en jeu :",
                "  &e- CUBOID : &7Clic Droit = Pos 1, Clic Gauche = Pos 2",
                "  &e- RADIUS : &7Clic Gauche = Centre, Clic Droit = Rayon",
                "  &e- POLYGON : &7Clic Droit = Ajouter point, Gauche = Fin",
                "", "&eClic pour obtenir"));

        inv.setItem(20, createItem(Material.DIAMOND_SWORD, "&cModifier Stats (Absolues)", "&7PV, Dégâts, Vitesse, Knockback, Portée."));

        // Particules de zone (slots 21, 24, 25)
        inv.setItem(21, createToggleItem(Material.GLOWSTONE_DUST, "&eActiver Particules Bordure", zone.isParticlesEnabled()));
        inv.setItem(24, createItem(Material.WRITABLE_BOOK, "&aParticule Quand On Rentre", 
                "&7Actuelle : &d" + zone.getParticleInside(), 
                "", 
                "&eClic pour la modifier"));
        inv.setItem(25, createItem(Material.WRITABLE_BOOK, "&cParticule Quand On Sort", 
                "&7Actuelle : &d" + zone.getParticleOutside(), 
                "", 
                "&eClic pour la modifier"));
        
        // Nouveaux boutons : Membres, Beacon Effects, Nid d'ennemis, Sous-sections (slots 26, 27, 28, 29)
        inv.setItem(26, createItem(Material.PLAYER_HEAD, "§bGestion des Membres", "§7Membres actuels: §a" + zone.getMembers().size(), "", "§eClic pour gérer les membres et leurs rôles"));
        inv.setItem(27, createItem(Material.BEACON, "§bEffets de Beacon", "§7Effets actifs: §e" + zone.getBeaconEffects().size(), "", "§eClic pour configurer les effets de beacon"));
        
        String nestBadge = zone.isDangerNest() ? "§c✔ Actif" : "§7✖ Inactif";
        inv.setItem(28, createItem(Material.NETHER_STAR, "§cNid d'Ennemis (Danger Nest)", "§7Statut: " + nestBadge, "", "§eClic pour configurer le mode Nid d'Ennemis"));
        inv.setItem(29, createItem(Material.MAP, "§bSous-sections (Multi-sections)", "§7Sections additionnelles: §e" + zone.getSubSections().size(), "", "§eClic pour fusionner et étendre la zone"));

        inv.setItem(31, createItem(Material.LAVA_BUCKET, "&cSupprimer la zone", "&4Attention: Action immédiate"));
        inv.setItem(35, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openZoneParticleSelector(Player player, String zoneId, String type) {
        WDMenuHolder holder = new WDMenuHolder("ZONE_PARTICLE_" + type.toUpperCase(), zoneId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text("Particule Zone (" + type + ")"));
        holder.setInventory(inv);

        String[] particles = {
            "FLAME", "SOUL_FIRE_FLAME", "HEART", "WITCH", "PORTAL", 
            "HAPPY_VILLAGER", "CLOUD", "SMOKE", "CRIT", "REVERSE_PORTAL", 
            "SOUL", "COMPOSTER", "NONE"
        };

        int slot = 0;
        for (String p : particles) {
            Material mat = p.equals("NONE") ? Material.BARRIER : Material.GLOWSTONE_DUST;
            inv.setItem(slot++, createItem(mat, "&b" + p, "&7Sélectionner cette particule"));
        }
        inv.setItem(17, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Stats Zone: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createModifierItem(Material.APPLE, "&cPV (Santé Absolue)", mods.getHealthValue()));
        inv.setItem(11, createModifierItem(Material.IRON_SWORD, "&cDégâts Absolus", mods.getDamageValue()));
        inv.setItem(12, createModifierItem(Material.FEATHER, "&bVitesse Absolue", mods.getSpeedValue()));
        inv.setItem(13, createModifierItem(Material.ENDER_EYE, "&eDétection Absolue", mods.getFollowRangeValue()));
        inv.setItem(14, createModifierItem(Material.SHIELD, "&7Knockback Absolu", mods.getKnockbackValue()));

        inv.setItem(15, createItem(Material.IRON_CHESTPLATE, "&aÉquipement (Armure)", "&7Tier: &e" + mods.getEquipmentTier(), "&7Chance: &a" + (mods.getEquipmentChance()*100) + "%", "", "&eClic pour changer"));
        inv.setItem(16, createModifierItem(Material.GOLDEN_APPLE, "&aRégénération Absolue", mods.getRegenerationValue()));

        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
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
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createModifierItem(Material mat, String name, double value) {
        String valStr = (value == -1.0) ? "&7(Non défini / Vanilla)" : "&e" + String.format("%.2f", value);
        return createItem(mat, name, 
                "&7Valeur actuelle : " + valStr,
                "",
                "&aClic Gauche: &f+1.0   &cClic Droit: &f-1.0",
                "&aShift+Clic Gauche: &f+10.0",
                "&cShift+Clic Droit: &f-10.0",
                "&eClic Milieu (Tchat): &fDéfinir précis");
    }

    private ItemStack createToggleItem(Material mat, String name, boolean active) {
        return createItem(mat, name, 
                "&7Statut : " + (active ? "&aActivé" : "&cDésactivé"),
                "",
                "&eClic pour basculer");
    }

    public void openBiomeSpawnConfig(Player player, String biomeName) {
        WDMenuHolder holder = new WDMenuHolder("BIOME_SPAWN_CONFIG", biomeName);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Spawns : " + biomeName));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            boolean active = var.getAllowedBiomes().contains(biomeName.toUpperCase());
            Material mat = getVariantHeadMaterial(var.getType());
            inv.setItem(slot++, createItem(mat, 
                    active ? "&a[Actif] " + var.getId() : "&7[Inactif] " + var.getId(),
                    "&7Type: &f" + var.getType().name(),
                    "&7Statut: " + (active ? "&aAutorisé dans ce biome" : "&7Spawns standards (non autorisé)"),
                    "",
                    "&eClic pour basculer l'autorisation"));
        }

        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Conditions : " + variantId));
        holder.setInventory(inv);

        // Météo (slot 10)
        String w = var.getSpawnWeather();
        Material wMat = w.equalsIgnoreCase("RAINY") ? Material.WATER_BUCKET : (w.equalsIgnoreCase("CLEAR") ? Material.SUNFLOWER : Material.GLASS_BOTTLE);
        inv.setItem(10, createItem(wMat, "&bMétéo requise", "&7Actuel : &e" + w, "", "&eClic pour basculer (ANY -> RAINY -> CLEAR)"));

        // Moment journée (slot 12)
        String t = var.getSpawnTime();
        Material tMat = t.equalsIgnoreCase("DAY") ? Material.CAMPFIRE : (t.equalsIgnoreCase("NIGHT") ? Material.SOUL_CAMPFIRE : Material.CLOCK);
        inv.setItem(12, createItem(tMat, "&bMoment requis", "&7Actuel : &e" + t, "", "&eClic pour basculer (ANY -> DAY -> NIGHT)"));

        // Cave Spawn (slot 11)
        String c = var.getCaveSpawn();
        Material cMat = c.equalsIgnoreCase("ONLY_CAVES") ? Material.COBBLESTONE : (c.equalsIgnoreCase("NO_CAVES") ? Material.GRASS_BLOCK : Material.COAL_ORE);
        inv.setItem(11, createItem(cMat, "&bSpawn dans Caves", "&7Actuel : &e" + c, "", "&eClic pour basculer (ANY -> ONLY_CAVES -> NO_CAVES)"));

        // Biome actuel (slot 14)
        inv.setItem(14, createItem(Material.GRASS_BLOCK, "&aGérer les Biomes Autorisés", 
                "&7Actuellement : &e" + (var.getAllowedBiomes().isEmpty() ? "Tous (Par défaut)" : var.getAllowedBiomes().size() + " biomes spécifiés"), 
                "", "&eClic pour ouvrir la configuration des biomes"));

        // Biomes autorisés (slots 18-44)
        int slot = 18;
        for (String biome : var.getAllowedBiomes()) {
            if (slot >= 45) break;
            inv.setItem(slot++, createItem(Material.PAPER, "&f" + biome, "", "&cClic pour retirer ce biome"));
        }

        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openVariantBiomesEditor(Player player, String variantId, int page, String filter) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("VARIANT_BIOMES_EDIT", variantId + ":" + page + ":" + filter);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Biomes de " + variantId));
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
                    active ? "&a" + biome : "&7" + biome, 
                    "", 
                    active ? "&c[CLIC] Retirer" : "&a[CLIC] Ajouter"));
        }

        // Filtres
        inv.setItem(45, createItem(Material.LIME_DYE, "&aCatégorie: Iris", "&7Afficher les biomes Iris."));
        inv.setItem(46, createItem(Material.BLUE_DYE, "&bCatégorie: Minecraft/Vanilla", "&7Afficher les biomes Minecraft."));
        inv.setItem(47, createItem(Material.BOOK, "&fCatégorie: Tous", "&7Afficher tous les biomes."));

        // Navigation
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "&ePage Précédente (" + page + ")"));
        }
        
        String curB = getBiomeKeyString(player.getLocation());
        boolean hasCur = var.getAllowedBiomes().contains(curB.toUpperCase()) || var.getAllowedBiomes().contains(curB);
        inv.setItem(49, createItem(Material.COMPASS, "&6Biome Actuel : " + (hasCur ? "&aInclus" : "&cExclu"), 
                "&7Votre biome : &d" + curB, 
                "", 
                "&eClic pour l'ajouter/retirer"));
                
        if (page < maxPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, "&ePage Suivante (" + (page + 2) + ")"));
        }

        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openLeatherColorSelector(Player player, String variantId, String slotName, String armorItem) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_LEATHER_COLOR", variantId + ":" + slotName + ":" + armorItem);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Couleur Cuir: " + slotName));
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
            inv.setItem(i, createItem(item, "§eCouleur #" + colors[i], "", "§eClic pour sélectionner cette couleur"));
        }

        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
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
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Config Spawner Block"));
        holder.setInventory(inv);

        // Status active
        inv.setItem(10, createToggleItem(Material.LEVER, "&eSpawner Actif", spawner.isActive()));

        // Interval
        inv.setItem(11, createItem(Material.CLOCK, "&bIntervalle d'Apparition", 
                "&7Actuel : &e" + spawner.getInterval() + " secondes",
                "",
                "&aClic Gauche: &f+1s   &cClic Droit: &f-1s",
                "&aShift+Clic Gauche: &f+10s",
                "&cShift+Clic Droit: &f-10s",
                "&eClic Milieu: &fDéfinir précis"));

        // Rayon
        inv.setItem(12, createItem(Material.BEACON, "&bRayon de Détection / Spawn", 
                "&7Actuel : &e" + spawner.getRadius() + " blocs",
                "",
                "&aClic Gauche: &f+1   &cClic Droit: &f-1",
                "&aShift+Clic Gauche: &f+5",
                "&cShift+Clic Droit: &f-5",
                "&eClic Milieu: &fDéfinir précis"));

        // Max entités proches
        inv.setItem(13, createItem(Material.IRON_BARS, "&bMax Monstres Proches", 
                "&7Actuel : &e" + spawner.getMaxNearby() + " max",
                "",
                "&aClic Gauche: &f+1   &cClic Droit: &f-1",
                "&aShift+Clic Gauche: &f+5",
                "&cShift+Clic Droit: &f-5",
                "&eClic Milieu: &fDéfinir précis"));

        // Variants weights list manager
        inv.setItem(15, createItem(Material.CHEST, "&6Monstres & Poids", 
                "&7Configurer les variantes de monstres",
                "&7qui peuvent apparaître ici et",
                "&7leurs chances relatives (poids).",
                "",
                "&eClic pour gérer"));

        // Variation Interval (+/- seconds)
        inv.setItem(20, createItem(Material.CLOCK, "&bVariation d'Intervalle (sec)", 
                "&7Actuel : &e+/- " + spawner.getIntervalRange() + " secondes",
                "",
                "&aClic Gauche: &f+1s   &cClic Droit: &f-1s",
                "&aShift+Clic Gauche: &f+5s   &cShift+Clic Droit: &f-5s",
                "&eClic Milieu: &fDéfinir précis"));

        // Dispersion range (spawn range)
        inv.setItem(21, createItem(Material.COMPASS, "&bRayon de Dispersion (Spawn)", 
                "&7Actuel : &e" + spawner.getSpawnRange() + " blocs",
                "",
                "&aClic Gauche: &f+1   &cClic Droit: &f-1",
                "&aShift+Clic Gauche: &f+5   &cShift+Clic Droit: &f-5",
                "&eClic Milieu: &fDéfinir précis"));

        // Copy spawner
        inv.setItem(22, createItem(Material.PAPER, "&aCopier ce spawner", 
                "&7Enregistre la configuration de ce spawner", 
                "&7pour pouvoir la coller sur un autre bloc.", 
                "", "&eClic pour copier"));

        // Paste spawner (only if clipboard has one)
        fr.wilddifficulty.spawner.CustomSpawner clipboard = plugin.getSpawnerManager().getSpawnerClipboards().get(player.getUniqueId());
        if (clipboard != null) {
            inv.setItem(23, createItem(Material.SHEARS, "&aColler la configuration", 
                    "&7Applique les réglages copiés sur ce spawner.", 
                    "", "&eClic pour coller"));
        }

        // Particle spawner
        inv.setItem(24, createItem(Material.GLOWSTONE_DUST, "&bParticules du Spawner", 
                "&7Actuel : &e" + spawner.getParticleType(), 
                "", "&eClic pour basculer", "&7(SMOKE -> FLAME -> PORTAL -> NONE)"));

        // Sound spawner
        inv.setItem(25, createItem(Material.JUKEBOX, "&bSons du Spawner", 
                "&7Actuel : &e" + spawner.getSoundType(), 
                "", "&eClic pour basculer", "&7(PLING -> AMBIENT -> EXPLODE -> NONE)"));

        inv.setItem(31, createItem(Material.LAVA_BUCKET, "&cSupprimer ce spawner", "&4Attention: Supprime le bloc", "&4spawner de la configuration."));
        inv.setItem(35, createItem(Material.BARRIER, "&cQuitter"));
        player.openInventory(inv);
    }

    public void openSpawnerVariantsEditor(Player player, Location loc) {
        String contextId = fr.wilddifficulty.spawner.SpawnerManager.toKey(loc);
        fr.wilddifficulty.spawner.CustomSpawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        if (spawner == null) return;

        WDMenuHolder holder = new WDMenuHolder("SPAWNER_VARIANTS", contextId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Monstres du Spawner"));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            boolean active = spawner.getVariantWeights().containsKey(var.getId());
            int weight = spawner.getVariantWeights().getOrDefault(var.getId(), 0);

            Material mat = getVariantHeadMaterial(var.getType());
            
            if (active) {
                inv.setItem(slot++, createItem(mat, "&a[Inclus] " + var.getId(),
                        "&7Type: &f" + var.getType().name(),
                        "&7Poids actuel : &e" + weight,
                        "",
                        "&aClic Gauche: &fRetirer du spawner",
                        "&cClic Droit: &fChanger poids (Shift: +5, Clic: +1)",
                        "&eClic Milieu: &fDéfinir poids précis"));
            } else {
                inv.setItem(slot++, createItem(mat, "&7[Exclus] " + var.getId(),
                        "&7Type: &f" + var.getType().name(),
                        "",
                        "&eClic Gauche: &fAjouter au spawner"));
            }
        }

        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openVariantReinforcementSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_REINFORCEMENT", variantId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Sbire de renfort à la mort"));
        holder.setInventory(inv);

        int slot = 0;
        for (MobVariant var : plugin.getVariantManager().getAllVariants()) {
            if (slot >= 45) break;
            if (var.getId().equals(variantId)) continue; // Can't reinforce with itself directly
            Material mat = getVariantHeadMaterial(var.getType());
            inv.setItem(slot++, createItem(mat, "&f" + var.getId(), "&7Sélectionner comme renfort"));
        }

        inv.setItem(49, createItem(Material.BARRIER, "&cRetirer le renfort", "&7Aucun monstre ne spawneront à la mort"));
        inv.setItem(53, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openCustomModelDataSelector(Player player, String variantId) {
        MobVariant var = plugin.getVariantManager().getVariant(variantId);
        if (var == null) return;

        WDMenuHolder holder = new WDMenuHolder("SELECT_CMD", variantId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Custom Model Data"));
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
            inv.setItem(slot++, createItem(Material.GOLD_INGOT, "&eModèle : &6" + p, "&7Appliquer le CustomModelData &b" + p));
        }

        inv.setItem(18, createItem(Material.EMERALD, "&aValeur personnalisée (Tchat)", "&7Saisir un identifiant CMD précis"));
        inv.setItem(22, createItem(Material.BARRIER, "&cRéinitialiser (0)", "&7Retirer le CustomModelData"));
        inv.setItem(26, createItem(Material.BARRIER, "&cRetour"));
        player.openInventory(inv);
    }

    public void openRangedProjectileSelector(Player player, String variantId) {
        WDMenuHolder holder = new WDMenuHolder("SELECT_RANGED_TYPE", variantId);
        Inventory inv = Bukkit.createInventory(holder, 18, Component.text("Projectile à distance"));
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
            inv.setItem(slot++, createItem(mat, "&f" + t, "&7Sélectionner comme projectile"));
        }

        inv.setItem(17, createItem(Material.BARRIER, "&cRetour"));
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
                net.kyori.adventure.text.Component.text("§8Scaling Extérieur — " + zone.getId()));
        holder.setInventory(inv);

        // === CAP GLOBAL (slot 4) ===
        inv.setItem(4, createItem(Material.BARRIER, "§cMultiplicateur Maximum (Cap)",
                "§7Limite le bonus maximum toutes stats confondues.",
                "§7Actuel: §c" + zone.getExtMaxMult() + "x",
                "",
                "§aClic Gauche: §f+0.5x   §cClic Droit: §f-0.5x",
                "§eClic Milieu: §fDéfinir précis (Tchat)"));

        // === PV / HP (slots 10–12) ===
        inv.setItem(10, createItem(Material.HEART_OF_THE_SEA, "§c❤ Bonus PV",
                "§7Augmente les PV des mobs hors zone.",
                "§7Pas: §e" + (zone.getExtStepHp() > 0 ? zone.getExtStepHp() : (zone.getExtStep() > 0 ? zone.getExtStep() + " §7(global)" : "§cNon défini")),
                "§7Bonus/pas: §a+" + String.format("%.1f", (zone.getExtMultHp() > 0 ? zone.getExtMultHp() : zone.getExtMultPerStep()) * 100) + "%",
                "",
                "§aClic Gauche: §fPas +50   §cClic Droit: §fPas -50",
                "§aShift+Clic G: §fBonus +5%  §cShift+Clic D: §fBonus -5%",
                "§eClic Milieu: §fDéfinir précis (Tchat)"));

        inv.setItem(11, createItem(Material.RED_DYE, "§cDésactiver Bonus PV",
                "§7Remet le bonus PV à 0 (utilise le global)."));

        // === DÉGÂTS / DMG (slots 19–21) ===
        inv.setItem(19, createItem(Material.DIAMOND_SWORD, "§6⚔ Bonus Dégâts",
                "§7Augmente les dégâts des mobs hors zone.",
                "§7Pas: §e" + (zone.getExtStepDmg() > 0 ? zone.getExtStepDmg() : (zone.getExtStep() > 0 ? zone.getExtStep() + " §7(global)" : "§cNon défini")),
                "§7Bonus/pas: §a+" + String.format("%.1f", (zone.getExtMultDmg() > 0 ? zone.getExtMultDmg() : zone.getExtMultPerStep()) * 100) + "%",
                "",
                "§aClic Gauche: §fPas +50   §cClic Droit: §fPas -50",
                "§aShift+Clic G: §fBonus +5%  §cShift+Clic D: §fBonus -5%",
                "§eClic Milieu: §fDéfinir précis (Tchat)"));

        inv.setItem(20, createItem(Material.ORANGE_DYE, "§6Désactiver Bonus Dégâts",
                "§7Remet le bonus dégâts à 0 (utilise le global)."));

        // === VITESSE / SPD (slots 28–30) ===
        inv.setItem(28, createItem(Material.FEATHER, "§bBonus Vitesse",
                "§7Augmente la vitesse des mobs hors zone.",
                "§7Pas: §e" + (zone.getExtStepSpd() > 0 ? zone.getExtStepSpd() : (zone.getExtStep() > 0 ? zone.getExtStep() + " §7(global)" : "§cNon défini")),
                "§7Bonus/pas: §a+" + String.format("%.1f", (zone.getExtMultSpd() > 0 ? zone.getExtMultSpd() : zone.getExtMultPerStep()) * 100) + "%",
                "",
                "§aClic Gauche: §fPas +50   §cClic Droit: §fPas -50",
                "§aShift+Clic G: §fBonus +5%  §cShift+Clic D: §fBonus -5%",
                "§eClic Milieu: §fDéfinir précis (Tchat)"));

        inv.setItem(29, createItem(Material.LIGHT_BLUE_DYE, "§bDésactiver Bonus Vitesse",
                "§7Remet le bonus vitesse à 0 (utilise le global)."));
        // === GLOBAL FALLBACK (slot 40) ===
        inv.setItem(40, createItem(Material.CLOCK, "§7Bonus Global (Fallback)",
                "§7Utilisé si une stat n'a pas son propre pas.",
                "§7Pas: §e" + zone.getExtStep() + " blocs",
                "§7Bonus/pas: §a+" + String.format("%.1f", zone.getExtMultPerStep() * 100) + "%",
                "",
                "§aClic Gauche: §fPas +50   §cClic Droit: §fPas -50",
                "§aShift+Clic G: §fBonus +5%  §cShift+Clic D: §fBonus -5%"));

        // === APERÇU (slot 49) ===
        double simulatedDist = zone.getRadius() + 200;
        double hpPct   = (zone.computeHpExtMult(simulatedDist)  - 1) * 100;
        double dmgPct  = (zone.computeDmgExtMult(simulatedDist) - 1) * 100;
        double spdPct  = (zone.computeSpdExtMult(simulatedDist) - 1) * 100;
        inv.setItem(49, createItem(Material.SPYGLASS, "§eAperçu à " + (int) simulatedDist + " blocs du centre",
                "§7PV:     §c+" + String.format("%.0f", hpPct)  + "%",
                "§7Dégâts: §6+" + String.format("%.0f", dmgPct) + "%",
                "§7Vitesse:§b+" + String.format("%.0f", spdPct) + "%"));
        inv.setItem(53, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openZoneMembersConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_MEMBERS", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Membres: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(0, createItem(Material.NAME_TAG, "§aAjouter un Membre", "§7Ajouter un joueur via son pseudo dans le tchat.", "", "§eClic pour saisir le pseudo"));

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

        inv.setItem(53, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openZoneBeaconEffectsConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_BEACON", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Beacon Effects: " + zoneId));
        holder.setInventory(inv);

        String[] effects = {"SPEED", "HASTE", "RESISTANCE", "JUMP_BOOST", "INCREASE_DAMAGE", "REGENERATION", "FIRE_RESISTANCE", "NIGHT_VISION"};
        Material[] icons = {Material.SUGAR, Material.GOLDEN_PICKAXE, Material.SHIELD, Material.RABBIT_FOOT, Material.DIAMOND_SWORD, Material.GHAST_TEAR, Material.MAGMA_CREAM, Material.GOLDEN_CARROT};

        for (int i = 0; i < effects.length; i++) {
            String eff = effects[i];
            boolean active = zone.getBeaconEffects().containsKey(eff);
            int amp = zone.getBeaconEffects().getOrDefault(eff, 0);
            inv.setItem(10 + i, createItem(icons[i], (active ? "§a✔ " : "§7✖ ") + eff,
                    "§7Statut: " + (active ? "§aActif (Niv. " + (amp + 1) + ")" : "§cInactif"),
                    "",
                    "§aClic Gauche: §fBasculer/Augmenter le niveau",
                    "§cClic Droit: §fDésactiver l'effet"));
        }

        inv.setItem(26, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openZoneDangerNestConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_DANGER_NEST", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Nid d'Ennemis: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.NETHER_STAR, "§cNid d'Ennemis (Danger Nest)", zone.isDangerNest()));
        inv.setItem(12, createItem(Material.REDSTONE, "§cBoost Spawn Rate", "§7Multiplicateur: §e" + zone.getNestSpawnBoost() + "x", "", "§aClic Gauche: +0.5   §cClic Droit: -0.5"));
        inv.setItem(14, createItem(Material.DIAMOND_SWORD, "§cModifier Stats Nest (PV, Dégâts, Vitesse)", "§7Stats additionnelles appliquées aux entités dans ce nid."));

        inv.setItem(26, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openZoneSubsectionsConfig(Player player, String zoneId) {
        DifficultyZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) return;

        WDMenuHolder holder = new WDMenuHolder("ZONE_SUBSECTIONS", zoneId);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Sub-sections: " + zoneId));
        holder.setInventory(inv);

        inv.setItem(0, createItem(Material.GOLDEN_HOE, "§aAjouter la Sélection Actuelle", "§7Ajoute la forme/sélection de votre houe comme nouvelle sous-section de la zone.", "", "§eClic pour fusionner"));

        int slot = 9;
        for (fr.wilddifficulty.zone.ZoneSection sec : zone.getSubSections()) {
            if (slot >= 27) break;
            inv.setItem(slot++, createItem(Material.MAP, "§eSous-section: " + sec.getId(), "§7Type: " + sec.getType().name(), "", "§cClic pour supprimer cette sous-section"));
        }

        inv.setItem(35, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openPlayerSettingsGui(Player player) {
        fr.wilddifficulty.player.PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());

        WDMenuHolder holder = new WDMenuHolder("PLAYER_SETTINGS", player.getUniqueId().toString());
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Paramètres de Jeu"));
        holder.setInventory(inv);

        fillBorders(inv);

        inv.setItem(11, createItem(Material.DIAMOND_SWORD, "§bDifficulté Personnelle",
                "§7Niveau actuel: §e" + pd.getDifficultyLevel(),
                "§7Multiplicateur dégâts pris: §a" + String.format("%.2f", pd.getDamageMultiplier()) + "x",
                "",
                "§eClic pour basculer le niveau:",
                "§7FACILE -> NORMAL -> DIFFICILE -> EXTREME"));

        inv.setItem(13, createToggleItem(Material.POTION, "§bBarre de Soif", pd.isThirstEnabled()));
        inv.setItem(15, createItem(Material.WITHER_SKELETON_SKULL, "§cMode Hardcore Personnel",
                "§7Statut: " + (pd.isHardcoreEnabled() ? "§a✔ Actif" : "§c✖ Inactif"),
                "§7Régén. naturelle: " + (pd.isHardcoreNoRegen() ? "§cDésactivée" : "§aActivée"),
                "§7Régén. pommes dorées: " + (pd.isHardcoreAllowGoldenApples() ? "§aAutorisée" : "§cBloquée"),
                "§7Régén. potions: " + (pd.isHardcoreAllowPotions() ? "§aAutorisée" : "§cBloquée"),
                "§7Stuff mort: " + (pd.isHardcoreInstantDeathDespawn() ? "§cDespawn immédiat" : "§aTimer normal"),
                "",
                "§eClic pour configurer votre Hardcore"));

        inv.setItem(26, createItem(Material.BARRIER, "§cFermer"));
        player.openInventory(inv);
    }

    public void openPersonalHardcoreSettingsGui(Player player) {
        fr.wilddifficulty.player.PlayerData pd = plugin.getPlayerSettingsManager().getPlayerData(player.getUniqueId(), player.getName());

        WDMenuHolder holder = new WDMenuHolder("PLAYER_HARDCORE_CONFIG", player.getUniqueId().toString());
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Hardcore Personnel"));
        holder.setInventory(inv);

        fillBorders(inv);

        inv.setItem(10, createToggleItem(Material.WITHER_SKELETON_SKULL, "§cActiver Mode Hardcore", pd.isHardcoreEnabled()));
        inv.setItem(11, createToggleItem(Material.APPLE, "§eDésactiver Régénération Naturelle", pd.isHardcoreNoRegen()));
        inv.setItem(12, createToggleItem(Material.GOLDEN_APPLE, "§aAutoriser Régén. Pommes Dorées", pd.isHardcoreAllowGoldenApples()));
        inv.setItem(13, createToggleItem(Material.POTION, "§dAutoriser Régén. Potions", pd.isHardcoreAllowPotions()));
        inv.setItem(14, createToggleItem(Material.BEACON, "§bAutoriser Régén. Zones Sûres", pd.isHardcoreAllowSafezoneRegen()));
        inv.setItem(15, createToggleItem(Material.LAVA_BUCKET, "§4Despawn Immédiat du Stuff Mort", pd.isHardcoreInstantDeathDespawn()));

        inv.setItem(26, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openThirstHardcoreAdminGui(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ADMIN_THIRST_HARDCORE", "admin");
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Soif, Hardcore & Mort"));
        holder.setInventory(inv);

        inv.setItem(10, createToggleItem(Material.POTION, "§bActiver Système de Soif Globalement", plugin.getMainConfigManager().isThirstEnabled()));
        inv.setItem(11, createItem(Material.CLOCK, "§bMultiplicateur Vitesse Dégradation Soif",
                "§7Multiplicateur actuel: §e" + String.format("%.2fx", plugin.getMainConfigManager().getThirstDrainMultiplier()),
                "§7Immobile: §aPas de perte de soif (0.0x)",
                "§7Marche / Course: §eÉpuisement dynamique (style faim)",
                "",
                "§aClic Gauche: +0.25x (vider plus vite)",
                "§cClic Droit: -0.25x (vider moins vite)",
                "§eClic Milieu (Tchat): Définir multiplicateur précis"));
        inv.setItem(12, createItem(Material.WITHER_SKELETON_SKULL, "§cConfigurer le Mode Hardcore",
                "§7Statut global: " + (plugin.getMainConfigManager().isHardcoreEnabled() ? "§a✔ Actif" : "§c✖ Inactif"),
                "§7Pas de régén: " + (plugin.getMainConfigManager().isHardcoreNoRegen() ? "§a✔" : "§c✖"),
                "§7Multiplicateur dégâts pris: §e" + String.format("%.2fx", plugin.getMainConfigManager().getHardcoreDamageTakenMult()),
                "§7Multiplicateur faim: §e" + String.format("%.2fx", plugin.getMainConfigManager().getHardcoreHungerDrainMult()),
                "",
                "§eClic pour configurer le mode Hardcore"));

        inv.setItem(13, createItem(Material.WATER_BUCKET, "§bConfigurer les Sources d'Eau",
                "§7Définir les points d'hydratation apportés",
                "§7par chaque seau, bouteille, chaudron, etc.",
                "",
                "§eClic pour configurer les valeurs"));

        inv.setItem(14, createItem(Material.CHEST, "§eTemps Despawn Stuff Mort",
                "§7Temps avant disparition: §e" + plugin.getMainConfigManager().getDeathItemDespawnSeconds() + " sec",
                "",
                "§aClic Gauche: +60s   §cClic Droit: -60s",
                "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(15, createItem(Material.LAVA_BUCKET, "§cDéshydratation par Chaleur (Lave)",
                "§7Augmente la déshydratation près de la lave/feu.",
                "§7Statut: " + (plugin.getMainConfigManager().isThirstHeatDrainEnabled() ? "§a✔ Actif" : "§c✖ Inactif"),
                "§7Multiplicateur chaleur: §e" + String.format("%.2fx", plugin.getMainConfigManager().getThirstHeatDrainMultiplier()),
                "",
                "§aClic Gauche: Basculer Actif/Inactif",
                "§aShift+Clic Gauche: +0.25x (Multiplicateur)",
                "§cClic Droit: -0.25x (Multiplicateur)",
                "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(26, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }

    public void openThirstSourcesAdminGui(Player player) {
        WDMenuHolder holder = new WDMenuHolder("ADMIN_THIRST_SOURCES", "admin");
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Restauration d'Eau par Source"));
        holder.setInventory(inv);

        fillBorders(inv);
        MainConfigManager mainCfg = plugin.getMainConfigManager();

        inv.setItem(10, createItem(Material.WATER_BUCKET, "§bSeau d'eau",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreWaterBucket() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(11, createItem(Material.POTION, "§bBouteille d'eau",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreWaterBottle() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(12, createItem(Material.GLASS_BOTTLE, "§dAutres Potions",
                "§7Restauration: §a+" + mainCfg.getThirstRestorePotion() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(13, createItem(Material.CAULDRON, "§bChaudron d'eau",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreCauldron() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(14, createItem(Material.WATER_BUCKET, "§bSource d'eau (Bloc)",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreWaterBlock() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(15, createItem(Material.MILK_BUCKET, "§fSeau de Lait",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreMilkBucket() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(16, createItem(Material.HONEY_BOTTLE, "§eFiole de Miel",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreHoneyBottle() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(19, createItem(Material.MELON_SLICE, "§aTranche de Melon",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreMelonSlice() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(20, createItem(Material.APPLE, "§cPomme / Pomme Dorée",
                "§7Restauration: §a+" + mainCfg.getThirstRestoreApple() + " pts",
                "", "§aClic Gauche: +1 pt   §cClic Droit: -1 pt", "§eClic Milieu (Tchat): Définir précis"));

        inv.setItem(35, createItem(Material.BARRIER, "§cRetour"));
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
            inv.setItem(22, createItem(Material.BARRIER, "§cAucune Zone Éditable", "§7Vous n'avez le rôle Gestionnaire (Niveau 3)", "§7sur aucune zone actuellement."));
        } else {
            int slot = 10;
            for (DifficultyZone zone : editableZones) {
                if (slot == 17 || slot == 26 || slot == 35 || slot == 44) slot += 2;
                if (slot >= 44) break;
                String role = isAdmin ? "Administrateur" : "Gestionnaire (Niveau 3)";
                inv.setItem(slot++, createItem(Material.MAP, "§eZone: " + zone.getId(),
                        "§7Type: §f" + zone.getType().name(),
                        "§7Monde: §f" + zone.getWorld(),
                        "§7Votre rôle: §a" + role,
                        "",
                        "§eClic pour ouvrir l'éditeur de cette zone"));
            }
        }

        inv.setItem(53, createItem(Material.BARRIER, "§cFermer"));
        player.openInventory(inv);
    }

    public void openHardcoreConfigGui(Player player) {
        WDMenuHolder holder = new WDMenuHolder("HARDCORE_CONFIG", "admin");
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Configuration Hardcore"));
        holder.setInventory(inv);
        MainConfigManager mainCfg = plugin.getMainConfigManager();

        inv.setItem(10, createToggleItem(Material.WITHER_SKELETON_SKULL, "§cActiver Mode Hardcore Globalement", mainCfg.isHardcoreEnabled()));
        inv.setItem(12, createToggleItem(Material.GOLDEN_APPLE, "§eDésactiver Régénération Naturelle", mainCfg.isHardcoreNoRegen()));

        inv.setItem(14, createItem(Material.NETHERITE_SWORD, "§cMultiplicateur Dégâts Subis",
                "§7Actuel: §e" + String.format("%.2fx", mainCfg.getHardcoreDamageTakenMult()),
                "",
                "§aClic Gauche: +0.25x   §cClic Droit: -0.25x"));

        inv.setItem(16, createItem(Material.ROTTEN_FLESH, "§6Multiplicateur Épuisement Faim",
                "§7Actuel: §e" + String.format("%.2fx", mainCfg.getHardcoreHungerDrainMult()),
                "",
                "§aClic Gauche: +0.25x   §cClic Droit: -0.25x"));

        inv.setItem(26, createItem(Material.BARRIER, "§cRetour"));
        player.openInventory(inv);
    }
}
