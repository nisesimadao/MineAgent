# MineAgent TODO

## Next survival workflow

- [x] Confirm inventory after Baritone finishes cutting trees.
- [x] Craft logs into planks.
- [x] Craft a crafting table if missing.
- [x] Craft sticks and basic tools when materials allow.
- [x] Build a small survival base using collected wood, not creative commands.
- [x] Add a safe return / stop workflow so Baritone does not wander too far.
- [x] Add observable checkpoints for gather -> craft -> build macros.

## MCP tools to add

- [x] Global `stop_all`: stop Baritone, release keys, clear queued pulses.
- [x] `release_all_keys` / cancel all key pulses.
- [x] `baritone_stop` wrapper.
- [x] `read_chat` for command/server feedback.
- [x] FAWE/WorldEdit command wrappers (consolidated to fawe_command).
- [x] `baritone_task_status` or practical progress proxy.
- [x] `wait_for_inventory` and `wait_for_container` polling helpers.
- [x] Richer container metadata: screen title, lore/components, custom names.
- [x] Baritone status and command bridge.
- [x] Baritone goto wrapper.
- [x] Inventory listing.
- [x] Craft item wrapper.
- [x] Select hotbar slot.
- [x] Place block from selected inventory item.
- [x] GUI slot click / pickup.
- [x] Raw-ish container packet helpers.
- [x] Click GUI slot by item id or display name.
- [x] Generic key signal sender.
- [x] List all Minecraft keybindings, including mod-added keys.
- [x] Packet/event logging for recent inbound/outbound packets.
- [ ] Custom payload send/read helpers if needed for plugin workflows.
- [x] Screenshot from Minecraft client window or framebuffer.
- [x] Task macro: gather wood -> craft -> build shelter.
- [x] MCP-side conditional waits for inventory/container state.
- [x] Survival macro hotbar/container slot mapping cleanup.

## Design notes

- Prefer survival-legit actions for gameplay tasks.
- Keep `run_command` available for testing, but avoid it for survival automation unless explicitly requested.
- Treat Baritone-Meteor as `baritone-meteor`; also support plain `baritone`.
- Actions must be paired with state checks where possible; issuing an action is not proof it succeeded.
- Avoid long-running Baritone work without a stop/leash/health check.
