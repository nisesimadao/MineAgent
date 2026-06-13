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
    description: "Send a FAWE/WorldEdit command (e.g., //set stone, //pos1). Optionally waits for feedback by checking logs.",
    inputSchema: {
      type: "object",
      properties: {
        command: { type: "string" },
        waitForFeedback: { type: "boolean", description: "If true, advises to check read_chat for result." }
      },
      required: ["command"],
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
    description: "List non-empty slots in the live Fabric client player's inventory. Returns a snapshot for polling.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "wait_for_inventory",
    description: "Poll inventory, returns inventory content with a snapshot timestamp.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "get_container",
    description: "List the currently open container menu id, implementation class, slots, and title metadata.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "wait_for_container",
    description: "Poll open container menu, returns slots and title with a snapshot timestamp.",
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
    name: "craft_item",
    description: "Perform crafting in the open crafting menu (supports planks, crafting_table, sticks, wooden_pickaxe, wooden_axe, wooden_shovel).",
    inputSchema: {
      type: "object",
      properties: {
        recipe: {
          type: "string",
          enum: ["planks", "crafting_table", "sticks", "wooden_pickaxe", "wooden_axe", "wooden_shovel"],
          description: "The recipe name to craft."
        },
        count: {
          type: "integer",
          minimum: 1,
          description: "Number of times to craft. Defaults to 1."
        }
      },
      required: ["recipe"],
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
    description: "Check whether Baritone is loaded in the live Fabric client, command prefix, and detailed task status.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "get_packet_log",
    description: "Get the recent network packet logs (inbound/outbound).",
    inputSchema: {
      type: "object",
      properties: {
        count: { type: "integer", minimum: 1, maximum: 100 }
      },
      additionalProperties: false,
    },
  },
  {
    name: "get_screenshot",
    description: "Take a screenshot of the client and save to a temporary file, returning the file path.",
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
  {
    name: "run_survival_macro",
    description: "Start the survival workflow macro: gather wood -> craft planks/table/tools -> build a simple shelter.",
    inputSchema: {
      type: "object",
      properties: {
        targetLogs: { type: "integer", minimum: 1, description: "Number of logs to gather. Default is 6." },
        maxWanderDistance: { type: "integer", minimum: 10, description: "Max distance from start position before forcing return. Default is 40." }
      },
      additionalProperties: false,
    },
  },
  {
    name: "survival_macro_status",
    description: "Get the status of the currently running survival macro.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
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
  if (name === "run_survival_macro") {
    if (macroState.active) {
      return { content: [{ type: "text", text: JSON.stringify({ ok: false, error: "Survival macro is already running" }) }] };
    }
    macroState.active = true;
    macroState.step = "GATHERING_WOOD";
    macroState.message = "Macro started. Gathering wood.";
    macroState.logsGathered = 0;
    macroState.targetLogs = args.targetLogs || 6;
    macroState.maxWanderDistance = args.maxWanderDistance || 40;
    macroState.startPos = null;
    macroState.error = null;
    
    // Start macro loop asynchronously
    runMacroLoop();
    
    return { content: [{ type: "text", text: JSON.stringify({ ok: true, message: "Survival macro started successfully", state: macroState }) }] };
  }
  
  if (name === "survival_macro_status") {
    return { content: [{ type: "text", text: JSON.stringify({ ok: true, state: macroState }) }] };
  }

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

let macroState = {
  active: false,
  step: "IDLE",
  message: "",
  logsGathered: 0,
  targetLogs: 6,
  startPos: null,
  maxWanderDistance: 40,
  error: null
};

async function callBridge(name, args) {
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
    payload = { ok: false, error: text };
  }

  if (!response.ok || payload.ok === false) {
    throw new Error(payload.error || `Bridge returned HTTP ${response.status}`);
  }
  return payload.data;
}

async function runMacroLoop() {
  while (macroState.active) {
    try {
      await tickMacro();
    } catch (err) {
      macroState.active = false;
      macroState.error = err.message || String(err);
      macroState.message = `Macro failed: ${macroState.error}`;
      try {
        await callBridge("stop_all", {});
      } catch (_) {}
    }
    await new Promise((resolve) => setTimeout(resolve, 1500));
  }
}

async function prepareItemInHand(itemName) {
  const inv = await callBridge("get_inventory", {});
  if (!inv || !inv.items) return false;
  
  let hotbarSlot = -1;
  let inventorySlot = -1;
  
  for (const item of inv.items) {
    if (item.item === itemName || item.item.endsWith(itemName)) {
      if (item.slot >= 36 && item.slot <= 44) {
        hotbarSlot = item.slot - 36;
      } else {
        inventorySlot = item.slot;
      }
      break;
    }
  }
  
  if (hotbarSlot !== -1) {
    await callBridge("select_hotbar_slot", { slot: hotbarSlot });
    return true;
  }
  
  if (inventorySlot !== -1) {
    await callBridge("click_slot", { slot: inventorySlot, button: 0, type: "pickup" });
    await callBridge("click_slot", { slot: 36, button: 0, type: "pickup" });
    await callBridge("click_slot", { slot: inventorySlot, button: 0, type: "pickup" });
    await callBridge("select_hotbar_slot", { slot: 0 });
    return true;
  }
  
  return false;
}

async function tickMacro() {
  const status = await callBridge("get_status", {});
  if (!status || status.message === "client player is not in-world" || status.x === undefined) {
    macroState.message = "Waiting for player to join world...";
    return;
  }

  const inv = await callBridge("get_inventory", {});

  if (macroState.step === "GATHERING_WOOD") {
    if (!macroState.startPos) {
      macroState.startPos = { x: status.x, y: status.y, z: status.z };
      macroState.message = "Starting Baritone mining for wood...";
      await callBridge("baritone_command", { command: "mine oak_log birch_log spruce_log jungle_log acacia_log dark_oak_log mangrove_log cherry_log pale_oak_log" });
    }

    let logCount = 0;
    if (inv && inv.items) {
      for (const item of inv.items) {
        if (item.item.endsWith("_log") || item.item.endsWith("_wood") || item.item.endsWith("_stem")) {
          logCount += item.count;
        }
      }
    }
    macroState.logsGathered = logCount;
    macroState.message = `Gathering wood: ${logCount}/${macroState.targetLogs} logs collected.`;

    const dx = status.x - macroState.startPos.x;
    const dy = status.y - macroState.startPos.y;
    const dz = status.z - macroState.startPos.z;
    const dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
    if (dist > macroState.maxWanderDistance) {
      macroState.message = `Safety: player wandered ${Math.round(dist)}m (max ${macroState.maxWanderDistance}m). Stopping wood gather and returning to start.`;
      await callBridge("stop_all", {});
      await callBridge("baritone_goto", { x: Math.floor(macroState.startPos.x), z: Math.floor(macroState.startPos.z) });
      
      if (logCount >= 1) {
        macroState.step = "CRAFTING_PLANKS";
      } else {
        throw new Error("Wandered too far without gathering any wood");
      }
      return;
    }

    if (logCount >= macroState.targetLogs) {
      await callBridge("stop_all", {});
      macroState.step = "CRAFTING_PLANKS";
      macroState.message = `Wood gathering completed (${logCount} logs). Moving to plank crafting.`;
    }
  }

  else if (macroState.step === "CRAFTING_PLANKS") {
    let logItem = null;
    if (inv && inv.items) {
      for (const item of inv.items) {
        if (item.item.endsWith("_log") || item.item.endsWith("_wood") || item.item.endsWith("_stem")) {
          logItem = item;
          break;
        }
      }
    }

    if (logItem) {
      macroState.message = `Crafting planks from ${logItem.item} (count: ${logItem.count})...`;
      await callBridge("craft_item", { recipe: "planks", count: logItem.count });
      await new Promise(r => setTimeout(r, 500));
    }
    
    macroState.step = "CRAFTING_TABLE";
    macroState.message = "Plank crafting completed. Moving to crafting table crafting.";
  }

  else if (macroState.step === "CRAFTING_TABLE") {
    const hasCraftingTable = inv && inv.items && inv.items.some(item => item.item === "minecraft:crafting_table");
    if (hasCraftingTable) {
      macroState.step = "CRAFTING_TOOLS";
      macroState.message = "Crafting table already exists. Moving to crafting tools.";
      return;
    }

    let plankCount = 0;
    if (inv && inv.items) {
      for (const item of inv.items) {
        if (item.item.endsWith("_planks")) {
          plankCount += item.count;
        }
      }
    }

    if (plankCount < 4) {
      throw new Error(`Not enough planks to craft a crafting table (need 4, have ${plankCount}).`);
    }

    macroState.message = "Crafting crafting table...";
    await callBridge("craft_item", { recipe: "crafting_table", count: 1 });
    await new Promise(r => setTimeout(r, 500));
    macroState.step = "CRAFTING_TOOLS";
  }

  else if (macroState.step === "CRAFTING_TOOLS") {
    let stickCount = 0;
    if (inv && inv.items) {
      for (const item of inv.items) {
        if (item.item === "minecraft:stick") {
          stickCount += item.count;
        }
      }
    }

    if (stickCount < 4) {
      macroState.message = "Crafting sticks...";
      await callBridge("craft_item", { recipe: "sticks", count: 1 });
      await new Promise(r => setTimeout(r, 500));
    }

    macroState.message = "Placing crafting table...";
    const success = await prepareItemInHand("minecraft:crafting_table");
    if (!success) {
      throw new Error("Could not find crafting table in inventory to place.");
    }

    const px = Math.floor(status.x);
    const py = Math.floor(status.y);
    const pz = Math.floor(status.z);

    let targetX = px + 1;
    let targetY = py;
    let targetZ = pz;
    let targetBlock = await callBridge("get_block", { x: targetX, y: targetY, z: targetZ });
    
    if (!targetBlock.block.includes("air")) {
      targetX = px;
      targetZ = pz + 1;
      targetBlock = await callBridge("get_block", { x: targetX, y: targetY, z: targetZ });
    }

    if (!targetBlock.block.includes("air")) {
      throw new Error("No clear air space to place crafting table near player.");
    }

    await callBridge("place_block", { x: targetX, y: targetY, z: targetZ, face: "up" });
    await new Promise(r => setTimeout(r, 800));

    macroState.message = "Opening crafting table...";
    await callBridge("look_at", { x: targetX + 0.5, y: targetY + 0.5, z: targetZ + 0.5 });
    await callBridge("key_signal", { key: "use", action: "pulse", ticks: 2 });
    await new Promise(r => setTimeout(r, 1000));

    const container = await callBridge("get_container", {});
    if (!container.title.toLowerCase().includes("crafting")) {
      throw new Error("Failed to open crafting table interface.");
    }

    macroState.message = "Crafting wooden pickaxe and axe...";
    await callBridge("craft_item", { recipe: "wooden_pickaxe", count: 1 });
    await new Promise(r => setTimeout(r, 500));
    await callBridge("craft_item", { recipe: "wooden_axe", count: 1 });
    await new Promise(r => setTimeout(r, 500));

    await callBridge("key_signal", { key: "inventory", action: "pulse", ticks: 2 });
    await new Promise(r => setTimeout(r, 500));

    macroState.step = "BUILDING_SHELTER";
  }

  else if (macroState.step === "BUILDING_SHELTER") {
    macroState.message = "Building shelter...";
    const px = Math.floor(status.x);
    const py = Math.floor(status.y);
    const pz = Math.floor(status.z);

    let plankItemName = null;
    if (inv && inv.items) {
      for (const item of inv.items) {
        if (item.item.endsWith("_planks")) {
          plankItemName = item.item;
          break;
        }
      }
    }

    if (!plankItemName) {
      let logItem = null;
      if (inv && inv.items) {
        for (const item of inv.items) {
          if (item.item.endsWith("_log") || item.item.endsWith("_wood") || item.item.endsWith("_stem")) {
            logItem = item;
            break;
          }
        }
      }
      if (logItem) {
        await callBridge("craft_item", { recipe: "planks", count: logItem.count });
        await new Promise(r => setTimeout(r, 500));
        return;
      }
      throw new Error("No planks or logs found to build shelter.");
    }

    await prepareItemInHand(plankItemName);

    for (let dx = -1; dx <= 1; dx++) {
      for (let dz = -1; dz <= 1; dz++) {
        if (dx === 0 && dz === 0) continue;
        const tx = px + dx;
        const ty = py;
        const tz = pz + dz;
        const block = await callBridge("get_block", { x: tx, y: ty, z: tz });
        if (block.block.includes("air")) {
          await callBridge("place_block", { x: tx, y: ty, z: tz, face: "up" });
          await new Promise(r => setTimeout(r, 250));
        }
      }
    }

    for (let dx = -1; dx <= 1; dx++) {
      for (let dz = -1; dz <= 1; dz++) {
        if (dx === 0 && dz === 0) continue;
        const tx = px + dx;
        const ty = py + 1;
        const tz = pz + dz;
        const block = await callBridge("get_block", { x: tx, y: ty, z: tz });
        if (block.block.includes("air")) {
          await callBridge("place_block", { x: tx, y: ty, z: tz, face: "up" });
          await new Promise(r => setTimeout(r, 250));
        }
      }
    }

    for (let dx = -1; dx <= 1; dx++) {
      for (let dz = -1; dz <= 1; dz++) {
        const tx = px + dx;
        const ty = py + 2;
        const tz = pz + dz;
        const block = await callBridge("get_block", { x: tx, y: ty, z: tz });
        if (block.block.includes("air")) {
          await callBridge("place_block", { x: tx, y: ty, z: tz, face: "up" });
          await new Promise(r => setTimeout(r, 250));
        }
      }
    }

    macroState.active = false;
    macroState.step = "COMPLETED";
    macroState.message = "Shelter built. Survival macro completed successfully!";
  }
}