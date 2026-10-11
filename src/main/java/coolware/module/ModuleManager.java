package coolware.module;

import coolware.module.combat.KillAura;
import coolware.module.combat.Reach;
import coolware.module.combat.Velocity;
import coolware.module.ghost.BridgeAssist;
import coolware.module.movement.Fly;
import coolware.module.movement.NoSlow;
import coolware.module.movement.Scaffold;
import coolware.module.movement.Sprint;
import coolware.module.player.ChestStealer;
import coolware.module.player.BedBreaker;
import coolware.module.player.InvManager;
import coolware.module.visuals.*;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<Module>();

    public ModuleManager() {
        // Combat
        registerModule(new KillAura());
        registerModule(new Velocity());
        registerModule(new Reach());

        // Movement
        registerModule(new Sprint());
        registerModule(new Fly());
        registerModule(new NoSlow());
        registerModule(new Scaffold());

        // Visuals
        registerModule(new ClickGUI());
        registerModule(new HUD());
        registerModule(Theme.getInstance());
        registerModule(new Fullbright());
        registerModule(new ESP());
        registerModule(new ItemPhysics());
        registerModule(new Freelook());
        registerModule(new Animations());
        registerModule(new SessionInfo());
        registerModule(new TargetHUD());
        registerModule(new PotionHUD());
        registerModule(new ScoreboardModule());
        registerModule(new ArrayListModule());
        registerModule(new NotificationModule());
        registerModule(new Keybinds());
        registerModule(new CustomHotbar());

        // Player
        registerModule(new ChestStealer());
        registerModule(new InvManager());
        registerModule(new BedBreaker());
        registerModule(new coolware.module.player.InventoryMove());

        // Ghost
        registerModule(new BridgeAssist());

        // Miscellaneous / Client
        registerModule(new coolware.module.miscellaneous.DiscordRPCModule());
        registerModule(new coolware.module.miscellaneous.Killsults());
    }

    public void registerModule(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass().equals(clazz)) {
                return (T) m;
            }
        }
        return null;
    }

    public Module getModuleByName(String name) {
        if (name == null) return null;
        String cleanName = name.replaceAll("\\s+", "");
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name) || m.getName().replaceAll("\\s+", "").equalsIgnoreCase(cleanName)) {
                return m;
            }
        }
        return null;
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> list = new ArrayList<Module>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                list.add(m);
            }
        }
        return list;
    }

    public void onKeyPress(int key) {
        if (key == Keyboard.KEY_NONE) return;
        for (Module m : modules) {
            if (m.getKeyCode() == key) {
                m.toggle();
            }
        }
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isToggled()) {
                try {
                    m.onUpdate();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    public void onRender2D() {
        for (Module m : modules) {
            if (m.isToggled()) {
                try {
                    m.onRender2D();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        if (coolware.Coolware.getInstance().getShaderRenderManager() != null) {
            try {
                coolware.Coolware.getInstance().getShaderRenderManager().render(coolware.util.shader.base.ShaderRenderType.OVERLAY);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void onRender3D(float partialTicks) {
        for (Module m : modules) {
            if (m.isToggled()) {
                try {
                    m.onRender3D(partialTicks);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        if (coolware.Coolware.getInstance().getShaderRenderManager() != null) {
            try {
                coolware.Coolware.getInstance().getShaderRenderManager().render(coolware.util.shader.base.ShaderRenderType.CAMERA);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
