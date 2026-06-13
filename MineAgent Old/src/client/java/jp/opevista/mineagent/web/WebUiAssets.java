package jp.opevista.mineagent.web;

public final class WebUiAssets {
    private WebUiAssets() {
    }

    public static String html() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>MineAgent</title>
                  <style>
                    body{font-family:system-ui,Segoe UI,sans-serif;background:#111;color:#eee;margin:24px;line-height:1.4}
                    main{max-width:1100px;margin:auto}
                    section{border-top:1px solid #333;padding:16px 0}
                    textarea,input,select,button{font:inherit;border:1px solid #555;background:#181818;color:#eee;padding:8px}
                    textarea{width:100%;min-height:82px;box-sizing:border-box}
                    button{cursor:pointer;margin:4px 4px 4px 0;background:#2a5}
                    pre{background:#181818;border:1px solid #333;padding:12px;overflow:auto;max-height:340px}
                    .row{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
                  </style>
                </head>
                <body>
                <main>
                  <h1>MineAgent</h1>
                  <section>
                    <h2>Status</h2>
                    <button onclick="refresh()">Refresh</button>
                    <pre id="status"></pre>
                  </section>
                  <section>
                    <h2>Instruction</h2>
                    <textarea id="instruction" placeholder="status"></textarea>
                    <div class="row"><button onclick="instruct()">Send</button><button onclick="stop()">Stop</button></div>
                  </section>
                  <section>
                    <h2>Logs</h2>
                    <pre id="logs"></pre>
                  </section>
                  <section>
                    <h2>Packet Console</h2>
                    <div class="row">
                      <select id="direction"><option>BOTH</option><option>C2S</option><option>S2C</option></select>
                      <input id="filter" placeholder="filter">
                      <button onclick="packets()">Refresh</button>
                    </div>
                    <pre id="packets"></pre>
                  </section>
                  <section>
                    <h2>Send Wrapped Packet</h2>
                    <select id="packetType"><option>chat_message</option><option>command</option></select>
                    <textarea id="packetArgs">{"message":"hello"}</textarea>
                    <button onclick="sendPacket()">Send Packet</button>
                  </section>
                  <section>
                    <h2>CustomPayload</h2>
                    <input id="channel" value="mineagent:agent_to_server">
                    <textarea id="payload">{"type":"status","message":"hello"}</textarea>
                    <button onclick="sendPayload()">Send CustomPayload</button>
                  </section>
                </main>
                <script>
                  async function j(url, options){const r=await fetch(url, options); return await r.json();}
                  async function refresh(){status.textContent=JSON.stringify(await j('/api/status'), null, 2); logs.textContent=JSON.stringify(await j('/api/logs'), null, 2);}
                  async function instruct(){await j('/api/instruct',{method:'POST',body:JSON.stringify({user:'webui',instruction:instruction.value})}); refresh();}
                  async function stop(){await j('/api/stop',{method:'POST'}); refresh();}
                  async function packets(){packetsEl=await j('/api/packets?direction='+direction.value+'&filter='+encodeURIComponent(filter.value)); document.getElementById('packets').textContent=JSON.stringify(packetsEl,null,2);}
                  async function sendPacket(){await j('/api/packets/send',{method:'POST',body:JSON.stringify({type:packetType.value,args:JSON.parse(packetArgs.value)})}); packets();}
                  async function sendPayload(){await j('/api/custom-payload/send',{method:'POST',body:JSON.stringify({channel:channel.value,payload:JSON.parse(payload.value)})}); packets();}
                  refresh(); packets();
                </script>
                </body>
                </html>
                """;
    }
}
