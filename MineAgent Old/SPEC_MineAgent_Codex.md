# Codex 実装仕様書: MineAgent Fabric

## 0. このドキュメントの目的

この仕様書は、OpenAI Codex にそのまま渡して、Minecraft 26.1.2 Fabric 向けの自作 MOD **MineAgent** を実装させるための開発指示書である。

MineAgent は、チャット・WebUI・MOD内コマンドから自然言語指示を受け取り、LLM が Minecraft 用ツールを選択して、プレイヤー操作・チャット・コマンド・Baritone・インベントリ操作・戦闘・採掘・クラフト・建築・パケット読取/送信を実行するクライアント主体のエージェント MOD である。

前提として、この MOD は別の自作サーバーロック MOD により、自分のサーバー環境でしか使えない。したがって MineAgent 側は公開サーバー対策よりも、自由度・拡張性・デバッグ性を優先する。

---

## 1. Codex への最初の指示

Codex は以下の方針で作業すること。

1. まずリポジトリを確認し、既存の Fabric テンプレート・Gradle・Minecraft/Fabric/Loom のバージョンを尊重する。
2. 既存コードがない場合は、Fabric 26.1.2 対応の最小 MOD 構成を作成する。
3. いきなり全機能を完成させようとせず、**MVP v0.1** をまずビルド可能な状態で実装する。
4. その後、Packet Gateway と WebUI を段階的に追加する。
5. 迷った場合は、追加質問で止まらず、最小で堅牢な実装を選び、`docs/ASSUMPTIONS.md` に仮定を書く。
6. すべての変更後に `./gradlew build` を実行する。
7. Windows 環境も考慮し、パス区切りやシェル依存を避ける。
8. 危険操作の道徳的制限はこの MOD の主目的ではない。ただし、デバッグ用の停止機能・ログ・タスクキャンセルは必ず実装する。
9. LLM のレスポンスは自由文ではなく、原則 JSON の `actions` 配列として扱う。
10. 破綻しやすい Raw Packet は、最初はラップ済み Packet Wrapper を優先する。

---

## 2. プロジェクト概要

### 2.1 名称

- MOD名: `MineAgent`
- mod id: `mineagent`
- Java package: `jp.opevista.mineagent`

### 2.2 対象

- Minecraft: `26.1.2`
- Loader: Fabric
- Fabric API: 対象 Minecraft バージョンに対応する最新版
- Mapping: リポジトリ既存設定を優先。なければ Fabric 26.1.2 標準の設定を使用。
- Java: Minecraft/Fabric/Loom の要求バージョンを優先。既存設定がなければ Gradle toolchain を Fabric テンプレートに合わせる。

### 2.3 実行形態

基本は **クライアント MOD** として動かす。

理由:

- 自分のプレイヤーを操作する
- チャットを読む
- Baritone を呼ぶ
- クライアント側パケット送信/ログを扱う
- ローカル WebUI を開く

ただし、自作サーバー MOD との連携用に CustomPayload の C2S/S2C チャンネルを用意する。

---

## 3. 主要機能

### 3.1 入力経路

MineAgent は次の入力を受け付ける。

1. Minecraft チャット
2. MineAgent WebUI
3. クライアントコマンド
4. 自作サーバー MOD からの CustomPayload
5. 将来: Discord/Webhook/音声入力

---

### 3.2 チャット指示

形式:

```text
<agentName> <instruction>
```

例:

```text
opevista 木を64個集めて拠点に戻って
opevista Steveを追いかけて
opevista 近くの敵を全部倒して
opevista このチェストの中身を確認して
opevista 最近のS2Cパケットを20件見て
```

処理:

1. クライアントが受信したチャットを監視する。
2. `agentName` と一致する接頭辞か確認する。
3. 送信者が `registered_users.json` に登録済みか確認する。
4. 登録済みなら `InstructionRouter` に渡す。
5. 未登録ならログに `ignored_unregistered_user` として記録し、何もしない。

注意:

- 送信者名だけでなく、可能なら UUID も保持する。
- UUID が取得できない場合は名前一致で動作してよい。
- 自分のサーバーロック MOD が存在する前提なので、MineAgent 側で公開サーバー利用を過剰に拒否しない。

---

### 3.3 WebUI 指示

デフォルト:

```text
http://127.0.0.1:25712
```

WebUI で可能にすること:

