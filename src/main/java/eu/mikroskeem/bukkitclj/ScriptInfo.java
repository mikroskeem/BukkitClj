/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj;

import java.nio.file.Path;

/**
 * @author Mark Vainomaa
 */
public final class ScriptInfo {
    private final String namespace;
    private final Path scriptPath;

    public ScriptInfo(String namespace, Path scriptPath) {
        this.namespace = namespace;
        this.scriptPath = scriptPath;
    }
}
