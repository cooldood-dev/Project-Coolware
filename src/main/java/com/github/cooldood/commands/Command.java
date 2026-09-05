package com.github.cooldood.commands;

public abstract class Command {
    public abstract String name();

    public abstract boolean execute(String[] args);

    public abstract String[] usage();
}