- 自然言語指示の送信
- 現在タスク表示
- 停止/一時停止/再開
- ログ表示
- LLM の最後のレスポンス表示
- 実行された Tool Call 表示
- Packet Log 表示
- Packet Filter 設定
- CustomPayload 送信
- Wrapped Packet 送信
- Baritone コマンド送信
- Config 簡易表示

MVP では JDK 標準の `com.sun.net.httpserver.HttpServer` を使ってよい。外部依存は最小にする。

---

### 3.4 クライアントコマンド

Fabric の client command として以下を登録する。

```text
/mineagent status
/mineagent ask <instruction>
/mineagent stop
/mineagent pause
/mineagent resume
/mineagent logs
/mineagent web
/mineagent reload
/mineagent users add <name>
/mineagent users remove <name>
/mineagent packets recent [limit]
/mineagent packets clear
/mineagent packets trace <filter> <seconds>
```

`/mineagent web` は WebUI の URL をチャットに表示する。

---

## 4. アーキテクチャ

### 4.1 全体構造

```text
Chat / WebUI / Client Command / CustomPayload
        ↓
InstructionRouter
        ↓
TaskManager
        ↓
AgentLoop
        ↓
LlmClient
        ↓
ToolRegistry
        ├─ Minecraft High-level Tools
        │   ├─ StatusTool
        │   ├─ ChatTool
        │   ├─ CommandTool
        │   ├─ BaritoneTool
        │   ├─ PlayerControlTool
        │   ├─ LookTool
        │   ├─ CombatTool
        │   ├─ GatherTool
        │   ├─ InventoryTool
        │   ├─ ContainerTool
        │   ├─ CraftTool
        │   └─ BuildTool
        │
        └─ Packet Gateway Tools
            ├─ PacketReadRecentTool
            ├─ PacketTraceTool
            ├─ PacketSendWrappedTool
            ├─ PacketSendCustomPayloadTool
            └─ PacketReplayTool
```

---

### 4.2 推奨ディレクトリ

```text
src/main/java/jp/opevista/mineagent/
 ├─ MineAgentMod.java
 ├─ MineAgentClient.java
 │
 ├─ config/
 │   ├─ MineAgentConfig.java
 │   ├─ ConfigManager.java
 │   ├─ UserRegistry.java
 │   └─ RegisteredUser.java
 │
 ├─ chat/
 │   ├─ ChatListener.java
 │   └─ ChatInstructionParser.java
 │
 ├─ command/
 │   └─ MineAgentClientCommands.java
 │
 ├─ web/
 │   ├─ WebServer.java
 │   ├─ WebRoutes.java
 │   ├─ WebResponse.java
 │   └─ WebUiAssets.java
 │
 ├─ llm/
 │   ├─ LlmClient.java
 │   ├─ OpenAiCompatibleClient.java
 │   ├─ LlmRequest.java
 │   ├─ LlmResponse.java
 │   ├─ AgentPromptBuilder.java
 │   ├─ AgentLoop.java
 │   └─ JsonRepair.java
 │
 ├─ task/
 │   ├─ TaskManager.java
 │   ├─ AgentTask.java
 │   ├─ TaskState.java
 │   └─ TaskLog.java
 │
 ├─ tools/
 │   ├─ Tool.java
 │   ├─ ToolCall.java
 │   ├─ ToolResult.java
 │   ├─ ToolRegistry.java
 │   ├─ StatusTool.java
 │   ├─ ChatTool.java
 │   ├─ CommandTool.java
 │   ├─ BaritoneTool.java
 │   ├─ PlayerControlTool.java
 │   ├─ LookTool.java
 │   ├─ CombatTool.java
 │   ├─ GatherTool.java
 │   ├─ InventoryTool.java
 │   ├─ ContainerTool.java
 │   ├─ CraftTool.java
 │   └─ BuildTool.java
 │
 ├─ network/
 │   ├─ PacketGateway.java
 │   ├─ PacketDirection.java
 │   ├─ PacketEvent.java
 │   ├─ PacketLogger.java
 │   ├─ PacketFilter.java
 │   ├─ PacketSummaryExtractor.java
 │   ├─ PacketTraceSession.java
 │   ├─ PacketWrapperSender.java
 │   ├─ CustomPayloadBridge.java
 │   └─ PacketReplayBuffer.java
 │
 ├─ network/tools/
 │   ├─ PacketReadRecentTool.java
 │   ├─ PacketTraceTool.java
 │   ├─ PacketSendWrappedTool.java
 │   ├─ PacketSendCustomPayloadTool.java
 │   └─ PacketReplayTool.java
 │
 ├─ util/
 │   ├─ JsonUtil.java
 │   ├─ McThread.java
 │   ├─ Texts.java
 │   └─ RingBuffer.java
 │
 └─ mixin/
     ├─ ClientConnectionMixin.java
     ├─ ClientPlayNetworkHandlerMixin.java
     └─ MinecraftClientMixin.java
```

