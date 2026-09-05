package com.github.cooldood.mixins.net.minecraft.network;

import com.github.cooldood.bridge.net.minecraft.network.NetworkManagerBridge;
import com.github.cooldood.events.Bus;
import com.github.cooldood.events.impl.PacketEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public abstract class NetworkManagerMixin implements NetworkManagerBridge {

    @Shadow
    private Channel channel;

    @Shadow
    private INetHandler packetListener;

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        try {
            if (Bus.post(new PacketEvent.Send(packet))) {
                ci.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void onChannelReadHead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        try {
            if (Bus.post(new PacketEvent.Receive(packet)))
                ci.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // minecraft code called in channelRead0, cause idk what "context" is
    @Override
    public void bridge$processPacket(Packet<INetHandler> packet) {
        // maybe post receive event here? probably not.

        if (this.channel.isOpen())
        {
            try
            {
                packet.processPacket(this.packetListener);
            }
            catch (ThreadQuickExitException var4)
            {
                ;
            }
        }
    }
}