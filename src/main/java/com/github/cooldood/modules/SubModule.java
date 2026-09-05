package com.github.cooldood.modules;

import com.github.cooldood.Main;
import com.github.cooldood.utils.client.MathUtil;
import com.github.cooldood.utils.minecraft.ChatUtil;
import com.github.cooldood.utils.render.EasingUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;

@Getter
// todo: add text submodule
public class SubModule {
    private final Module parentModule;

    private final Field field;
    private final ArrayList<SubModule> children;

    @Setter
    private SubModule parent;

    public final RegisterSubModule annotation;

    public SubModule(Module parentModule, Field field) {
        this.parentModule = parentModule;
        this.field = field;
        this.children = new ArrayList<>();
        this.annotation = field.getAnnotation(RegisterSubModule.class);
    }

    public void set(Object object) {
        try {
            if (isSlider()) {
                double value = MathHelper.clamp_double(
                        MathUtil.roundTo(
                                MathUtil.toNearest(Double.parseDouble(object.toString()), annotation.increment()),
                                String.valueOf(annotation.increment()).split("\\.")[1].length()
                        ),
                        annotation.min(),
                        annotation.max()
                );

                if (field.getType() == double.class) field.set(parentModule, value);
                else if (field.getType() == float.class) field.set(parentModule, (float) value);
                else if (field.getType() == long.class) field.set(parentModule, (long) value);
                else field.set(parentModule, (int) value);
            }
            else field.set(parentModule, object);

            // save config whenever anything changes!
            ModuleManager.saveConfig(Main.baseConfig);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to set field {} of {} to {}", field.getName(), parentModule, object, e);
            ChatUtil.prefixMessage("&cFailed to set field &f" + field.getName() + " &cof &f" + parentModule + " &cto &f" + object);
        }
    }

    public Object get() {
        try {
            return field.get(parentModule);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSlider() {
        return this.getField().getType() == double.class
                || this.getField().getType() == float.class
                || this.getField().getType() == long.class
                || this.getField().getType() == int.class;
    }

    public boolean shouldRender() {
        return shouldRender(false, false);
    }

    public boolean shouldRender(boolean ignoreSubCategory, boolean ignoreRightClick) {
        boolean parentOpen = ignoreRightClick || this.parentModule.isOpen();

        if (!ignoreSubCategory && !parentOpen && EasingUtil.getAnimation(this.parentModule.getUniqueKey("")) == -1)
            return false;

        if (this.parent == null) return true;
        if (!this.parent.shouldRender(ignoreSubCategory, ignoreRightClick) && !ignoreRightClick) return false;
        if (EasingUtil.getAnimation(this.parent.getUniqueKey()) != -1) return true;

        if (this.parent.getField().getType() == boolean.class)
            return (boolean) this.parent.get();
        else if (this.parent.getField().getType() == SubCategory.class) {
            if (ignoreSubCategory) return true;
            return ((SubCategory) this.parent.get()).open;
        }
        else if (this.parent.getField().getType().isEnum()) {
            Enum<?> parentValue = (Enum<?>) this.parent.get();

            for (String string : this.getAnnotation().modeParentString()) {
                if (
                        parentValue.name().equalsIgnoreCase(string)
                        || EasingUtil.getAnimation((this.parent.getUniqueKey() + string).toLowerCase()) != -1
                )
                    return this.parent.shouldRender(ignoreSubCategory, ignoreRightClick);
            }

            return false;
        }

        return this.parent.shouldRender(ignoreSubCategory, ignoreRightClick);
    }

    // messy, but being smart is hard.
    public double getAnimationProgress() {
        double parentAnimationProgress = -1;
        SubModule parent = this.getParent();
        SubModule child = this;
        while (parent != null) {
            double parentAnimation = EasingUtil.getAnimation(parent.getUniqueKey());

            // handle enums differently todo: enums with multiple parents flicker a little, iykyk
            if (parent.getField().getType().isEnum()) {
                for (String mode : child.getAnnotation().modeParentString()) {
                    parentAnimation = Math.max(parentAnimation, EasingUtil.getAnimation((parent.getUniqueKey() + mode).toLowerCase()));
                }
            }

            if (parentAnimation != -1) {
                if (parentAnimationProgress == -1) parentAnimationProgress = parentAnimation;
                else parentAnimationProgress *= parentAnimation;
            }

            child = parent;
            parent = parent.getParent();
        }

        return parentAnimationProgress;
    }

    public String getUniqueKey() {
        return this.parentModule.getUniqueKey(this.getAnnotation().name() + this.getAnnotation().description());
    }

    public ColorSettingValues colorSettingValues = new ColorSettingValues(false, 0, 0, 0);

    @AllArgsConstructor
    public static class ColorSettingValues {
        public boolean open;
        public int mX, mY;
        public float hueValue;
    }
}
