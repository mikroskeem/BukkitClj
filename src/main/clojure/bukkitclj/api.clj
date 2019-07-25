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
