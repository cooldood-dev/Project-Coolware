package com.github.cooldood.utils.client;

import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.SubModule;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FuzzySearchUtil {
    public static Optional<ExtractedResult> fuzzySearch(String find, List<String> list) {
        return FuzzySearch.extractAll(find, list, 40).stream().max(Comparator.comparingInt(ExtractedResult::getScore));
    }

    public static Module findModule(String moduleToFind) {
        // i love fuzzysearch.
        // might add tags to the modules so its more accurate but idk
        return fuzzySearch(moduleToFind, ModuleManager.getModuleNames()).map(extractedResult -> ModuleManager.getModules().get(extractedResult.getIndex())).orElse(null);
    }

    public static SubModule findSubModule(String subModuleToFind, Module parentModule) {
        ArrayList<SubModule> subModules = parentModule.getChildren().stream().filter(e -> e.shouldRender(true, true)).collect(Collectors.toCollection(ArrayList::new));
        List<String> subModuleNames = subModules.stream().map(subModule -> subModule.getAnnotation().name()).collect(Collectors.toList());

        return fuzzySearch(subModuleToFind, subModuleNames).map(extractedResult -> subModules.get(extractedResult.getIndex())).orElse(null);
    }

    public static List<Module> getSimilarModules(String moduleToFind, int limit) {
        return FuzzySearch.extractAll(moduleToFind, ModuleManager.getModuleNames(), limit).stream().map(extractedResult -> ModuleManager.getModules().get(extractedResult.getIndex())).collect(Collectors.toList());
    }
}
