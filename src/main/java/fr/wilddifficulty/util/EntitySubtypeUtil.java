package fr.wilddifficulty.util;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public final class EntitySubtypeUtil {

    private EntitySubtypeUtil() {}

    /**
     * Applique un sous-type / variante esthétique de mob vanilla sur l'entité.
     * Le paramètre subType correspond à la chaîne de caractères sauvegardée dans StatModifiers (entitySubtype).
     */
    public static void applySubtype(LivingEntity entity, String subType) {
        if (subType == null || subType.isBlank() || "none".equalsIgnoreCase(subType)) return;

        try {
            String clean = subType.toUpperCase().trim();

            // 1. CREEPER (POWERED)
            if (entity instanceof Creeper creeper) {
                if ("POWERED".equals(clean) || "CHARGED".equals(clean) || "TRUE".equals(clean)) {
                    creeper.setPowered(true);
                } else if ("NORMAL".equals(clean) || "FALSE".equals(clean)) {
                    creeper.setPowered(false);
                }
                return;
            }

            // 2. IRON GOLEM (CRACKED STATE)
            if (entity instanceof IronGolem golem) {
                try {
                    Class<?> crackClass = Class.forName("org.bukkit.entity.IronGolem$CrackState");
                    Object crackVal = Enum.valueOf((Class<Enum>) crackClass, clean);
                    golem.getClass().getMethod("setCrackState", crackClass).invoke(golem, crackVal);
                } catch (Throwable ex) {
                    double pct = 1.0;
                    if ("LOW".equals(clean)) pct = 0.7;
                    else if ("MEDIUM".equals(clean)) pct = 0.45;
                    else if ("HIGH".equals(clean)) pct = 0.2;
                    double maxHp = golem.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    golem.setHealth(Math.min(maxHp * pct, golem.getHealth()));
                }
                return;
            }

            // 3. ENDERMAN (CARRIED BLOCK)
            if (entity instanceof Enderman enderman) {
                try {
                    Material mat = Material.matchMaterial(clean);
                    if (mat != null && mat.isBlock()) {
                        enderman.setCarriedBlock(org.bukkit.Bukkit.createBlockData(mat));
                    }
                } catch (Throwable ignored) {}
                return;
            }

            // 4. CHICKEN VARIANTS (Paper 1.21+ / Registry / Baby)
            if (entity instanceof Chicken chicken) {
                if ("BABY".equalsIgnoreCase(clean) || "POUSSIN".equalsIgnoreCase(clean)) {
                    chicken.setBaby();
                    return;
                }
                if (applyRegistryVariant(chicken, new String[]{"CHICKEN_VARIANT"}, "setVariant", clean)) return;
                setEnumProperty(chicken, "setVariant", "org.bukkit.entity.Chicken$Variant", clean);
                return;
            }

            // 5. COW VARIANTS (Paper 1.21+ / Registry / Baby)
            if (entity instanceof Cow cow) {
                if ("BABY".equalsIgnoreCase(clean) || "VEAU".equalsIgnoreCase(clean)) {
                    cow.setBaby();
                    return;
                }
                if (applyRegistryVariant(cow, new String[]{"COW_VARIANT"}, "setVariant", clean)) return;
                setEnumProperty(cow, "setVariant", "org.bukkit.entity.Cow$Variant", clean);
                return;
            }

            // 6. PIG VARIANTS (Paper 1.21+ / Registry / Baby)
            if (entity instanceof Pig pig) {
                if ("BABY".equalsIgnoreCase(clean)) {
                    pig.setBaby();
                    return;
                }
                if (applyRegistryVariant(pig, new String[]{"PIG_VARIANT"}, "setVariant", clean)) return;
                setEnumProperty(pig, "setVariant", "org.bukkit.entity.Pig$Variant", clean);
                return;
            }

            // 7. WOLF VARIANTS
            if (entity instanceof Wolf wolf) {
                if (applyRegistryVariant(wolf, new String[]{"WOLF_VARIANT"}, "setVariant", clean)) return;
                setEnumProperty(wolf, "setVariant", "org.bukkit.entity.Wolf$Variant", clean);
                return;
            }

            // 8. CAT TYPES
            if (entity instanceof Cat cat) {
                if (applyRegistryVariant(cat, new String[]{"CAT_VARIANT", "CAT_TYPE"}, "setCatType", clean)) return;
                setEnumProperty(cat, "setCatType", "org.bukkit.entity.Cat$Type", clean);
                return;
            }

            // 9. FROG VARIANTS
            if (entity instanceof Frog frog) {
                if (applyRegistryVariant(frog, new String[]{"FROG_VARIANT"}, "setVariant", clean)) return;
                setEnumProperty(frog, "setVariant", "org.bukkit.entity.Frog$Variant", clean);
                return;
            }

            // 10. FOX TYPES
            if (entity instanceof Fox fox) {
                try {
                    Fox.Type type = Fox.Type.valueOf(clean);
                    fox.setFoxType(type);
                } catch (Throwable ignored) {}
                return;
            }

            // 11. AXOLOTL VARIANTS
            if (entity instanceof Axolotl axolotl) {
                try {
                    Axolotl.Variant var = Axolotl.Variant.valueOf(clean);
                    axolotl.setVariant(var);
                } catch (Throwable ignored) {}
                return;
            }

            // 12. PARROT VARIANTS
            if (entity instanceof Parrot parrot) {
                try {
                    Parrot.Variant var = Parrot.Variant.valueOf(clean);
                    parrot.setVariant(var);
                } catch (Throwable ignored) {}
                return;
            }

            // 13. HORSE COLOR / STYLE
            if (entity instanceof Horse horse) {
                if (clean.startsWith("STYLE_")) {
                    try {
                        Horse.Style style = Horse.Style.valueOf(clean.substring(6));
                        horse.setStyle(style);
                    } catch (Throwable ignored) {}
                } else {
                    try {
                        Horse.Color color = Horse.Color.valueOf(clean);
                        horse.setColor(color);
                    } catch (Throwable ignored) {}
                }
                return;
            }

            // 14. LLAMA / TRADER LLAMA
            if (entity instanceof Llama llama) {
                try {
                    Llama.Color color = Llama.Color.valueOf(clean);
                    llama.setColor(color);
                } catch (Throwable ignored) {}
                return;
            }

            // 15. RABBIT TYPES
            if (entity instanceof Rabbit rabbit) {
                setEnumProperty(rabbit, "setRabbitType", "org.bukkit.entity.Rabbit$Type", clean);
                return;
            }

            // 16. MUSHROOM COW
            if (entity instanceof MushroomCow mooshroom) {
                try {
                    MushroomCow.Variant var = MushroomCow.Variant.valueOf(clean);
                    mooshroom.setVariant(var);
                } catch (Throwable ignored) {}
                return;
            }

            // 17. VILLAGER & ZOMBIE VILLAGER
            if (entity instanceof Villager villager) {
                applyVillagerSubtype(villager, clean);
                return;
            }
            if (entity instanceof ZombieVillager zv) {
                applyZombieVillagerSubtype(zv, clean);
                return;
            }

            // 18. PANDA
            if (entity instanceof Panda panda) {
                try {
                    Panda.Gene gene = Panda.Gene.valueOf(clean);
                    panda.setMainGene(gene);
                    panda.setHiddenGene(gene);
                } catch (Throwable ignored) {}
                return;
            }

            // 19. SHEEP COLOR / SHEARED
            if (entity instanceof Sheep sheep) {
                if ("SHEARED".equals(clean)) {
                    sheep.setSheared(true);
                } else {
                    try {
                        DyeColor color = DyeColor.valueOf(clean);
                        sheep.setColor(color);
                    } catch (Throwable ignored) {}
                }
                return;
            }

            // 20. AGEABLE BABY FALLBACK
            if (entity instanceof Ageable ageable) {
                if ("BABY".equalsIgnoreCase(clean) || "BEBE".equalsIgnoreCase(clean)) {
                    ageable.setBaby();
                }
            }

        } catch (Throwable ignored) {}
    }

    private static boolean applyRegistryVariant(Object entity, String[] registryFieldNames, String methodName, String subTypeKey) {
        String keyClean = subTypeKey.toLowerCase().trim();
        for (String regName : registryFieldNames) {
            try {
                Field regField = Registry.class.getField(regName);
                Object registryObj = regField.get(null);
                if (registryObj != null) {
                    Method getMethod = registryObj.getClass().getMethod("get", NamespacedKey.class);
                    Object variantVal = getMethod.invoke(registryObj, NamespacedKey.minecraft(keyClean));
                    if (variantVal != null) {
                        for (Method m : entity.getClass().getMethods()) {
                            if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                                try {
                                    m.invoke(entity, variantVal);
                                    return true;
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static void setEnumProperty(Object target, String methodName, String enumClassName, String enumValueName) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            Object enumVal = null;
            for (Object obj : enumClass.getEnumConstants()) {
                if (((Enum<?>) obj).name().equalsIgnoreCase(enumValueName)) {
                    enumVal = obj;
                    break;
                }
            }
            if (enumVal != null) {
                Method method = target.getClass().getMethod(methodName, enumClass);
                method.invoke(target, enumVal);
            }
        } catch (Throwable ignored) {}
    }

    private static void applyVillagerSubtype(Villager villager, String clean) {
        try {
            if (clean.startsWith("PROF_")) {
                String pName = clean.substring(5).toLowerCase();
                Villager.Profession prof = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(pName));
                if (prof != null) villager.setProfession(prof);
            } else {
                String tName = clean.toLowerCase();
                Villager.Type vType = Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(tName));
                if (vType != null) villager.setVillagerType(vType);
            }
        } catch (Throwable ignored) {}
    }

    private static void applyZombieVillagerSubtype(ZombieVillager zv, String clean) {
        try {
            if (clean.startsWith("PROF_")) {
                String pName = clean.substring(5).toLowerCase();
                Villager.Profession prof = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(pName));
                if (prof != null) zv.setVillagerProfession(prof);
            } else {
                String tName = clean.toLowerCase();
                Villager.Type vType = Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(tName));
                if (vType != null) zv.setVillagerType(vType);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Retourne la liste des skins/subtypes configurables pour une EntityType donnée.
     */
    public static List<SubtypeOption> getAvailableSubtypes(EntityType entityType) {
        List<SubtypeOption> list = new ArrayList<>();

        if (entityType == EntityType.CREEPER) {
            list.add(new SubtypeOption("NORMAL", "Creeper Normal", "TNT"));
            list.add(new SubtypeOption("POWERED", "Creeper Chargé / Explosif", "REDSTONE_TORCH"));
        } else if (entityType == EntityType.IRON_GOLEM) {
            list.add(new SubtypeOption("NONE", "Golem Intact", "IRON_BLOCK"));
            list.add(new SubtypeOption("LOW", "Fissures Légères", "IRON_INGOT"));
            list.add(new SubtypeOption("MEDIUM", "Fissures Moyennes", "IRON_INGOT"));
            list.add(new SubtypeOption("HIGH", "Fortement Fissuré", "CRACKED_STONE_BRICKS"));
        } else if (entityType == EntityType.ENDERMAN) {
            list.add(new SubtypeOption("GRASS_BLOCK", "Enderman avec Bloc d'Herbe", "GRASS_BLOCK"));
            list.add(new SubtypeOption("TNT", "Enderman avec TNT", "TNT"));
            list.add(new SubtypeOption("PODZOL", "Enderman avec Podzol", "PODZOL"));
            list.add(new SubtypeOption("DIRT", "Enderman avec Terre", "DIRT"));
            list.add(new SubtypeOption("SAND", "Enderman avec Sable", "SAND"));
            list.add(new SubtypeOption("RED_SAND", "Enderman avec Sable Rouge", "RED_SAND"));
            list.add(new SubtypeOption("GRAVEL", "Enderman avec Gravier", "GRAVEL"));
            list.add(new SubtypeOption("PUMPKIN", "Enderman avec Citrouille", "PUMPKIN"));
            list.add(new SubtypeOption("CACTUS", "Enderman avec Cactus", "CACTUS"));
            list.add(new SubtypeOption("NETHERRACK", "Enderman avec Netherrack", "NETHERRACK"));
        } else if (entityType == EntityType.CHICKEN) {
            list.add(new SubtypeOption("NORMAL", "Poule Normale (Adulte)", "CHICKEN_SPAWN_EGG"));
            list.add(new SubtypeOption("BABY", "Poussin (Bébé)", "EGG"));
            list.add(new SubtypeOption("TEMPERATE", "Poule Tempérée", "CHICKEN_SPAWN_EGG"));
            list.add(new SubtypeOption("WARM", "Poule Chaude", "CHICKEN_SPAWN_EGG"));
            list.add(new SubtypeOption("COLD", "Poule Froide", "CHICKEN_SPAWN_EGG"));
        } else if (entityType == EntityType.COW) {
            list.add(new SubtypeOption("NORMAL", "Vache Normale (Adulte)", "COW_SPAWN_EGG"));
            list.add(new SubtypeOption("BABY", "Veau (Bébé)", "MILK_BUCKET"));
            list.add(new SubtypeOption("TEMPERATE", "Vache Tempérée", "COW_SPAWN_EGG"));
            list.add(new SubtypeOption("WARM", "Vache Chaude", "COW_SPAWN_EGG"));
            list.add(new SubtypeOption("COLD", "Vache Froide", "COW_SPAWN_EGG"));
        } else if (entityType == EntityType.PIG) {
            list.add(new SubtypeOption("NORMAL", "Cochon Normal", "PIG_SPAWN_EGG"));
            list.add(new SubtypeOption("BABY", "Cochonnet (Bébé)", "CARROT"));
            list.add(new SubtypeOption("TEMPERATE", "Cochon Tempéré", "PIG_SPAWN_EGG"));
            list.add(new SubtypeOption("WARM", "Cochon Chaud", "PIG_SPAWN_EGG"));
            list.add(new SubtypeOption("COLD", "Cochon Froid", "PIG_SPAWN_EGG"));
        } else if (entityType == EntityType.WOLF) {
            list.add(new SubtypeOption("PALE", "Loup Pâle (Plaines)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("WOODS", "Loup des Bois (Forêt)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("ASHEN", "Loup Cendré (Taïga)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("BLACK", "Loup Noir (Taïga Ancienne)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("CHESTNUT", "Loup Châtain (Épicéas)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("RUSTY", "Loup Rouille (Jungle)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("SPOTTED", "Loup Tacheté (Savane)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("STRIPED", "Loup Rayé (Badlands)", "WOLF_SPAWN_EGG"));
            list.add(new SubtypeOption("SNOWY", "Loup Enneigé (Bosquet)", "WOLF_SPAWN_EGG"));
        } else if (entityType == EntityType.CAT) {
            list.add(new SubtypeOption("TABBY", "Chat Tigré", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("TUXEDO", "Chat Tuxedo (Noir & Blanc)", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("RED", "Chat Roux", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("SIAMESE", "Chat Siamois", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("SHORT_HAIR", "Chat Poil Court", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("RAGDOLL", "Chat Ragdoll", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("BIRMAN", "Chat Sacré de Birmanie", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("CALICO", "Chat Calico (Tricolore)", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("BRITISH_SHORTHAIR", "Chat British Shorthair", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("PERSIAN", "Chat Persan", "CAT_SPAWN_EGG"));
            list.add(new SubtypeOption("ALL_BLACK", "Chat Noir Intégral", "CAT_SPAWN_EGG"));
        } else if (entityType == EntityType.FROG) {
            list.add(new SubtypeOption("TEMPERATE", "Grenouille Tempérée (Verte)", "FROG_SPAWN_EGG"));
            list.add(new SubtypeOption("WARM", "Grenouille Chaude (Blanche)", "FROG_SPAWN_EGG"));
            list.add(new SubtypeOption("COLD", "Grenouille Froide (Verte Foncée)", "FROG_SPAWN_EGG"));
        } else if (entityType == EntityType.FOX) {
            list.add(new SubtypeOption("RED", "Renard Roux", "FOX_SPAWN_EGG"));
            list.add(new SubtypeOption("SNOW", "Renard des Neiges (Blanc)", "FOX_SPAWN_EGG"));
        } else if (entityType == EntityType.AXOLOTL) {
            list.add(new SubtypeOption("LUCY", "Axolotl Rose (Lucy)", "AXOLOTL_SPAWN_EGG"));
            list.add(new SubtypeOption("WILD", "Axolotl Brun (Wild)", "AXOLOTL_SPAWN_EGG"));
            list.add(new SubtypeOption("GOLD", "Axolotl Or (Yellow)", "AXOLOTL_SPAWN_EGG"));
            list.add(new SubtypeOption("CYAN", "Axolotl Cyan", "AXOLOTL_SPAWN_EGG"));
            list.add(new SubtypeOption("BLUE", "Axolotl Bleu Rare", "AXOLOTL_SPAWN_EGG"));
        } else if (entityType == EntityType.PARROT) {
            list.add(new SubtypeOption("RED_BLUE", "Perroquet Rouge & Bleu", "PARROT_SPAWN_EGG"));
            list.add(new SubtypeOption("BLUE", "Perroquet Bleu", "PARROT_SPAWN_EGG"));
            list.add(new SubtypeOption("GREEN", "Perroquet Vert", "PARROT_SPAWN_EGG"));
            list.add(new SubtypeOption("YELLOW_BLUE", "Perroquet Jaune & Bleu", "PARROT_SPAWN_EGG"));
            list.add(new SubtypeOption("GREY", "Perroquet Gris", "PARROT_SPAWN_EGG"));
        } else if (entityType == EntityType.HORSE) {
            list.add(new SubtypeOption("WHITE", "Cheval Blanc", "LEATHER_HORSE_ARMOR"));
            list.add(new SubtypeOption("CREAMY", "Cheval Crème", "LEATHER_HORSE_ARMOR"));
            list.add(new SubtypeOption("CHESTNUT", "Cheval Alzan / Alezan", "LEATHER_HORSE_ARMOR"));
            list.add(new SubtypeOption("BROWN", "Cheval Brun", "LEATHER_HORSE_ARMOR"));
            list.add(new SubtypeOption("BLACK", "Cheval Noir", "LEATHER_HORSE_ARMOR"));
            list.add(new SubtypeOption("GRAY", "Cheval Gris", "LEATHER_HORSE_ARMOR"));
            list.add(new SubtypeOption("DARK_BROWN", "Cheval Brun Foncé", "LEATHER_HORSE_ARMOR"));
            list.add(new SubtypeOption("STYLE_WHITE", "Robe: Taches Blanches", "WHITE_WOOL"));
            list.add(new SubtypeOption("STYLE_WHITEFIELD", "Robe: Champ Blanc", "WHITE_WOOL"));
            list.add(new SubtypeOption("STYLE_WHITE_DOTS", "Robe: Points Blancs", "WHITE_WOOL"));
            list.add(new SubtypeOption("STYLE_BLACK_DOTS", "Robe: Points Noirs", "BLACK_WOOL"));
        } else if (entityType == EntityType.LLAMA || entityType == EntityType.TRADER_LLAMA) {
            list.add(new SubtypeOption("CREAMY", "Llama Crème", "LLAMA_SPAWN_EGG"));
            list.add(new SubtypeOption("WHITE", "Llama Blanc", "LLAMA_SPAWN_EGG"));
            list.add(new SubtypeOption("BROWN", "Llama Brun", "LLAMA_SPAWN_EGG"));
            list.add(new SubtypeOption("GRAY", "Llama Gris", "LLAMA_SPAWN_EGG"));
        } else if (entityType == EntityType.RABBIT) {
            list.add(new SubtypeOption("BROWN", "Lapin Brun", "RABBIT_SPAWN_EGG"));
            list.add(new SubtypeOption("WHITE", "Lapin Blanc", "RABBIT_SPAWN_EGG"));
            list.add(new SubtypeOption("BLACK", "Lapin Noir", "RABBIT_SPAWN_EGG"));
            list.add(new SubtypeOption("BLACK_AND_WHITE", "Lapin Noir & Blanc", "RABBIT_SPAWN_EGG"));
            list.add(new SubtypeOption("GOLD", "Lapin Doré", "RABBIT_SPAWN_EGG"));
            list.add(new SubtypeOption("SALT_AND_PEPPER", "Lapin Poivre & Sel", "RABBIT_SPAWN_EGG"));
            list.add(new SubtypeOption("KILLER", "Lapin Tueur (Caerbannog)", "RABBIT_FOOT"));
        } else if (entityType == EntityType.MOOSHROOM) {
            list.add(new SubtypeOption("RED", "Champimeuh Rouge", "RED_MUSHROOM"));
            list.add(new SubtypeOption("BROWN", "Champimeuh Brune", "BROWN_MUSHROOM"));
        } else if (entityType == EntityType.VILLAGER || entityType == EntityType.ZOMBIE_VILLAGER) {
            list.add(new SubtypeOption("PLAINS", "Villageois des Plaines", "EMERALD"));
            list.add(new SubtypeOption("DESERT", "Villageois du Désert", "SAND"));
            list.add(new SubtypeOption("JUNGLE", "Villageois de la Jungle", "JUNGLE_LEAVES"));
            list.add(new SubtypeOption("SAVANNA", "Villageois de la Savane", "ACACIA_LOG"));
            list.add(new SubtypeOption("SNOW", "Villageois des Neiges", "SNOW_BLOCK"));
            list.add(new SubtypeOption("SWAMP", "Villageois du Marais", "LILY_PAD"));
            list.add(new SubtypeOption("TAIGA", "Villageois de la Taïga", "SPRUCE_LOG"));
            list.add(new SubtypeOption("PROF_FARMER", "Profession: Fermier", "WHEAT"));
            list.add(new SubtypeOption("PROF_LIBRARIAN", "Profession: Bibliothécaire", "BOOK"));
            list.add(new SubtypeOption("PROF_CLERIC", "Profession: Prêtre", "BREWING_STAND"));
            list.add(new SubtypeOption("PROF_ARMORER", "Profession: Armurier", "BLAST_FURNACE"));
            list.add(new SubtypeOption("PROF_WEAPONSMITH", "Profession: Forgeron d'armes", "GRINDSTONE"));
            list.add(new SubtypeOption("PROF_TOOLSMITH", "Profession: Forgeron d'outils", "SMITHING_TABLE"));
            list.add(new SubtypeOption("PROF_BUTCHER", "Profession: Boucher", "SMOKER"));
            list.add(new SubtypeOption("PROF_CARTOGRAPHER", "Profession: Cartographe", "CARTOGRAPHY_TABLE"));
        } else if (entityType == EntityType.PANDA) {
            list.add(new SubtypeOption("NORMAL", "Panda Normal", "BAMBOO"));
            list.add(new SubtypeOption("LAZY", "Panda Paresseux", "BAMBOO"));
            list.add(new SubtypeOption("WORRIED", "Panda Inquiet", "BAMBOO"));
            list.add(new SubtypeOption("PLAYFUL", "Panda Joueur", "BAMBOO"));
            list.add(new SubtypeOption("BROWN", "Panda Brun (Rare)", "BAMBOO"));
            list.add(new SubtypeOption("WEAK", "Panda Maladif", "BAMBOO"));
            list.add(new SubtypeOption("AGGRESSIVE", "Panda Agressif", "BAMBOO"));
        } else if (entityType == EntityType.SHEEP) {
            list.add(new SubtypeOption("SHEARED", "Mouton Tondu", "SHEARS"));
            for (DyeColor col : DyeColor.values()) {
                list.add(new SubtypeOption(col.name(), "Mouton " + col.name(), col.name() + "_WOOL"));
            }
        }

        return list;
    }

    public record SubtypeOption(String id, String displayName, String iconMaterial) {}
}
