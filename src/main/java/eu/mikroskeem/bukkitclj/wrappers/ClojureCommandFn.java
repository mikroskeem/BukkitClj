/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj.wrappers;

import clojure.lang.IFn;
import clojure.lang.Namespace;
import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import eu.mikroskeem.bukkitclj.BukkitClj;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Mark Vainomaa
 */
public final class ClojureCommandFn extends Command {
    private final Namespace namespace;
    private final IFn handler;
    private IFn tabcompleteHandler;

    public ClojureCommandFn(Namespace namespace, String name, String permission, String[] aliases, IFn handler) {
        super(name, "", "", Collections.emptyList());
        this.namespace = namespace;
        this.handler = handler;
        this.setPermission(permission);
        this.setAliases(Arrays.asList(aliases));
    }

    public void setTabcompleteHandler(IFn tabcompleteHandler) {
        if (this.tabcompleteHandler != null) {
            throw new IllegalStateException("Tab complete handler is already set");
        }
        this.tabcompleteHandler = tabcompleteHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender))
            return true;

        try (Timing t = Timings.of(BukkitClj.getInstance(),
                "Script " + namespace.getName().getName() + " command '" + this.getName() + "'")) {
            this.handler.invoke(sender, label, Arrays.asList(args));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (tabcompleteHandler == null) {
            return super.tabComplete(sender, label, args);
        }

        try (Timing t = Timings.of(BukkitClj.getInstance(),
                "Script " + namespace.getName().getName() + " command '" + this.getName() + "' tab complete handler")) {
            Object result = this.tabcompleteHandler.invoke(sender, label, Arrays.asList(args));
            if (result instanceof List) {
                return (List<String>) result;
            } else if (result instanceof Collection) {
                return new ArrayList<>((Collection<String>) result);
            } else {
                // Class cast exception prone, nothing for us to do here really.
                return (List<String>) result;
            }
        }
    }
}
