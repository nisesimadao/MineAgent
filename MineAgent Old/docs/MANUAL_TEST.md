# Manual Test

1. Build the mod with `./gradlew build`.
2. Add the produced jar from `build/libs` to a Minecraft 26.1.2 Fabric client.
3. Start the client.
4. Confirm `config/mineagent/config.json` and `registered_users.json` are created.
5. Join a world or server.
6. Run `/mineagent status`.
7. Run `/mineagent ask status`.
8. Confirm the task log shows a mock LLM response and `get_status` tool result.
9. Run `/mineagent ask hello`.
10. Confirm the mock LLM queues `send_chat`.
11. Open `http://127.0.0.1:25712`.
12. Send an instruction from WebUI.
13. Press Stop in WebUI and confirm task state changes.
14. Run `/mineagent packets recent 20`.
15. Refresh Packet Console in WebUI and confirm C2S/S2C summaries appear after network activity.
16. Use WebUI Send Wrapped Packet with `chat_message`.
17. Use WebUI CustomPayload and confirm a logged bridge event appears.
