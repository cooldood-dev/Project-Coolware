package com.github.cooldood.commands.impl;

import com.github.cooldood.Main;
import com.github.cooldood.commands.Command;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.utils.client.FuzzySearchUtil;
import com.github.cooldood.utils.minecraft.ChatUtil;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigCommand extends Command {
    @Override
    public String name() {
        return "config";
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 1) return false;

        switch (args[0]) {
            case "folder":
                ModuleManager.openConfigFolder();
                return true;

            case "save":
                if (args.length < 2) return false;

                if (args.length < 3) {
                    ModuleManager.saveConfig(args[1]);
                    ChatUtil.prefixMessage("&cConfig &f" + args[1] + " &csaved!");
                } else {
                    Module module = FuzzySearchUtil.findModule(args[1]);

                    if (module == null) return false;

                    ModuleManager.saveModule(module, args[2]);
                    ChatUtil.prefixMessage("&cModule &f" + module.getAnnotation().name() + " &csaved into separate folder &f" + args[2] + "&c!");
                }
                return true;

            case "load":
                if (args.length < 3) {
                    if (ModuleManager.loadConfig(args[1]))
                        ChatUtil.prefixMessage("&cSuccessfully loaded config: &f" + args[1]);
                } else {
                    if (args[1].equals("keybinds")) {
                        if (ModuleManager.loadKeybinds(args[2]))
                            ChatUtil.prefixMessage("&cSuccessfully loaded keybinds from: &f" + args[2]);
                    }
                    else {

                        Module module = FuzzySearchUtil.findModule(args[1]);

                        if (module == null) return false;

                        String configFileText = ModuleManager.readConfig(module.getAnnotation().name() + "/" + args[2]);

                        if (configFileText != null) {
                            ModuleManager.loadModule(module, configFileText);
                            ChatUtil.prefixMessage("&cSuccessfully loaded config &f" + args[2] + " &cfor module: &f" + module.getAnnotation().name());
                        }
                    }
                }
                return true;

            case "list":
                if (args.length < 2) {
                    List<File> files = ModuleManager.getConfigFiles("");

                    List<File> configFiles = files.stream().filter(e -> e.isFile() && !e.getName().equals(Main.baseConfig+Main.configExtension)).collect(Collectors.toList());

                    if (configFiles.isEmpty()) ChatUtil.prefixMessage("&7No config files found!");
                    else {
                        ChatUtil.prefixMessage("&f" + configFiles.size() + " &cConfig files found: ");
                        ChatUtil.chat("&c" + configFiles.stream().map(e -> e.getName().replace(".cfg", "")).collect(Collectors.joining("&f, &c")));
                    }

                    List<File> folders = files.stream().filter(File::isDirectory).collect(Collectors.toList());
                    if (folders.isEmpty()) ChatUtil.prefixMessage("&7No modules have separate configs!");
                    else {
                        ChatUtil.prefixMessage("&f" + folders.size() + " &cModules have separate configs: ");
                        ChatUtil.chat("&c" + folders.stream().map(File::getName).collect(Collectors.joining("&f, &c")));
                    }
                } else {
                    Module module = FuzzySearchUtil.findModule(args[1]);

                    if (module == null) return false;

                    List<File> subConfigs = ModuleManager.getConfigFiles(module.getAnnotation().name());

                    if (subConfigs.isEmpty())
                        ChatUtil.prefixMessage("&c" + module.getAnnotation().name() + " &chas no subconfigs!");
                    else {
                        ChatUtil.prefixMessage("&f" + module.getAnnotation().name() + " &chas &f" + subConfigs.size() + " &csubconfigs: ");
                        ChatUtil.chat("&c" + subConfigs.stream().map(File::getName).collect(Collectors.joining("&f, &c")));
                    }
                }
                return true;
        }

        return false;
    }

    @Override
    public String[] usage() {
        return new String[] {
                "folder",

                "list",
                "list <module>",

                "load <name>",
                "load <module> <name>",
                "load keybinds <name>",

                "save <name>",
                "save <module> <name>",
        };
    }
}
