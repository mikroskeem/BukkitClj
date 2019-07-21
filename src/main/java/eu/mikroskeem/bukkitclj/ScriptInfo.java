/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import eu.mikroskeem.bukkitclj.wrappers.ClojureCommandFn;
import eu.mikroskeem.bukkitclj.wrappers.ClojureListenerFn;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static eu.mikroskeem.bukkitclj.BukkitClj.getInstance;

/**
 * @author Mark Vainomaa
 */
public final class ScriptInfo {
    private final String namespace;
    private final Path scriptPath;
    private final String scriptName;
    private final List<ClojureListenerFn> listeners;
    private final List<ClojureCommandFn> commands;
    private final List<Permission> permissions;

    public ScriptInfo(String namespace, Path scriptPath) {
        this.namespace = namespace;
        this.scriptPath = scriptPath;
        this.scriptName = scriptPath.getFileName().toString();
        this.listeners = new LinkedList<>();
        this.commands = new LinkedList<>();
        this.permissions = new LinkedList<>();
    }

    public String getNamespace() {
        return namespace;
    }

    public Path getScriptPath() {
        return scriptPath;
    }

    public String getScriptName() {
        return scriptName;
    }

    public List<ClojureListenerFn> getListeners() {
        return listeners;
    }

    public List<ClojureCommandFn> getCommands() {
        return commands;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void unload(boolean unregister) {
        IFn scriptDeinitFunc = Clojure.var(getNamespace(), "script-deinit");
        try {
            scriptDeinitFunc.invoke();
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException) || !e.getMessage().startsWith("Attempting to call unbound fn:")) {
                getInstance().getSLF4JLogger().error("Failed to deinitialize {}", getScriptPath(), e);
            }
        }

        if (unregister) {
            // Unregister listeners
            for (ClojureListenerFn listener : getListeners()) {
                listener.getEventHandlerList().unregister(listener);
            }

            // Unregister commands
            for (ClojureCommandFn command : getCommands()) {
                command.unregister(Bukkit.getCommandMap());
            }

            // Unregister permissions
            for (Permission permission : getPermissions()) {
                Bukkit.getPluginManager().removePermission(permission);
            }
        }
    }
}
