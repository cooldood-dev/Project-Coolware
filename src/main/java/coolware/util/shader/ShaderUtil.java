package coolware.util.shader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL20;

public class ShaderUtil {
    public static int createShader(String fragmentShader, String vertexShader) {
        String fragSource = getShaderResource(fragmentShader);
        String vertSource = getShaderResource(vertexShader);
        if (fragSource != null && vertSource != null) {
            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(frag, fragSource);
            GL20.glShaderSource(vert, vertSource);
            GL20.glCompileShader(frag);
            GL20.glCompileShader(vert);
            if (!compileShader(frag) || !compileShader(vert)) {
                return -1;
            }
            int program = GL20.glCreateProgram();
            GL20.glAttachShader(program, frag);
            GL20.glAttachShader(program, vert);
            GL20.glValidateProgram(program);
            GL20.glLinkProgram(program);
            GL20.glDeleteShader(frag);
            GL20.glDeleteShader(vert);
            return program;
        }
        System.err.println("An error occurred whilst creating shader: " + fragmentShader + " / " + vertexShader);
        return -1;
    }

    private static boolean compileShader(int shader) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 1) {
            return true;
        }
        String log = GL20.glGetShaderInfoLog(shader, 8192);
        System.err.println("Error while compiling shader:\n" + log);
        return false;
    }

    public static String getShaderResource(String name) {
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("rise/shader/" + name)).getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            System.err.println("An error occurred while getting shader resource: " + name);
            e.printStackTrace();
            return null;
        }
    }
}
