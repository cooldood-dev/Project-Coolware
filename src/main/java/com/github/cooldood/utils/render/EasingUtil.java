package com.github.cooldood.utils.render;

import lombok.AllArgsConstructor;

import java.util.HashMap;

// I made EasingFunctions enum base myself and asked ai "implement all of these easing functions in order https://easings.net/"
public class EasingUtil {

    @AllArgsConstructor
    public enum EasingFunctions {
        Normal {
            @Override public double ease(double x) {return x;}
        },
        Ease_In_Sine {
            @Override public double ease(double x) {return 1 - Math.cos((x * Math.PI) / 2);}
        },
        Ease_Out_Sine {
            @Override public double ease(double x) {return Math.sin((x * Math.PI) / 2);}
        },
        Ease_In_Out_Sine {
            @Override public double ease(double x) {return -(Math.cos(Math.PI * x) - 1) / 2;}
        },
        Ease_In_Quad {
            @Override public double ease(double x) {return x * x;}
        },
        Ease_Out_Quad {
            @Override public double ease(double x) {return 1 - (1 - x) * (1 - x);}
        },
        Ease_In_Out_Quad {
            @Override public double ease(double x) {return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;}
        },
        Ease_In_Cubic {
            @Override public double ease(double x) {return x * x * x;}
        },
        Ease_Out_Cubic {
            @Override public double ease(double x) {return 1 - Math.pow(1 - x, 3);}
        },
        Ease_In_Out_Cubic {
            @Override public double ease(double x) {return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;}
        },
        Ease_In_Quart {
            @Override public double ease(double x) {return x * x * x * x;}
        },
        Ease_Out_Quart {
            @Override public double ease(double x) {return 1 - Math.pow(1 - x, 4);}
        },
        Ease_In_Out_Quart {
            @Override public double ease(double x) {return x < 0.5 ? 8 * x * x * x * x : 1 - Math.pow(-2 * x + 2, 4) / 2;}
        },
        Ease_In_Quint {
            @Override public double ease(double x) {return x * x * x * x * x;}
        },
        Ease_Out_Quint {
            @Override public double ease(double x) {return 1 - Math.pow(1 - x, 5);}
        },
        Ease_In_Out_Quint {
            @Override public double ease(double x) {return x < 0.5 ? 16 * x * x * x * x * x : 1 - Math.pow(-2 * x + 2, 5) / 2;}
        },
        Ease_In_Expo {
            @Override public double ease(double x) {return x == 0 ? 0 : Math.pow(2, 10 * x - 10);}
        },
        Ease_Out_Expo {
            @Override public double ease(double x) {return x == 1 ? 1 : 1 - Math.pow(2, -10 * x);}
        },
        Ease_In_Out_Expo {
            @Override public double ease(double x) {return x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? Math.pow(2, 20 * x - 10) / 2 : (2 - Math.pow(2, -20 * x + 10)) / 2;}
        },
        Ease_In_Circ {
            @Override public double ease(double x) {return 1 - Math.sqrt(1 - Math.pow(x, 2));}
        },
        Ease_Out_Circ {
            @Override public double ease(double x) {return Math.sqrt(1 - Math.pow(x - 1, 2));}
        },
        Ease_In_Out_Circ {
            @Override public double ease(double x) {
                return x < 0.5
                        ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2
                        : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2;
            }
        },
        Ease_In_Back {
            @Override public double ease(double x) {
                final double c1 = 1.70158;
                final double c3 = c1 + 1;

                return c3 * x * x * x - c1 * x * x;
            }
        },
        Ease_Out_Back {
            @Override public double ease(double x) {
                final double c1 = 1.70158;
                final double c3 = c1 + 1;

                return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
            }
        },
        Ease_In_Out_Back {
            @Override public double ease(double x) {
                final double c1 = 1.70158;
                final double c2 = c1 * 1.525;

                return x < 0.5
                        ? (Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2
                        : (Math.pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2;
            }
        },
        Ease_In_Elastic {
            @Override public double ease(double x) {
                final double c4 = (2 * Math.PI) / 3;

                return x == 0
                        ? 0
                        : x == 1
                        ? 1
                        : -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4);
            }
        },
        Ease_Out_Elastic {
            @Override public double ease(double x) {
                final double c4 = (2 * Math.PI) / 3;

                return x == 0
                        ? 0
                        : x == 1
                        ? 1
                        : Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1;
            }
        },
        Ease_In_Out_Elastic {
            @Override public double ease(double x) {
                final double c5 = (2 * Math.PI) / 4.5;

                return x == 0
                        ? 0
                        : x == 1
                        ? 1
                        : x < 0.5
                        ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2
                        : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1;
            }
        },
        Ease_In_Bounce {
            @Override public double ease(double x) {
                return 1 - Ease_Out_Bounce.ease(1 - x);
            }
        },
        Ease_Out_Bounce {
            @Override public double ease(double x) {
                final double n1 = 7.5625;
                final double d1 = 2.75;

                if (x < 1 / d1) {
                    return n1 * x * x;
                } else if (x < 2 / d1) {
                    return n1 * (x -= 1.5 / d1) * x + 0.75;
                } else if (x < 2.5 / d1) {
                    return n1 * (x -= 2.25 / d1) * x + 0.9375;
                } else {
                    return n1 * (x -= 2.625 / d1) * x + 0.984375;
                }
            }
        },
        Ease_In_Out_Bounce {
            @Override public double ease(double x) {
                return x < 0.5
                        ? (1 - Ease_Out_Bounce.ease(1 - 2 * x)) / 2
                        : (1 + Ease_Out_Bounce.ease(2 * x - 1)) / 2;
            }
        };

        public abstract double ease(double x);
    }

    private static final HashMap<String, Animation> animations = new HashMap<>();

    public static void addAnimation(String key, long duration, boolean up, EasingFunctions easingFunction) {
        Animation animation = animations.get(key);

        if (animation == null) animations.put(key, new Animation(System.currentTimeMillis(), duration, up, easingFunction));
        else {
            animations.remove(key);

            double currentProgress = (double) (System.currentTimeMillis() - animation.start) / animation.duration;

            if (currentProgress > 1) {
                animations.put(key, new Animation(System.currentTimeMillis(), duration, up, easingFunction));
                return;
            }
            double currentValue = animation.up
                    ? animation.easingFunction.ease(currentProgress)
                    : 1 - animation.easingFunction.ease(currentProgress);

            animations.put(key, new Animation(System.currentTimeMillis() - Math.round((up ? currentValue : 1 - currentValue) * duration), duration, up, easingFunction));

        }
    }

    public static double getAnimation(String key) {
        Animation animation = animations.get(key);

        if (animation == null) return -1;

        double progress = (double) (System.currentTimeMillis() - animation.start) / animation.duration;

        if (progress > 1) {
            animations.remove(key);
            return -1;
        }

        double returnValue = animation.up ? animation.easingFunction.ease(progress) : 1 - animation.easingFunction.ease(progress);
        // math.signNum returns 0, which we do NOT want.
        double minValue = 0.001;
        if (returnValue < minValue && returnValue > -minValue) returnValue = minValue * (returnValue < 0 ? -1 : 1);

        return returnValue;
    }

    public static Animation getAnimationDetails(String key) {
        return animations.get(key);
    }

    public static void removeAnimation(String key) {
        animations.remove(key);
    }

    @AllArgsConstructor
    public static class Animation {
        public long start;
        public long duration;
        public boolean up;
        public EasingFunctions easingFunction;
    }
}