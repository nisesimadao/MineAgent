package jp.opevista.mineagent.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import jp.opevista.mineagent.MineAgentClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

public final class MineAgentClientCommands {
    private MineAgentClientCommands() {
    }

    public static void register(MineAgentClient mod) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("mineagent")
                        .then(ClientCommands.literal("status").executes(ctx -> {
                            ctx.getSource().sendFeedback(Component.literal(mod.taskManager().statusText()));
                            return 1;
                        }))
                        .then(ClientCommands.literal("ask")
                                .then(ClientCommands.argument("instruction", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String instruction = StringArgumentType.getString(ctx, "instruction");
                                            mod.taskManager().startTask("command", "local", instruction);
                                            ctx.getSource().sendFeedback(Component.literal("MineAgent task started: " + instruction));
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("stop").executes(ctx -> {
                            mod.taskManager().stopCurrent("command");
                            ctx.getSource().sendFeedback(Component.literal("MineAgent stopped current task"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("web").executes(ctx -> {
                            ctx.getSource().sendFeedback(Component.literal("MineAgent WebUI: http://127.0.0.1:" + mod.config().webUi.port));
                            return 1;
                        }))
                        .then(ClientCommands.literal("logs").executes(ctx -> {
                            ctx.getSource().sendFeedback(Component.literal(String.join("\n", mod.taskManager().recentLogs(10))));
                            return 1;
                        }))
                        .then(ClientCommands.literal("reload").executes(ctx -> {
                            mod.reload();
                            ctx.getSource().sendFeedback(Component.literal("MineAgent config reloaded"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("packets")
                                .then(ClientCommands.literal("recent")
                                        .executes(ctx -> {
                                            ctx.getSource().sendFeedback(Component.literal(mod.packetGateway().recentAsText(20)));
                                            return 1;
                                        })
                                        .then(ClientCommands.argument("limit", IntegerArgumentType.integer(1, 200))
                                                .executes(ctx -> {
                                                    int limit = IntegerArgumentType.getInteger(ctx, "limit");
                                                    ctx.getSource().sendFeedback(Component.literal(mod.packetGateway().recentAsText(limit)));
                                                    return 1;
                                                })))
                                .then(ClientCommands.literal("clear").executes(ctx -> {
                                    mod.packetGateway().clear();
                                    ctx.getSource().sendFeedback(Component.literal("MineAgent packet log cleared"));
                                    return 1;
                                })))
        ));
    }
}
