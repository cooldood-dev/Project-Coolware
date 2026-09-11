package com.github.cooldood.mixins.net.minecraft.network.play.server;

import com.github.cooldood.bridge.net.minecraft.network.play.server.S12PacketEntityVelocityBridge;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(S12PacketEntityVelocity.class)
public class S12PacketEntityVelocityMixin implements S12PacketEntityVelocityBridge {
    @Shadow
    private int motionX;

    @Shadow
    private int motionY;

    @Shadow
    private int motionZ;

    @Override
    public void bridge$setMotionX(double motionXIn) {
        this.motionX = (int) (MathHelper.clamp_double(motionXIn, -3.9D, 3.9D) * 8000.0D);
    }

    @Override
    public void bridge$setMotionY(double motionYIn) {
        this.motionY = (int) (MathHelper.clamp_double(motionYIn, -3.9D, 3.9D) * 8000.0D);
    }

    @Override
    public void bridge$setMotionZ(double motionZIn) {
        this.motionZ = (int) (MathHelper.clamp_double(motionZIn, -3.9D, 3.9D) * 8000.0D);
    }
}
