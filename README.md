# MineAgent

MineAgent is a Fabric client mod plus a tiny MCP bridge for controlling the live Minecraft Java client from Codex.

This avoids the Mineflayer protocol-version problem by running commands inside the real Fabric client instead of logging in a separate bot.

## Build

```powershell
.\gradlew.bat build
```

The mod jar is written to:

```text
build/libs/mineagent-0.2.0.jar
```

## Use

1. Put the jar in the Fabric client's `mods` folder.
2. Start Minecraft `26.1.2` with Fabric.
3. Open a world or LAN/server session.
4. The mod listens on `http://127.0.0.1:17890`.

Add this to `C:\Users\opevi\.codex\config.toml`:

```toml
[mcp_servers.mineagent]
command = 'C:\nvm4w\nodejs\node.exe'
args = ['E:\Coding\MCmod\MineAgent\mcp\mineagent-mcp.js']
startup_timeout_sec = 30
```

Then restart Codex.

## Tools

- `get_status`
- `send_chat`
- `run_command`
- `look_at`
- `move_input`
- `key_signal`
- `list_keybindings`
- `release_all_keys`
- `baritone_stop`
- `stop_all`
- `read_chat`
- `fawe_command`
- `fawe_pos1`
- `fawe_pos2`
- `fawe_pos_select`
- `fawe_set`
- `fawe_walls`
- `fawe_replace`
- `fawe_undo`
- `fawe_redo`
- `jump`
- `select_hotbar_slot`
- `get_block`
- `place_block`
- `get_inventory`
- `wait_for_inventory`
- `get_container`
- `wait_for_container`
- `click_slot`
- `container_click`
- `click_slot_by_item`
- `craft_item`
- `container_button`
- `baritone_status`
- `baritone_command`
- `baritone_goto`
- `get_packet_log`
- `get_screenshot`
- `run_survival_macro`
- `survival_macro_status`

For survival gameplay, prefer the physical tools (`look_at`, `key_signal`, `place_block`, `craft_item`, Baritone wrappers, and the wait helpers). Keep `run_command` for explicit testing/admin work.

If Baritone is installed in the same Fabric client, use `baritone_command` for commands such as `goto`, `mine`, `follow`, and `stop`. The `#` prefix is added automatically.

If FAWE or WorldEdit is available on the server, use the `fawe_*` tools for selection edits. Follow with `read_chat` to inspect command success or error feedback.

`wait_for_inventory` and `wait_for_container` run on the MCP side and poll the live client until a simple item/title condition is met or the timeout expires. Timeout results return `ok:false` with the last observed snapshot so an agent can decide what to do next.
