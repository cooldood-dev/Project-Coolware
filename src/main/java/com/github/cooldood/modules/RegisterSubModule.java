package com.github.cooldood.modules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterSubModule {
    String name();
    String description() default "";
    String parent() default "";
    String[] modeParentString() default "";

    // for sliders
    double min() default 0;
    double max() default 1;
    double increment() default 0.01;

    boolean dangerous() default false;
}
