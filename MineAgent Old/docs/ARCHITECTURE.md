# Architecture

MineAgent is organized around a single client initializer:

```text
MineAgentClient
  ConfigManager / UserRegistry
  TaskManager
  AgentLoop
  ToolRegistry
  PacketGateway
  WebServer
```

Inputs from `/mineagent ask`, WebUI `/api/instruct`, and future chat/custom-payload routes create an `AgentTask`. The `AgentLoop` builds an observation JSON, calls a configured `LlmClient`, parses strict JSON `actions`, and executes tools through `ToolRegistry`.

Minecraft state mutations are queued on the client thread through Minecraft's client executor. LLM HTTP calls run off-thread.

Packet logging is handled by a client `Connection` mixin that records summarized C2S/S2C packet events into an in-memory ring-like log.