---

## 5. 設定ファイル

### 5.1 `config/mineagent/config.json`

```json
{
  "enabled": true,
  "agentName": "opevista",
  "triggerMode": "name_prefix",
  "serverLock": {
    "required": true,
    "modId": "your_server_lock_mod",
    "failMode": "disable_agent"
  },
  "webUi": {
    "enabled": true,
    "host": "127.0.0.1",
    "port": 25712,
    "openOnStart": false,
    "requireTokenWhenNotLocalhost": true,
    "token": ""
  },
  "llm": {
    "enabled": true,
    "provider": "openai_compatible",
    "apiMode": "chat_completions",
    "baseUrl": "http://localhost:1234/v1",
    "apiKey": "",
    "model": "local-model",
    "temperature": 0.2,
    "timeoutMs": 60000,
    "maxOutputTokens": 2048,
    "jsonRepairAttempts": 1
  },
  "agentLoop": {
    "enabled": true,
    "maxSteps": 100,
    "tickInterval": 5,
    "autoContinue": true,
    "stopOnDeath": true
  },
  "permissions": {
    "allowCommands": true,
    "allowCombat": true,
    "allowPvP": true,
    "allowBaritone": true,
    "allowInventoryControl": true,
    "allowBlockPlaceBreak": true,
    "allowContainerControl": true,
    "allowCrafting": true,
    "allowWebUiControl": true,
    "allowPacketRead": true,
    "allowPacketSend": true,
    "allowRawPacketSend": true,
    "allowCustomPayload": true,
    "allowPacketReplay": true
  },
  "packetLayer": {
    "enabled": true,
    "readPackets": true,
    "sendPackets": true,
    "logPackets": true,
    "maxPacketLogEntries": 5000,
    "defaultPacketView": "summary",
    "include": [
      "Chat",
      "GameMessage",
      "Entity",
      "Health",
      "Inventory",
      "Screen",
      "BlockUpdate",
      "CustomPayload",
      "PlayerPosition"
    ],
    "exclude": [
      "Chunk",
      "Light",
      "KeepAlive"
    ]
  },
  "baritone": {
    "enabled": true,
    "mode": "optional",
    "preferApi": false,
    "commandPrefix": "#"
  }
}
```

---

### 5.2 `config/mineagent/registered_users.json`

```json
{
  "users": [
    {
      "name": "opevista",
      "uuid": "",
      "role": "owner"
    }
  ]
}
```

---

## 6. LLM 仕様

### 6.1 API

MVP は OpenAI-compatible Chat Completions 形式でよい。

例:

```http
POST {baseUrl}/chat/completions
```

リクエスト概要:

```json
{
  "model": "local-model",
  "temperature": 0.2,
  "messages": [
    {
      "role": "system",
      "content": "You are MineAgent..."
    },
    {
      "role": "user",
      "content": "{... observation json ...}"
    }
  ]
}
```

将来的に `responses` API モードも追加できる設計にする。

---

### 6.2 LLM に渡す System Prompt

```text
You are MineAgent, a Minecraft control agent running inside a trusted private Fabric client.
You must choose from the provided tools only.
Return STRICT JSON only.
Do not use Markdown.
Do not explain outside JSON.
Your output schema is:
{
  "status": "continue" | "done" | "failed",
  "message": "short status message",
  "actions": [
    {
      "tool": "tool_name",
      "args": {}
    }
  ]
}
Prefer high-level tools over low-level packet tools.
Use packet tools when high-level tools are not enough, when debugging, or when explicitly requested.
If a task needs multiple steps, do a small useful next step and return status "continue".
If the instruction is complete, return status "done".
```

---

### 6.3 Observation JSON

`AgentPromptBuilder` は毎ステップ以下を作る。

