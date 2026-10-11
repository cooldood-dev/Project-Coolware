package coolware.config;

import coolware.Coolware;
import coolware.module.Module;
import coolware.module.ModuleManager;
import coolware.module.visuals.Theme;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.setting.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final File CONFIG_DIR = new File(Minecraft.getMinecraft().mcDataDir, "coolware/configs");
    public static final String DEFAULT_CONFIG = "default";

    public static File getConfigDir() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        return CONFIG_DIR;
    }

    public static boolean saveConfig(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = DEFAULT_CONFIG;
        }
        if (!name.endsWith(".cfg")) {
            name += ".cfg";
        }

        File file = new File(getConfigDir(), name);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write("# Coolware v" + Coolware.VERSION + " Config File: " + name);
            writer.newLine();
            writer.write("# Format: ModuleName:Property:Value");
            writer.newLine();
            writer.newLine();

            ModuleManager mm = Coolware.getInstance().getModuleManager();
            if (mm != null) {
                for (Module mod : mm.getModules()) {
                    // Save module state and keybind
                    writer.write(mod.getName() + ":toggled:" + mod.isToggled());
                    writer.newLine();
                    writer.write(mod.getName() + ":key:" + mod.getKeyCode());
                    writer.newLine();

                    // Save custom settings
                    for (Setting s : mod.getSettings()) {
                        if (s instanceof BooleanSetting) {
                            writer.write(mod.getName() + ":setting:" + s.getName() + ":" + ((BooleanSetting) s).isEnabled());
                            writer.newLine();
                        } else if (s instanceof NumberSetting) {
                            writer.write(mod.getName() + ":setting:" + s.getName() + ":" + ((NumberSetting) s).getValue());
                            writer.newLine();
                        } else if (s instanceof ModeSetting) {
                            writer.write(mod.getName() + ":setting:" + s.getName() + ":" + ((ModeSetting) s).getMode());
                            writer.newLine();
                        }
                    }

                    // Extra Theme properties if mod is Theme
                    if (mod instanceof Theme) {
                        Theme t = (Theme) mod;
                        writer.write("Theme:colorMode:" + t.getColorMode().name());
                        writer.newLine();
                        writer.write("Theme:primaryHex:" + t.getPrimaryHex());
                        writer.newLine();
                        writer.write("Theme:secondaryHex:" + t.getSecondaryHex());
                        writer.newLine();
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loadConfig(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = DEFAULT_CONFIG;
        }
        if (!name.endsWith(".cfg")) {
            name += ".cfg";
        }

        File file = new File(getConfigDir(), name);
        if (!file.exists()) {
            return false;
        }

        ModuleManager mm = Coolware.getInstance().getModuleManager();
        if (mm == null) return false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(":", 4);
                if (parts.length < 3) continue;

                String modName = parts[0];
                String propType = parts[1];

                Module mod = mm.getModuleByName(modName);
                if (mod == null) {
                    if (modName.equalsIgnoreCase("Theme") && propType.equalsIgnoreCase("colorMode")) {
                        try {
                            Theme.getInstance().setColorMode(Theme.ColorMode.valueOf(parts[2]));
                        } catch (Exception ignored) {}
                    } else if (modName.equalsIgnoreCase("Theme") && propType.equalsIgnoreCase("primaryHex")) {
                        Theme.getInstance().setPrimaryHex(parts[2]);
                    } else if (modName.equalsIgnoreCase("Theme") && propType.equalsIgnoreCase("secondaryHex")) {
                        Theme.getInstance().setSecondaryHex(parts[2]);
                    }
                    continue;
                }

                if (propType.equalsIgnoreCase("toggled")) {
                    boolean toggled = Boolean.parseBoolean(parts[2]);
                    mod.setToggled(toggled);
                } else if (propType.equalsIgnoreCase("key")) {
                    try {
                        int key = Integer.parseInt(parts[2]);
                        mod.setKeyCode(key);
                    } catch (Exception ignored) {}
                } else if (propType.equalsIgnoreCase("setting") && parts.length >= 4) {
                    String settingName = parts[2];
                    String val = parts[3];

                    for (Setting s : mod.getSettings()) {
                        if (s.getName().equalsIgnoreCase(settingName)) {
                            if (s instanceof BooleanSetting) {
                                ((BooleanSetting) s).setEnabled(Boolean.parseBoolean(val));
                            } else if (s instanceof NumberSetting) {
                                try {
                                    ((NumberSetting) s).setValue(Double.parseDouble(val));
                                } catch (Exception ignored) {}
                            } else if (s instanceof ModeSetting) {
                                ((ModeSetting) s).setMode(val);
                            }
                            break;
                        }
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> listConfigs() {
        List<String> list = new ArrayList<String>();
        File dir = getConfigDir();
        File[] files = dir.listFiles((d, f) -> f.toLowerCase().endsWith(".cfg"));
        if (files != null) {
            for (File f : files) {
                list.add(f.getName().replace(".cfg", ""));
            }
        }
        return list;
    }
}
