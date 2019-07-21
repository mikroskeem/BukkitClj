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
import eu.mikroskeem.bukkitclj.api.ScriptManager;
import eu.mikroskeem.bukkitclj.command.BukkitCljCommand;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static eu.mikroskeem.bukkitclj.Utils.apply;
import static eu.mikroskeem.bukkitclj.Utils.get;
import static eu.mikroskeem.bukkitclj.Utils.run;

/**
 * @author Mark Vainomaa
 */
public final class BukkitClj extends JavaPlugin implements ScriptManager {
    private Path scriptsPath;
    private ClassLoader clojureClassLoader;
    private final ReentrantReadWriteLock loadingLock = new ReentrantReadWriteLock();
    private final Map<String, ScriptInfo> scripts = new HashMap<>(); // Script filename -> script info
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
        // Register commands
        apply(getCommand("bukkitclj"), cmd -> {
            BukkitCljCommand executor = new BukkitCljCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        });

        // Load scripts
        getSLF4JLogger().info("Loading scripts...");
        long startTime = System.nanoTime();
        try {
            loadingLock.writeLock().lock();
            get(() -> Files.list(this.scriptsPath)).filter(it -> it.getFileName().toString().endsWith(".clj")).forEach(scriptFile -> {
                loadScript(scriptFile.getFileName().toString());
            });
        } finally {
            loadingLock.writeLock().unlock();
        }
        long endTime = System.nanoTime();
        getSLF4JLogger().info("Loaded {} script(s) in {}ms!", scripts.size(), TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
    }

    @Override
    public void onDisable() {
        for (ScriptInfo script : scripts.values()) {
            script.unload(false);
        }
    }

    @Override
    public ScriptInfo getScript(String name) {
        try {
            loadingLock.readLock().lock();
            return scripts.get(name);
        } finally {
            loadingLock.readLock().unlock();
        }
    }

    @Override
    public ScriptInfo loadScript(String name) {
        try {
            loadingLock.writeLock().lock();
            if (getScript(name) != null) {
                throw new IllegalArgumentException("Given script is already loaded!");
            }

            Path scriptPath = scriptsPath.resolve(name);
            if (Files.exists(scriptPath)) {
                try {
                    ScriptInfo info = loadScriptFromFile(scriptPath);
                    scripts.put(info.getScriptName(), info);
                    return info;
                } catch (Compiler.CompilerException e) {
                    throw new RuntimeException("Failed to compile " + scriptPath.getFileName(), e);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize " + scriptPath.getFileName(), e);
                }
            }
            return null;
        } finally {
            loadingLock.writeLock().unlock();
        }
    }

    @Override
    public void unloadScript(ScriptInfo script) {
        try {
            loadingLock.writeLock().lock();
            if (scripts.remove(script.getScriptName()) != script) {
                throw new IllegalArgumentException("Given script is not loaded!");
            }

            script.unload(true);
        } finally {
            loadingLock.writeLock().unlock();
        }
    }

    @Override
    public void reloadScript(String name) {
        try {
            loadingLock.writeLock().lock();

            ScriptInfo info = scripts.get(name);
            if (info == null) {
                throw new IllegalArgumentException("Given script is not loaded!");
            }

            unloadScript(info);
            loadScript(name);
        } finally {
            loadingLock.writeLock().unlock();
        }
    }

    @Override
    public List<ScriptInfo> listScripts() {
        try {
            loadingLock.readLock().lock();
            return Collections.unmodifiableList(new ArrayList<>(scripts.values()));
        } finally {
            loadingLock.readLock().unlock();
        }
    }

    private ScriptInfo loadScriptFromFile(Path scriptFile) {
        String ns = getNamespace(scriptFile);
        ScriptInfo info = currentScript = new ScriptInfo(ns, scriptFile);

        try (Reader reader = Files.newBufferedReader(scriptFile)) {
            // Compile script and load it
            Compiler.load(reader, scriptFile.toString(), scriptFile.getFileName().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Initialize script if init method is present
        IFn scriptInitFunc = Clojure.var(ns, "script-init");
        try {
            scriptInitFunc.invoke();
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException) || !e.getMessage().startsWith("Attempting to call unbound fn:")) {
                throw new RuntimeException(e);
            }
        }

        currentScript = null;
        return info;
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