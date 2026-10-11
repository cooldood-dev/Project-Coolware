package coolware;

import coolware.command.CommandManager;
import coolware.config.ConfigManager;
import coolware.font.FontManager;
import coolware.gui.clickgui.novoline.NovolineDropdownGUI;
import coolware.gui.hud.HUDOverlay;
import coolware.module.ModuleManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class Coolware {
    public static final String NAME = "Coolware";
    public static final String VERSION = "1.8";

    private static Coolware instance;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private NovolineDropdownGUI clickGuiScreen;
    private coolware.util.shader.ShaderRenderManager shaderRenderManager;

    public static Coolware getInstance() {
        if (instance == null) {
            instance = new Coolware();
        }
        return instance;
    }

    public void init() {
        System.out.println("[" + NAME + "] Initializing " + NAME + " v" + VERSION + "...");
        // Initialize Core Security Engine & Ratchet
        byte[] defaultEntropy = new byte[32];
        java.util.Arrays.fill(defaultEntropy, (byte) 0x3C);
        coolware.security.SecurityEngine.initializeRatchet(defaultEntropy);
        FontManager.init();
        this.shaderRenderManager = new coolware.util.shader.ShaderRenderManager();
        this.moduleManager = new ModuleManager();
        this.commandManager = new CommandManager();
        this.clickGuiScreen = new NovolineDropdownGUI();
        // Load default configuration if present
        ConfigManager.loadConfig(ConfigManager.DEFAULT_CONFIG);
        System.out.println("[" + NAME + "] Loaded " + moduleManager.getModules().size() + " modules.");
    }

    public void onKeyPress(int key) {
        if (key == Keyboard.KEY_RSHIFT || key == 54) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null) {
                // Check which style the ClickGUI module has selected
                coolware.module.visuals.ClickGUI clickGuiModule = moduleManager != null
                        ? moduleManager.getModule(coolware.module.visuals.ClickGUI.class) : null;
                if (clickGuiModule != null && clickGuiModule.isCoolMode()) {
                    // Open the classic card-style ClickGuiScreen
                    clickGuiModule.onEnable();
                } else {
                    // Open the default Novoline GUI
                    if (clickGuiScreen == null) {
                        clickGuiScreen = new NovolineDropdownGUI();
                    }
                    mc.displayGuiScreen(clickGuiScreen);
                }
            }
            return;
        }

        if (moduleManager != null) {
            moduleManager.onKeyPress(key);
        }
    }


    public void onTick() {
        try {
            coolware.util.MovementUtil.onClientTick();
            coolware.util.PlayerUtil.updateLastBlocking();
            if (moduleManager != null) {
                moduleManager.onTick();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void onRender2D() {
        try {
            HUDOverlay.render();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (moduleManager != null) {
            try {
                moduleManager.onRender2D();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

    public void onShutdown() {
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public NovolineDropdownGUI getClickGuiScreen() {
        return clickGuiScreen;
    }

    public coolware.util.shader.ShaderRenderManager getShaderRenderManager() {
        return shaderRenderManager;
    }
}
