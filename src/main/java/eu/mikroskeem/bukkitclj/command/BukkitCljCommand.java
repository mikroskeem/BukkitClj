/*
 * Copyright (c) 2019 Mark Vainomaa
 *
 * This source code is proprietary software and must not be distributed and/or copied without the express permission of Mark Vainomaa
 */

package eu.mikroskeem.bukkitclj.command;

import eu.mikroskeem.bukkitclj.BukkitClj;
import eu.mikroskeem.bukkitclj.ScriptInfo;
import eu.mikroskeem.bukkitclj.api.ScriptManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * @author Mark Vainomaa
 */
public final class BukkitCljCommand implements CommandExecutor, TabCompleter {
    private final ScriptManager manager;

    public BukkitCljCommand(ScriptManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("usage: /bukkitclj <list/load/unload/reload>"); // TODO
            return true;
        }

        switch (args[0]) {
            case "list": {
                if (args.length != 1) {
                    sender.sendMessage("usage: /bukkitclj list"); // TODO
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
                    sender.sendMessage("usage: /bukkitclj load <script name>"); // TODO
                    return true;
                }

                String scriptName = args[1];
                try {
                    if (manager.loadScript(scriptName) == null) {
                        throw new IllegalArgumentException("File does not exist");
                    }
                    sender.sendMessage("Script '" + scriptName + "' loaded"); // TODO
                } catch (Exception e) {
                    sender.sendMessage("Failed to load script '" + scriptName + "': " + e.getMessage()); // TODO
                    BukkitClj.getInstance().getSLF4JLogger().warn("Failed to load {}", scriptName, e);
                }
                break;
            }
            case "unload": {
                if (args.length != 2) {
                    sender.sendMessage("usage: /bukkitclj unload <script name>"); // TODO
                    return true;
                }

                String scriptName = args[1];
                try {
                    ScriptInfo info = manager.getScript(scriptName);
                    if (info == null) {
                        sender.sendMessage("Script '" + scriptName + "' is not loaded"); // TODO
                        break;
                    }
                    manager.unloadScript(info);
                    sender.sendMessage("Script '" + scriptName + "' unloaded"); // TODO
                } catch (Exception e) {
                    sender.sendMessage("Failed to unload script '" + scriptName + "': " + e.getMessage()); // TODO
                    BukkitClj.getInstance().getSLF4JLogger().warn("Failed to load {}", scriptName, e);
                }
                break;
            }
            case "reload": {
                if (args.length != 2) {
                    sender.sendMessage("usage: /bukkitclj reload <script name>"); // TODO
                    return true;
                }

                String scriptName = args[1];
                try {
                    manager.reloadScript(scriptName);
                    sender.sendMessage("Script '" + scriptName + "' reloaded"); // TODO
                } catch (Exception e) {
                    sender.sendMessage("Failed to reload script '" + scriptName + "': " + e.getMessage()); // TODO
                    BukkitClj.getInstance().getSLF4JLogger().warn("Failed to reload {}", scriptName, e);
                }
                break;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
