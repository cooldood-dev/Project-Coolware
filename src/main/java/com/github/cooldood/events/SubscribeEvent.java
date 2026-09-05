package com.github.cooldood.events;

// in theory I don't need this, I could use comments to annotate, but that looks worse and could lead to me FAILING
public @interface SubscribeEvent {
    /// lower number = goes first.
    int priority() default 1000;
}
