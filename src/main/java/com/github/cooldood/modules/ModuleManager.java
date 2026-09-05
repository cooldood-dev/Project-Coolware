package com.github.cooldood.modules;

import com.github.cooldood.Main;
import com.github.cooldood.screens.ClickGUI.ClickGUIScreen;
import com.github.cooldood.screens.ClickGUI.ModuleRenderer;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.KeybindHandler;
import com.github.cooldood.utils.minecraft.ChatUtil;
import com.github.cooldood.utils.render.FontUtil;
import com.github.cooldood.utils.render.draggable.DraggableRenderer;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private static final HashMap<Class<? extends Module>, Module> modules = new HashMap<>();

    public static void init() {
        for (Class<?> clazz : C.reflections.getTypesAnnotatedWith(RegisterModule.class)) {
            try {
                Main.LOGGER.info("Registering module: {}", clazz.getSimpleName());

                RegisterModule annotation = clazz.getAnnotation(RegisterModule.class);

                Module m = (Module) clazz.newInstance();

                for (Field field : clazz.getDeclaredFields()) {
                    RegisterSubModule registerSubModule = field.getAnnotation(RegisterSubModule.class);

                    if (registerSubModule != null) {
                        SubModule subModule = new SubModule(m, field);

                        if (!registerSubModule.parent().isEmpty()) {
                            // todo: children have to be below parents in the file, fix later if needed <3
                            for (SubModule sm : m.getChildren()) {
                                if (registerSubModule.parent().equals(sm.getAnnotation().name())) {
                                    sm.getChildren().add(subModule);
                                    subModule.setParent(sm);
                                    break;
                                }
                            }
                        }
                        m.getChildren().add(subModule);
                    }
                }

                Class<? extends Module> moduleClazz = (Class<? extends Module>) clazz;

                // dumb code ig. adds the extra settings to modules
                SubModule general = new SubModule(m, moduleClazz.getField("general"));
                SubModule hide = new SubModule(m, moduleClazz.getField("hide"));
                SubModule holdKeybind = new SubModule(m, moduleClazz.getField("holdKeybind"));

                general.getChildren().add(hide); general.getChildren().add(holdKeybind);
                hide.setParent(general); holdKeybind.setParent(general);
                // default to hiding render modules from arraylist
                hide.set(annotation.category() == Category.RENDER || annotation.category() == Category.CLIENT);

                m.getChildren().add(general);
                m.getChildren().add(hide);
                m.getChildren().add(holdKeybind);

                m.setAnnotation(annotation);
                if (annotation.keybind() != -1) {
                    KeybindHandler.registerKeybind(m, annotation.keybind());
                }
                modules.put(moduleClazz, m);
                m.setEnabled(annotation.enabledByDefault());
            } catch (Exception e) {
                Main.LOGGER.error("Failed to register module: {}", clazz.getSimpleName(), e);
            }
        }

        loadConfig(Main.baseConfig);
        DraggableRenderer.loadDraggingPositions();
        ClickGUIScreen.loadCategoryPositions();
    }

    public static boolean isEnabled(Class<? extends Module> clazz) {
        Module module = ModuleManager.getModule(clazz);
        return module != null && module.isEnabled();
    }
    public static void setEnabled(Class<? extends Module> clazz, boolean enabled) {
        ModuleManager.getModule(clazz).setEnabled(enabled);
    }

    public static Module getModule(Class<? extends Module> clazz) {
        return modules.get(clazz);
    }

    public static List<String> getModuleNames() {
        return getModules().stream().map(module -> module.getAnnotation().name()).collect(Collectors.toList());
    }

    public static List<Module> getEnabledModules() {
        return getModules().stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }

    public static List<Module> getModules() {
        return new ArrayList<>(modules.values()).stream()
                .filter(m -> !(m instanceof com.github.cooldood.modules.impl.client.ThemeModule))
                .collect(Collectors.toList());
    }

    public static List<Module> getModulesByCategory(Category category) {
        return getModulesByCategory(category, getModules());
    }

    public static List<Module> getModulesByCategory(Category category, List<Module> modules) {
        return modules.stream()
                .filter(e -> e.getAnnotation().category().equals(category))
                .sorted(Comparator.comparingInt(module -> FontUtil.getStringWidth(ModuleRenderer.moduleName(module), 10)))
                .collect(Collectors.toList());
    }

    public static String getModuleName(Class<? extends Module> clazz) {
        Module module = getModule(clazz);
        return module != null ? module.getAnnotation().name() : null;
    }


    public static void openConfigFolder() {
        try {
            Files.createDirectories(Paths.get(Main.configPath));
            Desktop.getDesktop().open(new File(Main.configPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> getConfigFiles(String folderPath) {
        return Arrays.asList(Objects.requireNonNull(Paths.get(Main.configPath + folderPath).toFile().listFiles()));
    }


    // god bless https://github.com/google/gson/blob/main/UserGuide.md
    public static void saveConfig(String configName) {
        if (!C.isInGame()) return;

        HashMap<String, LinkedTreeMap<String, Object>> moduleJSON = new HashMap<>();

        List<Module> modules = getModules();

        for (Module m : modules) saveModule(moduleJSON, m);

        saveConfig(Main.configPath, configName, C.gson.toJson(moduleJSON));
    }

    private static void saveModule(HashMap<String, LinkedTreeMap<String, Object>> moduleJSON, Module module) {
        LinkedTreeMap<String, Object> subModules = new LinkedTreeMap<>();

        subModules.put("enabled", module.isEnabled());
        subModules.put("keybind", module.getKeybind());

        for (SubModule sm : module.getChildren()) {
            subModules.put(sm.getAnnotation().name(), sm.get());
        }

        moduleJSON.put(module.getAnnotation().name(), subModules);
    }

    public static String saveModule(Module module) {
        HashMap<String, LinkedTreeMap<String, Object>> moduleJSON = new HashMap<>();

        saveModule(moduleJSON, module);

        return C.gson.toJson(moduleJSON);
    }

    public static void saveModule(Module module, String configName) {
        saveConfig(Main.configPath + module.getAnnotation().name() + "/", configName, saveModule(module));
    }

    public static void saveConfig(String filePath, String fileName, String JSON) {
        try {
            Files.createDirectories(Paths.get(filePath));
            Files.write(Paths.get(filePath + fileName + Main.configExtension), JSON.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean loadConfig(String configName) {
        String configFileText = readConfig(configName);
        if (configFileText == null) return false;

        for (Module m : modules.values()) loadModule(m, configFileText);

        return true;
    }


    public static boolean loadKeybinds(String configName) {
        String configFileText = readConfig(configName);
        if (configFileText == null) return false;

        KeybindHandler.keybindsMap.clear();

        HashMap<String, LinkedTreeMap<String, Object>> modulesJSON = C.gson.fromJson(configFileText, HashMap.class);

        for (Module m : modules.values()) {
            if (modulesJSON.containsKey(m.getAnnotation().name())) {
                int keybind = getKeybindValue(modulesJSON.get(m.getAnnotation().name()));
                if (keybind != -1)
                    KeybindHandler.registerKeybind(m, keybind);
            }
        }

        return true;
    }

    private static int getKeybindValue(LinkedTreeMap<String, Object> subModules) {
        // it thinks -1 is a double :(
        return (int) Double.parseDouble(subModules.get("keybind").toString());
    }


    public static void loadModule(Module module, String configFileText) {
        HashMap<String, LinkedTreeMap<String, Object>> modulesJSON = C.gson.fromJson(configFileText, HashMap.class);

        if (!modulesJSON.containsKey(module.getAnnotation().name())) {
            Main.LOGGER.warn("Module config missing: {}. Defaulting to {}", module.getAnnotation().name(), module.getAnnotation().enabledByDefault());
            module.setEnabled(module.getAnnotation().enabledByDefault());
            if (module.getAnnotation().keybind() != -1) {
                KeybindHandler.registerKeybind(module, module.getAnnotation().keybind());
            }
        }
        else {
            LinkedTreeMap<String, Object> subModules = modulesJSON.get(module.getAnnotation().name());
            module.setEnabled(subModules.get("enabled").equals(true));
            // it thinks -1 is a double :(
            int keybind = -1;
            if (subModules.containsKey("keybind")) {
                keybind = (int) Double.parseDouble(subModules.get("keybind").toString());
            }
            if (keybind == -1 && module.getAnnotation().keybind() != -1) {
                keybind = module.getAnnotation().keybind();
            }
            if (keybind != -1)
                KeybindHandler.registerKeybind(module, keybind);
            else
                KeybindHandler.removeKeybind(module);

            for (SubModule subModule : module.getChildren()) {
                if (subModule.getField().getType() == SubCategory.class) continue;

                if (!subModules.containsKey(subModule.getAnnotation().name())) {
                    Main.LOGGER.warn(
                            "Module config missing submodule: {} of {}. Defaulting to {}",
                            subModule.getAnnotation().name(),
                            module.getAnnotation().name(),
                            subModule.get()
                    );
                    continue;
                }

                Object value = subModules.get(subModule.getAnnotation().name());

                if (subModule.getField().getType().isEnum()) {
                    Enum<?>[] enumConstants = (Enum<?>[]) subModule.getField().getType().getEnumConstants();
                    for (Enum<?> enumConstant : enumConstants)
                        if (enumConstant.name().equals(value)) subModule.set(enumConstant);
                }
                else if (subModule.getField().getType().equals(Color.class)) {
                    LinkedTreeMap<String, Object> colorMap = (LinkedTreeMap<String, Object>) value;
                    subModule.set(new Color((int) Double.parseDouble(colorMap.get("value").toString()), true));
                }
                else {
                    subModule.set(value);
                }
            }
        }
    }


    public static String readConfig(String configName) {
        try {
            File configFile = new File(Main.configPath + configName + Main.configExtension);
            if (!Files.exists(configFile.toPath())) {
                ChatUtil.prefixMessage("&cConfig file does not exist: &f" + configName);
                return null;
            }
            return FileUtils.readFileToString(configFile);
        } catch (IOException e) {
            ChatUtil.prefixMessage("&cFailed to read config: " + e.getMessage());
            return null;
        }
    }

}
