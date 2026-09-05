package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.minecraft.TimerUtil;

@RegisterModule(
        name = "Timer",
        description = "Speeds up or slows down the game timer.",
        category = Category.PLAYER,
        dangerous = true
)
public class Timer extends Module {
    @RegisterSubModule(name = "Timer Speed", min = 0.01, max = 10)
    public static float timerSpeed = 1;

    public static float prevTimer = 1;

    @SubscribeEvent
    public static void onPlayerUpdate(PlayerUpdateEvent event) {
        if (timerSpeed != prevTimer) {
            TimerUtil.popTimer(prevTimer);
            TimerUtil.pushTimer(timerSpeed);

            prevTimer = timerSpeed;
        }
    }

    @Override
    public String arrayListExtraInfo() {
        return "" + timerSpeed;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
        TimerUtil.popTimer(timerSpeed);
        prevTimer = -1;
    }
}
