/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import eu.mikroskeem.bukkitclj.wrappers.ClojureCommandFn;
import eu.mikroskeem.bukkitclj.wrappers.ClojureListenerFn;
import org.bukkit.permissions.Permission;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mark Vainomaa
 */
public final class ScriptInfo {
    private final String namespace;
    private final Path scriptPath;
    private final List<ClojureListenerFn> listeners;
    private final List<ClojureCommandFn> commands;
    private final List<Permission> permissions;

    public ScriptInfo(String namespace, Path scriptPath) {
        this.namespace = namespace;
        this.scriptPath = scriptPath;
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

    public List<ClojureListenerFn> getListeners() {
        return listeners;
    }

    public List<ClojureCommandFn> getCommands() {
        return commands;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }
}
