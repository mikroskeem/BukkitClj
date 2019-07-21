/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj.api;

import eu.mikroskeem.bukkitclj.ScriptInfo;

import java.util.List;

/**
 * @author Mark Vainomaa
 */
public interface ScriptManager {
    ScriptInfo getScript(String name);

    ScriptInfo loadScript(String name);

    void unloadScript(ScriptInfo script);

    void reloadScript(String name);

    List<ScriptInfo> listScripts();
}
