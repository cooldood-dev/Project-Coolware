package com.github.cooldood.bridge.net.minecraft.network.play.server;

public interface S12PacketEntityVelocityBridge {
    static S12PacketEntityVelocityBridge from(Object instance) {
        return (S12PacketEntityVelocityBridge) instance;
    }

    void bridge$setMotionX(double motionXIn);
    void bridge$setMotionY(double motionYIn);
    void bridge$setMotionZ(double motionZIn);
}
