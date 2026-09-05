package com.github.cooldood.utils.render.draggable;

import com.github.cooldood.Main;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderTickEvent;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.ScreenUtil;
import com.github.cooldood.utils.minecraft.ChatUtil;
import com.github.cooldood.utils.render.RenderUtil;
import com.github.cooldood.utils.tenacity.render.blur.KawaseBloom;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.gui.GuiChat;
import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class DraggableRenderer {
    public static ArrayList<Draggable> draggables = new ArrayList<>();

    private static Draggable dragging = null;
    private static Rectangle draggingCoords = null;

    private final static String draggablesPath = Main.extraSavedFeaturesPath;
    private final static String draggablesFile = "draggables" + Main.configExtension;

    public static boolean isBloom = false;

    @SubscribeEvent
    public static void drawDraggables(RenderTickEvent event) {
        
        // PostProcessing Pass
        if (ModuleManager.isEnabled(com.github.cooldood.modules.impl.client.PostProcessing.class)) {
            isBloom = true;
            KawaseBloom.framebuffer = com.github.cooldood.utils.render.RenderUtil.createFrameBuffer(KawaseBloom.framebuffer, true);
            KawaseBloom.framebuffer.framebufferClear();
            KawaseBloom.framebuffer.bindFramebuffer(false);

            for (Draggable draggable : draggables) {
                if (!shouldRender(draggable)) continue;
                try {
                    int renderX = (int) (draggable.x * C.res().getScaledWidth());
                    int renderY = (int) (draggable.y * C.res().getScaledHeight());
                    if (draggable.anchor == Draggable.Anchor.RIGHT) renderX -= draggable.width;
                    GL11.glPushMatrix();
                    GL11.glTranslated(renderX, renderY, 0);
                    draggable.render.call();
                    GL11.glPopMatrix();
                } catch (Exception e) {}
            }
            
            KawaseBloom.framebuffer.unbindFramebuffer();
            int iter = (int) Math.max(1, com.github.cooldood.modules.impl.client.PostProcessing.iterations);
            int off = (int) Math.max(1, com.github.cooldood.modules.impl.client.PostProcessing.offset);
            KawaseBloom.renderBlur(KawaseBloom.framebuffer.framebufferTexture, iter, off);
            C.mc.getFramebuffer().bindFramebuffer(false);
            isBloom = false;
        }


        // Normal Pass
        for (Draggable draggable : draggables) {
            if (!shouldRender(draggable)) continue;

            try {
                int renderX = (int) (draggable.x * C.res().getScaledWidth());
                int renderY = (int) (draggable.y * C.res().getScaledHeight());
                
                if (draggable.anchor == Draggable.Anchor.RIGHT) {
                    renderX -= draggable.width;
                }

                GL11.glPushMatrix();
                GL11.glTranslated(renderX, renderY, 0);
                double[] size = draggable.render.call();
                GL11.glPopMatrix();
                draggable.width = size[0];
                draggable.height = size[1];

                if (canDrag()) {
                    boolean isHovered = ScreenUtil.getMouseX() >= renderX && ScreenUtil.getMouseX() <= renderX + size[0] 
                                     && ScreenUtil.getMouseY() >= renderY && ScreenUtil.getMouseY() <= renderY + size[1];

                    draggingCoords = dragging == null ? new Rectangle((int) ScreenUtil.getMouseX() - renderX, (int) ScreenUtil.getMouseY() - renderY) : draggingCoords;

                    if (dragging == draggable) {
                        GL11.glPushMatrix();
                        GL11.glTranslated(renderX, renderY, 0);
                        RenderUtil.drawRectOutline(0, 0, size[0], size[1], 1, Color.WHITE);
                        GL11.glPopMatrix();

                        double newX = ScreenUtil.getMouseX() - draggingCoords.width;
                        if (draggable.anchor == Draggable.Anchor.RIGHT) newX += size[0];
                        draggable.x = newX / C.res().getScaledWidth();
                        
                        draggable.y = (ScreenUtil.getMouseY() - draggingCoords.height) / C.res().getScaledHeight();
                    }

                    dragging = Mouse.isButtonDown(0) ? dragging == null && isHovered ? draggable : dragging : null;
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean canDrag() {
        return C.mc.currentScreen instanceof GuiChat;
    }

    public static void saveDraggables() {
        HashMap<String, double[]> draggingJSON = new HashMap<>();

        File file = new File(draggablesPath + "/" + draggablesFile);
        if (file.exists()) {
            try {
                draggingJSON = C.gson.fromJson(FileUtils.readFileToString(file), HashMap.class);
            } catch (Exception e) {
                e.printStackTrace();
                ChatUtil.prefixMessage("Failed to read previous positions json");
            }
        }

        if (draggingJSON == null) draggingJSON = new HashMap<>();

        for (Draggable draggable : draggables) {
            draggingJSON.put(draggable.id, new double[] {draggable.x, draggable.y});
        }

        try {
            Files.createDirectories(Paths.get(draggablesPath));
            Files.write(file.toPath(), C.gson.toJson(draggingJSON).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadDraggingPositions() {
        File file = new File(draggablesPath + "/" + draggablesFile);
        if (!file.exists()) {
            System.err.println("No draggables config found!");
            return;
        }

        try {
            HashMap<String, ArrayList<Double>>  draggingJSON = C.gson.fromJson(FileUtils.readFileToString(file), HashMap.class);

            for (Draggable draggable : draggables) {
                if (draggingJSON.containsKey(draggable.id)) {
                    ArrayList<Double> positions = draggingJSON.get(draggable.id);
                    draggable.x = Double.parseDouble(String.valueOf(positions.get(0)));
                    draggable.y = Double.parseDouble(String.valueOf(positions.get(1)));
                }
                else {
                    System.err.println("Dragable config for " + draggable.id + " not found!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ChatUtil.prefixMessage("Failed to read previous positions json");
        }
    }

    private static boolean shouldRender(Draggable draggable) {
        return (draggable.conditions.test(null) || C.mc.currentScreen instanceof GuiChat)
                && draggable.canRender.test(null);
    }
}
