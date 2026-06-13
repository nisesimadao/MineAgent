package jp.opevista.mineagent.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "pauseOnLostFocus", at = @At("HEAD"), cancellable = true)
    private void onPauseOnLostFocus(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