```json
{
  "agent": {
    "name": "opevista",
    "trustedEnvironment": true,
    "operatorMode": true
  },
  "task": {
    "id": "task-...",
    "instruction": "木を64個集めて拠点に戻って",
    "step": 3,
    "maxSteps": 100,
    "state": "running"
  },
  "player": {
    "name": "opevista",
    "position": [120, 64, -340],
    "yaw": 90.0,
    "pitch": 12.0,
    "health": 20.0,
    "food": 18,
    "dimension": "minecraft:overworld",
    "gamemode": "survival"
  },
  "inventory": {
    "selectedSlot": 0,
    "items": [
      {"slot": 0, "id": "minecraft:iron_sword", "count": 1},
      {"slot": 1, "id": "minecraft:bread", "count": 8}
    ]
  },
  "world": {
    "time": "night",
    "weather": "clear",
    "nearbyEntities": [
      {"id": 42, "type": "minecraft:zombie", "name": "Zombie", "distance": 8.3}
    ],
    "nearbyBlocksSummary": {
      "minecraft:oak_log": 12,
      "minecraft:coal_ore": 3
    }
  },
  "recentToolResults": [],
  "recentPacketEvents": [],
  "availableTools": []
}
```

---

### 6.4 LLM 出力

```json
{
  "status": "continue",
  "message": "木を集めます",
  "actions": [
    {
      "tool": "send_chat",
      "args": {
        "message": "了解。木を64個集めます。"
      }
    },
    {
      "tool": "baritone",
      "args": {
        "command": "#mine minecraft:oak_log"
      }
    }
  ]
}
```

パース失敗時:

1. JSON 部分を抽出する。
2. 失敗したら `JsonRepair` で1回だけ修復を試す。
3. それでも失敗したらタスクを failed にし、ログに残す。

---

## 7. Tool 共通仕様

### 7.1 Java Interface

```java
public interface Tool {
    String name();
    String description();
    JsonObject schema();
    ToolResult execute(JsonObject args, ToolExecutionContext context);
}
```

### 7.2 ToolResult

```java
public final class ToolResult {
    public boolean ok;
    public String message;
    public JsonObject data;
}
```

### 7.3 Tool 実行ルール

- Minecraft Client の状態変更は必ず client thread で実行する。
- `McThread.runOnClient(...)` ヘルパーを用意する。
- 失敗しても例外で MOD 全体を落とさない。
- すべての tool call と result を `TaskLog` に記録する。

---

## 8. High-level Tools

### 8.1 `get_status`

現在のプレイヤー状態を返す。

戻り値:

- 座標
- yaw/pitch
- HP
- 空腹度
- ディメンション
- ゲームモード
- 選択ホットバー
- 周囲エンティティ概要

---

### 8.2 `get_inventory`

インベントリを返す。

---

### 8.3 `send_chat`

チャットを送信する。

```json
{
  "message": "了解"
}
```

実装:

- 通常チャットは `sendChatMessage` 相当。
- `/` から始まる場合はコマンド送信扱いにしてよい。
- バージョン差分があるので Codex は現在の mappings で正しいメソッドを確認する。

---

### 8.4 `run_command`

サーバーコマンドを送る。

```json
{
  "command": "/say hello"
}
```

実装:

- 先頭 `/` はあってもなくても受け入れる。
- クライアントから送信できる通常コマンドとして扱う。

---

### 8.5 `baritone`

Baritone コマンドを実行する。

```json
{
  "command": "#goto 120 64 -340"
}
```

実装方針:

1. Baritone が存在する場合のみ有効。
2. API が簡単に使えるなら API を使う。
3. 難しい場合はローカルチャットコマンドとして `#...` を実行する。
4. サーバーに `#...` が漏れない方法を優先する。
5. Baritone が未導入なら `ok=false` で返す。

---

### 8.6 `control_player`

基本操作。

```json
{
  "action": "forward | back | left | right | jump | sneak | sprint | attack | use | stop",
  "durationMs": 500
}
```

実装:

- MinecraftClient の key binding pressed 状態を短時間変更する。
- duration 経過後に必ず元に戻す。
- `stop` はすべての移動キーを解除する。

---

### 8.7 `look_at`

指定対象を見る。

```json
{
  "targetType": "position | entity | player",
  "target": "120 64 -340"
}
```

実装:

- 座標または entity id/name から yaw/pitch を計算して視点変更する。

---

### 8.8 `combat`

戦闘行動。

```json
{
  "mode": "nearest_hostile | nearest_player | named_player | all_hostile | defend_area",
  "target": "Steve",
  "durationSec": 60
}
```

