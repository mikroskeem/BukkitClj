/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import clojure.lang.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mark Vainomaa
 */
public final class LoggerHelper {
    public static Logger get(Namespace ns) {
        return LoggerFactory.getLogger("BukkitClj/" + ns.getName().getName());
    }

    public static void log(String level, Logger logger, String fmt, Object[] args) {
        switch (level) {
            case "debug": { logger.debug(fmt, args); break; }
            case "info": { logger.info(fmt, args); break; }
            case "warn": { logger.warn(fmt, args); break; }
            case "error": { logger.error(fmt, args); break; }
        }
    }
}
