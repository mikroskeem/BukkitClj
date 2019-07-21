/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import clojure.java.api.Clojure;
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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static eu.mikroskeem.bukkitclj.Utils.get;
import static eu.mikroskeem.bukkitclj.Utils.run;

/**
 * @author Mark Vainomaa
 */
public final class BukkitClj extends JavaPlugin {
    private Path scriptsPath;
    private ClassLoader clojureClassLoader;
    private Map<String, ScriptInfo> scripts = new HashMap<>();
    private static ScriptInfo currentScript = null;

    @Override
    public void onLoad() {
        // Set up scripts directory
        scriptsPath = getDataFolder().toPath().resolve("scripts");
        if (Files.notExists(scriptsPath)) {
            run(() -> Files.createDirectories(scriptsPath));
        }

        // Hack classloaders to make Clojure runtime behave
        ClassLoader oldTCL = Thread.currentThread().getContextClassLoader();
        ClassLoader pluginCl = BukkitClj.class.getClassLoader();
        clojureClassLoader = new DynamicClassLoader(pluginCl);
        try {
            Thread.currentThread().setContextClassLoader(pluginCl);
            RT.init();
            Var.pushThreadBindings(RT.map(Compiler.LOADER, clojureClassLoader));

            // Load Clojure & bukkitclj runtime
            run(() -> RT.load("clojure/core"));
            run(() -> RT.load("bukkitclj/api"));
            run(() -> RT.load("bukkitclj/internal"));
        } finally {
            // Restore class loader hackery
            Thread.currentThread().setContextClassLoader(oldTCL);
        }
    }

    @Override
    public void onEnable() {
        loadScripts();
    }

    private List<ScriptInfo> loadScripts() {
        List<ScriptInfo> scripts = new LinkedList<>();

        // List all the script files in scripts path
        get(() -> Files.list(this.scriptsPath)).filter(it -> it.getFileName().toString().endsWith(".clj")).forEach(scriptFile -> {
            run(() -> {
                Object script;
                String ns = getNamespace(scriptFile);
                currentScript = new ScriptInfo(ns, scriptFile);
                try (Reader reader = Files.newBufferedReader(scriptFile)) {
                    // Compile script and load it
                    script = Compiler.load(reader, scriptFile.toString(), scriptFile.getFileName().toString());
                } catch (Compiler.CompilerException e) {
                    getSLF4JLogger().error("Failed to compile {}", scriptFile.getFileName(), e);
                    return;
                }

                // Initialize script if init method is present
                IFn scriptInitFunc = Clojure.var(ns, "script-init");
                try {
                    scriptInitFunc.invoke();
                } catch (Exception e) {
                    if (!e.getMessage().startsWith("Attempting to call unbound fn:")) {
                        getSLF4JLogger().error("Failed to initialize {}", scriptFile.getFileName(), e);
                        return;
                    }
                }

                // Add script to scripts list
                scripts.add(currentScript);
                currentScript = null;
            });
        });

        return scripts;
    }

    public static void createEventListener(Namespace namespace, Class<? extends Event> eventClass,
                                           Keyword priorityKeyword, boolean ignoreCancelled, IFn handler) {
        if (currentScript == null) {
            throw new IllegalStateException("Can only register listeners at script load");
        }

        String ns = namespace.getName().getName();
        if (!currentScript.getNamespace().equals(ns)) {
            throw new IllegalStateException("Namespace mismatch!");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Function cannot be nil!");
        }

        // Convert event priority
        EventPriority priority = enumMatch(priorityKeyword.getName(), EventPriority.class);
        if (priority == null) {
            getInstance().getSLF4JLogger().error("Function {} has invalid priority {}", handler, priorityKeyword);
            return;
        }

        // Register listener
        BukkitClj plugin = JavaPlugin.getPlugin(BukkitClj.class);
        ClojureListenerFn executor = new ClojureListenerFn(namespace, handler, eventClass);
        plugin.getServer().getPluginManager().registerEvent(eventClass, executor, priority, executor, plugin, ignoreCancelled);
        currentScript.getListeners().add(executor);
    }

    public static void createCommand(Namespace namespace, String commandName, String permission, IFn handler) {
        if (currentScript == null) {
            throw new IllegalStateException("Can only register commands at script load");
        }

        String ns = namespace.getName().getName();
        if (!currentScript.getNamespace().equals(ns)) {
            throw new IllegalStateException("Namespace mismatch!");
        }

        if (commandName == null) {
            throw new IllegalArgumentException("Command name cannot be nil!");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Function cannot be nil!");
        }

        // Register command
        BukkitClj plugin = JavaPlugin.getPlugin(BukkitClj.class);
        ClojureCommandFn command = new ClojureCommandFn(commandName, permission, handler);
        plugin.getServer().getCommandMap().register(commandName, "bukkitclj" + ns, command);
        currentScript.getCommands().add(command);
    }

    public static void createPermission(Namespace namespace, String name, boolean override, Keyword def) {
        if (currentScript == null) {
            throw new IllegalStateException("Can only register commands at script load");
        }

        String ns = namespace.getName().getName();
        if (!currentScript.getNamespace().equals(ns)) {
            throw new IllegalStateException("Namespace mismatch!");
        }

        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be nil!");
        }

        // Convert default
        PermissionDefault permDef = enumMatch(def.getName(), PermissionDefault.class);
        if (permDef == null) {
            getInstance().getSLF4JLogger().error("Invalid permission {} default {}", name, def.getName());
            return;
        }

        // Try to register
        PluginManager plm = getInstance().getServer().getPluginManager();
        if (plm.getPermission(name) != null) {
            if (override) {
                plm.removePermission(name);
            } else {
                getInstance().getSLF4JLogger().warn("Permission {} is already registered, skipping", name);
                return;
            }
        }

        Permission perm = new Permission(name, permDef);
        plm.addPermission(perm);
        currentScript.getPermissions().add(perm);
    }

    public static BukkitClj getInstance() {
        return JavaPlugin.getPlugin(BukkitClj.class);
    }

    private static String getNamespace(Path file) {
        // TODO: failure handling?
        Symbol ns = (Symbol) RT.var("bukkitclj.internal", "get-file-ns").invoke(file.toString());
        return ns.getName();
    }

    private static <T extends Enum<T>> T enumMatch(String name, Class<T> enumClass) {
        String value = name.replace('-', '_').toUpperCase(Locale.ROOT);
        for (T enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equals(value))
                return enumConstant;
        }
        return null;
    }
}