<div align="center">

<img src="https://img.shields.io/badge/Paper-26.2-orange?style=for-the-badge&logo=minecraft" alt="Paper 26.2"/>
<img src="https://img.shields.io/badge/Java-21-blue?style=for-the-badge&logo=openjdk" alt="Java 21"/>
<img src="https://img.shields.io/badge/Version-1.0.0-green?style=for-the-badge" alt="Version 1.0.0"/>
<img src="https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge" alt="Proprietary"/>

# 🐺 WildDifficulty

**Un plugin de scaling de difficulté avancé pour serveurs Minecraft Paper**  
**An advanced difficulty scaling plugin for Minecraft Paper servers**

</div>

---

## 🇫🇷 Français

### 📖 Description

**WildDifficulty** est un plugin Minecraft conçu pour Paper 26.2 (1.21.4) qui enrichit profondément l'expérience de combat en rendant les mobs bien plus dangereux et variés. Il introduit un système de **variantes de mobs**, de **zones de difficulté**, de **biomes personnalisés**, de **survie avancée** et bien plus encore.

### ✨ Fonctionnalités

#### 🦇 Système de Variantes de Mobs
- Créez des variantes de mobs vanilla avec des statistiques personnalisées (PV, dégâts, vitesse, taille)
- Appliquez des équipements, des auras de particules, des effets de potion et des nametags colorés
- Support des skins personnalisés via tête de joueur ou URL de texture
- Spawn de monstres "PLAYER" via **Citizens** (NPCs)
- Spawn conditionnel par biome, zone, heure du jour (JOUR / NUIT / N'IMPORTE QUAND)
- Mort spawner : invoque d'autres variantes à la mort d'un mob

#### ⚔️ Escouades (Squads)
- Définissez des groupes de monstres qui apparaissent ensemble
- Multiplicateurs de PV, dégâts et vitesse par escouade
- Spawn automatique lors d'événements naturels

#### 🌍 Zones de Difficulté
- Créez des zones géographiques avec des règles de spawn exclusives
- Mode Safe Zone, effets de beacon, multiplicateurs par zone
- Gestion des membres avec niveaux de permission (1 à 5)
- Outil de zone interactif pour délimiter les zones en jeu

#### 🌅 Lune de Sang (Blood Moon)
- Événement nocturne spécial avec spawn multiplié
- Effets visuels et sonores configurables
- Activation automatique ou manuelle (`/wd bloodmoon`)

#### 💧 Système de Soif (Thirst)
- Jauge de soif à 10 bulles visible dans l'HUD
- Déshydratation progressive selon l'environnement
- Sources de chaleur (lave, feu, lave, feux de camp) qui accélèrent la déshydratation selon la proximité
- Réinitialisation à la mort et au respawn
- Configurable via GUI admin

#### 💀 Mode Hardcore Personnel
- Chaque joueur peut activer son propre mode hardcore
- Dispawn instantané de l'équipement à la mort
- Minuteur de despawn configurable

#### 🗺️ Biomes Personnalisés
- Configurez des règles de spawn par biome
- Outil de biome interactif pour éditer les règles en jeu
- Liste blanche et noire de variantes par biome

#### 🧱 Spawners Personnalisés
- Créez des spawners de variantes à des emplacements précis
- Effets de particules et sons configurables
- Plage et fréquence de spawn configurables

#### 📊 Scoreboard de Debug
- Affichage des stats en temps réel du mob ciblé
- Activable/désactivable à la volée avec `/wd scoreboard`

#### 🌐 Localisation Complète
- Tous les messages sont dans un fichier `lang.yml` éditable
- Support des codes couleur (`&a`, `&c`, etc.)
- Placeholders dynamiques (`{zone}`, `{count}`, etc.)

### ⚙️ Configuration

| Fichier | Description |
|---------|-------------|
| `config.yml` | Configuration principale (difficulté, thirst, blood moon...) |
| `lang.yml` | Tous les messages du plugin |
| `mob-variants.yml` | Définition des variantes de mobs |
| `biomes.yml` | Règles de spawn par biome |
| `mobs.yml` | Règles globales par type de mob |
| `zones.yml` | Sauvegarde des zones de difficulté |

### 🎮 Commandes

| Commande | Description | Permission |
|----------|-------------|------------|
| `/wd gui` | Ouvre le menu GUI principal | `wilddifficulty.admin` |
| `/wd reload` | Recharge toutes les configurations | `wilddifficulty.admin` |
| `/wd killall` | Supprime toutes les entités WD | `wilddifficulty.admin` |
| `/wd spawn <variante> [normal]` | Fait apparaître une variante | `wilddifficulty.admin` |
| `/wd spawnsquad <escouade>` | Fait apparaître une escouade | `wilddifficulty.admin` |
| `/wd edit` | Édite la variante pointée | `wilddifficulty.admin` |
| `/wd tp <zone>` | Téléporte à une zone | `wilddifficulty.admin` |
| `/wd bloodmoon` | Planifie une Lune de Sang | `wilddifficulty.admin` |
| `/wd biome_tool` | Donne l'outil de biome | `wilddifficulty.admin` |
| `/wd spawner_tool` | Donne l'outil de spawner | `wilddifficulty.admin` |
| `/wd zone_tool` | Donne l'outil de zone | `wilddifficulty.admin` |
| `/wd inspector_tool` | Donne l'inspecteur de mobs | `wilddifficulty.admin` |
| `/wd tools` | Donne tous les outils | `wilddifficulty.admin` |
| `/wd scoreboard` | Active/désactive le scoreboard | `wilddifficulty.admin` |
| `/wd debug` | Active/désactive les logs debug | `wilddifficulty.admin` |
| `/wd settings` | Paramètres personnels du joueur | `wilddifficulty.player.settings` |
| `/wd zone <args>` | Gestion manuelle des zones | `wilddifficulty.admin` |
| `/wd help` | Affiche l'aide | `wilddifficulty.admin` |

### 🔑 Permissions

| Permission | Description | Défaut |
|-----------|-------------|--------|
| `wilddifficulty.admin` | Accès à toutes les commandes admin | OP |
| `wilddifficulty.player.settings` | Accès aux paramètres personnels | Tous |
| `wilddifficulty.zone.manage` | Gestion de zone (niveau gestionnaire) | Tous |

### 🔧 Dépendances

| Plugin | Requis | Description |
|--------|--------|-------------|
| **Paper 26.2+** | ✅ Oui | Serveur de base |
| **Citizens 2.x** | ❌ Optionnel | Support des NPCs joueurs |

### 📥 Installation

1. Téléchargez le fichier JAR depuis les [Releases](https://github.com/Toryar1/WildDifficulty/releases)
2. Placez-le dans le dossier `plugins/` de votre serveur Paper
3. Redémarrez le serveur
4. Configurez les fichiers YAML dans `plugins/WildDifficulty/`
5. Utilisez `/wd gui` pour accéder à l'interface graphique

---

## 🇬🇧 English

### 📖 Description

**WildDifficulty** is a Minecraft plugin designed for Paper 26.2 (1.21.4) that deeply enhances the combat experience by making mobs much more dangerous and varied. It introduces a **mob variant system**, **difficulty zones**, **custom biomes**, **advanced survival mechanics**, and much more.

### ✨ Features

#### 🦇 Mob Variant System
- Create vanilla mob variants with custom stats (HP, damage, speed, size)
- Apply equipment, particle auras, potion effects, and colored nametags
- Custom skin support via player head or texture URL
- Spawn "PLAYER" monsters via **Citizens** (NPCs)
- Conditional spawning by biome, zone, time of day (DAY / NIGHT / ANY)
- Death spawner: summons other variants upon mob death

#### ⚔️ Squads
- Define groups of monsters that appear together
- HP, damage, and speed multipliers per squad
- Automatic spawning during natural events

#### 🌍 Difficulty Zones
- Create geographic zones with exclusive spawn rules
- Safe Zone mode, beacon effects, per-zone multipliers
- Member management with permission levels (1 to 5)
- Interactive zone tool for delimiting zones in-game

#### 🌅 Blood Moon
- Special nighttime event with multiplied spawning
- Configurable visual and sound effects
- Automatic or manual activation (`/wd bloodmoon`)

#### 💧 Thirst System
- 10-bubble thirst gauge visible in the HUD
- Progressive dehydration based on environment
- Heat sources (lava, fire, campfires) accelerate dehydration based on proximity
- Reset on death and respawn
- Configurable via admin GUI

#### 💀 Personal Hardcore Mode
- Each player can activate their own hardcore mode
- Instant equipment despawn on death
- Configurable despawn timer

#### 🗺️ Custom Biomes
- Configure spawn rules per biome
- Interactive biome tool to edit rules in-game
- Allow/deny lists of variants per biome

#### 🧱 Custom Spawners
- Create variant spawners at precise locations
- Configurable particle effects and sounds
- Configurable spawn range and frequency

#### 📊 Debug Scoreboard
- Real-time stat display for targeted mob
- Toggle on/off with `/wd scoreboard`

#### 🌐 Full Localization
- All messages are in an editable `lang.yml` file
- Color code support (`&a`, `&c`, etc.)
- Dynamic placeholders (`{zone}`, `{count}`, etc.)

### ⚙️ Configuration

| File | Description |
|------|-------------|
| `config.yml` | Main configuration (difficulty, thirst, blood moon...) |
| `lang.yml` | All plugin messages |
| `mob-variants.yml` | Mob variant definitions |
| `biomes.yml` | Spawn rules per biome |
| `mobs.yml` | Global rules per mob type |
| `zones.yml` | Difficulty zone data |

### 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/wd gui` | Opens the main GUI menu | `wilddifficulty.admin` |
| `/wd reload` | Reloads all configurations | `wilddifficulty.admin` |
| `/wd killall` | Removes all WD entities | `wilddifficulty.admin` |
| `/wd spawn <variant> [normal]` | Spawns a variant | `wilddifficulty.admin` |
| `/wd spawnsquad <squad>` | Spawns a squad | `wilddifficulty.admin` |
| `/wd edit` | Edits the targeted variant | `wilddifficulty.admin` |
| `/wd tp <zone>` | Teleports to a zone | `wilddifficulty.admin` |
| `/wd bloodmoon` | Schedules a Blood Moon | `wilddifficulty.admin` |
| `/wd biome_tool` | Gives the biome tool | `wilddifficulty.admin` |
| `/wd spawner_tool` | Gives the spawner tool | `wilddifficulty.admin` |
| `/wd zone_tool` | Gives the zone tool | `wilddifficulty.admin` |
| `/wd inspector_tool` | Gives the mob inspector | `wilddifficulty.admin` |
| `/wd tools` | Gives all tools | `wilddifficulty.admin` |
| `/wd scoreboard` | Toggles the debug scoreboard | `wilddifficulty.admin` |
| `/wd debug` | Toggles debug logging | `wilddifficulty.admin` |
| `/wd settings` | Personal player settings | `wilddifficulty.player.settings` |
| `/wd zone <args>` | Manual zone management | `wilddifficulty.admin` |
| `/wd help` | Shows help | `wilddifficulty.admin` |

### 🔑 Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `wilddifficulty.admin` | Full access to all admin commands | OP |
| `wilddifficulty.player.settings` | Access to personal settings | Everyone |
| `wilddifficulty.zone.manage` | Zone management (manager level) | Everyone |

### 🔧 Dependencies

| Plugin | Required | Description |
|--------|----------|-------------|
| **Paper 26.2+** | ✅ Yes | Base server |
| **Citizens 2.x** | ❌ Optional | Player NPC support |

### 📥 Installation

1. Download the JAR from [Releases](https://github.com/Toryar1/WildDifficulty/releases)
2. Place it in your Paper server's `plugins/` folder
3. Restart the server
4. Configure the YAML files in `plugins/WildDifficulty/`
5. Use `/wd gui` to access the graphical interface

---

## ⚖️ Licence / License

Ce logiciel est protégé par une licence propriétaire.  
This software is protected under a proprietary license.

**Toute reproduction, modification ou distribution sans autorisation écrite et compensation financière préalable est strictement interdite.**  
**Any reproduction, modification, or distribution without prior written authorization and financial compensation is strictly prohibited.**

Voir le fichier [LICENSE](LICENSE) pour les détails complets.  
See the [LICENSE](LICENSE) file for full details.

---

<div align="center">

Made with ❤️ by **Toryar1**

</div>
