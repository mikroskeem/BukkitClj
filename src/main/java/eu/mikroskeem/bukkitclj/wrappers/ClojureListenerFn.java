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

package eu.mikroskeem.bukkitclj.wrappers;

import clojure.lang.IFn;
import clojure.lang.Namespace;
import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import eu.mikroskeem.bukkitclj.BukkitClj;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Mark Vainomaa
 */
public final class ClojureListenerFn implements Listener, EventExecutor {
    private final Namespace namespace;
    private final IFn handler;
    private final Class<? extends Event> eventClass;
    private final Method getHandlerListMethod;
    private final EventPriority eventPriority;
    private final boolean ignoreCancelled;

    public ClojureListenerFn(Namespace namespace, IFn handler, Class<? extends Event> eventClass,
                             EventPriority eventPriority, boolean ignoreCancelled) {
        this.namespace = namespace;
        this.handler = handler;
        this.eventClass = eventClass;
        this.getHandlerListMethod = getHandlerListMethod(eventClass);
        this.eventPriority = eventPriority;
        this.ignoreCancelled = ignoreCancelled;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public HandlerList getEventHandlerList() {
        try {
            return (HandlerList) this.getHandlerListMethod.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get HandlerList of " + this.eventClass.getName(), e);
        }
    }

    public void register() {
        BukkitClj plugin = BukkitClj.getInstance();
        Bukkit.getServer().getPluginManager().registerEvent(eventClass, this, eventPriority, this, plugin, ignoreCancelled);
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (listener != this)
            return;

        if (!eventClass.isAssignableFrom(event.getClass()))
            return;

        
        handler.invoke(event);
    }

    private static final Map<Class<? extends Event>, Method> handlerListMethods = new WeakHashMap<>();
    private static Method getHandlerListMethod(Class<? extends Event> eventClass) {
        return handlerListMethods.computeIfAbsent(eventClass, (clazz) -> {
            try {
                return clazz.getMethod("getHandlerList");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Class " + clazz.getName() + " has no static getHandlerList method!");
            }
        });
    }
}
