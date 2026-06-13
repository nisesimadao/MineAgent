# MineAgent TODO

## Next survival workflow

- [ ] Confirm inventory after Baritone finishes cutting trees.
- [ ] Craft logs into planks.
- [ ] Craft a crafting table if missing.
- [ ] Craft sticks and basic tools when materials allow.
- [ ] Build a small survival base using collected wood, not creative commands.
- [ ] Add a safe return / stop workflow so Baritone does not wander too far.
- [ ] Add observable checkpoints for gather -> craft -> build macros.

## MCP tools to add

- [x] Global `stop_all`: stop Baritone, release keys, clear queued pulses.
- [x] `release_all_keys` / cancel all key pulses.
- [x] `baritone_stop` wrapper.
- [x] `read_chat` for command/server feedback.
- [x] FAWE/WorldEdit command wrappers.
- [ ] FAWE selection helpers by absolute coordinates.
- [ ] FAWE feedback helper that sends a command and waits for chat response.
- [ ] `baritone_task_status` or practical progress proxy.
- [ ] `wait_for_inventory` and `wait_for_container` polling helpers.
- [ ] Richer container metadata: screen title, lore/components, custom names.
- [x] Baritone status and command bridge.
- [x] Baritone goto wrapper.
- [x] Inventory listing.
- [ ] Craft item wrapper.
- [x] Select hotbar slot.
- [x] Place block from selected inventory item.
- [x] GUI slot click / pickup.
- [x] Raw-ish container packet helpers.
- [x] Click GUI slot by item id or display name.
- [x] Generic key signal sender.
- [x] List all Minecraft keybindings, including mod-added keys.
- [ ] Packet/event logging for recent inbound/outbound packets.
- [ ] Custom payload send/read helpers if needed for plugin workflows.
- [ ] Screenshot from Minecraft client window or framebuffer.
- [ ] Task macro: gather wood -> craft -> build shelter.
- [ ] FAWE/Commandの成功か失敗か、の出力を返す機能

## Design notes

- Prefer survival-legit actions for gameplay tasks.
- Keep `run_command` available for testing, but avoid it for survival automation unless explicitly requested.
- Treat Baritone-Meteor as `baritone-meteor`; also support plain `baritone`.
- Actions must be paired with state checks where possible; issuing an action is not proof it succeeded.
- Avoid long-running Baritone work without a stop/leash/health check.
