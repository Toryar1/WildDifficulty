<div align="center">

<a href="https://www.spigotmc.org/resources/wilddifficulty.137547/"><img src="https://img.shields.io/badge/SpigotMC-137547-yellow?style=for-the-badge&logo=spigotmc" alt="SpigotMC"/></a>
<img src="https://img.shields.io/badge/Paper-26.2-orange?style=for-the-badge&logo=minecraft" alt="Paper 26.2"/>
<img src="https://img.shields.io/badge/Java-21-blue?style=for-the-badge&logo=openjdk" alt="Java 21"/>
<img src="https://img.shields.io/badge/Version-1.0.0-green?style=for-the-badge" alt="Version 1.0.0"/>
<img src="https://img.shields.io/badge/Languages-10--Supported-purple?style=for-the-badge" alt="10 Languages"/>

# 🐺 WildDifficulty

**An advanced difficulty scaling, mob variants, and survival plugin for Minecraft Paper 1.21.4 servers**  
*Un plugin de scaling de difficulté avancé, variantes de mobs et survie pour serveurs Minecraft Paper 1.21.4*

</div>

---

## 📸 Screenshots & Video Demos / Captures d'écran & Vidéos

> [!TIP]
> Place your showcase images and video links below to demonstrate WildDifficulty in action!

### 🖼️ Gameplay & GUI Gallery

