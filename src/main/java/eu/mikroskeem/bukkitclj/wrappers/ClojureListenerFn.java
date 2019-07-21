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

import static eu.mikroskeem.bukkitclj.Utils.get;

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
        this.getHandlerListMethod = get(() -> this.eventClass.getMethod("getHandlerList"));
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public HandlerList getEventHandlerList() {
        return get(() -> (HandlerList) this.getHandlerListMethod.invoke(null));
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (listener != this)
            return;

        if (!eventClass.isAssignableFrom(event.getClass()))
            return;

        handler.invoke(event);
    }
}
