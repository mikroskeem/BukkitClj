/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj.wrappers;

import clojure.lang.IFn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author Mark Vainomaa
 */
public final class ClojureCommandFn extends Command {
    private final IFn handler;

    public ClojureCommandFn(String name, String permission, String[] aliases, IFn handler) {
        super(name, "", "", Collections.emptyList());
        this.handler = handler;
        this.setPermission(permission);
        this.setAliases(Arrays.asList(aliases));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender))
            return true;

        this.handler.invoke(sender, label, Arrays.asList(args));
        return true;
    }
}