MVP 実装:

- `nearest_hostile`
- `named_player`
- `nearest_player`

動作:

1. 対象を探す。
2. `look_at` する。
3. 武器を持っていれば選択する。
4. 距離が遠ければ近づく。
5. 攻撃 packet wrapper または attack key を使う。
6. duration または stop で終了。

---

### 8.9 `gather`

アイテム収集。

```json
{
  "item": "minecraft:oak_log",
  "count": 64,
  "returnAfterDone": true
}
```

MVP:

- Baritone があれば `#mine <block>` を使う。
- アイテム名が block として扱える場合のみ対応。
- 足りない数を inventory から計算する。
- 完了判定は inventory count。

---

### 8.10 `inventory`

```json
{
  "action": "inspect | select_hotbar | drop | equip | use_item",
  "item": "minecraft:bread",
  "slot": 1,
  "count": 1
}
```

---

### 8.11 `container`

チェスト/かまど/作業台操作。

MVP では `inspect` と `close` のみでよい。  
v0.2 で `take`, `put`, `move_item` を追加。

---

### 8.12 `craft`

指定アイテムをクラフト。

MVP では未実装スタブでよい。  
`ok=false`, `message="craft tool is not implemented yet"` を返す。

---

### 8.13 `build`

ブロック設置/簡易建築。

MVP では未実装スタブでよい。

---

## 9. Packet Gateway

### 9.1 目的

MineAgent は高レベル Tool だけでなく、実際の Minecraft パケットを読み、ログし、必要に応じて送信できる。

用途:

- デバッグ
- LLM の観測精度向上
- CustomPayload による自作サーバー MOD 連携
- 通常 API では扱いにくい操作の実行
- Packet Console からの手動検証

---

### 9.2 設計方針

1. Packet は全部を LLM に渡さない。
2. `PacketSummaryExtractor` で意味情報に要約する。
3. WebUI では直近ログを見られるようにする。
4. 送信は最初 `Wrapped Packet` を優先する。
5. Raw Packet Send はバージョン差分が激しいので後回し。ただし設計だけ入れておく。

---

### 9.3 Packet 読み取り

Mixin で以下に差し込む。

- C2S: `ClientConnection.send(...)`
- S2C: `ClientConnection.channelRead0(...)` またはバージョン上の該当受信処理
- CustomPayload: `ClientPlayNetworkHandler` / common handler の custom payload 処理

Codex は Fabric/Yarn/Mojmap の現行メソッド名を確認して、最も壊れにくい injection point を選ぶこと。

---

### 9.4 PacketEvent

```java
public final class PacketEvent {
    public long timeMs;
    public PacketDirection direction;
    public String packetClass;
    public String packetName;
    public JsonObject summary;
    public String rawToString;
}
```

### 9.5 PacketDirection

```java
public enum PacketDirection {
    C2S,
    S2C
}
```

---

### 9.6 PacketLogger

要件:

- RingBuffer 形式
- 最大件数は config の `maxPacketLogEntries`
- include/exclude filter 対応
- WebUI/API から取得できる
- clear できる
- trace session に流せる

---

### 9.7 PacketSummaryExtractor

最初に対応するパケット概要:

| 種別 | 抽出する情報 |
|---|---|
| Chat/GameMessage | text |
| EntitySpawn | entityId, type, pos |
| EntityPosition | entityId, pos |
| EntityVelocity | entityId, velocity |
| HealthUpdate | health, food |
| Inventory/Screen | slot, item |
| BlockUpdate | pos, block |
| PlayerPositionLook | pos, yaw, pitch |
| CustomPayload | channel, payload summary |
| Disconnect | reason |

未知の packet は:

```json
{
  "unparsed": true,
  "toString": "..."
}
```

でよい。

---

## 10. Packet Tools

### 10.1 `packet_read_recent`

```json
{
  "direction": "C2S | S2C | BOTH",
  "limit": 20,
  "filter": "Entity"
}
```

直近ログを返す。

---

### 10.2 `packet_trace`

```json
{
  "direction": "C2S | S2C | BOTH",
  "packetName": "Entity",
  "durationSec": 10
}
```

指定時間監視して結果を返す。

実装が複雑なら、MVP では trace session を登録して WebUI/ログに流すだけでよい。

---

### 10.3 `packet_send_custom_payload`

