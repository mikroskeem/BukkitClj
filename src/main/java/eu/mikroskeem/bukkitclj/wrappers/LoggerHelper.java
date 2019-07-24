/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj.wrappers;

import clojure.lang.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * @author Mark Vainomaa
 */
public final class LoggerHelper {
    private LoggerHelper() {}

    public static Logger get(Namespace ns) {
        return LoggerFactory.getLogger("BukkitClj/" + ns.getName().getName());
    }

    public static void log(Level level, Logger logger, String fmt, Object[] args) {
        switch (level) {
            case TRACE: logger.trace(fmt, args); break;
            case DEBUG: logger.debug(fmt, args); break;
            case INFO:  logger.info(fmt, args); break;
            case WARN:  logger.warn(fmt, args); break;
            case ERROR: logger.error(fmt, args); break;
        }
    }
}
