package com.github.cooldood.commands.impl;

import com.github.cooldood.commands.Command;
import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.FuzzySearchUtil;
import com.github.cooldood.utils.minecraft.ChatUtil;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListCommand extends Command {
    @Override
    public String name() {
        return "list";
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 1) return false;

        if (args[0].isEmpty()) {
            ChatUtil.prefixMessage("&cModules:");
            ChatUtil.chat(getFormattedModules(ModuleManager.getModules().stream()));

            return true;
        }

        switch (args[0]) {
            case "categories":
                ChatUtil.prefixMessage("&cCategories&f: " + Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.joining(", ")));
                return true;

            case "category":
                if (args.length < 2) return false;

                List<String> categories = Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.toList());

                Optional<ExtractedResult> best = FuzzySearchUtil.fuzzySearch(args[1], categories);
                if (!best.isPresent()) return false;

                ChatUtil.prefixMessage("&cModules In " + best.get().getString() + ":");
                ChatUtil.chat(getFormattedModules(ModuleManager.getModules().stream().filter(e -> e.getAnnotation().category() == Category.values()[best.get().getIndex()])));

                return true;

            default:
                    // .list <module>
                    Module module = FuzzySearchUtil.findModule(args[0]);
                    if (module == null) return false;

                    if (module.getChildren().isEmpty()) {
                        ChatUtil.prefixMessage("&c"+module.getAnnotation().name() + " has no submodules! Try again later :)");
                        return true;
                    }

                    if (args.length < 2) {
                        ChatUtil.prefixMessage("&cSubmodules In " + module.getAnnotation().name() + ":");
                        ChatUtil.chat(getFormattedSubModules(module.getChildren().stream()));
                    }

                    // .list <module> <setting>
                    else {
                        SubModule subModule = FuzzySearchUtil.findSubModule(args[1], module);
                        if (subModule == null) return false;

                        ChatUtil.prefixMessage("&cSettings for &6" + subModule.getAnnotation().name() + " &cin &6" + module.getAnnotation().name());

                        Class<?> type = subModule.getField().getType();
                        if (type.isEnum()) {
                            ChatUtil.chat(
                                    Arrays.stream(subModule.getField().getType().getEnumConstants()).map(
                                            e -> (((Enum<?>)e).name().equals(((Enum<?>) subModule.get()).name()) ? "&6" : "&c") + ((Enum<?>)e).name().toLowerCase().replace("_", " ")
                                    ).collect(Collectors.joining("&f, "))
                            );
                        }
                        else if (type == double.class || type == float.class || type == int.class || type == long.class) {
                            ChatUtil.chat(
                                    "&cCurrent Setting&f: " + subModule.get() +
                                            " &7- &cMin Setting&f: " + subModule.getAnnotation().min() +
                                            " &7- &cMax Setting&f: " + subModule.getAnnotation().max()
                            );
                        }
                        else {
                            if (!subModule.getAnnotation().description().isEmpty()) ChatUtil.chat("&7"+subModule.getAnnotation().description());
                            else ChatUtil.chat("&7No description provided.");
                        }

                        String visibleChildren = getFormattedSubModules(subModule.getChildren().stream());
                        if (!visibleChildren.isEmpty()) {
                            ChatUtil.prefixMessage("&cChildren&f:\n" + visibleChildren);
                        }
                    }
                    return true;
        }
    }

    private static String getFormattedModules(Stream<Module> moduleStream) {
        return moduleStream.map(
                module -> (module.getAnnotation().dangerous() ? "&4" : "&6") + module.getAnnotation().name() + " &7- " +
                        (module.isEnabled() ? "&aEnabled" : "&cDisabled") +
                        (module.getAnnotation().description().isEmpty() ? "" : " &7("+ module.getAnnotation().description() + ")")
        ).collect(Collectors.joining("\n"));
    }

    // holy crap.
    private static String getFormattedSubModules(Stream<SubModule> subModuleStream) {
        return subModuleStream.map(
                subModule -> (
                        subModule.getField().getType() == SubCategory.class ?
                                "\n&6" + subModule.getAnnotation().name()
                                :
                                subModule.shouldRender(true, true)
                                        ?
                                        (subModule.getAnnotation().dangerous() ? "&4" : "&c") + subModule.getAnnotation().name() + "&f: " +
                                                subModule.get() +
                                                (subModule.getAnnotation().description().isEmpty() ? "" : " &7("+ subModule.getAnnotation().description() + ")") : "")
        ).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"));
    }

    @Override
    public String[] usage() {
        return new String[]{
                "",
                "<module>",
                "<module> <setting>",
                "categories",
                "category <category>",
        };
    }
}
