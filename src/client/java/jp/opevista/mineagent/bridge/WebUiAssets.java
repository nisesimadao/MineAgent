package jp.opevista.mineagent.bridge;

final class WebUiAssets {
    private WebUiAssets() {
    }

    static String html() {
        return """
                <!doctype html>
                <html lang="ja">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>MineAgent Control</title>
                  <style>
                    :root {
                      color-scheme: dark;
                      --bg: #111315;
                      --panel: #181b1e;
                      --panel-2: #202429;
                      --line: #353b42;
                      --text: #eef2f4;
                      --muted: #9aa5ad;
                      --accent: #4fd08a;
                      --danger: #ff5d5d;
                      --warn: #e2b84f;
                      --focus: #69a7ff;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      background: var(--bg);
                      color: var(--text);
                      font: 14px/1.45 "Cascadia Code", "Yu Gothic UI", Consolas, monospace;
                    }
                    header {
                      position: sticky;
                      top: 0;
                      z-index: 2;
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      gap: 16px;
                      padding: 14px 18px;
                      border-bottom: 1px solid var(--line);
                      background: rgba(17, 19, 21, 0.94);
                      backdrop-filter: blur(8px);
                    }
                    h1 { margin: 0; font-size: 18px; letter-spacing: 0; }
                    main {
                      display: grid;
                      grid-template-columns: minmax(320px, 420px) minmax(360px, 1fr);
                      gap: 14px;
                      padding: 14px;
                    }
                    section {
                      border: 1px solid var(--line);
                      background: var(--panel);
                      border-radius: 8px;
                      overflow: hidden;
                    }
                    .section-head {
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      gap: 10px;
                      padding: 10px 12px;
                      border-bottom: 1px solid var(--line);
                      background: var(--panel-2);
                    }
                    h2 { margin: 0; font-size: 13px; font-weight: 700; }
                    .body { padding: 12px; }
                    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
                    .row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
                    label { display: grid; gap: 5px; color: var(--muted); font-size: 12px; }
                    input, select, textarea, button {
                      min-height: 34px;
                      border: 1px solid var(--line);
                      border-radius: 6px;
                      background: #101214;
                      color: var(--text);
                      font: inherit;
                    }
                    input, select, textarea { width: 100%; padding: 7px 9px; }
                    textarea { resize: vertical; min-height: 92px; }
                    input[type="checkbox"] { width: 18px; min-height: 18px; accent-color: var(--accent); }
                    input[type="range"] { padding: 0; accent-color: var(--accent); }
                    button {
                      cursor: pointer;
                      padding: 7px 10px;
                      background: #232a2f;
                      transition: border-color .12s ease, transform .12s ease, background .12s ease;
                    }
                    button:hover { border-color: var(--focus); background: #2b333a; }
                    button:active { transform: translateY(1px); }
                    .primary { border-color: #38865e; background: #173626; }
                    .danger { border-color: #804040; background: #391c1c; }
                    .warn { border-color: #786233; background: #332812; }
                    .pill {
                      display: inline-flex;
                      align-items: center;
                      gap: 7px;
                      min-height: 28px;
                      padding: 4px 9px;
                      border: 1px solid var(--line);
                      border-radius: 999px;
                      color: var(--muted);
                      background: #121518;
                    }
                    .dot { width: 8px; height: 8px; border-radius: 999px; background: var(--danger); }
                    .dot.on { background: var(--accent); }
                    pre {
                      margin: 0;
                      max-height: 360px;
                      overflow: auto;
                      white-space: pre-wrap;
                      word-break: break-word;
                      color: #d7dee3;
                    }
                    .log {
                      min-height: 170px;
                      padding: 10px;
                      border: 1px solid var(--line);
                      border-radius: 6px;
                      background: #0d0f11;
                    }
                    .stack { display: grid; gap: 10px; }
                    .checkbox-line { display: flex; align-items: center; gap: 8px; color: var(--text); }
                    .span-2 { grid-column: 1 / -1; }
                    .range-value { color: var(--text); }
                    @media (max-width: 900px) {
                      main { grid-template-columns: 1fr; }
                      .grid { grid-template-columns: 1fr; }
                    }
                  </style>
                </head>
                <body>
                  <header>
                    <h1>MineAgent Control</h1>
                    <div class="row">
                      <span class="pill"><span id="healthDot" class="dot"></span><span id="healthText">checking</span></span>
                      <button onclick="refreshAll()">Refresh</button>
                      <button class="danger" onclick="tool('stop_all')">Stop All</button>
                    </div>
                  </header>
                  <main>
                    <div class="stack">
                      <section>
                        <div class="section-head">
                          <h2>Combat</h2>
                          <span class="pill"><span id="combatDot" class="dot"></span><span id="combatText">idle</span></span>
                        </div>
                        <div class="body stack">
                          <div class="grid">
                            <label>Target name
                              <input id="targetName" placeholder="Zombie / player name">
                            </label>
                            <label>Target entity id
                              <input id="targetEntityId" type="number" min="0" placeholder="optional">
                            </label>
                            <label class="span-2">Range <span class="range-value" id="rangeValue">4.2</span>
                              <input id="range" type="range" min="2" max="6" step="0.1" value="4.2" oninput="rangeValue.textContent=this.value">
                            </label>
                          </div>
                          <div class="grid">
                            <label class="checkbox-line"><input id="includePlayers" type="checkbox"> Include players</label>
                            <label class="checkbox-line"><input id="bunnyHop" type="checkbox" checked> Bunny hop</label>
                            <label class="checkbox-line"><input id="sprintTap" type="checkbox" checked> Sprint tap</label>
                            <label class="checkbox-line"><input id="autoSelectSword" type="checkbox" checked> Auto sword</label>
                          </div>
                          <div class="row">
                            <button class="primary" onclick="combatSet(true)">Combat ON</button>
                            <button onclick="combatSet(false)">Apply OFF</button>
                            <button class="danger" onclick="tool('combat_stop')">Combat Stop</button>
                            <button onclick="tool('combat_status')">Status</button>
                          </div>
                        </div>
                      </section>

                      <section>
                        <div class="section-head"><h2>Quick Tools</h2></div>
                        <div class="body stack">
                          <div class="grid">
                            <button onclick="tool('get_status')">Player Status</button>
                            <button onclick="tool('get_inventory')">Inventory</button>
                            <button onclick="tool('get_container')">Container</button>
                            <button onclick="tool('read_chat', { count: 30 })">Read Chat</button>
                            <button onclick="tool('get_packet_log', { count: 30 })">Packet Log</button>
                            <button onclick="tool('get_screenshot')">Screenshot</button>
                          </div>
                        </div>
                      </section>

                      <section>
                        <div class="section-head"><h2>Movement / Key</h2></div>
                        <div class="body stack">
                          <div class="grid">
                            <label>Key
                              <select id="keyName">
                                <option>jump</option><option>attack</option><option>use</option><option>sneak</option><option>sprint</option><option>inventory</option><option>forward</option><option>back</option><option>left</option><option>right</option>
                              </select>
                            </label>
                            <label>Action
                              <select id="keyAction"><option>pulse</option><option>down</option><option>up</option></select>
                            </label>
                            <label>Ticks
                              <input id="keyTicks" type="number" min="1" max="100" value="2">
                            </label>
                          </div>
                          <div class="row">
                            <button onclick="sendKey()">Send Key</button>
                            <button onclick="tool('release_all_keys')">Release Keys</button>
                            <button onclick="tool('jump')">Jump</button>
                          </div>
                        </div>
                      </section>
                    </div>

                    <div class="stack">
                      <section>
                        <div class="section-head"><h2>Tool Runner</h2></div>
                        <div class="body stack">
                          <div class="grid">
                            <label>Chat
                              <input id="chatMessage" placeholder="message">
                            </label>
                            <label>Command
                              <input id="commandText" placeholder="time set day">
                            </label>
                            <label>Baritone
                              <input id="baritoneCommand" placeholder="goto 0 64 0 / mine oak_log">
                            </label>
                            <label>FAWE
                              <input id="faweCommand" placeholder="//set stone">
                            </label>
                            <label>Click GUI item
                              <input id="slotQuery" placeholder="display name or minecraft:item">
                            </label>
                            <label>Button
                              <select id="slotButton"><option value="0">Left</option><option value="1">Right</option></select>
                            </label>
                          </div>
                          <div class="row">
                            <button onclick="sendChat()">Send Chat</button>
                            <button onclick="runCommand()">Run Command</button>
                            <button onclick="baritone()">Baritone</button>
                            <button onclick="fawe()">FAWE</button>
                            <button onclick="clickByItem()">Click GUI Item</button>
                          </div>
                        </div>
                      </section>

                      <section>
                        <div class="section-head"><h2>Remote Sign</h2></div>
                        <div class="body stack">
                          <div class="grid">
                            <label>X
                              <input id="signX" type="number" value="0">
                            </label>
                            <label>Y
                              <input id="signY" type="number" value="64">
                            </label>
                            <label>Z
                              <input id="signZ" type="number" value="0">
                            </label>
                            <label>Material
                              <input id="signMaterial" value="oak" placeholder="oak / spruce / minecraft:cherry_sign">
                            </label>
                            <label class="span-2">Text
                              <textarea id="signText" rows="4" placeholder="Line 1&#10;Line 2&#10;Line 3&#10;Line 4"></textarea>
                            </label>
                            <label>Facing
                              <select id="signFacing"><option>south</option><option>north</option><option>east</option><option>west</option></select>
                            </label>
                            <label>Rotation
                              <input id="signRotation" type="number" min="0" max="15" value="0">
                            </label>
                            <label>Color
                              <select id="signColor"><option>black</option><option>white</option><option>red</option><option>blue</option><option>green</option><option>yellow</option><option>gold</option></select>
                            </label>
                          </div>
                          <div class="row">
                            <label class="checkbox-line"><input id="signWall" type="checkbox"> Wall sign</label>
                            <label class="checkbox-line"><input id="signGlowing" type="checkbox"> Glowing</label>
                            <button class="primary" onclick="placeSign()">Place Sign</button>
                          </div>
                        </div>
                      </section>

                      <section>
                        <div class="section-head">
                          <h2>Output</h2>
                          <button onclick="output.textContent=''">Clear</button>
                        </div>
                        <div class="body">
                          <pre id="output" class="log"></pre>
                        </div>
                      </section>

                      <section>
                        <div class="section-head"><h2>Live Snapshot</h2></div>
                        <div class="body">
                          <pre id="snapshot" class="log"></pre>
                        </div>
                      </section>
                    </div>
                  </main>

                  <script>
                    const output = document.getElementById('output');
                    const snapshot = document.getElementById('snapshot');
                    const healthDot = document.getElementById('healthDot');
                    const healthText = document.getElementById('healthText');
                    const combatDot = document.getElementById('combatDot');
                    const combatText = document.getElementById('combatText');

                    async function postJson(url, payload) {
                      const response = await fetch(url, {
                        method: 'POST',
                        headers: { 'content-type': 'application/json' },
                        body: JSON.stringify(payload)
                      });
                      const text = await response.text();
                      try { return JSON.parse(text); } catch { return { ok: false, error: text }; }
                    }

                    async function tool(name, args = {}) {
                      const result = await postJson('/tool', { name, arguments: args });
                      print(name, result);
                      if (name.startsWith('combat_') || name === 'stop_all') await refreshCombat();
                      return result;
                    }

                    function print(label, value) {
                      output.textContent = `[${new Date().toLocaleTimeString()}] ${label}\\n${JSON.stringify(value, null, 2)}\\n\\n` + output.textContent;
                    }

                    function combatPayload(enabled) {
                      const entityId = targetEntityId.value.trim();
                      const payload = {
                        enabled,
                        targetName: targetName.value.trim(),
                        range: Number(range.value),
                        includePlayers: includePlayers.checked,
                        bunnyHop: bunnyHop.checked,
                        sprintTap: sprintTap.checked,
                        autoSelectSword: autoSelectSword.checked
                      };
                      if (entityId) payload.targetEntityId = Number(entityId);
                      return payload;
                    }

                    async function combatSet(enabled) { await tool('combat_set', combatPayload(enabled)); }
                    async function sendKey() { await tool('key_signal', { key: keyName.value, action: keyAction.value, ticks: Number(keyTicks.value) }); }
                    async function sendChat() { if (chatMessage.value.trim()) await tool('send_chat', { message: chatMessage.value.trim() }); }
                    async function runCommand() { if (commandText.value.trim()) await tool('run_command', { command: commandText.value.trim() }); }
                    async function baritone() { if (baritoneCommand.value.trim()) await tool('baritone_command', { command: baritoneCommand.value.trim() }); }
                    async function fawe() { if (faweCommand.value.trim()) await tool('fawe_command', { command: faweCommand.value.trim(), waitForFeedback: true }); }
                    async function clickByItem() {
                      if (slotQuery.value.trim()) await tool('click_slot_by_item', {
                        query: slotQuery.value.trim(),
                        button: Number(slotButton.value),
                        type: 'pickup'
                      });
                    }
                    async function placeSign() {
                      await tool('place_sign', {
                        x: Number(signX.value),
                        y: Number(signY.value),
                        z: Number(signZ.value),
                        text: signText.value,
                        material: signMaterial.value.trim() || 'oak',
                        wall: signWall.checked,
                        facing: signFacing.value,
                        rotation: Number(signRotation.value),
                        color: signColor.value,
                        glowing: signGlowing.checked
                      });
                    }

                    async function refreshHealth() {
                      try {
                        const response = await fetch('/health');
                        const data = await response.json();
                        healthDot.classList.toggle('on', !!data.ok);
                        healthText.textContent = data.ok ? `online :${data.port}` : 'offline';
                      } catch {
                        healthDot.classList.remove('on');
                        healthText.textContent = 'offline';
                      }
                    }

                    async function refreshCombat() {
                      const result = await postJson('/tool', { name: 'combat_status', arguments: {} });
                      const data = result.data || {};
                      combatDot.classList.toggle('on', !!data.enabled);
                      combatText.textContent = data.enabled ? 'enabled' : 'idle';
                      return result;
                    }

                    async function refreshAll() {
                      await refreshHealth();
                      const status = await postJson('/tool', { name: 'get_status', arguments: {} });
                      const combat = await refreshCombat();
                      snapshot.textContent = JSON.stringify({ status, combat }, null, 2);
                    }

                    refreshAll();
                    setInterval(refreshAll, 2500);
                  </script>
                </body>
                </html>
                """;
    }
}
