package com.github.cooldood.utils.client;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PlayerUpdateEvent;
import com.github.cooldood.utils.render.RenderUtil;
import lombok.Setter;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

public class ScreenUtil {
    @Setter
    private static GuiScreen guiToDisplay = null;

    @SubscribeEvent
    public static void onPlayerTick(PlayerUpdateEvent event) {
        if (guiToDisplay != null) {
            C.mc.displayGuiScreen(guiToDisplay);
            guiToDisplay = null;
        }
    }

    public static double getMouseX() {
        return (double) Mouse.getX() / C.res().getScaleFactor();
    }

    public static double getMouseY() {
        return (double) (C.mc.displayHeight - Mouse.getY()) / C.res().getScaleFactor();
    }

    public static int[] fixMousePos(double mouseX, double mouseY) {
        float[] translations = RenderUtil.getCurrentTranslation();

        mouseX -= translations[0];
        mouseY -= translations[1];

        mouseX /= translations[3];
        mouseY /= translations[4];

        return new int[] {(int) mouseX, (int) mouseY};
    }

    public static boolean isMouseOver(double x, double y, double width, double height, double mouseX, double mouseY) {
        if (width < 0) {
            width = -width;
            x -= width;
        }
        if (height < 0) {
            height = -height;
            y -= height;
        }

        int[] mousePos = fixMousePos(mouseX, mouseY);

        return mousePos[0] >= x && mousePos[1] >= y && mousePos[0] <= x + width && mousePos[1] <= y + height;
    }
}
