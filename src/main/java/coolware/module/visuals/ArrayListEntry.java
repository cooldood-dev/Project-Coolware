package coolware.module.visuals;

import coolware.module.Module;
import coolware.util.vector.Vector2d;
import java.awt.Color;

public final class ArrayListEntry {
    public Module module;
    public Vector2d position = new Vector2d(5000.0, 0.0);
    public Vector2d targetPosition = new Vector2d(5000.0, 0.0);
    public float animationTime;
    public String tag = "";
    public float nameWidth = 0.0F;
    public float tagWidth = 0.0F;
    public Color color = Color.WHITE;
    public String translatedName = "";
    public boolean atm = false;
    public String displayName = "";
    public String displayTag = "";
    public boolean hasTag;

    public float getTotalWidth() {
        return this.nameWidth + this.tagWidth;
    }

    public ArrayListEntry(Module module) {
        this.module = module;
    }

    public boolean shouldShow(ArrayListModule parent) {
        if (this.module == null) {
            return false;
        }

        if (this.module instanceof ClickGUI) {
            return false;
        }

        String filter = parent.getModulesToShow();
        if ("Exclude render".equalsIgnoreCase(filter)) {
            return !this.module.getCategory().equals(coolware.module.Category.VISUALS)
                    && !this.module.getCategory().equals(coolware.module.Category.CLIENT);
        } else if ("Only bound".equalsIgnoreCase(filter)) {
            return this.module.getKeyCode() != 0;
        }

        return true;
    }

    public Module getModule() {
        return this.module;
    }

    public Vector2d getPosition() {
        return this.position;
    }

    public Vector2d getTargetPosition() {
        return this.targetPosition;
    }

    public float getAnimationTime() {
        return this.animationTime;
    }

    public String getTag() {
        return this.tag;
    }

    public float getNameWidth() {
        return this.nameWidth;
    }

    public float getTagWidth() {
        return this.tagWidth;
    }

    public Color getColor() {
        return this.color;
    }

    public String getTranslatedName() {
        return this.translatedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDisplayTag() {
        return this.displayTag;
    }

    public boolean isHasTag() {
        return this.hasTag;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public void setPosition(Vector2d position) {
        this.position = position;
    }

    public void setTargetPosition(Vector2d vector2d) {
        this.targetPosition = vector2d;
    }

    public void setAnimationTime(float animationTime) {
        this.animationTime = animationTime;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setNameWidth(float nameWidth) {
        this.nameWidth = nameWidth;
    }

    public void setTagWidth(float tagWidth) {
        this.tagWidth = tagWidth;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setTranslatedName(String translatedName) {
        this.translatedName = translatedName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisplayTag(String displayTag) {
        this.displayTag = displayTag;
    }

    public void setHasTag(boolean hasTag) {
        this.hasTag = hasTag;
    }
}
