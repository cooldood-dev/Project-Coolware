package coolware.util.shader.base;

import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL20;

public class ShaderUniforms {
    public ShaderUniforms() {
    }

    public static void uniformFB(int program, String name, FloatBuffer floatBuffer) {
        GL20.glUniform1(getLocation(program, name), floatBuffer);
    }

    public static void uniform1i(int program, String name, int value) {
        GL20.glUniform1i(getLocation(program, name), value);
    }

    public static void uniform2i(int program, String name, int v1, int v2) {
        GL20.glUniform2i(getLocation(program, name), v1, v2);
    }

    public static void uniform1f(int program, String name, float value) {
        GL20.glUniform1f(getLocation(program, name), value);
    }

    public static void uniform2f(int program, String name, float v1, float v2) {
        GL20.glUniform2f(getLocation(program, name), v1, v2);
    }

    public static void uniform3f(int program, String name, float v1, float v2, float v3) {
        GL20.glUniform3f(getLocation(program, name), v1, v2, v3);
    }

    public static void uniform4f(int program, String name, float v1, float v2, float v3, float v4) {
        GL20.glUniform4f(getLocation(program, name), v1, v2, v3, v4);
    }

    private static int getLocation(int program, String name) {
        return GL20.glGetUniformLocation(program, name);
    }
}
