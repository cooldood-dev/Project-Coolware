package com.github.cooldood.mixins.net.minecraftforge.fml.common.network.handshake;

import com.github.cooldood.Main;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(FMLHandshakeMessage.ModList.class)
public class FMLHandshakeMessage$ModListMixin {

    @Shadow
    private Map<String, String> modTags;

    // remove mod id. we do NOT want a mod id.
    @Inject(method = "<init>(Ljava/util/List;)V", at = @At("RETURN"))
    private void removeModID(List<ModContainer> modList, CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isIntegratedServerRunning()) {
            /*
            for (ModContainer mod : modList) {
                modTags.put(mod.getModId(), mod.getVersion());
            }
            */
            // we remove the modid after its added

            modTags.remove(Main.MOD_ID);
        }
    }

}