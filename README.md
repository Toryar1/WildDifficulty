<div align="center">

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

### ✨ Key Features

#### 🦇 Mob Variants & Custom Aesthetics
- **Custom Attributes**: Customize health, damage, speed, follow range, knockback resistance, scale, and passive regen.
- **Visual Equipment & Skins**: Equip custom armor, head textures (player heads or texture URLs), custom model data, and colored leather armor.
- **Citizens NPC Support**: Spawn "PLAYER" type mob variants powered by Citizens NPCs.
- **Death Spawners**: Trigger follow-up mob spawns when a variant dies.
- **Particle & Sound Auras**: Attach permanent visual particle fields and sound effects to variants.

#### ⚔️ Mob Squads
- Define groups of monsters that spawn together with custom group multipliers.
- Configure spawn triggers based on natural mob spawn events.

#### 🌍 Difficulty Zones & Distance Scaling
- **Origin Distance Scaling**: Difficulty increases gradually as players venture further from the world origin.
- **Cuboid, Radius & Polygon Zones**: Define custom geographic areas with exclusive spawn rules, safe-zone protections, beacon effect boosts, and member permission tiers.
- **Interactive Zone Wand**: Draw and edit difficulty zone boundaries directly in-game with the Golden Hoe tool.

#### 🩸 Blood Moon Event
- Nighttime event with multiplied mob spawns, enraged mob behaviors, custom sounds, and particle visual effects.
- Automatic night scheduling or instant admin trigger (`/wd bloodmoon`).

#### 💧 Thirst & Heat Survival System
- Custom 10-bubble thirst bar rendered in the action bar / HUD.
- Heat dehydration: Lava, fire, and heat sources drain thirst faster based on proximity.
- Configurable hydration restore points for water buckets, bottles, and cauldrons.

#### 💀 Personal Hardcore Mode
- Per-player optional hardcore toggle with instant item despawning on death.

#### 🌐 10-Language i18n & Dynamic YML Comments
- Fully translated into 10 languages: **English (`en`)**, **Français (`fr`)**, **Deutsch (`de`)**, **Español (`es`)**, **Português (`pt_BR`)**, **Nederlands (`nl`)**, **Polski (`pl`)**, **Русский (`ru`)**, **简体中文 (`zh_CN`)**, **Italiano (`it`)**.
- Changing language via `/wd language <code>` dynamically updates in-game menus, chat messages, AND all inline YML configuration comments in real-time!

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
| `/wd language <code` | Switches the plugin language dynamically |
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

<details>
<summary>🇫🇷 Cliquez ici pour la version française</summary>

## 🇫🇷 Français

### 📖 Description

**WildDifficulty** est un plugin de scaling de difficulté et de survie complète conçu pour Paper 1.21.4 (Java 21). Il enrichit l'expérience de combat en ajoutant des variantes de monstres, des zones de difficulté géographiques, de la déshydratation liée à la chaleur, des événements de Lune de Sang et des interfaces d'édition en jeu.

### ✨ Fonctionnalités Principales

- **Variantes de Mobs** : Statistiques personnalisées (PV, dégâts, vitesse, knockback), équipements, têtes personnalisées (URLs/skins), auras de particules et NPCs Citizens.
- **Escouades** : Invoquez des groupes de monstres coordonnés.
- **Zones & Distance Scaling** : Zones cubiques, circulaires ou polygonales avec l'outil de zone interactif.
- **Lune de Sang** : Événement nocturne configurable avec sons et effets visuels.
- **Système de Soif & Chaleur** : Jauge de soif HUD et déshydratation près des sources de chaleur.
- **Support 10 Langues** : Anglais, Français, Allemand, Espagnol, Portugais, Néerlandais, Polonais, Russe, Chinois, Italien avec traduction dynamique des commentaires YAML.

</details>

---

<div align="center">

Made with ❤️ by **Toryar** | Discord: **Toryar**

</div>
