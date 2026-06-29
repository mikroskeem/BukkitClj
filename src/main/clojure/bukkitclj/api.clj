(ns bukkitclj.api
  (:import (org.bukkit Bukkit ChatColor)
           (org.bukkit.command CommandSender)
           (org.bukkit.entity Player)
           (org.bukkit.event Cancellable Event)
           (eu.mikroskeem.bukkitclj BukkitClj ScriptHelper))
  (:gen-class))

(defn get-player
  "Returns an online player object by name"
  [^String name]
  (Bukkit/getPlayer name))

(defn get-player-exact
  "Retruns an online player object by the player's exact name"
  [^String name]
  (Bukkit/getPlayerExact name))

(defn all-players
  "Returns all online players"
  []
  (Bukkit/getOnlinePlayers))

(defn bukkitclj-instance
  "Returns the BukkitClj plugin instance"
  []
  (BukkitClj/getInstance))

(defmacro on
  "Adds an event listener for the specified event"
  [the-class options func]
   `(ScriptHelper/createEventListener
     ~*ns*
     (if-not (instance? Class ~the-class)
       (Class/forName (.toString ~the-class))
       ~the-class)
     (:priority ~options :normal)
     (:ignore-cancelled ~options false)
     ~func))

(defn cancel-event
  "Cancels a cancellable event"
  [^Event event]
  (when (instance? Cancellable event)
    (.setCancelled event true)))

(defmacro def-command
  "Defines a command"
  [options func]
  `(ScriptHelper/createCommand
    ~*ns*
    (:name ~options nil)
    (:permission ~options nil)
    (into-array String (:aliases ~options []))
    ~func))

(defmacro def-command-completion
  "Defines a command completion"
  [options func]
  `(ScriptHelper/createCommandCompletion
    ~*ns*
    (:name ~options nil)
    ~func))

(defmacro def-brigadier-command
  "Defines a Brigadier command from a built LiteralCommandNode, or a LiteralArgumentBuilder
   (which is built automatically). The command literal/tree, executors, suggestions and
   permission gating all live in the node itself.

   options:
     :description - optional help description
     :aliases     - optional vector of alias literals

   Brigadier commands are registered through Paper's command lifecycle. They go live at startup
   and are re-registered on every server reload; a runtime script (re)load triggers that reload
   automatically, so they hot-reload like ordinary def-command commands."
  [options node]
  `(let [node# ~node
         node# (if (instance? com.mojang.brigadier.tree.LiteralCommandNode node#)
                 node#
                 (.build ^com.mojang.brigadier.builder.LiteralArgumentBuilder node#))]
     (ScriptHelper/createBrigadierCommand
       ~*ns*
       node#
       (:description ~options nil)
       (into-array String (:aliases ~options [])))))

(defmacro def-permission
  "Defines a permission.
  Not usually needed except when defining who has said permission by default"
  [opts]
  `(ScriptHelper/createPermission
    ~*ns*
    (:name ~opts nil)
    (:override ~opts false)
    (:default ~opts :op)))

(defn message
  "Sends a message to a CommandSender"
  [^CommandSender sender ^String message]
  (.sendMessage sender message))

(defn colorize
  "Colorizes a message using the ChatColor utility"
  [^String message]
  (ChatColor/translateAlternateColorCodes \& message))

(defn has-perm
  "Returns true if the given CommandSender has the given permission"
  [^CommandSender sender ^String node]
  (.hasPermission sender node))

;(defn schedule
;  "Schedules a task"
;  [options func]
;  ())
