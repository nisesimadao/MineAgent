package jp.opevista.mineagent.tools;

import jp.opevista.mineagent.MineAgentClient;
import net.minecraft.client.Minecraft;

public record ToolExecutionContext(MineAgentClient mod, Minecraft client) {
}
