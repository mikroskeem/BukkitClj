/*
 * This file is part of project BukkitClj, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2019-2020 Mark Vainomaa <mikroskeem@mikroskeem.eu>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package eu.mikroskeem.bukkitclj;

import clojure.lang.Compiler;
import clojure.lang.DynamicClassLoader;
import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import eu.mikroskeem.bukkitclj.wrappers.ClojureCommandFn;
import eu.mikroskeem.bukkitclj.wrappers.ClojureListenerFn;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.Closeable;
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
        validateScriptState(namespace, "Can only register listeners at script load");
        validateArgument(handler, "Function cannot be nil!");

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
        validateScriptState(namespace, "Can only register commands at script load");
        validateArgument(commandName, "Command name cannot be nil!");
        validateArgument(handler, "Function cannot be nil!");

        // Register command
        BukkitClj plugin = BukkitClj.getInstance();
        ClojureCommandFn command = new ClojureCommandFn(namespace, commandName, permission, aliases, handler);
        command.register(plugin.getServer().getCommandMap());
        BukkitClj.currentScript.getCommands().put(commandName, command);
    }

    public static void createCommandCompletion(Namespace namespace, String commandName, IFn handler) {
        validateScriptState(namespace, "Can only register command completions at script load");
        validateArgument(commandName, "Command name cannot be nil!");
        validateArgument(handler, "Function cannot be nil!");

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
        validateScriptState(namespace, "Can only create permissions at script load");
        validateArgument(name, "Permission name cannot be nil!");

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

    static ContextClassloaderWrapper withContextClassloader(ClassLoader classloader) {
        return new ContextClassloaderWrapper(classloader);
    }

    static ContextClassloaderWrapper withNewDynClassloader(ScriptInfo info) {
        String name = "ScriptClassLoader[" + info.getScriptName() + " (" + info.getNamespace() + ")]";
        DynamicClassLoader dynamicClassLoader = new NamedDynamicClassLoader(BukkitClj.clojureClassLoader, name);
        Var.pushThreadBindings(RT.map(Compiler.LOADER, dynamicClassLoader));
        return new ContextClassloaderWrapper(dynamicClassLoader, () -> {
            Var.popThreadBindings();
        });
    }

    private static void validateScriptState(Namespace ns, String unsetScriptMessage) {
        if (BukkitClj.currentScript == null) {
            throw new IllegalStateException(unsetScriptMessage);
        }

        if (!BukkitClj.currentScript.getNamespace().equals(ns.getName().getName())) {
            throw new IllegalStateException("Namespace mismatch!");
        }
    }

    private static void validateArgument(Object argument, String message) {
        if (argument == null) {
            throw new IllegalArgumentException(message);
        }
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

    public static class ContextClassloaderWrapper implements Closeable {
        private final ClassLoader oldClassLoader;
        private final ClassLoader classLoader;
        private final Runnable extraCallback;

        public ContextClassloaderWrapper(ClassLoader classLoader, Runnable extraCallback) {
            this.oldClassLoader = Thread.currentThread().getContextClassLoader();
            this.classLoader = classLoader;
            this.extraCallback = extraCallback;
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        public ContextClassloaderWrapper(ClassLoader classLoader) {
            this(classLoader, null);
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public void close() {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            if (extraCallback != null) {
                extraCallback.run();
            }
        }
    }
}
