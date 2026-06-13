package jp.opevista.mineagent.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public final class InventoryTool implements Tool {
    @Override
    public String name() {
        return "get_inventory";
    }

    @Override
    public String description() {
        return "Return a compact inventory listing.";
    }

    @Override
    public JsonObject schema() {
        return new JsonObject();
    }

    @Override
    public ToolResult execute(JsonObject args, ToolExecutionContext context) {
        LocalPlayer player = context.client().player;
        JsonObject data = new JsonObject();
        JsonArray items = new JsonArray();
        if (player == null) {
            data.add("items", items);
            return ToolResult.ok("client player is not in-world", data);
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                JsonObject item = new JsonObject();
                item.addProperty("slot", i);
                item.addProperty("item", stack.getItem().toString());
                item.addProperty("count", stack.getCount());
                items.add(item);
            }
        }
        data.add("items", items);
        data.addProperty("selectedSlot", player.getInventory().getSelectedSlot());
        return ToolResult.ok("inventory collected", data);
    }
}
