package jp.opevista.mineagent.bridge;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Locale;

public final class CombatMode {
    private static boolean enabled;
    private static int targetEntityId = -1;
    private static String targetName = "";
    private static double range = 4.2;
    private static boolean includePlayers;
    private static boolean bunnyHop = true;
    private static boolean sprintTap = true;
    private static boolean autoSelectSword = true;
    private static int sprintTapTicks;
    private static int jumpAttackWindowTicks;
    private static int lastAttackTicks;
    private static String message = "Combat mode is idle.";

    static {
        ClientTickEvents.END_CLIENT_TICK.register(CombatMode::tick);
    }

    private CombatMode() {
    }

    public static JsonObject configure(Minecraft client, JsonObject args) {
        if (args.has("enabled")) {
            enabled = JsonHttp.bool(args, "enabled", enabled);
        }
        targetEntityId = JsonHttp.integer(args, "targetEntityId", targetEntityId);
        targetName = JsonHttp.string(args, "targetName", targetName).trim();
        range = clamp(JsonHttp.number(args, "range", range), 2.0, 6.0);
        includePlayers = JsonHttp.bool(args, "includePlayers", includePlayers);
        bunnyHop = JsonHttp.bool(args, "bunnyHop", bunnyHop);
        sprintTap = JsonHttp.bool(args, "sprintTap", sprintTap);
        autoSelectSword = JsonHttp.bool(args, "autoSelectSword", autoSelectSword);

        if (!enabled) {
            releaseCombatKeys(client);
            message = "Combat mode disabled.";
        } else {
            message = "Combat mode enabled.";
        }
        return status(client);
    }

    public static JsonObject stop(Minecraft client) {
        enabled = false;
        targetEntityId = -1;
        targetName = "";
        sprintTapTicks = 0;
        jumpAttackWindowTicks = 0;
        releaseCombatKeys(client);
        message = "Combat mode stopped.";
        return status(client);
    }

    public static JsonObject status(Minecraft client) {
        JsonObject data = new JsonObject();
        data.addProperty("enabled", enabled);
        data.addProperty("targetEntityId", targetEntityId);
        data.addProperty("targetName", targetName);
        data.addProperty("range", range);
        data.addProperty("includePlayers", includePlayers);
        data.addProperty("bunnyHop", bunnyHop);
        data.addProperty("sprintTap", sprintTap);
        data.addProperty("autoSelectSword", autoSelectSword);
        data.addProperty("sprintTapTicks", sprintTapTicks);
        data.addProperty("jumpAttackWindowTicks", jumpAttackWindowTicks);
        data.addProperty("lastAttackTicks", lastAttackTicks);
        data.addProperty("message", message);

        LivingEntity target = findTarget(client);
        if (target != null) {
            JsonObject targetData = new JsonObject();
            targetData.addProperty("id", target.getId());
            targetData.addProperty("name", target.getName().getString());
            targetData.addProperty("type", target.getType().toString());
            targetData.addProperty("health", target.getHealth());
            LocalPlayer player = client.player;
            if (player != null) {
                targetData.addProperty("distance", Math.sqrt(player.distanceToSqr(target)));
            }
            data.add("currentTarget", targetData);
        }
        return data;
    }

    private static void tick(Minecraft client) {
        if (!enabled) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            message = "Waiting for player to join world.";
            return;
        }

        LivingEntity target = findTarget(client);
        if (target == null) {
            releaseCombatKeys(client);
            message = "No valid combat target.";
            return;
        }

        face(player, target);
        driveMovement(client, player);
        selectSword(player);

        double distance = Math.sqrt(player.distanceToSqr(target));
        float attackStrength = player.getAttackStrengthScale(0.0F);
        lastAttackTicks++;

        if (distance > range) {
            message = "Closing distance to target.";
            return;
        }

        if (attackStrength >= 0.96F) {
            prepareCritical(player);
        }

        if (shouldAttack(player, attackStrength)) {
            if (sprintTap) {
                client.options.keySprint.setDown(false);
                sprintTapTicks = 2;
            }
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            lastAttackTicks = 0;
            jumpAttackWindowTicks = 0;
            message = "Attack sent at full cooldown.";
        } else {
            message = "Tracking target; waiting for cooldown/critical window.";
        }
    }

    private static LivingEntity findTarget(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return null;
        }

        if (targetEntityId >= 0) {
            Entity entity = client.level.getEntity(targetEntityId);
            if (entity instanceof LivingEntity living && isValidTarget(player, living)) {
                return living;
            }
            return null;
        }

        String normalizedTargetName = targetName.toLowerCase(Locale.ROOT);
        double searchRange = Math.max(range + 2.0, 8.0);
        return client.level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(searchRange),
                        entity -> isValidTarget(player, entity)
                                && (normalizedTargetName.isEmpty()
                                || entity.getName().getString().toLowerCase(Locale.ROOT).contains(normalizedTargetName))
                )
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static boolean isValidTarget(LocalPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof Player && !includePlayers && targetEntityId < 0 && targetName.isBlank()) {
            return false;
        }
        return player.distanceToSqr(entity) <= 64.0;
    }

    private static void face(LocalPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dy = target.getEyeY() - player.getEyeY();
        double dz = target.getZ() - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    private static void driveMovement(Minecraft client, LocalPlayer player) {
        client.options.keyUp.setDown(true);
        if (sprintTapTicks > 0) {
            sprintTapTicks--;
        } else {
            client.options.keySprint.setDown(true);
        }

        if (bunnyHop && player.onGround()) {
            player.jumpFromGround();
        }
    }

    private static void selectSword(LocalPlayer player) {
        if (!autoSelectSword) {
            return;
        }

        int bestSlot = -1;
        int bestRank = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int rank = swordRank(stack.getItem().toString());
            if (rank > bestRank) {
                bestRank = rank;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0) {
            player.getInventory().setSelectedSlot(bestSlot);
        }
    }

    private static int swordRank(String itemId) {
        if (!itemId.endsWith("_sword")) {
            return -1;
        }
        if (itemId.contains("netherite")) {
            return 6;
        }
        if (itemId.contains("diamond")) {
            return 5;
        }
        if (itemId.contains("iron")) {
            return 4;
        }
        if (itemId.contains("stone")) {
            return 3;
        }
        if (itemId.contains("golden")) {
            return 2;
        }
        if (itemId.contains("wooden")) {
            return 1;
        }
        return 0;
    }

    private static void prepareCritical(LocalPlayer player) {
        if (player.onGround()) {
            player.jumpFromGround();
            jumpAttackWindowTicks = 6;
        } else if (jumpAttackWindowTicks > 0) {
            jumpAttackWindowTicks--;
        }
    }

    private static boolean shouldAttack(LocalPlayer player, float attackStrength) {
        if (attackStrength < 0.96F) {
            return false;
        }
        if (jumpAttackWindowTicks <= 0) {
            return true;
        }
        return !player.onGround() && player.getDeltaMovement().y < -0.02;
    }

    private static void releaseCombatKeys(Minecraft client) {
        release(client.options.keyUp);
        release(client.options.keySprint);
    }

    private static void release(KeyMapping key) {
        key.setDown(false);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
