package coolware.util.shader.base;

import coolware.util.shader.ShaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class RiseShaderProgram {
    private final int programId;

    public RiseShaderProgram(String fragmentShader, String vertexShader) {
        this.programId = ShaderUtil.createShader(fragmentShader, vertexShader);
    }

    public static void drawQuad(double x, double y, double width, double height) {
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2d(x, y);
        GL11.glEnd();
    }

    public static void drawScaledQuad() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        drawQuad(0.0, 0.0, sr.getScaledWidth_double(), sr.getScaledHeight_double());
    }

    public void start() {
        GL20.glUseProgram(this.programId);
    }

    public static void stop() {
        GL20.glUseProgram(0);
    }

    public int getProgramId() {
        return this.programId;
    }
}
