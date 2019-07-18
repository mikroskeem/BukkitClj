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
import clojure.lang.RT;
import clojure.lang.Var;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static eu.mikroskeem.bukkitclj.Utils.apply;
import static eu.mikroskeem.bukkitclj.Utils.get;
import static eu.mikroskeem.bukkitclj.Utils.run;

/**
 * @author Mark Vainomaa
 */
public final class BukkitClj extends JavaPlugin {
    private Path scriptsPath;
    private ClassLoader clojureClassLoader;
    private Map<String, ScriptInfo> scripts = new HashMap<>();

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
        clojureClassLoader = apply(new DynamicClassLoader(pluginCl), cl -> {
            //cl.addURL(get(() -> getFile().toURI().toURL()));
        });
        try {
            Thread.currentThread().setContextClassLoader(pluginCl);
            RT.init();
            Var.pushThreadBindings(RT.map(Compiler.LOADER, clojureClassLoader));

            // Load Clojure & bukkitclj runtime
            run(() -> RT.load("clojure/core"));
            run(() -> RT.load("bukkitclj/api"));
        } finally {
            // Restore class loader hackery
            Thread.currentThread().setContextClassLoader(oldTCL);
        }
    }

    @Override
    public void onEnable() {
        ClassLoader oldTCL = Thread.currentThread().getContextClassLoader();
        ClassLoader pluginCl = BukkitClj.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginCl);
            loadScripts();
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCL);
        }
    }

    private List<ScriptInfo> loadScripts() {
        List<ScriptInfo> scripts = new LinkedList<>();

        // List all the script files in scripts path
        get(() -> Files.list(this.scriptsPath)).filter(it -> it.getFileName().toString().endsWith(".clj")).forEach(scriptFile -> {
            run(() -> {
                Object script;
                String ns = getNamespace(scriptFile);
                try (Reader reader = Files.newBufferedReader(scriptFile)) {
                    // Compile script and load it

                    script = Compiler.load(reader, scriptFile.toString(), scriptFile.getFileName().toString());
                } catch (Compiler.CompilerException e) {
                    getSLF4JLogger().error("Failed to compile {}", scriptFile.getFileName(), e);
                    return;
                }

                // Initialize script if init method is present
                IFn scriptInitFunc = Clojure.var(ns, "script-init");
                if (scriptInitFunc != null) {
                    try {
                        scriptInitFunc.invoke();
                    } catch (Exception e) {
                        getSLF4JLogger().error("Failed to initialize {}", scriptFile.getFileName(), e);
                        return;
                    }
                }

                // Add script to scripts list
                scripts.add(new ScriptInfo(ns, scriptFile));
            });
        });

        return scripts;
    }

    public static void createEventListener(Class<? extends Event> eventClass, IFn handler) {
        // TODO: get caller class/script file
        BukkitClj plugin = JavaPlugin.getPlugin(BukkitClj.class);
        ClojureListenerFn executor = new ClojureListenerFn(handler, eventClass);
        plugin.getServer().getPluginManager().registerEvent(eventClass, executor, EventPriority.NORMAL, executor, plugin);
    }

    public static BukkitClj getInstance() {
        return JavaPlugin.getPlugin(BukkitClj.class);
    }

    private static String getNamespace(Path file) {
        // TODO: Try to read `ns` from file directly rather than extracting from file name
        String fileName = file.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf("."));
    }
}