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
import org.bukkit.command.Command;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static eu.mikroskeem.bukkitclj.BukkitClj.getInstance;

/**
 * @author Mark Vainomaa
 */
public final class ScriptInfo {
    private final String namespace;
    private final Path scriptPath;
    private final String scriptName;
    private final List<ClojureListenerFn> listeners;
    private final Map<String, ClojureCommandFn> commands;
    private final Map<Permission, Boolean> permissions;

    public ScriptInfo(String namespace, Path scriptPath) {
        this.namespace = namespace;
        this.scriptPath = scriptPath;
        this.scriptName = scriptPath.getFileName().toString();
        this.listeners = new LinkedList<>();
        this.commands = new LinkedHashMap<>();
        this.permissions = new LinkedHashMap<>();
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

    public Map<String, ClojureCommandFn> getCommands() {
        return commands;
    }

    public Map<Permission, Boolean> getPermissions() {
        return permissions;
    }

    public void load() {
        for (ClojureListenerFn listener : getListeners()) {
            listener.register();
        }

        for (ClojureCommandFn command : getCommands().values()) {
            Bukkit.getServer().getCommandMap().register(command.getName(), "bukkitclj" + namespace, command);
        }

        for (Map.Entry<Permission, Boolean> entry : getPermissions().entrySet()) {
            Permission permission = entry.getKey();
            boolean override = entry.getValue();
            PluginManager plm = BukkitClj.getInstance().getServer().getPluginManager();
            if (plm.getPermission(permission.getName()) != null) {
                if (override) {
                    plm.removePermission(permission.getName());
                } else {
                    BukkitClj.logger().warn("Permission {} is already registered, skipping", permission.getName());
                    return;
                }
            }

            plm.addPermission(permission);
        }
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
            Map<String, Command> knownCommands = new HashMap<>(Bukkit.getCommandMap().getKnownCommands());
            for (ClojureCommandFn command : getCommands().values()) {
                command.unregister(Bukkit.getCommandMap());
                knownCommands.forEach((label, cmd) -> {
                    if (cmd == command) {
                        Bukkit.getCommandMap().getKnownCommands().remove(label);
                    }
                });
            }

            // Unregister permissions
            for (Permission permission : getPermissions().keySet()) {
                Bukkit.getPluginManager().removePermission(permission);
            }
        }
    }
}
