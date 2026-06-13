# MineAgent

MineAgent is a Minecraft 26.1.2 Fabric client mod MVP that accepts trusted natural-language instructions from client commands, chat triggers, a local WebUI, and packet tools.

## Build

```powershell
./gradlew build
```

The project uses Fabric Loom `net.fabricmc.fabric-loom`, Java 25, Fabric Loader, and Fabric API for Minecraft 26.1.2.

## Commands

- `/mineagent status` shows current task and player status.
- `/mineagent ask <instruction>` starts a task.
- `/mineagent stop` stops the current task.
- `/mineagent web` prints `http://127.0.0.1:25712`.
- `/mineagent logs` shows recent task logs.
- `/mineagent packets recent [limit]` shows recent packet summaries.
- `/mineagent packets clear` clears packet logs.
- `/mineagent reload` reloads config and users.

## Config

MineAgent creates and reads:

- `config/mineagent/config.json`
- `config/mineagent/registered_users.json`

Set `"llm.provider": "mock"` for offline testing, or `"openai_compatible"` with `baseUrl`, `apiKey`, and `model` for an OpenAI-compatible chat-completions endpoint.

## Chat Trigger

Messages matching:

```text
<agentName> <instruction>
```

are parsed by the chat trigger path. The sender must be registered in `registered_users.json`.

## WebUI

When enabled, the local server starts on:

```text
http://127.0.0.1:25712
```

Implemented API:

- `GET /api/status`
- `POST /api/instruct`
- `POST /api/stop`
- `GET /api/logs`
- `GET /api/packets`
- `POST /api/packets/send`
- `POST /api/custom-payload/send`

## Packet Console

The MVP logs C2S/S2C packet summaries through a `Connection` mixin. It implements tool support for `packet_read_recent`, `packet_send_wrapped` for `chat_message` and `command`, and `packet_send_custom_payload`.

## Known Limits

Baritone is optional and currently invoked through local `#` chat commands. Raw packet send and advanced player-control tools are intentionally stubbed for later versions.
