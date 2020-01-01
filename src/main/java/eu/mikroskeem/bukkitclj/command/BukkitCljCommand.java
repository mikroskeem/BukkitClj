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

package eu.mikroskeem.bukkitclj.command;

import eu.mikroskeem.bukkitclj.BukkitClj;
import eu.mikroskeem.bukkitclj.ScriptInfo;
import eu.mikroskeem.bukkitclj.api.ScriptManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mark Vainomaa
 */
public final class BukkitCljCommand implements CommandExecutor, TabCompleter {
    private final ScriptManager manager;
    private final List<String> allSubcommands = Arrays.asList("list", "load", "unload", "reload");

    public BukkitCljCommand(ScriptManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(String.format("Usage: /%s <%s>", label, String.join("/", allSubcommands)));
            return true;
        }

        switch (args[0]) {
            case "list": {
                if (args.length != 1) {
                    sender.sendMessage(String.format("Usage: /%s list", label));
                    return true;
                }

                List<ScriptInfo> scripts = manager.listScripts();
                sender.sendMessage("Loaded scripts (" + scripts.size() + "):");
                for (ScriptInfo script : scripts) {
                    sender.sendMessage("- " + script.getScriptName());
                }
                break;
            }
            case "load": {
                if (args.length != 2) {
                    sender.sendMessage(String.format("Usage: /%s load <script file name>", label));
                    return true;
                }

                String scriptName = args[1];
                try {
                    if (manager.loadScript(scriptName) == null) {
                        throw new IllegalArgumentException("File does not exist");
                    }
                    sender.sendMessage(String.format("Script '%s' loaded successfully", scriptName));
                } catch (Exception e) {
                    sender.sendMessage("Failed to load script '" + scriptName + "': " + e.getMessage());
                    BukkitClj.getInstance().getSLF4JLogger().warn("Failed to load {}", scriptName, e);
                }
                break;
            }
            case "unload": {
                if (args.length != 2) {
                    sender.sendMessage(String.format("Usage: /%s unload <script file name>", label));
                    return true;
                }

                String scriptName = args[1];
                try {
                    ScriptInfo info = manager.getScript(scriptName);
                    if (info == null) {
                        sender.sendMessage("Script '" + scriptName + "' is not loaded");
                        break;
                    }
                    manager.unloadScript(info);
                    sender.sendMessage("Script '" + scriptName + "' unloaded successfully!");
                } catch (Exception e) {
                    sender.sendMessage("Failed to unload script '" + scriptName + "': " + e.getMessage());
                    BukkitClj.getInstance().getSLF4JLogger().warn("Failed to load {}", scriptName, e);
                }
                break;
            }
            case "reload": {
                if (args.length != 2) {
                    sender.sendMessage(String.format("Usage: /%s reload <script file name>", label));
                    return true;
                }

                String scriptName = args[1];
                try {
                    manager.reloadScript(scriptName);
                    sender.sendMessage("Script '" + scriptName + "' reloaded successfully!");
                } catch (Exception e) {
                    sender.sendMessage("Failed to reload script '" + scriptName + "': " + e.getMessage());
                    BukkitClj.getInstance().getSLF4JLogger().warn("Failed to reload {}", scriptName, e);
                }
                break;
            }
            default: {
                sender.sendMessage(String.format("Usage: /%s <%s>", label, String.join("/", allSubcommands)));
                return true;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String lastArg = args.length > 0 ? args[args.length - 1] : "";
        Collection<String> completions = Collections.emptyList();
        if (args.length == 1) {
            completions = allSubcommands;
        } else if (args.length == 2) {
            if (args[0].equals("load")) {
                try {
                    List<Path> scriptFiles = Files.list(manager.getScriptsDirectory())
                            .filter(p -> p.toString().endsWith(".clj"))
                            .collect(Collectors.toList());

                    // Exclude already loaded scripts
                    manager.listScripts().stream()
                            .map(ScriptInfo::getScriptPath)
                            .forEach(scriptFiles::remove);

                    completions = scriptFiles.stream()
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .collect(Collectors.toList());
                } catch (IOException ignored) {}
            } else if (args[0].equals("unload") || args[0].equals("reload")) {
                completions = manager.listScripts().stream()
                        .map(ScriptInfo::getScriptPath)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .collect(Collectors.toList());
            }
        }
        return StringUtil.copyPartialMatches(lastArg, completions, new LinkedList<>());
    }
}