```json
{
  "channel": "mineagent:agent_to_server",
  "payload": {
    "type": "status",
    "message": "hello"
  }
}
```

Fabric の CustomPayload API を使う。Payload は JSON 文字列として送ってよい。

---

### 10.4 `packet_send_wrapped`

```json
{
  "type": "chat_message | command | use_item | attack_entity | interact_entity | player_move | click_slot",
  "args": {}
}
```

初期対応:

#### chat_message

```json
{
  "type": "chat_message",
  "args": {
    "message": "hello"
  }
}
```

#### command

```json
{
  "type": "command",
  "args": {
    "command": "say hello"
  }
}
```

#### attack_entity

```json
{
  "type": "attack_entity",
  "args": {
    "entityId": 42,
    "sneaking": false
  }
}
```

#### use_item

```json
{
  "type": "use_item",
  "args": {
    "hand": "main_hand"
  }
}
```

#### player_move

```json
{
  "type": "player_move",
  "args": {
    "x": 120.0,
    "y": 64.0,
    "z": -340.0,
    "yaw": 90.0,
    "pitch": 0.0,
    "onGround": true
  }
}
```

#### click_slot

```json
{
  "type": "click_slot",
  "args": {
    "syncId": 1,
    "slot": 10,
    "button": 0,
    "actionType": "PICKUP"
  }
}
```

---

### 10.5 `packet_replay`

v0.2 以降。MVP では stub でよい。

```json
{
  "eventIds": ["..."],
  "delayMs": 50
}
```

---

## 11. CustomPayload Bridge

### 11.1 チャンネル

```text
mineagent:server_to_agent
mineagent:agent_to_server
mineagent:tool_result
mineagent:debug
```

### 11.2 Server → Agent

Payload 例:

```json
{
  "type": "instruction",
  "from": "server",
  "instruction": "拠点に戻ってチェストを整理して"
}
```

受信時:

1. `CustomPayloadBridge` が受け取る。
2. `InstructionRouter` に渡す。
3. `source=server_payload` としてタスク作成。

---

### 11.3 Agent → Server

Payload 例:

```json
{
  "type": "status",
  "agent": "opevista",
  "task": "gathering_iron",
  "position": [120, 64, -340],
  "inventory": {
    "minecraft:iron_ore": 18
  }
}
```

---

## 12. WebUI API

### 12.1 `GET /`

HTML UI を返す。

### 12.2 `GET /api/status`

```json
{
  "enabled": true,
  "running": true,
  "paused": false,
  "currentTask": {
    "id": "task-...",
    "instruction": "木を64個集めて",
    "state": "running",
    "step": 5
  },
  "player": {
    "position": [120, 64, -340],
    "health": 20.0
  }
}
```

### 12.3 `POST /api/instruct`

```json
{
  "user": "webui",
  "instruction": "鉄を32個集めて"
}
```

### 12.4 `POST /api/stop`

現在タスクを停止。

### 12.5 `POST /api/pause`

一時停止。

### 12.6 `POST /api/resume`

再開。

### 12.7 `GET /api/logs`

タスクログ。

### 12.8 `GET /api/packets?direction=BOTH&limit=100&filter=Entity`

Packet Log。

### 12.9 `POST /api/packets/send`

`packet_send_wrapped` と同じ形式。

### 12.10 `POST /api/custom-payload/send`

`packet_send_custom_payload` と同じ形式。

---

## 13. WebUI 画面

MVP は1ファイル HTML でよい。

画面構成:

```text
MineAgent

[Status]
Enabled: true
Task: 木を64個集めて
Position: 120 64 -340
HP: 20

[Instruction]
textarea
[Send]
[Stop] [Pause] [Resume]

[Tool Log]
...

[Packet Console]
Direction select
Filter input
[Refresh]
packet table

[Send Wrapped Packet]
type select
args JSON textarea
[Send Packet]

[CustomPayload]
channel input
payload JSON textarea
[Send CustomPayload]
```

デザインはシンプルでよい。ダークテーマ推奨。

---

## 14. Agent Loop

### 14.1 流れ

```text
Instruction received
↓
TaskManager creates AgentTask
↓
AgentLoop observe
↓
PromptBuilder creates observation
↓
LlmClient call
↓
Parse JSON actions
↓
ToolRegistry executes actions
↓
Append results
↓
If status=continue and task not stopped, next step
↓
Done / Failed / Stopped
```

### 14.2 停止条件

