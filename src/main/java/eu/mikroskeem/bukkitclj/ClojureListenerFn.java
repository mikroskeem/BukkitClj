/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import clojure.lang.IFn;
import clojure.lang.Namespace;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

/**
 * @author Mark Vainomaa
 */
public final class ClojureListenerFn implements Listener, EventExecutor {
    private final Namespace namespace;
    private final IFn handler;
    private final Class<? extends Event> eventClass;

    public ClojureListenerFn(Namespace namespace, IFn handler, Class<? extends Event> eventClass) {
        this.namespace = namespace;
        this.handler = handler;
        this.eventClass = eventClass;
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
