package com.github.cooldood.commands.impl;

import com.github.cooldood.commands.Command;
import com.github.cooldood.commands.CommandManager;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.SubModule;
import com.github.cooldood.utils.client.FuzzySearchUtil;
import com.github.cooldood.utils.minecraft.ChatUtil;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import net.minecraft.util.MathHelper;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SetCommand extends Command {
    @Override
    public String name() {
        return "set";
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 3) {
            return false;
        }

        Module module = FuzzySearchUtil.findModule(args[0]);

        if (module == null) {
            ChatUtil.prefixMessage("Cannot find module: " + args[0]);
            return false;
        }

        SubModule subModule = FuzzySearchUtil.findSubModule(args[1], module);

        if (subModule == null) {
            ChatUtil.prefixMessage("Cannot find submodule: " + args[1]);
            return false;
        }

        Class<?> type = subModule.getField().getType();
        String settingValue = args[2];

        if (type.isEnum()) {
            List<String> enumValues = Arrays.stream(type.getEnumConstants()).map(e -> ((Enum<?>)e).name().toLowerCase()).collect(Collectors.toList());
            Optional<ExtractedResult> enumValue =  FuzzySearchUtil.fuzzySearch(settingValue, enumValues);
            if (!enumValue.isPresent()) {
                ChatUtil.prefixMessage("Invalid enum value for &f" + subModule.annotation.name() + " &c(&f" + args[1] + "&c)");
                return true;
            }
            Object value = type.getEnumConstants()[enumValue.get().getIndex()];

            ChatUtil.prefixMessage("&c" + module.getAnnotation().name() + "'s " + subModule.getAnnotation().name() + " &fchanged from &c" + subModule.get() + " &fto &c" + ((Enum<?>) value).name());
            subModule.set(value);

            return true;
        }
        else if (type == double.class || type == float.class || type == int.class || type == long.class) {
            // i think i googled this regex, it looks really complex and idk how it works
            if (!settingValue.matches("-?\\d+(\\.\\d+)?")) {
                ChatUtil.prefixMessage("Cannot set &f" + subModule.annotation.name() + " &cto non numerical value: &f" + args[1]);
                return true;
            }

            double value = Double.parseDouble(settingValue);
            // make sure you keep within bounds :)
            value = Math.max(Math.min(subModule.getAnnotation().max(), value), subModule.getAnnotation().min());

            ChatUtil.prefixMessage("&c" + module.getAnnotation().name() + "'s " + subModule.getAnnotation().name() + " &fchanged from &c" + subModule.get() + " &fto &c" + value);
            subModule.set(value);

            return true;
        }
        else if (type == boolean.class) {
            if (!settingValue.equals("true") && !settingValue.equals("false")) {
                ChatUtil.prefixMessage("Invalid boolean arguement for &f" + subModule.annotation.name() + " &c(&f" + args[1] + "&c)");
                return false;
            }

            subModule.set(settingValue.equals("true"));
            ChatUtil.prefixMessage(module.getAnnotation().name() + "'s " + subModule.getAnnotation().name() + " &fchanged to " + (settingValue.equals("true") ? "&aEnabled" : "&cDisabled"));
            return true;
        }
        else if (type == Color.class) {
            String remainingArgs = CommandManager.gatherArgs(args, 2);
            if (!remainingArgs.matches("[0-9]+ [0-9]+ [0-9]+ [0-9]+") || args.length < 6) {
                ChatUtil.prefixMessage("Invalid RGBA value for &f" + subModule.annotation.name() + " &c(&f" + remainingArgs + "&c) expected: &f255, 255, 255, 255");
                return true;
            }

            int r = MathHelper.clamp_int(Integer.parseInt(args[2]), 0, 255);
            int g = MathHelper.clamp_int(Integer.parseInt(args[3]), 0, 255);
            int b = MathHelper.clamp_int(Integer.parseInt(args[4]), 0, 255);
            int a = MathHelper.clamp_int(Integer.parseInt(args[5]), 0, 255);

            subModule.set(new Color(r, g, b, a));
            ChatUtil.prefixMessage(module.getAnnotation().name() + "'s " + subModule.getAnnotation().name() + " &fchanged to colour: &c" + remainingArgs);
            return true;
        }

        return false;
    }

    @Override
    public String[] usage() {
        return new String[] { "<module> <setting> <value>" };
    }
}