- `/mineagent stop`
- WebUI stop
- task status `done`
- task status `failed`
- player death
- maxSteps 超過
- LLM API エラー連続
- JSON parse 失敗

### 14.3 Tick 実行

- Client tick で AgentLoop を進める。
- LLM HTTP 呼び出しは別スレッド。
- Minecraft 状態変更は client thread に戻す。

---

## 15. Baritone 連携

Baritone は optional。

### 15.1 検出

- MOD list から `baritone` または既知の mod id を探す。
- 見つからなければ `baritone` tool は disabled。

### 15.2 実行

優先順位:

1. Baritone API が使える場合は API
2. 無理ならローカル `#command` を実行
3. それも無理なら `ok=false`

### 15.3 初期コマンド例

```text
#goto x y z
#mine minecraft:oak_log
#follow player Steve
#stop
```

---

## 16. ログ

### 16.1 TaskLog

記録するもの:

- 受信 instruction
- source
- LLM request summary
- LLM raw response
- tool call
- tool result
- errors
- state changes

### 16.2 PacketLog

記録するもの:

- time
- direction
- packetName
- summary
- rawToString

### 16.3 保存

MVP:

- メモリ上のみでよい。

v0.2:

- `logs/mineagent/YYYY-MM-DD.log`
- `logs/mineagent/packets-YYYY-MM-DD.jsonl`

---

## 17. MVP v0.1 の実装範囲

Codex はまずこれを完成させる。

### 17.1 必須

- Fabric MOD としてビルド成功
- Client initializer
- Config loader
- User registry
- Client command `/mineagent status`
- Client command `/mineagent ask`
- Client command `/mineagent stop`
- Chat trigger parser
- WebUI server
- `/api/status`
- `/api/instruct`
- `/api/stop`
- LLM client
- JSON action parser
- ToolRegistry
- Tools:
  - `get_status`
  - `get_inventory`
  - `send_chat`
  - `run_command`
  - `baritone`
- PacketLogger
- C2S/S2C packet summary logging
- Packet WebUI表示
- Packet tools:
  - `packet_read_recent`
  - `packet_send_custom_payload`
  - `packet_send_wrapped` の `chat_message` と `command`

### 17.2 v0.1 で stub 可

- `combat`
- `gather`
- `inventory` の高度操作
- `container`
- `craft`
- `build`
- `packet_trace`
- `packet_replay`
- Raw Packet reflection sender

stub は必ず `ok=false` で、未実装理由を返す。

---

## 18. v0.2 実装範囲

- `control_player`
- `look_at`
- `combat`
- `gather`
- `packet_send_wrapped` の attack/use/move/click_slot
- CustomPayload S2C instruction
- Packet trace
- ログの jsonl 保存

---

## 19. v0.3 実装範囲

- Container 操作
- Craft 操作
- Build 操作
- 複数タスクキュー
- WebSocket によるリアルタイム UI
- Baritone API 直接連携
- Packet replay
- 設定編集 UI

---

## 20. テスト

### 20.1 Unit Test

可能なら JUnit を追加して以下をテストする。

- `ChatInstructionParser`
- `ConfigManager`
- `UserRegistry`
- `ToolCall` JSON parse
- `PacketFilter`
- `RingBuffer`

### 20.2 Manual Test

README に手順を書く。

1. Minecraft クライアントを起動
2. MOD がロードされる
3. `/mineagent status` が動く
4. `/mineagent web` が URL を表示する
5. WebUI が開く
6. `/mineagent ask 今の状態を説明して` が LLM に渡る
7. LLM mock または local OpenAI-compatible API が JSON actions を返す
8. `send_chat` が実行される
9. Packet Log に C2S/S2C が表示される
10. `/mineagent stop` で停止できる

---

## 21. LLM Mock

API キーや local LLM がなくても動作確認できるようにする。

config:

```json
"llm": {
  "enabled": true,
  "provider": "mock"
}
```

Mock 動作:

- instruction に `状態` があれば `get_status`
- instruction に `hello` があれば `send_chat`
- それ以外は `send_chat("MineAgent mock response")`

---

## 22. README 要件

Codex は README.md に以下を書くこと。

- MineAgent の概要
- セットアップ
- ビルド方法
- 実行方法
- config 説明
- WebUI URL
- チャット指示形式
- コマンド一覧
- LLM 設定
- Packet Console 説明
- Baritone optional 説明
- 既知の制限