| Main Admin Menu (`/wd gui`) | Mob Inspector & Analysis |
|:---------------------------:|:-----------------------:|
| ![Admin Menu Placeholder](https://via.placeholder.com/600x350/1e1e2e/ffffff?text=Main+Admin+GUI+Menu) | ![Mob Inspector Placeholder](https://via.placeholder.com/600x350/1e1e2e/ffffff?text=Mob+Inspector+Tool) |

| Blood Moon Event | Thirst & Hardcore Systems |
|:----------------:|:-------------------------:|
| ![Blood Moon Placeholder](https://via.placeholder.com/600x350/1e1e2e/ffffff?text=Blood+Moon+Event) | ![Thirst HUD Placeholder](https://via.placeholder.com/600x350/1e1e2e/ffffff?text=Thirst+HUD+%26+Hardcore) |

### 🎥 Video Demonstrations

- 🎬 **Trailer & Feature Overview**: [Watch Demo Video (YouTube / Streamable / mp4)](https://your-video-link-here.com)
- 🎥 **GUI Walkthrough & Configuration**: [Watch Tutorial Video](https://your-video-link-here.com)

---

## 🇬🇧 English

### 📖 Description

**WildDifficulty** is an all-in-one difficulty scaling and survival enhancement plugin designed for Paper 1.21.4 (Java 21). It transforms your server's combat dynamics by adding custom mob variants, geographic difficulty zones, environmental heat & thirst mechanics, Blood Moon events, custom mob spawners, and extensive in-game GUI editors.

---

### 🎮 In-Game GUI Customization Systems

All features and configuration parameters in **WildDifficulty** can be managed live in-game without touching configuration files through interactive GUI menus (`/wd gui`).

<details>
<summary><b>🧭 Main Administration Hub (<code>/wd gui</code>)</b></summary>

The central control panel provides immediate access to all plugin subsystems:

| Icon / Slot | Category | Description |
|:---|:---|:---|
| ⚔️ **Diamond Sword** | **Global Modifiers** | Adjust server-wide health, attack damage, movement speed, and detection range multipliers. |
| 🧟 **Zombie Head** | **Mob Variants** | Create, configure, clone, and manage custom monster variants. |
| 💀 **Skeleton Skull** | **Mob Squads** | Create coordinated monster squads with specialized team bonuses. |
| 🗺️ **Map** | **Difficulty Zones** | Define custom geographic difficulty areas (Cuboid, Radius, Polygon). |
| 🩸 **Redstone** | **Blood Moon Event** | Configure night event frequency, multipliers, sounds, particles, and potion buffs. |
| ⚙️ **Comparator** | **General Settings** | Adjust spawn distances, player variant caps, daylight burning, and thirst/hardcore systems. |
| 🛒 **Chest Minecart** | **Admin Tools** | Receive all interactive configuration wands (Zone Hoe, Spawner Shovel, Biome Compass, Inspector Stick). |
| 🚫 **Barrier / Egg** | **Vanilla Blockers** | Toggle blocking of vanilla hostile or passive spawns. |
| 📖 **Book** | **Language Selector** | Dynamically switch between 10 supported languages in real-time. |
| 🟩 **Command Block** | **Reload Config** | Instantly reload all YAML configurations and language dictionaries. |

</details>

<details>
<summary><b>🦇 Mob Variants Customization Hub (<code>/wd gui</code> ➔ Mob Variants)</b></summary>

Create and customize unique monster variants with specialized attributes, aesthetics, behaviors, and loot tables.

<details>
<summary><b>📊 1. Attributes & Modifiers</b></summary>

Fine-tune core mob attributes with precise scaling:
- **Max Health**: Absolute or bonus health pool with automatic synchronization.
- **Attack Damage**: Custom melee damage output.
- **Movement Speed**: Custom walking/running velocity.
- **Follow / Detection Range**: Extended vision range (e.g. 32 to 100+ blocks).
- **Knockback Resistance**: Percentage of knockback ignored by the mob.
- **Scale / Size**: Dynamic resizing of the entity model (Minecraft 1.20.5+ / 1.21+).
- **Passive Health Regeneration**: Natural HP recovery over time.

</details>

<details>
<summary><b>⚔️ 2. Special Behaviors & AI</b></summary>

Enhance monster combat dynamics:
- **Daylight / Burn Immune**: Prevent sunlight burning for undead variants.
- **Aggressiveness Mode**: Configurable hostility (Always Hostile, Night Only, Retaliation Only).
- **Protection Bypass**: Option to bypass safe-zones or claim protections.
- **Death Explosion (Suicide Attack)**: Trigger custom explosions upon death.
- **On-Hit Potion Effects**: Apply debuffs to targets on attack (Slowness, Poison, Wither, Blindness, Nausea, Darkness).
- **Permanent Aura Effects**: Ongoing potion buffs (Speed, Strength, Resistance, Invisibility, Fire Resistance).
- **Custom BossBar**: Dedicated health bar displayed to nearby players with configurable color and style (Solid, 6/10/12/20 segments).
- **Ranged Projectile Attacks**: Enable shooting arrows, fireballs, wither skulls, or snowballs even on melee mobs.
- **Death Spawners (Reinforcements)**: Automatically summon minion variants when the parent monster dies.

</details>

<details>
<summary><b>🎨 3. Appearance, Skins & Aesthetics</b></summary>

Give your mobs distinctive visual identities:
- **Dynamic Nametags**: Formatted display with `{name}`, `{hp}`, `{max_hp}`, and `{level}` placeholders.
- **Custom Player Head Skins**: Select from the built-in skin bank, specify player usernames, or apply Base64/Texture URLs.
- **Permanent Particle Auras**: Continuous visual aura around the mob (Flame, Soul Fire, Redstone Dust, Portal, Smoke, Witch, Enchantment, etc.).
- **CustomModelData**: Set item model data IDs for custom resource pack integration.

</details>

<details>
<summary><b>🛡️ 4. Visual Equipment & Colored Armor</b></summary>

Full equipment loadout customizer:
- **Equipment Slots**: Helmet, Chestplate, Leggings, Boots, Main Hand weapon, and Off-Hand shield/item.
- **Armor Tiers**: Leather, Chainmail, Iron, Golden, Diamond, and Netherite.
- **RGB Leather Dyeing**: In-game color picker to apply custom hex colors to leather armor pieces.
- **Equipment Drop Rates**: Set per-slot drop chances (0% to 100%).

</details>

<details>
<summary><b>🌲 5. Spawn Conditions & Biomes Whitelist</b></summary>

Control where and when variants appear naturally:
- **Interactive Biome Selector**: Multi-page GUI to toggle allowed/denied biomes with search filtering.
- **Y-Level Altitude Range**: Restrict spawning between minimum and maximum Y coordinates (e.g. Deepslate caves only).
- **Light Level Range**: Minimum and maximum light levels required for spawning.
- **Spawn Time**: Day, Night, or Any time.
- **World Whitelist**: Specify target dimensions (Overworld, Nether, The End, or custom worlds).
- **Spawn Weight**: Relative rarity chance among other variants.

</details>

<details>
<summary><b>📦 6. Custom Drops & Custom Audio</b></summary>

Reward players with specialized loot:
- **Drop Tables**: Add any item with custom drop percentages and min/max quantity bounds.
- **Experience Multiplier**: Custom XP reward on kill.
- **Sound Effects**: Customize mob sounds for Spawn, Hurt, Attack, and Death events.

</details>

<details>
<summary><b>🏷️ 7. Conditional Nametags</b></summary>

Configure adaptive nametags that change based on mob health thresholds or player distance.

</details>

</details>

<details>
<summary><b>⚔️ Mob Squads Customization Hub (<code>/wd gui</code> ➔ Squads)</b></summary>

Create coordinated monster squads that spawn together to challenge players:

- 👥 **Squad Composition**: Add multiple variant types to the squad and specify the count for each member.
- 👑 **Squad Leader**: Assign a designated leader variant that commands the group.
- 📈 **Team Multipliers**: Apply group-wide bonus multipliers for Health, Attack Damage, and Movement Speed.
- ⚡ **Natural Spawn Triggers**: Set trigger chance on natural monster spawns.
- 📍 **Formation Spread Radius**: Configure the spawn radius around the trigger location.

</details>

<details>
<summary><b>🌍 Difficulty Zones & Geofencing Hub (<code>/wd gui</code> ➔ Zones)</b></summary>

Create geographic difficulty areas with distinct scaling rules and protections:

<details>
<summary><b>📐 Zone Geometries & Boundaries</b></summary>

- **Cuboid Zones**: Define 3D bounding boxes using Position 1 and Position 2.
- **Radius Zones**: Define spherical/cylindrical zones based on a central point and block radius.
- **Polygon Zones**: Create custom multi-point 2D boundary polygons drawn directly in-game with the Golden Hoe Wand.

</details>

<details>
<summary><b>🛡️ Zone Protections & Safe-Zones</b></summary>

- **Safe-Zone Mode**: Disable hostile mob spawns entirely inside the area.
- **Block Protection**: Prevent unauthorized block breaking and placing.
- **Container Protection**: Protect chests, barrels, and storage containers from non-members.

</details>

<details>
<summary><b>📊 Zone Modifiers & Distance Scaling</b></summary>

- **Attribute Multipliers**: Custom per-zone Health, Damage, Speed, Detection Range, and Knockback multipliers.
- **Step Distance Scaling**: Gradual difficulty escalation per distance step (`step`, `multPerStep`, `maxMult`).
- **Exclusive Biome Rules**: Override global biome settings within the zone boundaries.

</details>

<details>
<summary><b>🏛️ Beacon Buffs, Permissions & Visuals</b></summary>

- **Beacon Effects**: Grant beneficial ambient potion effects to players entering the zone.
- **Member Access Tiers**: Assign roles to players (Level 1: Visitor, Level 2: Builder, Level 3: Zone Manager).
- **Animated Border Particles**: Outline zone perimeters with customizable particle beams.
- **Danger Nests**: Define ultra-difficult sub-regions within larger zones.

</details>

</details>

<details>
<summary><b>🩸 Blood Moon Customization Hub (<code>/wd gui</code> ➔ Blood Moon)</b></summary>

Configure the apocalyptic nighttime event:

- **Activation & Scheduling**: Set the nightly spawn probability (0% to 100%) or trigger on demand (`/wd bloodmoon`).
- **Precise Time Window**: Automatically activates at dusk (tick 12,500) and concludes at dawn (tick 23,000).
- **Mob Stat Multipliers**: Independent multipliers for mob Health, Damage, Speed, Drop Rates, and Spawn Frequency.
- **Atmospheric Visuals & Sounds**: Configurable broadcast messages, custom sound effects, and red particle visual storms.
- **Temporary Potion Buffs**: Bestow global strength, resistance, or speed buffs upon all nighttime monsters.

</details>

<details>
<summary><b>💧 Thirst, Heat & Hardcore Survival Hub (<code>/wd gui</code> ➔ General Config ➔ Thirst & Hardcore)</b></summary>

Immersive survival mechanics for hardcore gameplay:

<details>
<summary><b>💧 Thirst System & Movement Drain</b></summary>

- **Action Bar HUD**: Real-time 10-bubble thirst display.
- **Dynamic Drain**: Adaptive water loss based on player movement (idle, walking, sprinting).
- **Damage Threshold**: Dehydration causes progressive damage when thirst reaches 0.

</details>

<details>
<summary><b>☀️ Heat Proximity Dehydration</b></summary>

- **Heat Detection**: Standing near lava, open fire, or campfires accelerates dehydration.
- **Configurable Multipliers**: Adjust heat drain sensitivity and effect radius.

</details>

<details>
<summary><b>🍶 Hydration Recovery Sources</b></summary>

Customize thirst restoration points for every drink and food source:
- Water Bucket, Water Bottle, Potions, Cauldrons, Open Water Blocks, Milk Buckets, Honey Bottles, Melon Slices, and Apples.

</details>

<details>
<summary><b>💀 Hardcore Mode (Server-Wide & Personal)</b></summary>

- **Natural Health Regen Disable**: Ultra Hardcore (UHC) style no-regen mode.
- **Regeneration Exceptions**: Toggle allowed regeneration via Golden Apples, Potions, Beacons, or Safe-Zones.
- **Death Penalty**: Configurable death item despawn timer or instant inventory destruction.
- **Personal Hardcore**: Individual players can toggle their own hardcore profile using `/wd settings`.

</details>

</details>

<details>
<summary><b>⛏️ Custom Block Spawners System (Spawner Tool)</b></summary>

Transform any spawner block into an advanced custom spawner:

- **Variant Pool**: Assign multiple custom variants to a single spawner with individual spawn weights.
- **Spawn Interval & Variance**: Set base delay (in seconds) and random variation intervals.
- **Spawn Range & Mob Limits**: Configure spawn radius and maximum concurrent live mobs.
- **Replication Wand**: Copy and paste entire spawner configurations across blocks with one click.
- **Visuals & Audio**: Custom activation particles and trigger sounds.

</details>

<details>
<summary><b>🧰 Administration Tools & Live Scoreboard (<code>/wd tools</code>)</b></summary>

Specialized interactive items for server administrators:

- 🌾 **Zone Wand (Golden Hoe)**: Left-click and right-click to place boundary points and construct polygon zones.
- ⛏️ **Spawner Tool (Netherite Shovel)**: Right-click spawner blocks to edit spawn pools and timings.
- 🧭 **Biome Compass**: Right-click anywhere to configure spawn parameters for the current biome.
- 🪄 **Mob Inspector Stick**: Right-click any entity to inspect its variant ID, current/max health, damage, speed, follow range, and active AI tags.
- 📋 **Live Debug Scoreboard**: Displays active variant counts, nearby mobs, current TPS, and active zone name.

</details>

---

### ✨ Key Features Overview

- **Mob Variants**: Comprehensive custom attributes, equipment, skins, particle auras, and Citizens NPC integration.
- **Mob Squads**: Coordinated monster groups with shared team bonus multipliers.
- **Difficulty Zones**: Geographic zones (Cuboid, Radius, Polygon) with safe-zone protections, beacon boosts, and distance scaling.
- **Blood Moon Event**: Highly configurable night event with enraged mobs, custom sounds, and particle visual effects.
- **Thirst & Heat Survival**: Action bar thirst HUD, heat source dehydration, and customizable water refill points.
- **Custom Block Spawners**: In-game shovel tool to create custom variant spawners with configurable rates, radii, and mob caps.
- **10 Languages Support**: Fully translated into English, French, German, Spanish, Portuguese, Dutch, Polish, Russian, Chinese, and Italian with real-time dynamic YAML comment updates.

---

### 🎮 Commands & Permissions

All admin commands require the `wilddifficulty.admin` permission.

| Command | Description |
|:--------|:------------|
| `/wd gui` | Opens the main interactive administration GUI |
| `/wd reload` | Reloads all configurations and language files |
| `/wd killall` | Despawns all active WildDifficulty mob variants |
| `/wd spawn <variant> [normal\|static]` | Spawns a custom mob variant |
| `/wd spawnsquad <squadId>` | Spawns a mob squad |
| `/wd edit` | Opens the GUI editor for the targeted mob variant |
| `/wd tp <zone>` | Teleports to a difficulty zone |
| `/wd zone <args>` | Manual commands for managing difficulty zones |
| `/wd biome_tool` | Gives the interactive Biome Configuration Wand |
| `/wd spawner_tool` | Gives the interactive Spawner Configuration Shovel |
| `/wd zone_tool` | Gives the interactive Zone Configuration Hoe |
| `/wd inspector_tool` | Gives the Mob Inspector analysis tool |
| `/wd tools` | Gives all administration tools |
| `/wd scoreboard` | Toggles the real-time mob analysis scoreboard |
| `/wd debug` | Toggles console debug logging |
| `/wd bloodmoon` | Forces a Blood Moon event for the upcoming night |
| `/wd language <code>` | Switches the plugin language dynamically |
| `/wd help` | Displays the WildTimber-styled help menu |

---

### ⚙️ Configuration Files

All configuration files are stored in `plugins/WildDifficulty/`:

- `config.yml` — Main plugin configuration (distance scaling, thirst settings, debug).
- `lang.yml` — Active language translation dictionary.
- `mobs.yml` — Global per-mob vanilla overrides.
- `biomes.yml` — Per-biome spawn rules and multipliers.
- `mob-variants.yml` — Custom mob variants and squads definition.
- `zones.yml` — Saved difficulty zones.
- `spawners.yml` — Saved custom block spawners.

---

## 📦 Downloads & Version Builds / Téléchargements

| File / Fichier | Compatible Server Version / Version Serveur |
|:---|:---|
| 📦 `WildDifficulty-paper-1.19-1.0.0.jar` | Compatible Paper / Spigot **1.19.4** |
| 📦 `WildDifficulty-paper-1.20-1.0.0.jar` | Compatible Paper / Spigot **1.20** |
| 📦 `WildDifficulty-paper-1.20.6-1.0.0.jar` | Compatible Paper / Spigot **1.20.6** |
| 📦 `WildDifficulty-paper-1.21-1.0.0.jar` | Compatible Paper / Spigot **1.21.1 / 1.21.3** |
| 📦 `WildDifficulty-paper-26.1-1.0.0.jar` | Compatible Paper / Purpur **26.1** |
| 📦 `WildDifficulty-paper-26.2-1.0.0.jar` | Compatible Paper / Purpur **26.2** *(Déployé sur serveur de test)* |

---

<details>
<summary>🇫🇷 Cliquez ici pour la version française</summary>

## 🇫🇷 Français

### 📖 Description

**WildDifficulty** est un plugin de scaling de difficulté et de survie complète conçu pour Paper 1.21.4 (Java 21). Il enrichit l'expérience de combat en ajoutant des variantes de monstres, des zones de difficulté géographiques, de la déshydratation liée à la chaleur, des événements de Lune de Sang, des spawners de blocs personnalisés et des interfaces d'édition en jeu complètes.

---

### 🎮 Systèmes de Personnalisation en Jeu (GUIs)

Toutes les fonctionnalités et paramètres de configuration de **WildDifficulty** sont éditables en direct sans quitter le jeu via des menus interactifs (`/wd gui`).

<details>
<summary><b>🧭 Menu Principal d'Administration (<code>/wd gui</code>)</b></summary>

Le tableau de bord central permet de naviguer vers chaque sous-système :

| Icône | Catégorie | Description |
|:---|:---|:---|
| ⚔️ **Épée en Diamant** | **Modificateurs Globaux** | Ajuste les multiplicateurs généraux de santé, dégâts, vitesse et détection sur le serveur. |
| 🧟 **Tête de Zombie** | **Variantes de Mobs** | Crée, configure, duplique et gère vos variantes de monstres uniques. |
| 💀 **Crâne de Squelette** | **Escouades** | Crée des groupes de monstres coordonnés avec bonus d'équipe. |
| 🗺️ **Carte** | **Zones de Difficulté** | Délimite des zones géographiques (Cuboid, Rayon, Polygone) avec protections et multiplicateurs. |
| 🩸 **Redstone** | **Lune de Sang** | Configure les chances d'apparition, les multiplicateurs, sons, particules et buffs de la Lune de Sang. |
| ⚙️ **Comparateur** | **Configuration Générale** | Gère les distances de spawn, le cap de variantes, l'anti-combustion et le système de soif. |
| 🛒 **Wagonnet avec Coffre** | **Outils d'Admin** | Reçoit l'ensemble des outils interactifs (Houe de Zone, Pelle de Spawner, Boussole de Biome, Bâton d'Inspecteur). |
| 🚫 **Barrière / Œuf** | **Bloquer Mobs Vanilla** | Active/désactive le blocage des spawns de monstres ou passifs vanilla. |
| 📖 **Livre** | **Sélecteur de Langue** | Change instantanément la langue parmi les 10 langues supportées. |
| 🟩 **Bloc de Commande** | **Recharger Config** | Recharge à chaud tous les fichiers YAML et les commentaires de configuration. |

</details>

<details>
<summary><b>🦇 Gestionnaire des Variantes de Mobs (<code>/wd gui</code> ➔ Variantes)</b></summary>

Créez et personnalisez des variantes de monstres avec statistiques, apparences, comportements et drops dédiés.

<details>
<summary><b>📊 1. Statistiques & Modificateurs d'Attributs</b></summary>

- **Santé Maximale (PV)** : Points de vie personnalisés avec synchronisation automatique.
- **Dégâts d'Attaque** : Dégâts infligés au corps-à-corps ou par projectile.
- **Vitesse de Déplacement** : Vélocité de marche et de sprint du mob.
- **Portée de Détection (Follow Range)** : Distance de repérage des cibles (jusqu'à 100+ blocs).
- **Résistance au Recul (Knockback)** : Pourcentage de résistance aux coups et projections subis.
- **Taille / Échelle (Scale)** : Redimensionnement dynamique du modèle 3D du mob (Minecraft 1.20.5+ / 1.21+).
- **Régénération Passive** : Récupération naturelle de vie au fil du temps.

</details>

<details>
<summary><b>⚔️ 2. Comportements Spéciaux & IA</b></summary>

- **Immunité au Soleil** : Empêche les zombies/squelettes de brûler à la lumière du jour.
- **Mode d'Agressivité** : Configuration de l'hostilité (Toujours hostile, Nuit uniquement, Riposte uniquement).
- **Ignorer les Protections** : Permet au mob de poursuivre et attaquer les joueurs dans les claims/zones sûres.
- **Explosion à la Mort (Attaque Suicide)** : Déclenche une explosion personnalisée lorsque le monstre meurt.
- **Effets de Potion à l'Impact (On-Hit)** : Inflige des malus aux joueurs touchés (Lenteur, Poison, Wither, Cécité, Nausée, Obscurité).
- **Aura de Potions Permanente** : Buffs continus sur le monstre (Vitesse, Force, Résistance, Invisibilité, Résistance au feu).
- **BossBar Personnalisée** : Barre de vie affichée aux joueurs proches avec couleur et style segmenté au choix (Pleine, 6, 10, 12, 20 segments).
- **Tirs de Projectiles à Distance** : Permet aux monstres au corps-à-corps de tirer des flèches, boules de feu ou crânes de wither.
- **Spawners de Mort (Renforts)** : Invoque automatiquement des sbires personnalisés à la mort du monstre parent.

</details>

<details>
<summary><b>🎨 3. Apparence, Skins & Esthétique</b></summary>

- **Nametags Dynamiques** : Formatage du texte au-dessus du monstre avec placeholders `{name}`, `{hp}`, `{max_hp}` et `{level}`.
- **Têtes & Skins Personnalisés** : Sélection parmi la banque de têtes intégrée, pseudo de joueur ou URL de texture Base64.
- **Aura de Particules Permanente** : Champ visuel autour du mob (Flammes, Feu d'Âmes, Redstone, Portail, Fumée, Sorcière, Enchantements, etc.).
- **CustomModelData** : Assignation d'un ID de modèle personnalisé pour l'intégration de resource packs serveur.

</details>

<details>
<summary><b>🛡️ 4. Équipements & Armures Teintées</b></summary>

- **Emplacements d'Équipement** : Casque, Plastron, Jambières, Bottes, Main Principale et Main Secondaire.
- **Niveaux d'Armure** : Cuir, Cotte de mailles, Fer, Or, Diamant et Netherite.
- **Teinture RGB Cuir** : Sélecteur de couleurs en jeu pour teinter les armures en cuir dans n'importe quel code hexadécimal.
- **Chances de Drop d'Équipement** : Taux de drop configurable par pièce (0% à 100%).

</details>

<details>
<summary><b>🌲 5. Conditions de Spawn & Biomes</b></summary>

- **Sélecteur de Biomes Interactif** : Menu multi-pages pour autoriser/interdire les biomes avec filtre de recherche.
- **Plage d'Altitude Y** : Restreint l'apparition entre une altitude minimale et maximale (ex: grottes d'abîme uniquement).
- **Niveau de Lumière** : Seuils de luminosité requis pour le spawn.
- **Moment de Spawn** : Jour, Nuit ou Tout le temps.
- **Monde Assigné** : Liste blanche de mondes (Overworld, Nether, End ou mondes custom).
- **Poids d'Apparition** : Rareté relative par rapport aux autres monstres.

</details>

<details>
<summary><b>📦 6. Drops Personnalisés & Sons</b></summary>

- **Tables de Butin** : Ajout d'objets personnalisés avec pourcentages de chance et quantités min/max.
- **Multiplicateur d'XP** : Gain d'expérience personnalisé lors de l'élimination.
- **Effets Sonores** : Personnalisation des sons de Spawn, Blessure, Attaque et Mort.

</details>

<details>
<summary><b>🏷️ 7. Noms Conditionnels</b></summary>

- Configuration de nametags adaptatifs évoluant selon la santé du monstre ou la distance du joueur.

</details>

</details>

<details>
<summary><b>⚔️ Gestionnaire des Escouades (<code>/wd gui</code> ➔ Escouades)</b></summary>

Créez des groupes de monstres coordonnés qui apparaissent ensemble pour défier les joueurs :

- 👥 **Composition du Groupe** : Ajoutez plusieurs variantes et déterminez le nombre de monstres par membre.
- 👑 **Leader d'Escouade** : Désignez un chef de groupe qui guide la meute.
- 📈 **Bonus d'Équipe** : Multiplicateurs collectifs de PV, de dégâts et de vitesse de déplacement.
- ⚡ **Déclencheurs d'Apparition** : Pourcentage de chance de remplacer un spawn vanilla naturel par l'escouade.
- 📍 **Rayon de Dispersion** : Définition de la distance de dispersion des monstres autour du point d'apparition.

</details>

<details>
<summary><b>🌍 Gestionnaire des Zones de Difficulté (<code>/wd gui</code> ➔ Zones)</b></summary>

Délimitez des zones géographiques avec des règles de difficulté et protections dédiées :

<details>
<summary><b>📐 1. Formes Géométriques & Tracé de Frontières</b></summary>

- **Zones Cubiques (Cuboid)** : Définition par Position 1 et Position 2.
- **Zones Circulaires (Radius)** : Définition par point central et rayon en blocs.
- **Zones Polygonales (Polygon)** : Tracé 2D multi-points libre à l'aide de l'Outil Houe en Or.

</details>

<details>
<summary><b>🛡️ 2. Protections de Zone & Safe-Zones</b></summary>

- **Mode Safe-Zone** : Désactivation totale du spawn des monstres hostiles dans l'enceinte de la zone.
- **Protection Anti-Grief** : Interdiction de destruction et de placement de blocs pour les non-membres.
- **Protection des Coffres** : Sécurisation des conteneurs, coffres et tonneaux.

</details>

<details>
<summary><b>📊 3. Modificateurs de Zone & Scaling par Distance</b></summary>

- **Multiplicateurs d'Attributs** : Multiplicateurs locaux pour la santé, les dégâts, la vitesse, la détection et le knockback.
- **Scaling Progressif par Distance** : Augmentation de la difficulté par paliers de distance (`step`, `multPerStep`, `maxMult`).
- **Règles de Biomes Exclusives** : Surcharge des règles globales de biome à l'intérieur de la zone.

</details>

<details>
<summary><b>🏛️ 4. Effets de Balise (Beacon), Rôles de Membres & Particules</b></summary>

- **Effets de Balise (Beacon)** : Effets de potion bénéfiques accordés aux joueurs présents dans la zone.
- **Gestion des Rôles** : Rôles d'accès pour les joueurs (Niveau 1: Visiteur, Niveau 2: Constructeur, Niveau 3: Gestionnaire de zone).
- **Particules de Bordure** : Visualisation animée des contours de la zone (Dust, Flammes, End Rod, etc.).
- **Nids de Danger (Danger Nests)** : Création de sous-zones de danger extrême au sein d'une grande région.

</details>

</details>

<details>
<summary><b>🩸 Événement de la Lune de Sang (<code>/wd gui</code> ➔ Lune de Sang)</b></summary>

Configurez l'événement nocturne apocalyptique :

- **Planification & Fréquence** : Probabilité d'apparition par nuit (0% à 100%) ou déclenchement forcé immédiat (`/wd bloodmoon`).
- **Plage Temporelle Précise** : Début à 12 500 ticks (crépuscule) et fin à 23 000 ticks (aube).
- **Multiplicateurs d'Événement** : Augmentation des PV, dégâts, vitesse, taux de drops et taux de spawn nocturnes.
- **Ambiance Visuelle & Sonore** : Messages diffusés sur le serveur, sons d'alerte et tempête de particules rouges.
- **Buffs de Potions Globaux** : Octroi d'effets de potion (Force, Résistance, Vitesse) aux monstres pendant la nuit.

</details>

<details>
<summary><b>💧 Système de Soif, Chaleur & Hardcore (<code>/wd gui</code> ➔ Soif & Hardcore)</b></summary>

Mécaniques de survie immersives pour un gameplay hardcore :

<details>
<summary><b>💧 1. Barre de Soif HUD & Dégradation Dynamique</b></summary>

- **Affichage HUD** : Jauge 10 bulles dans l'action bar du joueur.
- **Dégradation Dynamique** : Consommation d'eau adaptée selon les actions (immobile, marche, sprint).
- **Seuil Critique** : Dégâts progressifs de déshydratation lorsque la jauge atteint zéro.

</details>

<details>
<summary><b>☀️ 2. Déshydratation par Chaleur & Proximité</b></summary>

- **Détection des Sources Chaudes** : La proximité de lave, feu ou feux de camp accélère la déshydratation.
- **Multiplicateur Configurable** : Ajustement de la sensibilité et du rayon d'action de la chaleur.

</details>

<details>
<summary><b>🍶 3. Sources de Restauration d'Hydratation</b></summary>

Points de restauration configurables pour chaque boisson et aliment :
- Seau d'eau, Bouteille d'eau, Potions, Chaudrons d'eau, Blocs d'eau ouverte, Seaux de lait, Fioles de miel, Tranches de melon et Pommes.

</details>

<details>
<summary><b>💀 4. Mode Hardcore (Global & Profil Personnel /wd settings)</b></summary>

- **Désactivation de la Régénération Naturelle** : Mode Ultra Hardcore (UHC) sans régénération passive de cœurs.
- **Exceptions de Régénération** : Autorisation/blocage de la régénération via Pommes Dorées, Potions, Balises ou Safe-Zones.
- **Pénalité de Mort** : Timer de disparition d'équipement configurable ou suppression immédiate du stuff à la mort.
- **Hardcore Personnel** : Chaque joueur peut activer son propre profil hardcore via la commande `/wd settings`.

</details>

</details>

<details>
<summary><b>⛏️ Spawners de Blocs Personnalisés (Outil de Spawner)</b></summary>

Transformez n'importe quel bloc spawner en générateur de variantes avancées :

- **Pool de Variantes** : Assignez plusieurs variantes personnalisées avec un poids de tirage individuel sur chaque spawner.
- **Intervalles & Variances** : Fréquence d'apparition en secondes et marge aléatoire.
- **Rayon & Limite de Mobs** : Portée d'activation et plafond de monstres vivants simultanés.
- **Outil Copier/Coller** : Répliquez la configuration d'un spawner sur d'autres blocs en un clic.
- **Visuels & Audio** : Particules d'activation et sons de déclenchement personnalisés.

</details>

<details>
<summary><b>🧰 Outils d'Administration & Scoreboard de Debug (<code>/wd tools</code>)</b></summary>

Objets interactifs pour la gestion du serveur en jeu :

- 🌾 **Houe de Zone (Golden Hoe)** : Clic gauche et droit pour placer des points de repère et tracer des polygones.
- ⛏️ **Pelle de Spawner (Netherite Shovel)** : Clic droit sur un spawner pour éditer ses pools et délais.
- 🧭 **Boussole de Biome (Compass)** : Clic droit pour ouvrir l'éditeur de spawn du biome actuel.
- 🪄 **Bâton d'Inspecteur (Inspector Stick)** : Clic droit sur un monstre pour afficher ses statistiques en temps réel, sa variante et ses tags d'IA.
- 📋 **Scoreboard Latéral de Debug** : Affiche le nombre de variantes actives, les monstres proches, le TPS du serveur et le nom de la zone active.

</details>

---

### ✨ Aperçu des Fonctionnalités Clés

- **Variantes de Mobs** : Statistiques personnalisées (PV, dégâts, vitesse, knockback), équipements, têtes personnalisées (URLs/skins), auras de particules et intégration NPCs Citizens.
- **Escouades** : Groupes de monstres coordonnés avec multiplicateurs d'équipe partagés.
- **Zones & Scaling par Distance** : Zones géographiques (Cuboid, Rayon, Polygone) avec protections, effets de balise et difficulté progressive.
- **Lune de Sang** : Événement nocturne paramétrable avec monstres enragés, sons immersifs et tempête visuelle.
- **Système de Soif & Chaleur** : Jauge de soif dans l'action bar, déshydratation près des sources de chaleur et restauration d'eau personnalisable.
- **Spawners Personnalisés** : Outil pelle pour créer des générateurs de variantes avec délais et rayons sur-mesure.
- **Support 10 Langues & Commentaires Dynamiques** : Traduction intégrale en 10 langues avec mise à jour en temps réel des commentaires dans les fichiers YAML.

---

### 🎮 Commandes & Permissions

Toutes les commandes d'administration nécessitent la permission `wilddifficulty.admin`.

| Commande | Description |
|:---------|:------------|
| `/wd gui` | Ouvre le menu d'administration principal interactif |
| `/wd reload` | Recharge l'ensemble des configurations et fichiers de langue |
| `/wd killall` | Supprime toutes les créatures de variantes actives |
| `/wd spawn <variante> [normal\|static]` | Fait apparaître une variante de monstre personnalisée |
| `/wd spawnsquad <idEscouade>` | Fait apparaître une escouade de monstres |
| `/wd edit` | Ouvre l'éditeur GUI de la variante pointée |
| `/wd tp <zone>` | Téléporte le joueur à une zone de difficulté |
| `/wd zone <args>` | Commandes manuelles de gestion des zones |
| `/wd biome_tool` | Donne la boussole de gestion des biomes |
| `/wd spawner_tool` | Donne la pelle de gestion des spawners |
| `/wd zone_tool` | Donne la houe de traçage des zones |
| `/wd inspector_tool` | Donne l'outil d'analyse (Inspecteur de Mobs) |
| `/wd tools` | Donne tous les outils d'administration d'un coup |
| `/wd scoreboard` | Active ou désactive le scoreboard de debug en temps réel |
| `/wd debug` | Active ou désactive les logs de debug dans la console |
| `/wd bloodmoon` | Planifie ou force une Lune de Sang |
| `/wd language <code>` | Change dynamiquement la langue du plugin et des commentaires YAML |
| `/wd help` | Affiche l'interface d'aide styled WildTimber |

---

### ⚙️ Fichiers de Configuration YAML

Tous les fichiers de configuration se trouvent dans `plugins/WildDifficulty/` :

- `config.yml` — Configuration principale du plugin (scaling par distance, système de soif, debug).
- `lang.yml` — Dictionnaire de traduction de la langue active.
- `mobs.yml` — Surcharges globales pour les monstres vanilla.
- `biomes.yml` — Règles d'apparition et multiplicateurs par biome.
- `mob-variants.yml` — Définition des variantes de monstres et des escouades.
- `zones.yml` — Sauvegarde des zones de difficulté géographiques.
- `spawners.yml` — Sauvegarde des blocs spawners personnalisés.

</details>

---

<div align="center">

Made with ❤️ by **Toryar** | Discord: **Toryar**

</div>
