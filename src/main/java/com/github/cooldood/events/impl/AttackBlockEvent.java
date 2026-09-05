package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;
import net.minecraft.util.BlockPos;

@AllArgsConstructor
public class AttackBlockEvent extends Event {
    public BlockPos pos;
}
