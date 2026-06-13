#!/usr/bin/env node

const BRIDGE_URL = process.env.MINEAGENT_URL || "http://127.0.0.1:17890";

const tools = [
  {
    name: "get_status",
    description: "Get the live Fabric client player's position, health, food, yaw, pitch, and dimension.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "send_chat",
    description: "Send a normal chat message from the live Fabric client player.",
    inputSchema: {
      type: "object",
      properties: { message: { type: "string" } },
      required: ["message"],
      additionalProperties: false,
    },
  },
  {
    name: "run_command",
    description: "Run a Minecraft command from the live Fabric client player. Omit the leading slash or include it.",
    inputSchema: {
      type: "object",
      properties: { command: { type: "string" } },
      required: ["command"],
      additionalProperties: false,
    },
  },
  {
    name: "look_at",
    description: "Turn the live Fabric client player to look at a world position.",
    inputSchema: {
      type: "object",
      properties: {
        x: { type: "number" },
        y: { type: "number" },
        z: { type: "number" },
      },
      required: ["x", "y", "z"],
      additionalProperties: false,
    },
  },
  {
    name: "move_input",
    description: "Hold one movement key for a short number of ticks.",
    inputSchema: {
      type: "object",
      properties: {
        direction: { type: "string", enum: ["forward", "back", "left", "right", "sneak", "sprint"] },
        ticks: { type: "integer", minimum: 1, maximum: 100 },
      },
      required: ["direction"],
      additionalProperties: false,
    },
  },
  {
    name: "key_signal",
    description: "Send a key signal to the live Fabric client. The key can be any Minecraft keybinding name, translated name, bound key, or common alias.",
    inputSchema: {
      type: "object",
      properties: {
        key: { type: "string" },
        action: { type: "string", enum: ["pulse", "down", "up"] },
        ticks: { type: "integer", minimum: 1, maximum: 100 }
      },
      required: ["key"],
      additionalProperties: false,
    },
  },
  {
    name: "list_keybindings",
    description: "List every keybinding registered in the live Minecraft client, including mod-added keys.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "release_all_keys",
    description: "Release every keybinding and clear queued key pulses.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "baritone_stop",
    description: "Send #stop to Baritone if it is available.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "stop_all",
    description: "Emergency stop: release all keys, clear key pulses, and send #stop to Baritone when available.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "read_chat",
    description: "Read recent received chat and game messages. Useful for checking command success or server/plugin feedback.",
    inputSchema: {
      type: "object",
      properties: {
        count: { type: "integer", minimum: 1, maximum: 200 },
        type: { type: "string", enum: ["", "chat", "game"] },
        contains: { type: "string" }
      },
      additionalProperties: false,
    },
  },
  {
    name: "fawe_command",
    description: "Send a FAWE/WorldEdit command. Accepts commands like //set stone, /pos1, or set stone. Use read_chat afterward for feedback.",
    inputSchema: {
      type: "object",
      properties: { command: { type: "string" } },
      required: ["command"],
      additionalProperties: false,
    },
  },
  {
    name: "fawe_pos1",
    description: "Set FAWE/WorldEdit selection position 1 to the current block position.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "fawe_pos2",
    description: "Set FAWE/WorldEdit selection position 2 to the current block position.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "fawe_set",
    description: "Run //set with the given FAWE/WorldEdit block pattern.",
    inputSchema: {
      type: "object",
      properties: { pattern: { type: "string", description: "Example: stone, oak_planks, 50%stone,50%andesite" } },
      required: ["pattern"],
      additionalProperties: false,
    },
  },
  {
    name: "fawe_walls",
    description: "Run //walls with the given FAWE/WorldEdit block pattern.",
    inputSchema: {
      type: "object",
      properties: { pattern: { type: "string" } },
      required: ["pattern"],
      additionalProperties: false,
    },
  },
  {
    name: "fawe_replace",
    description: "Run //replace from to for the current FAWE/WorldEdit selection.",
    inputSchema: {
      type: "object",
      properties: {
        from: { type: "string" },
        to: { type: "string" }
      },
      required: ["from", "to"],
      additionalProperties: false,
    },
  },
  {
    name: "fawe_undo",
    description: "Run //undo.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "fawe_redo",
    description: "Run //redo.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "jump",
    description: "Make the live Fabric client player jump once.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "select_hotbar_slot",
    description: "Select a hotbar slot in the live Fabric client. Slots are zero-based, 0 through 8.",
    inputSchema: {
      type: "object",
      properties: { slot: { type: "integer", minimum: 0, maximum: 8 } },
      required: ["slot"],
      additionalProperties: false,
    },
  },
  {
    name: "get_block",
    description: "Get the block and block state at a world position.",
    inputSchema: {
      type: "object",
      properties: {
        x: { type: "integer" },
        y: { type: "integer" },
        z: { type: "integer" },
      },
      required: ["x", "y", "z"],
      additionalProperties: false,
    },
  },
  {
    name: "place_block",
    description: "Place the currently selected hotbar item at a target air block using an adjacent reference block. Defaults to placing on top of the block below.",
    inputSchema: {
      type: "object",
      properties: {
        x: { type: "integer" },
        y: { type: "integer" },
        z: { type: "integer" },
        face: { type: "string", enum: ["up", "down", "north", "south", "east", "west"] }
      },
      required: ["x", "y", "z"],
      additionalProperties: false,
    },
  },
  {
    name: "get_inventory",
    description: "List non-empty slots in the live Fabric client player's inventory.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "get_container",
    description: "List the currently open container menu id, implementation class, and slots.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "click_slot",
    description: "Click a slot in the currently open container or inventory menu. Use type PICKUP for normal left/right pickup. Slot -999 clicks outside.",
    inputSchema: {
      type: "object",
      properties: {
        slot: { type: "integer" },
        button: { type: "integer", minimum: 0, maximum: 8 },
        type: {
          type: "string",
          enum: ["pickup", "quick_move", "swap", "clone", "throw", "quick_craft", "pickup_all"]
        }
      },
      required: ["slot"],
      additionalProperties: false,
    },
  },
  {
    name: "container_click",
    description: "Send a container click through Minecraft's multiplayer game mode. This mirrors normal client slot-click packet behavior for the open menu.",
    inputSchema: {
      type: "object",
      properties: {
        slot: { type: "integer" },
        button: { type: "integer", minimum: 0, maximum: 8 },
        type: {
          type: "string",
          enum: ["pickup", "quick_move", "swap", "clone", "throw", "quick_craft", "pickup_all"]
        }
      },
      required: ["slot"],
      additionalProperties: false,
    },
  },
  {
    name: "click_slot_by_item",
    description: "Find a slot in the open container by item id or display name and click it. Useful for plugin GUIs.",
    inputSchema: {
      type: "object",
      properties: {
        query: { type: "string", description: "Matches item id, hover name, or display name." },
        item: { type: "string", description: "Matches item id, for example minecraft:oak_planks." },
        displayName: { type: "string", description: "Matches the rendered item display or hover name." },
        contains: { type: "boolean", description: "Use substring matching. Defaults to true." },
        nth: { type: "integer", minimum: 0, description: "Click the nth match, zero-based. Defaults to 0." },
        button: { type: "integer", minimum: 0, maximum: 8 },
        type: {
          type: "string",
          enum: ["pickup", "quick_move", "swap", "clone", "throw", "quick_craft", "pickup_all"]
        }
      },
      additionalProperties: false,
    },
  },
  {
    name: "container_button",
    description: "Send a container button click packet for the current or specified container id.",
    inputSchema: {
      type: "object",
      properties: {
        button: { type: "integer" },
        containerId: { type: "integer" }
      },
      required: ["button"],
      additionalProperties: false,
    },
  },
  {
    name: "baritone_status",
    description: "Check whether Baritone is loaded in the live Fabric client and report the command prefix.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "baritone_command",
    description: "Send a Baritone chat command from the live Fabric client. Prefix # is added automatically when omitted.",
    inputSchema: {
      type: "object",
      properties: { command: { type: "string" } },
      required: ["command"],
      additionalProperties: false,
    },
  },
  {
    name: "baritone_goto",
    description: "Ask Baritone to pathfind to a target coordinate. If y is omitted, Baritone chooses terrain height.",
    inputSchema: {
      type: "object",
      properties: {
        x: { type: "integer" },
        y: { type: "integer" },
        z: { type: "integer" },
      },
      required: ["x", "z"],
      additionalProperties: false,
    },
  },
];

