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

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * A classloader that delegates to every loaded plugin's classloader, allowing
 * scripts to import or reference classes from any plugin by fully-qualified name
 * regardless of Paper's plugin classloader isolation.
 *
 * <p>Parent-first delegation means BukkitClj's own classes and the global legacy
 * plugin pool resolve first; the per-plugin fan-out in {@link #findClass(String)}
 * only runs for classes nobody in the parent chain exposed (e.g. isolated Paper
 * plugins). The lookup is dynamic so plugins loaded after BukkitClj are visible.
 *
 * @author Mark Vainomaa
 */
public final class AllPluginsClassLoader extends ClassLoader {
    static {
        registerAsParallelCapable();
    }

    public AllPluginsClassLoader(ClassLoader parent) {
        super("all-plugins", parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Plugin self = BukkitClj.getInstance();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin == self) {
                continue;
            }
            try {
                return plugin.getClass().getClassLoader().loadClass(name);
            } catch (ClassNotFoundException ignored) {
                // Try the next plugin
            }
        }
        throw new ClassNotFoundException(name);
    }
}
