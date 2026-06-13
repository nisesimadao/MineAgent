package jp.opevista.mineagent.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class MineAgentTools {
    private MineAgentTools() {
    }

    static CompletableFuture<JsonObject> execute(String name, JsonObject args) {
        Minecraft client = Minecraft.getInstance();
        if ("get_screenshot".equals(name)) {
            CompletableFuture<JsonObject> future = new CompletableFuture<>();
            client.execute(() -> {
                try {
                    getScreenshotAsync(client, future);
                } catch (Throwable t) {
                    JsonObject error = new JsonObject();
                    error.addProperty("ok", false);
                    error.addProperty("error", t.getMessage() == null ? t.toString() : t.getMessage());
                    future.complete(error);
                }
            });
            return future;
        }

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        client.execute(() -> {
            try {
                future.complete(runOnClient(client, name, args));
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("ok", false);
                error.addProperty("error", t.getMessage() == null ? t.toString() : t.getMessage());
                future.complete(error);
            }
        });
        return future;
    }

    private static JsonObject runOnClient(Minecraft client, String name, JsonObject args) {
        return switch (name) {
            case "get_status" -> getStatus(client);
            case "send_chat" -> sendChat(client, args);
            case "run_command" -> runCommand(client, args);
            case "look_at" -> lookAt(client, args);
            case "move_input" -> moveInput(client, args);
            case "key_signal" -> keySignal(client, args);
            case "list_keybindings" -> listKeybindings(client);
            case "release_all_keys" -> releaseAllKeys(client);
            case "baritone_stop" -> baritoneStop(client);
            case "stop_all" -> stopAll(client);
            case "read_chat" -> readChat(args);
            case "fawe_feedback" -> faweFeedback(client, args);
            case "fawe_command" -> faweCommand(client, args);
            case "fawe_pos1" -> faweFixedCommand(client, "/pos1");
            case "fawe_pos2" -> faweFixedCommand(client, "/pos2");
            case "fawe_pos_select" -> fawePosSelect(client, args);
            case "fawe_set" -> faweSet(client, args);
            case "fawe_walls" -> faweWalls(client, args);
            case "fawe_replace" -> faweReplace(client, args);
            case "fawe_undo" -> faweFixedCommand(client, "/undo");
            case "fawe_redo" -> faweFixedCommand(client, "/redo");
            case "select_hotbar_slot" -> selectHotbarSlot(client, args);
            case "jump" -> jump(client);
            case "get_block" -> getBlock(client, args);
            case "place_block" -> placeBlock(client, args);
            case "get_inventory" -> getInventory(client);
            case "wait_for_inventory" -> getInventory(client);
            case "get_container" -> getContainer(client);
            case "wait_for_container" -> getContainer(client);
            case "click_slot" -> clickSlot(client, args);
            case "container_click" -> containerClick(client, args);
            case "click_slot_by_item" -> clickSlotByItem(client, args);
            case "craft_item" -> craftItem(client, args);
            case "container_button" -> containerButton(client, args);
            case "baritone_status" -> baritoneStatus();
            case "baritone_command" -> baritoneCommand(client, args);
            case "baritone_goto" -> baritoneGoto(client, args);
            case "get_packet_log" -> getPacketLog(args);
            default -> fail("unknown tool: " + name);
        };
    }

    private static void getScreenshotAsync(Minecraft client, CompletableFuture<JsonObject> future) {
        try {
            java.io.File file = java.io.File.createTempFile("mineagent_screenshot_", ".png");
            net.minecraft.client.Screenshot.takeScreenshot(
                client.getMainRenderTarget(),
                (image) -> {
                    try {
                        image.writeToFile(file.toPath());
                        JsonObject data = new JsonObject();
                        data.addProperty("path", file.getAbsolutePath());
                        future.complete(ok("screenshot saved", data));
                    } catch (Exception e) {
                        future.complete(fail("Failed to save screenshot: " + e.getMessage()));
                    } finally {
                        image.close();
                    }
                }
            );
        } catch (Exception e) {
            future.complete(fail("Failed to take screenshot: " + e.getMessage()));
        }
    }

    private static JsonObject getPacketLog(JsonObject args) {
        int count = JsonHttp.integer(args, "count", 20);
        JsonObject data = new JsonObject();
        data.add("packets", PacketLog.recent(count));
        return ok("packet log collected", data);
    }

    private static JsonObject getStatus(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return ok("client player is not in-world");
        }

        JsonObject data = new JsonObject();
        JsonArray pos = new JsonArray();
        pos.add(player.getX());
        pos.add(player.getY());
        pos.add(player.getZ());
        data.add("position", pos);
        data.addProperty("x", player.getX());
        data.addProperty("y", player.getY());
        data.addProperty("z", player.getZ());
        data.addProperty("yaw", player.getYRot());
        data.addProperty("pitch", player.getXRot());
        data.addProperty("health", player.getHealth());
        data.addProperty("food", player.getFoodData().getFoodLevel());
        data.addProperty("name", player.getName().getString());
        if (player.level() != null) {
            data.addProperty("dimension", player.level().dimension().identifier().toString());
        }
        return ok("status collected", data);
    }

    private static JsonObject sendChat(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        String message = JsonHttp.string(args, "message", "");
        if (message.isBlank()) {
            return fail("message is required");
        }
        player.connection.sendChat(message);
        return ok("chat sent");
    }

    private static JsonObject runCommand(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        String command = JsonHttp.string(args, "command", "");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank()) {
            return fail("command is required");
        }
        player.connection.sendCommand(command);
        return ok("command sent");
    }

    private static JsonObject lookAt(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        double x = JsonHttp.number(args, "x", player.getX());
        double y = JsonHttp.number(args, "y", player.getEyeY());
        double z = JsonHttp.number(args, "z", player.getZ());
        double dx = x - player.getX();
        double dy = y - player.getEyeY();
        double dz = z - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        return ok("look direction updated");
    }

    private static JsonObject moveInput(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        String direction = JsonHttp.string(args, "direction", "forward").toLowerCase(Locale.ROOT);
        int ticks = Math.max(1, Math.min(JsonHttp.integer(args, "ticks", 10), 100));
        KeyPulse.schedule(client, direction, ticks);
        return ok("movement queued");
    }

    private static JsonObject keySignal(Minecraft client, JsonObject args) {
        String keyName = JsonHttp.string(args, "key", "");
        String action = JsonHttp.string(args, "action", "pulse").toLowerCase(Locale.ROOT);
        int ticks = Math.max(1, Math.min(JsonHttp.integer(args, "ticks", 2), 100));
        KeyMapping key = keyByName(client, keyName);
        if (key == null) {
            return fail("unknown key: " + keyName);
        }

        switch (action) {
            case "down" -> key.setDown(true);
            case "up" -> key.setDown(false);
            case "pulse" -> {
                key.setDown(true);
                KeyPulse.schedule(key, ticks);
            }
            default -> {
                return fail("unknown key action: " + action);
            }
        }
        return ok("key signal sent");
    }

    private static JsonObject listKeybindings(Minecraft client) {
        JsonObject data = new JsonObject();
        JsonArray keys = new JsonArray();
        for (KeyMapping mapping : client.options.keyMappings) {
            JsonObject key = new JsonObject();
            key.addProperty("name", mapping.getName());
            key.addProperty("translatedName", mapping.getTranslatedKeyMessage().getString());
            key.addProperty("category", mapping.getCategory().id().toString());
            key.addProperty("categoryLabel", mapping.getCategory().label().getString());
            key.addProperty("boundKey", mapping.saveString());
            key.addProperty("down", mapping.isDown());
            keys.add(key);
        }
        data.add("keys", keys);
        return ok("keybindings collected", data);
    }

    private static JsonObject releaseAllKeys(Minecraft client) {
        KeyPulse.clearAndRelease(client);
        return ok("all key inputs released");
    }

    private static JsonObject baritoneStop(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        if (!isBaritoneAvailable()) {
            return fail("baritone is not available");
        }
        player.connection.sendChat("#stop");
        return ok("baritone stop sent");
    }

    private static JsonObject stopAll(Minecraft client) {
        KeyPulse.clearAndRelease(client);
        LocalPlayer player = client.player;
        boolean baritoneStopSent = false;
        if (player != null && isBaritoneAvailable()) {
            player.connection.sendChat("#stop");
            baritoneStopSent = true;
        }
        JsonObject data = new JsonObject();
        data.addProperty("keysReleased", true);
        data.addProperty("baritoneStopSent", baritoneStopSent);
        return ok("stop all completed", data);
    }

    private static JsonObject readChat(JsonObject args) {
        int count = JsonHttp.integer(args, "count", 20);
        String type = JsonHttp.string(args, "type", "");
        String contains = JsonHttp.string(args, "contains", "");
        JsonObject data = new JsonObject();
        data.add("messages", ChatLog.recent(count, type, contains));
        data.addProperty("stored", ChatLog.size());
        return ok("chat messages collected", data);
    }

    private static JsonObject faweFeedback(Minecraft client, JsonObject args) {
        String command = JsonHttp.string(args, "command", "");
        if (command.isBlank()) {
            return fail("command is required");
        }
        
        // 送信直前の時刻を記録（もしくは現在のチャットサイズを保存）
        int initialSize = ChatLog.size();
        
        JsonObject result = sendFaweCommand(client, command);
        if (!result.get("ok").getAsBoolean()) {
            return result;
        }
        
        // 簡易的なフィードバック待機（本来は数ミリ秒待つべきだが、非同期ツールのためここでは送信成功のみ返す）
        JsonObject data = result.getAsJsonObject("data");
        data.addProperty("waitNote", "Wait a moment and call read_chat to see the result.");
        return ok("command sent for feedback", data);
    }

    private static JsonObject fawePosSelect(Minecraft client, JsonObject args) {
        int x = JsonHttp.integer(args, "x", 0);
        int y = JsonHttp.integer(args, "y", 0);
        int z = JsonHttp.integer(args, "z", 0);
        int pos = JsonHttp.integer(args, "pos", 1);
        return sendFaweCommand(client, "/pos" + pos + " " + x + " " + y + " " + z);
    }

    private static JsonObject faweCommand(Minecraft client, JsonObject args) {
        String command = JsonHttp.string(args, "command", "");
        boolean waitForFeedback = JsonHttp.bool(args, "waitForFeedback", false);

        if (command.isBlank()) {
            return fail("command is required");
        }

        JsonObject result = sendFaweCommand(client, command);
        if (!result.get("ok").getAsBoolean()) {
            return result;
        }

        if (waitForFeedback) {
            JsonObject data = result.getAsJsonObject("data");
            data.addProperty("waitNote", "Command sent. Call read_chat to verify result.");
        }
        return result;
    }

    private static JsonObject faweFixedCommand(Minecraft client, String command) {
        return sendFaweCommand(client, command);
    }

    private static JsonObject faweSet(Minecraft client, JsonObject args) {
        String pattern = JsonHttp.string(args, "pattern", "");
        if (pattern.isBlank()) {
            return fail("pattern is required");
        }
        return sendFaweCommand(client, "/set " + pattern);
    }

    private static JsonObject faweWalls(Minecraft client, JsonObject args) {
        String pattern = JsonHttp.string(args, "pattern", "");
        if (pattern.isBlank()) {
            return fail("pattern is required");
        }
        return sendFaweCommand(client, "/walls " + pattern);
    }

    private static JsonObject faweReplace(Minecraft client, JsonObject args) {
        String from = JsonHttp.string(args, "from", "");
        String to = JsonHttp.string(args, "to", "");
        if (from.isBlank() || to.isBlank()) {
            return fail("from and to are required");
        }
        return sendFaweCommand(client, "/replace " + from + " " + to);
    }

    private static JsonObject sendFaweCommand(Minecraft client, String command) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        String normalized = command;
        if (normalized.startsWith("//")) {
            normalized = normalized.substring(1);
        } else if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        player.connection.sendCommand(normalized);

        JsonObject data = new JsonObject();
        data.addProperty("sentCommand", normalized);
        data.addProperty("note", "Use read_chat after a short delay to inspect FAWE/WorldEdit feedback.");
        return ok("fawe command sent", data);
    }

    private static String normalizeFaweCommand(String command) {
        String trimmed = command.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("/")) {
            return trimmed;
        }
        return "/" + trimmed;
    }

    private static JsonObject jump(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        player.jumpFromGround();
        return ok("jumped");
    }

    private static JsonObject selectHotbarSlot(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        int slot = JsonHttp.integer(args, "slot", 0);
        if (slot < 0 || slot > 8) {
            return fail("hotbar slot must be between 0 and 8");
        }
        player.getInventory().setSelectedSlot(slot);
        JsonObject data = new JsonObject();
        data.addProperty("selectedSlot", player.getInventory().getSelectedSlot());
        return ok("hotbar slot selected", data);
    }

    private static JsonObject getBlock(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null || player.level() == null) {
            return fail("client player is not in-world");
        }
        int x = JsonHttp.integer(args, "x", (int) Math.floor(player.getX()));
        int y = JsonHttp.integer(args, "y", (int) Math.floor(player.getY() - 1.0));
        int z = JsonHttp.integer(args, "z", (int) Math.floor(player.getZ()));
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = player.level().getBlockState(pos);

        JsonObject data = new JsonObject();
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        data.addProperty("block", state.getBlock().toString());
        data.addProperty("state", state.toString());
        return ok("block collected", data);
    }

    private static JsonObject placeBlock(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null || player.level() == null || client.gameMode == null) {
            return fail("client player is not in-world");
        }

        int x = JsonHttp.integer(args, "x", (int) Math.floor(player.getX()));
        int y = JsonHttp.integer(args, "y", (int) Math.floor(player.getY() - 1.0));
        int z = JsonHttp.integer(args, "z", (int) Math.floor(player.getZ()));
        Direction face = direction(JsonHttp.string(args, "face", "up"));
        BlockPos targetPos = new BlockPos(x, y, z);
        BlockPos referencePos = targetPos.relative(face.getOpposite());
        BlockState targetState = player.level().getBlockState(targetPos);
        BlockState referenceState = player.level().getBlockState(referencePos);
        if (!targetState.isAir()) {
            return fail("target block is not air: " + targetState);
        }
        if (referenceState.isAir()) {
            return fail("reference block is air at " + referencePos);
        }

        Vec3 hit = Vec3.atCenterOf(referencePos).add(
                face.getStepX() * 0.5,
                face.getStepY() * 0.5,
                face.getStepZ() * 0.5
        );
        BlockHitResult hitResult = new BlockHitResult(hit, face, referencePos, false);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);

        JsonObject data = new JsonObject();
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        data.addProperty("face", face.name());
        data.addProperty("selectedSlot", player.getInventory().getSelectedSlot());
        return ok("place block action sent", data);
    }

    private static JsonObject getInventory(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }

        JsonObject data = new JsonObject();
        JsonArray items = new JsonArray();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                JsonObject item = new JsonObject();
                item.addProperty("slot", slot);
                item.addProperty("item", stack.getItem().toString());
                item.addProperty("count", stack.getCount());
                items.add(item);
            }
        }
        data.add("items", items);
        data.addProperty("selectedSlot", player.getInventory().getSelectedSlot());
        data.addProperty("snapshot", System.currentTimeMillis());
        return ok("inventory collected", data);
    }

    private static JsonObject getContainer(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }

        JsonObject data = new JsonObject();
        data.addProperty("containerId", player.containerMenu.containerId);
        data.addProperty("menuClass", player.containerMenu.getClass().getName());
        String title = "";
        if (client.screen != null) {
            title = client.screen.getTitle().getString();
        } else {
            title = player.containerMenu.getClass().getSimpleName();
        }
        data.addProperty("title", title);
        data.addProperty("snapshot", System.currentTimeMillis());
        JsonArray slots = new JsonArray();
        for (int i = 0; i < player.containerMenu.slots.size(); i++) {
            ItemStack stack = player.containerMenu.slots.get(i).getItem();
            JsonObject slot = new JsonObject();
            slot.addProperty("slot", i);
            slot.addProperty("hasItem", !stack.isEmpty());
            if (!stack.isEmpty()) {
                addStackInfo(slot, stack);
            }
            slots.add(slot);
        }
        data.add("slots", slots);
        return ok("container collected", data);
    }

    private static JsonObject clickSlot(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return fail("client player is not in-world");
        }

        int slot = JsonHttp.integer(args, "slot", -1);
        int button = JsonHttp.integer(args, "button", 0);
        String typeName = JsonHttp.string(args, "type", "pickup").toUpperCase(Locale.ROOT);
        if (slot < -999) {
            return fail("slot must be -999 or greater");
        }

        ContainerInput containerInput;
        try {
            containerInput = ContainerInput.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return fail("unknown click type: " + typeName);
        }

        client.gameMode.handleContainerInput(player.containerMenu.containerId, slot, button, containerInput, player);
        JsonObject data = new JsonObject();
        data.addProperty("containerId", player.containerMenu.containerId);
        data.addProperty("slot", slot);
        data.addProperty("button", button);
        data.addProperty("type", containerInput.name());
        return ok("slot click sent", data);
    }

    private static JsonObject containerClick(Minecraft client, JsonObject args) {
        return clickSlot(client, args);
    }

    private static JsonObject craftItem(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }

        net.minecraft.world.inventory.AbstractContainerMenu menu = player.containerMenu;
        boolean isCraftingMenu = menu instanceof net.minecraft.world.inventory.CraftingMenu;
        boolean isInventoryMenu = menu instanceof net.minecraft.world.inventory.InventoryMenu;

        if (!isCraftingMenu && !isInventoryMenu) {
            return fail("Neither crafting table menu nor inventory menu is open");
        }

        String recipe = JsonHttp.string(args, "recipe", "");
        int count = Math.max(1, JsonHttp.integer(args, "count", 1));

        int startInventorySlot = isCraftingMenu ? 10 : 9;
        int endInventorySlot = isCraftingMenu ? 45 : 44;

        try {
            for (int i = 0; i < count; i++) {
                performCraftingRecipe(client, menu, recipe, isCraftingMenu, startInventorySlot, endInventorySlot);
            }
            return ok("Crafted " + count + " " + recipe + " successfully");
        } catch (Exception e) {
            return fail("Crafting failed: " + e.getMessage());
        }
    }

    private static void performCraftingRecipe(
        Minecraft client,
        net.minecraft.world.inventory.AbstractContainerMenu menu,
        String recipe,
        boolean isCraftingMenu,
        int startInventorySlot,
        int endInventorySlot
    ) throws Exception {
        String[] materialSuffixes;
        int[][] gridSlotsList;

        if ("planks".equals(recipe)) {
            materialSuffixes = new String[]{"_log", "_wood", "_stem"};
            gridSlotsList = new int[][]{ {1} };
        } else if ("crafting_table".equals(recipe)) {
            materialSuffixes = new String[]{"_planks"};
            gridSlotsList = new int[][]{
                isCraftingMenu ? new int[]{1, 2, 4, 5} : new int[]{1, 2, 3, 4}
            };
        } else if ("sticks".equals(recipe)) {
            materialSuffixes = new String[]{"_planks"};
            gridSlotsList = new int[][]{
                isCraftingMenu ? new int[]{1, 4} : new int[]{1, 3}
            };
        } else if ("wooden_pickaxe".equals(recipe)) {
            if (!isCraftingMenu) {
                throw new Exception("wooden_pickaxe requires a crafting table");
            }
            materialSuffixes = new String[]{"_planks", "minecraft:stick"};
            gridSlotsList = new int[][]{
                {1, 2, 3},
                {5, 8}
            };
        } else if ("wooden_axe".equals(recipe)) {
            if (!isCraftingMenu) {
                throw new Exception("wooden_axe requires a crafting table");
            }
            materialSuffixes = new String[]{"_planks", "minecraft:stick"};
            gridSlotsList = new int[][]{
                {1, 2, 4},
                {5, 8}
            };
        } else if ("wooden_shovel".equals(recipe)) {
            if (!isCraftingMenu) {
                throw new Exception("wooden_shovel requires a crafting table");
            }
            materialSuffixes = new String[]{"_planks", "minecraft:stick"};
            gridSlotsList = new int[][]{
                {2},
                {5, 8}
            };
        } else {
            throw new Exception("Unknown recipe: " + recipe);
        }

        for (int m = 0; m < materialSuffixes.length; m++) {
            String suffix = materialSuffixes[m];
            int[] slots = gridSlotsList[m];
            int slotsNeeded = slots.length;
            int currentGridIndex = 0;

            while (currentGridIndex < slotsNeeded) {
                int materialSlot = -1;
                if (suffix.contains(":") || suffix.startsWith("minecraft:")) {
                    materialSlot = findItemSlot(menu, suffix, startInventorySlot, endInventorySlot, false);
                } else {
                    materialSlot = findItemSlot(menu, suffix, startInventorySlot, endInventorySlot, true);
                }

                if (materialSlot == -1) {
                    throw new Exception("Missing material for recipe " + recipe + " (searched for " + suffix + ")");
                }

                click(client, materialSlot, 0, ContainerInput.PICKUP);

                ItemStack carried = menu.getCarried();
                int countCarried = carried.getCount();

                if (countCarried == 0) {
                    throw new Exception("Failed to pick up item from slot " + materialSlot);
                }

                while (countCarried > 0 && currentGridIndex < slotsNeeded) {
                    int targetGridSlot = slots[currentGridIndex];
                    click(client, targetGridSlot, 1, ContainerInput.PICKUP);
                    countCarried--;
                    currentGridIndex++;
                }

                if (countCarried > 0) {
                    click(client, materialSlot, 0, ContainerInput.PICKUP);
                }
            }
        }

        click(client, 0, 0, ContainerInput.QUICK_MOVE);
    }

    private static int findItemSlot(net.minecraft.world.inventory.AbstractContainerMenu menu, String query, int startSlot, int endSlot, boolean isSuffix) {
        for (int i = startSlot; i <= endSlot; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty()) {
                String itemId = stack.getItem().toString();
                if (isSuffix) {
                    if (itemId.endsWith(query)) {
                        return i;
                    }
                } else {
                    if (itemId.equals(query)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static void click(Minecraft client, int slot, int button, ContainerInput type) {
        client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot, button, type, client.player);
    }

    private static JsonObject clickSlotByItem(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return fail("client player is not in-world");
        }

        String query = JsonHttp.string(args, "query", "");
        String item = JsonHttp.string(args, "item", "");
        String displayName = JsonHttp.string(args, "displayName", "");
        boolean contains = JsonHttp.bool(args, "contains", true);
        int nth = Math.max(0, JsonHttp.integer(args, "nth", 0));
        int button = JsonHttp.integer(args, "button", 0);
        String type = JsonHttp.string(args, "type", "pickup");

        if (query.isBlank() && item.isBlank() && displayName.isBlank()) {
            return fail("query, item, or displayName is required");
        }

        int seen = 0;
        JsonArray candidates = new JsonArray();
        for (int i = 0; i < player.containerMenu.slots.size(); i++) {
            ItemStack stack = player.containerMenu.slots.get(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (matchesStack(stack, query, item, displayName, contains)) {
                JsonObject candidate = new JsonObject();
                candidate.addProperty("slot", i);
                addStackInfo(candidate, stack);
                candidates.add(candidate);
                if (seen == nth) {
                    JsonObject clickArgs = new JsonObject();
                    clickArgs.addProperty("slot", i);
                    clickArgs.addProperty("button", button);
                    clickArgs.addProperty("type", type);
                    JsonObject clicked = clickSlot(client, clickArgs);
                    JsonObject data = clicked.has("data") && clicked.get("data").isJsonObject()
                            ? clicked.getAsJsonObject("data")
                            : new JsonObject();
                    data.add("matchedCandidates", candidates);
                    addStackInfo(data, stack);
                    return clicked.has("ok") && clicked.get("ok").getAsBoolean()
                            ? ok("matched slot clicked", data)
                            : clicked;
                }
                seen++;
            }
        }

        JsonObject data = new JsonObject();
        data.add("matchedCandidates", candidates);
        return fail("no matching slot found");
    }

    private static JsonObject containerButton(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null || player.connection == null) {
            return fail("client player is not in-world");
        }
        int button = JsonHttp.integer(args, "button", 0);
        int containerId = JsonHttp.integer(args, "containerId", player.containerMenu.containerId);
        player.connection.send(new ServerboundContainerButtonClickPacket(containerId, button));

        JsonObject data = new JsonObject();
        data.addProperty("containerId", containerId);
        data.addProperty("button", button);
        return ok("container button packet sent", data);
    }

    private static void addStackInfo(JsonObject object, ItemStack stack) {
        object.addProperty("item", stack.getItem().toString());
        object.addProperty("count", stack.getCount());
        object.addProperty("hoverName", stack.getHoverName().getString());
        object.addProperty("displayName", stack.getDisplayName().getString());
    }

    private static boolean matchesStack(ItemStack stack, String query, String item, String displayName, boolean contains) {
        if (!item.isBlank() && matchesText(stack.getItem().toString(), item, contains)) {
            return true;
        }
        if (!displayName.isBlank()
                && (matchesText(stack.getHoverName().getString(), displayName, contains)
                || matchesText(stack.getDisplayName().getString(), displayName, contains))) {
            return true;
        }
        if (!query.isBlank()) {
            return matchesText(stack.getItem().toString(), query, contains)
                    || matchesText(stack.getHoverName().getString(), query, contains)
                    || matchesText(stack.getDisplayName().getString(), query, contains);
        }
        return false;
    }

    private static boolean matchesText(String actual, String expected, boolean contains) {
        String normalizedActual = actual.toLowerCase(Locale.ROOT);
        String normalizedExpected = expected.toLowerCase(Locale.ROOT);
        return contains
                ? normalizedActual.contains(normalizedExpected)
                : normalizedActual.equals(normalizedExpected);
    }

    private static JsonObject baritoneStatus() {
        JsonObject data = new JsonObject();
        boolean available = isBaritoneAvailable();
        data.addProperty("loaded", available);
        data.addProperty("fabricModIdLoaded", FabricLoader.getInstance().isModLoaded("baritone"));
        data.addProperty("meteorModIdLoaded", FabricLoader.getInstance().isModLoaded("baritone-meteor"));
        data.addProperty("apiClassPresent", isClassPresent("baritone.api.BaritoneAPI"));
        data.addProperty("commandPrefix", "#");

        if (available) {
            try {
                Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
                Object provider = apiClass.getMethod("getProvider").invoke(null);
                Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
                if (baritone != null) {
                    Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
                    Object mineProcess = baritone.getClass().getMethod("getMineProcess").invoke(baritone);
                    Object followProcess = baritone.getClass().getMethod("getFollowProcess").invoke(baritone);

                    boolean isPathing = (boolean) pathingBehavior.getClass().getMethod("isPathing").invoke(pathingBehavior);
                    boolean isMining = (boolean) mineProcess.getClass().getMethod("isActive").invoke(mineProcess);
                    boolean isFollow = (boolean) followProcess.getClass().getMethod("isActive").invoke(followProcess);

                    data.addProperty("isActive", isPathing || isMining);
                    data.addProperty("isPathing", isPathing);
                    data.addProperty("isMining", isMining);
                    data.addProperty("isFollow", isFollow);
                }
            } catch (Throwable t) {
                data.addProperty("error", "Failed to get detailed Baritone status: " + t.getMessage());
            }
        }
        return ok("baritone status collected", data);
    }

    private static JsonObject baritoneCommand(Minecraft client, JsonObject args) {
        LocalPlayer player = client.player;
        if (player == null) {
            return fail("client player is not in-world");
        }
        if (!isBaritoneAvailable()) {
            return fail("baritone is not available: fabric mod id was not loaded and baritone.api.BaritoneAPI is not on the classpath");
        }

        String command = JsonHttp.string(args, "command", "");
        if (command.isBlank()) {
            return fail("command is required");
        }
        if (!command.startsWith("#")) {
            command = "#" + command;
        }
        player.connection.sendChat(command);
        return ok("baritone command sent");
    }

    private static JsonObject baritoneGoto(Minecraft client, JsonObject args) {
        int x = JsonHttp.integer(args, "x", 0);
        int y = JsonHttp.integer(args, "y", Integer.MIN_VALUE);
        int z = JsonHttp.integer(args, "z", 0);
        JsonObject commandArgs = new JsonObject();
        if (y == Integer.MIN_VALUE) {
            commandArgs.addProperty("command", "goto " + x + " " + z);
        } else {
            commandArgs.addProperty("command", "goto " + x + " " + y + " " + z);
        }
        return baritoneCommand(client, commandArgs);
    }

    private static boolean isBaritoneAvailable() {
        return FabricLoader.getInstance().isModLoaded("baritone")
                || FabricLoader.getInstance().isModLoaded("baritone-meteor")
                || isClassPresent("baritone.api.BaritoneAPI");
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, MineAgentTools.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static Direction direction(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "down" -> Direction.DOWN;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            default -> Direction.UP;
        };
    }

    private static KeyMapping keyByName(Minecraft client, String keyName) {
        String normalized = keyName.toLowerCase(Locale.ROOT);
        for (KeyMapping mapping : client.options.keyMappings) {
            if (matchesKey(mapping, normalized)) {
                return mapping;
            }
        }

        return switch (normalized) {
            case "forward", "up", "key_up" -> client.options.keyUp;
            case "back", "backward", "down", "key_down" -> client.options.keyDown;
            case "left", "key_left" -> client.options.keyLeft;
            case "right", "key_right" -> client.options.keyRight;
            case "jump", "space" -> client.options.keyJump;
            case "sneak", "shift" -> client.options.keyShift;
            case "sprint", "ctrl" -> client.options.keySprint;
            case "attack", "left_click" -> client.options.keyAttack;
            case "use", "right_click" -> client.options.keyUse;
            case "drop", "q" -> client.options.keyDrop;
            case "inventory", "e" -> client.options.keyInventory;
            case "swap", "swap_hands", "f" -> client.options.keySwapOffhand;
            default -> null;
        };
    }

    private static boolean matchesKey(KeyMapping mapping, String normalized) {
        if (mapping.getName().equalsIgnoreCase(normalized)) {
            return true;
        }
        if (mapping.saveString().equalsIgnoreCase(normalized)) {
            return true;
        }
        if (mapping.getCategory().id().toString().equalsIgnoreCase(normalized)) {
            return true;
        }
        if (mapping.getCategory().label().getString().equalsIgnoreCase(normalized)) {
            return true;
        }
        return mapping.getTranslatedKeyMessage().getString().equalsIgnoreCase(normalized);
    }

    private static JsonObject ok(String message) {
        return ok(message, new JsonObject());
    }

    private static JsonObject ok(String message, JsonObject data) {
        JsonObject root = new JsonObject();
        root.addProperty("ok", true);
        root.addProperty("message", message);
        root.add("data", data);
        return root;
    }

    private static JsonObject fail(String message) {
        JsonObject root = new JsonObject();
        root.addProperty("ok", false);
        root.addProperty("error", message);
        return root;
    }
}
