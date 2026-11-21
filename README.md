# Inventorio (Minecraft 26.3 Fabric port)

Unofficial port of **Inventorio** - "My vision of the Inventory Update" (Deep Pockets enchantment,
Tool Belt, Utility Belt and more) - to Minecraft **26.3** on **Fabric**.

## Credits

Inventorio was created by **LizardOfOz** ([Lizard-Of-Oz/Inventorio](https://github.com/Lizard-Of-Oz/Inventorio))
and is maintained by **RubixDev** ([RubixDev/Inventorio](https://github.com/RubixDev/Inventorio)).
All original code, textures and translations are theirs. This repository only adapts the
1.21.1 Fabric version to Minecraft 26.3.

## Licence

All Rights Reserved, as in the upstream project. See the upstream repositories for terms.

## Port status

Work in progress (phase 1: base project). The code was converted from Yarn to Mojang's official names
(Minecraft 26.x ships unobfuscated) but does **not compile yet** - remaining API drift is listed in the
porting notes. Temporarily disabled integrations are described in [`disabled/README.md`](disabled/README.md).

| Component            | Version              |
|----------------------|----------------------|
| Minecraft            | 26.3                 |
| Java                 | 25                   |
| Gradle               | 9.8.2                |
| Fabric Loom          | 1.19.4 (`net.fabricmc.fabric-loom`, no mappings) |
| Fabric Loader        | 0.20.1               |
| Fabric API           | 0.166.2+26.3         |
| Fabric Language Kotlin | 1.14.2+kotlin.2.4.21 |
| Cloth Config         | 26.3.158             |
| Mod Menu             | 21.0.1               |

Build with a JDK 25: `./gradlew build`.
