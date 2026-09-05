package com.github.cooldood.mixins.net.minecraft.client.gui;

import com.github.cooldood.utils.alts.Login;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(GuiDisconnected.class)
public class GuiDisconnectedMixin {
    @Shadow private IChatComponent message;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Login.Alt alt = Login.getAlt();
        if (alt == null) return;

        alt.json = alt.getUpdatedJson();

        String time = "";

        if (message.getUnformattedText().contains("Your account has been blocked.")) {
            alt.json.put("unbanDate", "never");
            Login.updateAltJSON(alt.uuid, alt.json);
        }
        else if (message.getUnformattedText().contains("You are temporarily blocked for ")) {
            time = message.getUnformattedText().split("You are temporarily blocked for ")[1].split(" from")[0];
        }
        else if (message.getUnformattedText().contains("You are temporarily banned for ")) {
            time = message.getUnformattedText().split("You are temporarily banned for ")[1].split(" from")[0];
        }

        if (time.isEmpty()) return;

        String[] timeSplit = time.replaceAll("[^0-9 ]", "").split(" ");
        int seconds = 0;
        // assumes seconds:minutes:hours:days:years
        int[] multi = new int[] { 1, 60, 3600, 86400, 31536000 };
        for (int i = 0; i < timeSplit.length; i++) {
            int multiIndex = (timeSplit.length-(multi.length-timeSplit.length))-i;
            // if u got decades or millis or whatever, quit.
            if (multiIndex > multi.length-1 || multiIndex < 0) continue;

            seconds += Integer.parseInt(timeSplit[i]) * multi[multiIndex];
        }

        alt.json.put("unbanDate", String.valueOf(Instant.now().plusSeconds(seconds)));
        Login.updateAltJSON(alt.uuid, alt.json);
    }
}
