# Assumptions

- Minecraft 26.1.2 uses unobfuscated Mojang official names, so this project uses `net.fabricmc.fabric-loom` without a Yarn mappings dependency.
- Java 25 is required by the 26.1 Fabric toolchain.
- The MVP is a client-only mod.
- The default LLM provider is `mock` so the mod can be tested without an API key.
- Baritone is optional and is invoked through local `#` chat commands when the `baritone` mod is loaded.
- `packet_send_custom_payload` is implemented as a logged MineAgent bridge event in v0.1. Direct raw 26.1 custom payload packet construction is deferred because the official payload API changed and should be implemented with a dedicated server-side bridge in v0.2.
- Packet summaries are intentionally compact and lossy. Full raw packet replay is out of scope for v0.1.
