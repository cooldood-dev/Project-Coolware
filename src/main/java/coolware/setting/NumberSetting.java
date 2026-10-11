package coolware.setting;

import coolware.module.Module;

public class NumberSetting extends Setting {
    private double value;
    private final double min;
    private final double max;
    private final double increment;

    public NumberSetting(String name, Module parent, double defaultValue, double min, double max, double increment) {
        super(name, parent);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getValue() {
        return value;
    }

    public float getValueFloat() {
        return (float) value;
    }

    public int getValueInt() {
        return (int) Math.round(value);
    }

    public void setValue(double value) {
        double precision = 1.0 / increment;
        double rounded = Math.round(Math.max(min, Math.min(max, value)) * precision) / precision;
        this.value = rounded;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }
}
