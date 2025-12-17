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

import clojure.java.api.Clojure;
import clojure.lang.Compiler;
import clojure.lang.DynamicClassLoader;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;
import eu.mikroskeem.bukkitclj.api.ScriptManager;
import eu.mikroskeem.bukkitclj.command.BukkitCljCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * @author Mark Vainomaa
 */
public final class BukkitClj extends JavaPlugin implements ScriptManager {
    static ClassLoader clojureClassLoader;
    private final ReentrantReadWriteLock loadingLock = new ReentrantReadWriteLock();
    private final Map<String, ScriptInfo> scripts = new HashMap<>(); // Script filename -> script info

    static Path scriptsPath;
    static Path scriptDataPath;
    static ScriptInfo currentScript = null;

    @Override
    public void onEnable() {
        // Set up scripts directory
        scriptsPath = getDataFolder().toPath().resolve("scripts");
        if (Files.notExists(scriptsPath)) {
            try {
                Files.createDirectories(scriptsPath);
            } catch (IOException e) {
                getSLF4JLogger().error("Failed to create {} directory", scriptsPath);
                setEnabled(false);
                return;
            }
        }

        // Set up scripts data directory
        scriptDataPath = getDataFolder().toPath().resolve("script-data");
        if (Files.notExists(scriptDataPath)) {
            try {
                Files.createDirectories(scriptDataPath);
            } catch (IOException e) {
                getSLF4JLogger().error("Failed to create {} directory", scriptDataPath);
                setEnabled(false);
                return;
            }
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
            RT.load("clojure/core");
            RT.load("bukkitclj/api");
            RT.load("bukkitclj/internal");
        } catch (Exception e) {
            logger().error("Failed to initialize Clojure runtime", e);
            setEnabled(false);
            return;
        } finally {
            // Restore class loader hackery
            Thread.currentThread().setContextClassLoader(oldTCL);
        }

        // Register commands
        registerCommand("bukkitclj", new BukkitCljCommand(this));

        // Load scripts
        logger().info("Loading scripts...");
        long startTime = System.nanoTime();
        loadingLock.writeLock().lock();
        try (Stream<Path> files = Files.list(scriptsPath)) {
            files.filter(it -> it.getFileName().toString().endsWith(".clj")).forEach(scriptFile -> {
                try {
                    loadScript(scriptFile.getFileName().toString());
                } catch (Exception e) {
                    logger().error("Failed to load {}", scriptFile, e);
                }
            });
        } catch (IOException e) {
            logger().error("Failed to list files in {}", scriptsPath, e);
            setEnabled(false);
            return;
        } finally {
            loadingLock.writeLock().unlock();
        }
        long endTime = System.nanoTime();
        logger().info("Loaded {} script(s) in {}ms!", scripts.size(), TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
    }

    @Override
    public void onDisable() {
        for (ScriptInfo script : scripts.values()) {
            script.unload(false);
        }
    }

    @Override
    public Path getScriptsDirectory() {
        return scriptsPath;
    }

    /**
     * @inheritDoc
     */
    @Override
    public ScriptInfo getScript(String name) {
        try {
            loadingLock.readLock().lock();
            return scripts.get(name);
        } finally {
            loadingLock.readLock().unlock();
        }
    }

    /**
     * @inheritDoc
     */
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

    /**
     * @inheritDoc
     */
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

    /**
     * @inheritDoc
     */
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

    /**
     * @inheritDoc
     */
    @Override
    public List<ScriptInfo> listScripts() {
        try {
            loadingLock.readLock().lock();
            return Collections.unmodifiableList(new ArrayList<>(scripts.values()));
        } finally {
            loadingLock.readLock().unlock();
        }
    }

    private ScriptInfo loadScriptFromFile(Path scriptFile) throws Exception {
        String ns = ScriptHelper.getNamespace(scriptFile);
        ScriptInfo info = currentScript = new ScriptInfo(ns, scriptFile);

        try (ScriptHelper.ContextClassloaderWrapper c = ScriptHelper.withNewDynClassloader()) {
            try (Reader reader = Files.newBufferedReader(scriptFile)) {
                // Compile script and load it
                Compiler.load(reader, scriptFile.toString(), scriptFile.getFileName().toString());
            }

            // Set classloader
            currentScript.setClassLoader((DynamicClassLoader) c.getClassLoader());

            // Register all gathered event handlers, commands and permissions
            currentScript.load();

            // Initialize script if init method is present
            IFn scriptInitFunc = Clojure.var(ns, "script-init");
            try {
                scriptInitFunc.invoke();
            } catch (Exception e) {
                if (!(e instanceof IllegalStateException) || !e.getMessage().startsWith("Attempting to call unbound fn:")) {
                    throw new RuntimeException(e);
                }
            }
        }

        currentScript = null;
        return info;
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            logger().warn("Command '{}' not registered in plugin.yml!", name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter) {
            command.setTabCompleter((TabCompleter) executor);
        }
    }

    public static BukkitClj getInstance() {
        return JavaPlugin.getPlugin(BukkitClj.class);
    }

    static Logger logger() {
        return getInstance().getSLF4JLogger();
    }
}