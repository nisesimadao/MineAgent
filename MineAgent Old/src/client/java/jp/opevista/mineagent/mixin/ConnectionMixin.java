package jp.opevista.mineagent.mixin;

import jp.opevista.mineagent.network.PacketDirection;
import jp.opevista.mineagent.network.PacketGateway;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.Connection")
public abstract class ConnectionMixin {
    @Inject(method = "send", at = @At("HEAD"))
    private void mineagent$logSend(Object packet, CallbackInfo ci) {
        PacketGateway.capture(PacketDirection.C2S, packet);
    }

    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void mineagent$logRead(Object context, Object packet, CallbackInfo ci) {
        PacketGateway.capture(PacketDirection.S2C, packet);
    }
}
