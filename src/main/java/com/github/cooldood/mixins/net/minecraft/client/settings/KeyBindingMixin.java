package com.github.cooldood.mixins.net.minecraft.client.settings;

import com.github.cooldood.bridge.net.minecraft.client.settings.KeyBindingBridge;
import com.github.cooldood.utils.minecraft.MovementUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.IntHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements KeyBindingBridge {
    @Shadow private boolean pressed;

    @Shadow
    private int pressTime;

    @Shadow
    @Final
    private static IntHashMap<KeyBinding> hash;

    @Shadow
    private int keyCode;

    @Override
    public void bridge$setDown(boolean pressed) {
        this.pressed = pressed;
        this.pressTime = pressed ? 1 : 0;
    }

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    public void isKeyDown(CallbackInfoReturnable<Boolean> cir) {
        KeyBinding keyBinding = hash.lookup(this.keyCode);
        if (MovementUtil.getOverriddenKeybinds().containsKey(keyBinding)) {
            cir.setReturnValue(MovementUtil.getOverriddenKeybinds().get(keyBinding));
        }
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    public void isPressed(CallbackInfoReturnable<Boolean> cir) {
        KeyBinding keyBinding = hash.lookup(this.keyCode);
        if (MovementUtil.getOverriddenKeybinds().containsKey(keyBinding)) {
            cir.setReturnValue(MovementUtil.getOverriddenKeybinds().get(keyBinding));
        }
    }
}
