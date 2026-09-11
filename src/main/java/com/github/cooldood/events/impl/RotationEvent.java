package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import com.github.cooldood.utils.minecraft.RotationUtil;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RotationEvent extends Event {
    public RotationUtil.Rotation rotation;
}