let buffer = "";
let nextLogId = 1;
let transportMode = null;

process.stdin.setEncoding("utf8");

process.stdin.on("data", (chunk) => {
  buffer += chunk;
  drainBuffer();
});

function drainBuffer() {
  while (buffer.length > 0) {
    if (/^Content-Length:/i.test(buffer)) {
      transportMode = "headers";

      const headerEnd = buffer.indexOf("\r\n\r\n");
      if (headerEnd === -1) return;

      const header = buffer.slice(0, headerEnd);
      const match = header.match(/Content-Length:\s*(\d+)/i);

      if (!match) {
        buffer = "";
        return;
      }

      const length = Number(match[1]);
      const bodyStart = headerEnd + 4;

      if (buffer.length < bodyStart + length) return;

      const body = buffer.slice(bodyStart, bodyStart + length);
      buffer = buffer.slice(bodyStart + length);

      handlePayload(body);
      continue;
    }

    const newline = buffer.indexOf("\n");
    if (newline === -1) return;

    transportMode ||= "newline";

    const line = buffer.slice(0, newline).trim();
    buffer = buffer.slice(newline + 1);

    if (line.length > 0) {
      handlePayload(line);
    }
  }
}

async function handlePayload(payload) {
  let message;

  try {
    message = JSON.parse(payload);
  } catch (error) {
    write({
      jsonrpc: "2.0",
      id: nextLogId++,
      error: {
        code: -32700,
        message: error.message,
      },
    });
    return;
  }

  // JSON-RPC request IDs may legally be 0.
  // MCP clients commonly use id: 0 for initialize.
  // Only ignore true notifications where the id property is absent.
  if (!Object.prototype.hasOwnProperty.call(message, "id")) {
    return;
  }

  try {
    const result = await dispatch(message.method, message.params || {});
    write({
      jsonrpc: "2.0",
      id: message.id,
      result,
    });
  } catch (error) {
    write({
      jsonrpc: "2.0",
      id: message.id,
      error: {
        code: -32000,
        message: error.message || String(error),
      },
    });
  }
}

