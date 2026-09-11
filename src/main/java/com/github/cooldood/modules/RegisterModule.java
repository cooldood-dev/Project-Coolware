package com.github.cooldood.modules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterModule {
    String name();
    String description();
    Category category();

    boolean enabledByDefault() default false;
    boolean dangerous() default false;
    int keybind() default -1;
}
