# Disabled for the 26.3 port

Files in this folder are **not compiled** and not referenced by any mixin config. They are kept for a
later port. They still use the 1.21.1 **Yarn** names, except `InventorioResources.kt.yarrp`, which was
remapped to Mojang names.

| What | Files | Why |
|---|---|---|
| Trinkets integration | `src/main/java/.../mixin/fabric/trinkets/*`, `src/main/kotlin/.../integration/trinkets/*`, `src/main/resources/inventorio-fabric.mixins.json` | Optional compat; no 26.3 port verified |
| Clumps integration | `src/main/kotlin/.../integration/ClumpsIntegration.kt` | Optional compat; removed from `InventorioFabric.fabricModIntegrations` |
| Early Loading Screen | (dev runtime dependency only) | Optional, dev-only |
| YARRP runtime data pack | `src/main/kotlin/.../InventorioResources.kt.yarrp` | YARRP has no 26.x build (latest v0.2.0 = MC 1.21.1). `src/.../InventorioResources.kt` is a stub that only keeps the `DEEP_POCKETS` key. The enchantment, its tags, the book recipe and the advancement are **not** registered yet. |

The `trinketsIntegration` global setting and `TrinketsTester` are still in `src` but have no effect.
