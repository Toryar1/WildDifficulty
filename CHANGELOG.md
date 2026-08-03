# 📜 WildDifficulty — Changelog

All notable changes to the **WildDifficulty** plugin will be documented in this file.  
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

🔗 **SpigotMC Resource**: [WildDifficulty on SpigotMC (Resource 137547)](https://www.spigotmc.org/resources/wilddifficulty.137547/)

## 📌 [1.1.0] - Planned / Work in Progress (TODO)

### 🛠️ Planned Fixes
- **GUI Back Buttons**: Fix back button navigation for:
  - Spawn Conditions GUI (`VARIANT_SPAWN_CONDITIONS`)
  - Equipment GUI (`VARIANT_EQUIPMENT`)
  - Drops GUI (`VARIANT_DROPS`)
  - Sounds GUI (`VARIANT_SOUNDS`)

---

## 🚀 [1.0.0] - 2026-07-30 — Initial Release on SpigotMC

### 🎉 Added & Highlights
- **SpigotMC Official Release**: Published WildDifficulty (Resource ID: 137547) for Paper / Spigot 1.21.4 (Java 21).
- **10 Languages Support**: Fully translated into English (`en`), French (`fr`), German (`de`), Spanish (`es`), Brazilian Portuguese (`pt_BR`), Dutch (`nl`), Polish (`pl`), Russian (`ru`), Simplified Chinese (`zh_CN`), and Italian (`it`).
- **Dynamic YML Comment Translator**: Switching language via `/wd language <code>` automatically updates all YML configuration comments in real-time.
- **Mob Variants System**: Custom attributes (HP, Damage, Speed, Detection Range, Knockback Resistance, Scale, Regen), visual equipment, custom head skins (Player heads & Base64/URL textures), and Citizens NPC support.
- **Mob Squads**: Coordinated monster groups with custom bonus multipliers.
- **Difficulty Zones**: Geographic zones (Cuboid, Radius, Polygon) with safe-zone protections, beacon effect boosts, and interactive Golden Hoe wands.
- **Blood Moon Event**: Configurable nighttime event with enraged mob behaviors, custom sounds, potion effects, and precise 12,500 to 23,000 tick active window.
- **Thirst & Heat Survival System**: 10-bubble action bar thirst HUD with heat proximity dehydration (lava, fire, campfires) and water refill mechanics.
- **Custom Block Spawners**: In-game shovel tool to create custom variant spawners with configurable rates, radii, sounds, and particle effects.
- **WildTimber Help Menu**: Sleek help interface styled after WildTimber.

### 🛠️ Fixes & Polish
- **Language Selector GUI**: Fixed click slot offset alignment (`{11..15, 20..24}`) for accurate flag selection.
- **Squad & Variant Editors**: Fixed missing back button handlers and translated all hardcoded labels.
- **Zone Tool Wand**: Replaced fallback command with direct item addition in inventory.
- **Global Modifiers GUI**: Centered items on slots 11 through 15 with Arrow Back button.
