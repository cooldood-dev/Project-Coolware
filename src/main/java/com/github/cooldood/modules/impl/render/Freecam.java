package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.MovementInputEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.PlayerUtil;
import com.github.cooldood.utils.minecraft.RotationUtil;
import org.lwjgl.input.Keyboard;

@RegisterModule(
        name = "Freecam",
        description = "Allows camera movement outside the player body.",
        category = Category.RENDER
)
// todo: terrible, fix.
public class Freecam extends Module {
    @RegisterSubModule(name = "Horizontal Speed", max = 10)
    public static float horizontalSpeed = 2;
    @RegisterSubModule(name = "Vertical Speed", max = 10)
    public static float verticalSpeed = 2;

    @RegisterSubModule(name = "Allow Interact", description = "Allows you to interact with the world while in freecam")
    public static boolean allowInteract = false;

    private static int perspectiveBefore = 0;

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldEvent event) {
        C.mc.gameSettings.thirdPersonView = 1;
    }

    @SubscribeEvent
    public static void onMovementInputEvent(MovementInputEvent event) {
        // make sure we can see through walls!
        PlayerUtil.noClipRender = true;

        if (C.mc.currentScreen != null) return;

        float y = Keyboard.isKeyDown(C.mc.gameSettings.keyBindJump.getKeyCode()) ? verticalSpeed : 0;
        y += Keyboard.isKeyDown(C.mc.gameSettings.keyBindSneak.getKeyCode()) ? -verticalSpeed : 0;

        if (PlayerUtil.fakeCameraPos == null)
            PlayerUtil.setFakeCameraPos(C.p().getPositionVector());
        if (PlayerUtil.fakeRotation == null)
            PlayerUtil.fakeRotation = PlayerUtil.realRotation = RotationUtil.getCurrentClientRotation();

        double x = event.movementInput.moveForward * Math.sin(Math.toRadians(-PlayerUtil.fakeRotation.yaw)) + event.movementInput.moveStrafe * Math.cos(Math.toRadians(-PlayerUtil.fakeRotation.yaw));
        double z = event.movementInput.moveForward * Math.cos(Math.toRadians(-PlayerUtil.fakeRotation.yaw)) - event.movementInput.moveStrafe * Math.sin(Math.toRadians(-PlayerUtil.fakeRotation.yaw));

        // reset all pressed
        event.movementInput.moveForward = event.movementInput.moveStrafe = 0;
        event.movementInput.sneak = event.movementInput.jump = false;

        // move camera!
        PlayerUtil.setFakeCameraPos(PlayerUtil.fakeCameraPos.addVector(x*horizontalSpeed, y, z*horizontalSpeed));
    }

    @Override
    protected void onEnable() {
        if (!C.isInGame()) {
            this.toggle();
            return;
        }

        perspectiveBefore = C.mc.gameSettings.thirdPersonView;
        C.mc.gameSettings.thirdPersonView = 1;

        // get ready to rumble
        PlayerUtil.setFakeCameraPos(C.p().getPositionVector());
        PlayerUtil.realPos = C.p().getPositionVector();

        PlayerUtil.fakeRotation = PlayerUtil.realRotation = RotationUtil.getCurrentClientRotation();
    }

    @Override
    protected void onDisable() {
        if (!C.isInGame()) return;

        // reset fake camera
        PlayerUtil.setFakeCameraPos(null);
        PlayerUtil.fakeRotation = null;

        // no more seeing through walls, not on constantly because i think it decreases fps
        PlayerUtil.noClipRender = false;

        C.mc.renderGlobal.loadRenderers();
        C.mc.gameSettings.thirdPersonView = perspectiveBefore;
    }
}