async function dispatch(method, params) {
  switch (method) {
    case "initialize":
      return {
        protocolVersion: params.protocolVersion || "2024-11-05",
        capabilities: {
          tools: {
            listChanged: false,
          },
        },
        serverInfo: {
          name: "mineagent-fabric-mcp",
          version: "0.1.0",
        },
      };

    case "tools/list":
      return { tools };

    case "tools/call":
      return callTool(params.name, params.arguments || {});

    case "ping":
      return {};

    case "resources/list":
      return { resources: [] };

    case "prompts/list":
      return { prompts: [] };

    default:
      throw new Error(`Unsupported method: ${method}`);
  }
}

async function callTool(name, args) {
  if (!tools.some((tool) => tool.name === name)) {
    throw new Error(`Unknown tool: ${name}`);
  }

  const response = await fetch(`${BRIDGE_URL}/tool`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify({
      name,
      arguments: args,
    }),
  });

  const text = await response.text();

  let payload;
  try {
    payload = JSON.parse(text);
  } catch {
    payload = {
      ok: false,
      error: text,
    };
  }

  if (!response.ok || payload.ok === false) {
    throw new Error(payload.error || `MineAgent bridge returned HTTP ${response.status}`);
  }

  return {
    content: [
      {
        type: "text",
        text: JSON.stringify(payload, null, 2),
      },
    ],
  };
}

function write(message) {
  const body = JSON.stringify(message);

  if (transportMode === "headers") {
    process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
  } else {
    process.stdout.write(`${body}\n`);
  }
}