# Packet Gateway

`PacketGateway` stores compact packet summaries in memory. It captures:

- C2S packets at `Connection.send`
- S2C packets at `Connection.channelRead0`

Each event records:

- ID
- timestamp
- direction
- packet class/name
- summary JSON
- `toString()` preview

The packet tools and WebUI `/api/packets` endpoint read from the same log.

## Sending

`packet_send_wrapped` implements:

- `chat_message`
- `command`

`packet_send_custom_payload` currently logs a MineAgent bridge payload event. Direct low-level CustomPayload packet construction is deferred to keep v0.1 buildable and version-stable.