---

## 23. AGENTS.md 要件

リポジトリルートに `AGENTS.md` を作成する。

内容:

```md
# AGENTS.md

## Project

MineAgent is a Minecraft 26.1.2 Fabric client mod that lets a trusted user control the client through natural-language instructions, LLM tool calls, WebUI, Baritone, and packet-level networking.

## Working rules

- Keep the mod buildable at all times.
- Prefer small, testable increments.
- Run `./gradlew build` after code changes.
- Use existing Gradle/Fabric/Loom versions if present.
- Do not replace the whole project structure unless necessary.
- Do not add heavy dependencies without a clear reason.
- Keep WebUI dependency-free unless a later task explicitly allows a frontend framework.
- All Minecraft state mutations must happen on the client thread.
- LLM HTTP calls must not block the client thread.
- Prefer high-level tools over raw packet operations.
- Packet logs must be filtered and summarized before being sent to the LLM.
- If a feature is not implemented yet, add a stub ToolResult instead of crashing.
- Document assumptions in `docs/ASSUMPTIONS.md`.

## Done means

- `./gradlew build` passes.
- New commands are listed in README.
- New tools are listed in docs/TOOLS.md.
- Manual test steps are updated.
```

---

## 24. docs 作成

Codex は次を作る。

```text
docs/
 ├─ ARCHITECTURE.md
 ├─ TOOLS.md
 ├─ PACKET_GATEWAY.md
 ├─ WEBUI.md
 ├─ ASSUMPTIONS.md
 └─ MANUAL_TEST.md
```

---

## 25. 完了条件

MVP v0.1 完了条件:

- `./gradlew build` が成功する
- Minecraft に MOD としてロードできる
- `/mineagent status` が表示される
- `/mineagent ask <instruction>` でタスクが作成される
- WebUI から instruction を送れる
- `/mineagent stop` と WebUI stop が効く
- Mock LLM で最低1つ tool が実行される
- OpenAI-compatible LLM 設定を入れれば JSON actions を実行できる
- Packet Log に C2S/S2C の summary が表示される
- `packet_read_recent` が tool として動く
- `packet_send_custom_payload` が少なくとも JSON payload を送れる
- README と docs が更新されている

---

## 26. Codex が最初に実行するべき作業順

1. リポジトリ調査
2. Gradle/Fabric バージョン確認
3. `AGENTS.md` 作成
4. `docs/ASSUMPTIONS.md` 作成
5. config / user registry 実装
6. client initializer 実装
7. command 実装
8. Tool interface / registry 実装
9. Mock LLM 実装
10. AgentLoop 最小実装
11. WebUI server 最小実装
12. PacketLogger 最小実装
13. Packet WebUI 表示
14. README/docs 更新
15. `./gradlew build`
16. エラー修正
17. 最終 diff レビュー

---

## 27. Codex に渡す短い実行プロンプト

以下を Codex の最初のタスクとして使う。

```text
このリポジトリで、Minecraft 26.1.2 Fabric クライアントMOD「MineAgent」のMVP v0.1を実装してください。

仕様は `SPEC_MineAgent_Codex.md` に従ってください。まずリポジトリ構成とGradle/Fabric/Loom設定を確認し、既存設定を尊重してください。既存コードがない場合は最小Fabric MOD構成を作ってください。

MVP v0.1の完了条件:
- ./gradlew build が通る
- /mineagent status, /mineagent ask, /mineagent stop が動く
- config/mineagent/config.json と registered_users.json を読み書きできる
- Chat trigger `<agentName> <instruction>` を parse できる
- Mock LLM と OpenAI-compatible LLM client の土台がある
- ToolRegistry と get_status/get_inventory/send_chat/run_command/baritone を実装
- WebUI を 127.0.0.1:25712 で起動し、status/instruct/stop/logs/packets API を実装
- PacketGateway の最小実装として C2S/S2C packet summary logging を入れる
- packet_read_recent, packet_send_custom_payload, packet_send_wrapped(chat_message, command) を実装
- README.md, AGENTS.md, docs/ARCHITECTURE.md, docs/TOOLS.md, docs/PACKET_GATEWAY.md, docs/MANUAL_TEST.md を作成/更新
- 変更後に ./gradlew build を実行し、失敗したら修正してください

迷った場合は質問で止まらず、最小で堅牢な実装を選び、docs/ASSUMPTIONS.md に仮定を書いてください。
```
