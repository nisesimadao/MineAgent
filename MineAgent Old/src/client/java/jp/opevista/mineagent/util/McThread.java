package jp.opevista.mineagent.util;

import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class McThread {
    private McThread() {
    }

    public static <T> CompletableFuture<T> supplyOnClient(Supplier<T> supplier) {
        Minecraft client = Minecraft.getInstance();
        CompletableFuture<T> future = new CompletableFuture<>();
        client.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
