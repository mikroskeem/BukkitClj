/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import eu.mikroskeem.bukkitclj.wrappers.ClojureCommandFn;
import eu.mikroskeem.bukkitclj.wrappers.ClojureListenerFn;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @author Mark Vainomaa
 */
public final class ScriptHelper {
    private ScriptHelper() {}

    /*
     * Creates an event listener wrapping Clojure function
     */
    public static void createEventListener(Namespace namespace, Class<? extends Event> eventClass,
                                           Keyword priorityKeyword, boolean ignoreCancelled, IFn handler) {
        if (BukkitClj.currentScript == null) {
            throw new IllegalStateException("Can only register listeners at script load");
        }

        String ns = namespace.getName().getName();
        if (!BukkitClj.currentScript.getNamespace().equals(ns)) {
            throw new IllegalStateException("Namespace mismatch!");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Function cannot be nil!");
        }

        // Convert event priority
        EventPriority priority = enumMatch(priorityKeyword.getName(), EventPriority.class);
        if (priority == null) {
            BukkitClj.logger().error("Function {} has invalid priority {}", handler, priorityKeyword);
            return;
        }

        // Register listener
        ClojureListenerFn executor = new ClojureListenerFn(namespace, handler, eventClass, priority, ignoreCancelled);
        BukkitClj.currentScript.getListeners().add(executor);
    }

    /*
     * Creates a command handler wrapping Clojure function
     */
    public static void createCommand(Namespace namespace, String commandName,
                                     String permission, String[] aliases, IFn handler) {
        if (BukkitClj.currentScript == null) {
            throw new IllegalStateException("Can only register commands at script load");
        }

        String ns = namespace.getName().getName();
        if (!BukkitClj.currentScript.getNamespace().equals(ns)) {
            throw new IllegalStateException("Namespace mismatch!");
        }

        if (commandName == null) {
            throw new IllegalArgumentException("Command name cannot be nil!");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Function cannot be nil!");
        }

        // Register command
        BukkitClj plugin = BukkitClj.getInstance();
        ClojureCommandFn command = new ClojureCommandFn(namespace, commandName, permission, aliases, handler);
        command.register(plugin.getServer().getCommandMap());
        BukkitClj.currentScript.getCommands().put(commandName, command);
    }

    public static void createCommandCompletion(Namespace namespace, String commandName, IFn handler) {
        if (BukkitClj.currentScript == null) {
            throw new IllegalStateException("Can only register commands at script load");
        }

        String ns = namespace.getName().getName();
        if (!BukkitClj.currentScript.getNamespace().equals(ns)) {
            throw new IllegalStateException("Namespace mismatch!");
        }

        if (commandName == null) {
            throw new IllegalArgumentException("Command name cannot be nil!");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Function cannot be nil!");
        }

        // Find command and register a completion to it
        ClojureCommandFn command = BukkitClj.currentScript.getCommands().get(commandName);
        if (command == null) {
            throw new IllegalArgumentException("Command '" + commandName + "' is not registered. Did you " +
                    "define command after defining completion?");
        }

        // Set tab complete handler
        command.setTabcompleteHandler(handler);
    }

    /*
     * Creates a permission node
     */
    public static void createPermission(Namespace namespace, String name, boolean override, Keyword def) {
        if (BukkitClj.currentScript == null) {
            throw new IllegalStateException("Can only create permissions at script load");
        }

        String ns = namespace.getName().getName();
        if (!BukkitClj.currentScript.getNamespace().equals(ns)) {
            throw new IllegalStateException("Namespace mismatch!");
        }

        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be nil!");
        }

        // Convert default
        PermissionDefault permDef = enumMatch(def.getName(), PermissionDefault.class);
        if (permDef == null) {
            BukkitClj.logger().error("Invalid permission {} default {}", name, def.getName());
            return;
        }

        Permission perm = new Permission(name, permDef);
        BukkitClj.currentScript.getPermissions().put(perm, override);
    }

    /*
     * Returns script's data file by namespace
     */
    public static File getScriptDataFile(Namespace namespace) {
        return BukkitClj.scriptDataPath.resolve(namespace.getName().getName() + ".edn").toFile();
    }

    /*
     * Attempts to fetch script's namespace
     */
    static String getNamespace(Path file) {
        // TODO: failure handling?
        Symbol ns = (Symbol) RT.var("bukkitclj.internal", "get-file-ns").invoke(file.toString());
        return ns.getName();
    }

    /*
     * Fuzzy-ish enum matching helper
     */
    private static <T extends Enum<T>> T enumMatch(String name, Class<T> enumClass) {
        String value = name.replace('-', '_').toUpperCase(Locale.ROOT);
        for (T enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equals(value))
                return enumConstant;
        }
        return null;
    }
}
