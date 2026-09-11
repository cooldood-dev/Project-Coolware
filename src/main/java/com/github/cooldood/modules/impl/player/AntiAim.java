package com.github.cooldood.modules.impl.player;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RotationEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.utils.client.MathUtil;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;

@RegisterModule(
        name = "Anti Aim",
        description = "Provides Anti Aim functionality for the client.",
        category = Category.PLAYER,
        dangerous = true
)
public class AntiAim extends Module {
    // always goes first
    @SubscribeEvent(priority = 1)
    public static void onPlayerUpdate(RotationEvent event) {
        event.rotation = RotationUtil.applyGcd(
                new RotationUtil.Rotation(
                        MathUtil.getRandomInRange(-90, 90),
                        PlayerUtil.lastRotation().yaw + MathUtil.getRandomInRange(-180, 180)
                )
        );
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
