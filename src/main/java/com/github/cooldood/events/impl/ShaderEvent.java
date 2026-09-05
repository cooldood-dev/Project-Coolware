package com.github.cooldood.events.impl;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ShaderEvent extends Event {
    private final boolean bloom;
    public ShaderEvent(boolean bloom) {
        this.bloom = bloom;
    }
    public boolean isBloom() {
        return bloom;
    }
}
