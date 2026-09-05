package com.github.cooldood.modules.impl.render;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.RenderWorldEvent;
import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.minecraft.TargetUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.Render3dUtil;
import com.github.cooldood.utils.render.RenderUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RegisterModule(
        name = "Nametags",
        description = "Provides Nametags functionality for the client.",
        category = Category.RENDER
)
public class Nametags extends Module {
    @RegisterSubModule(name = "Background")
    public static boolean background = false;
    @RegisterSubModule(name = "Background Colour", parent = "Background")
    public static Color backgroundColour = new Color(22,22,22,100);

    @RegisterSubModule(name = "Health")
    public static boolean health = false;


    @RegisterSubModule(name = "Scale With Distance")
    public static boolean scaleWithDistance = true;

    @RegisterSubModule(name = "Size Multiplier", min = 0, max = 2)
    public static float sizeMultiplier = 1;

    private static final double baseSizeMulti = 0.001;

    @SubscribeEvent
    public static void onRenderWorldEvent(RenderWorldEvent event) {
        Vec3 lerpedPlayerPos = Render3dUtil.lerpVec(new Vec3(C.p().prevPosX, C.p().prevPosY, C.p().prevPosZ), C.p().getPositionVector(), event.partialTicks);

        double sizeMulti = baseSizeMulti*sizeMultiplier;

        List<EntityLivingBase> sortedEntities = TargetUtil.getAllValidTargets(true).stream().sorted(
                Comparator.comparingDouble(e -> -e.getDistanceToEntity(C.p()))
        ).collect(Collectors.toList());

        for (EntityLivingBase entity : sortedEntities) {
            GL11.glPushMatrix();
            Vec3 lerpedEntityPos = Render3dUtil.lerpVec(new Vec3(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ), entity.getPositionVector(), event.partialTicks);

            RenderUtil.glTranslate(lerpedEntityPos.subtract(lerpedPlayerPos).add(new Vec3(0, entity.height + 0.5, 0)));

            Render3dUtil.rotateToPlayer(true);
            // idk why its upside-down, mb
            GL11.glRotatef(180, 0, 0, 1);

            double distanceToPlayer = lerpedPlayerPos.distanceTo(lerpedEntityPos);
            double scale = scaleWithDistance ? MathHelper.clamp_double(sizeMulti*distanceToPlayer, sizeMulti*5, sizeMulti*50) : sizeMulti*10;
            GL11.glScaled(scale, scale, scale);

            String entityName = entity.getDisplayName().getFormattedText();
            int fontSize = 50;

            float healthNumber = entity.getHealth() + TargetUtil.getAbsorption(entity);
            float healthPercentage = healthNumber / entity.getMaxHealth();
            String healthString = String.format("%.1f", healthNumber) + " ";

            float healthStringWidth = health ? FontUtil.getStringWidth(healthString, fontSize) : 0;
            float stringWidth = healthStringWidth + FontUtil.getStringWidth(entityName, fontSize);

            if (background) {
                RenderUtil.drawRect(-stringWidth / 2, -fontSize / 3f, stringWidth, fontSize, backgroundColour);
            }

            if (health) {
                FontUtil.drawString(healthString, -stringWidth / 2, -fontSize/2f, fontSize, RenderUtil.getProgressColour(healthPercentage), true);
            }

            FontUtil.drawString(entity.getDisplayName().getFormattedText(), -stringWidth / 2 + healthStringWidth, -fontSize/2f, fontSize, Color.WHITE, true);

            GL11.glPopMatrix();
        }
    }

    public static boolean shouldHideNametag(EntityLivingBase entityLivingBase) {
        return ModuleManager.isEnabled(Nametags.class) && TargetUtil.isValidTarget(entityLivingBase, true);
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
