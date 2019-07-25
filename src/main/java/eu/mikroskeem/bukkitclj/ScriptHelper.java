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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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
        BukkitClj plugin = JavaPlugin.getPlugin(BukkitClj.class);
        ClojureListenerFn executor = new ClojureListenerFn(namespace, handler, eventClass);
        plugin.getServer().getPluginManager().registerEvent(eventClass, executor, priority, executor, plugin, ignoreCancelled);
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
        ClojureCommandFn command = new ClojureCommandFn(commandName, permission, aliases, handler);
        command.register(plugin.getServer().getCommandMap());
        plugin.getServer().getCommandMap().register(commandName, "bukkitclj" + ns, command);
        BukkitClj.currentScript.getCommands().add(command);
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

        // Try to register
        PluginManager plm = BukkitClj.getInstance().getServer().getPluginManager();
        if (plm.getPermission(name) != null) {
            if (override) {
                plm.removePermission(name);
            } else {
                BukkitClj.logger().warn("Permission {} is already registered, skipping", name);
                return;
            }
        }

        Permission perm = new Permission(name, permDef);
        plm.addPermission(perm);
        BukkitClj.currentScript.getPermissions().add(perm);
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
