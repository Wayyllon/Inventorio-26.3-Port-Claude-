# Disabled for the 26.3 port

Files in this folder are **not compiled** and not referenced by any mixin config. They are kept for a
later port. They still use the 1.21.1 **Yarn** names.

| What | Files | Why |
|---|---|---|
| Trinkets integration | `src/main/java/.../mixin/fabric/trinkets/*`, `src/main/kotlin/.../integration/trinkets/*`, `src/main/resources/inventorio-fabric.mixins.json` | Optional compat; no 26.3 port verified |
| Clumps integration | `src/main/kotlin/.../integration/ClumpsIntegration.kt` | Optional compat; removed from `InventorioFabric.fabricModIntegrations` |
| Early Loading Screen | (dev runtime dependency only) | Optional, dev-only |

The `trinketsIntegration` global setting and `TrinketsTester` are still in `src` but have no effect.
