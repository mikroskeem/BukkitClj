/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj.wrappers;

import clojure.lang.IFn;
import clojure.lang.Namespace;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
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

    public ClojureListenerFn(Namespace namespace, IFn handler, Class<? extends Event> eventClass) {
        this.namespace = namespace;
        this.handler = handler;
        this.eventClass = eventClass;
        this.getHandlerListMethod = getHandlerListMethod(eventClass);
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
