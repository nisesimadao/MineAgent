# AGENTS.md

## Project

MineAgent is a Minecraft 26.1.2 Fabric client mod that lets a trusted user control the client through natural-language instructions, LLM tool calls, WebUI, Baritone, and packet-level networking.

## Working rules

- Keep the mod buildable at all times.
- Prefer small, testable increments.
- Run `./gradlew build` after code changes.
- Use existing Gradle/Fabric/Loom versions if present.
- Do not replace the whole project structure unless necessary.
- Do not add heavy dependencies without a clear reason.
- Keep WebUI dependency-free unless a later task explicitly allows a frontend framework.
- All Minecraft state mutations must happen on the client thread.
- LLM HTTP calls must not block the client thread.
- Prefer high-level tools over raw packet operations.
- Packet logs must be filtered and summarized before being sent to the LLM.
- If a feature is not implemented yet, add a stub ToolResult instead of crashing.
- Document assumptions in `docs/ASSUMPTIONS.md`.

## Done means

- `./gradlew build` passes.
- New commands are listed in README.
- New tools are listed in `docs/TOOLS.md`.
- Manual test steps are updated.
