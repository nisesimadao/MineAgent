# Tools

Implemented v0.1 tools:

- `get_status`: returns player name, position, yaw/pitch, health, food, and dimension when in-world.
- `get_inventory`: returns non-empty inventory slots and selected hotbar slot.
- `send_chat`: sends a normal chat message.
- `run_command`: sends a client-origin server command.
- `baritone`: sends a local Baritone command through chat when Baritone is loaded.
- `packet_read_recent`: returns recent summarized C2S/S2C packet events.
- `packet_send_wrapped`: supports `chat_message` and `command`.
- `packet_send_custom_payload`: records a JSON bridge payload event for `mineagent:agent_to_server`.

Unimplemented v0.1 stubs are documented for future work: combat, gather, advanced inventory/container/craft/build, packet trace, packet replay, and raw reflection packet sender.
