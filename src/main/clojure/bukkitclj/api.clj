(ns bukkitclj.api
  (:import (org.bukkit Bukkit ChatColor)
           (org.bukkit.command CommandSender)
           (org.bukkit.entity Player)
           (org.bukkit.event Cancellable Event)
           (eu.mikroskeem.bukkitclj BukkitClj ScriptHelper))
  (:gen-class))

(defn get-player
  "Gets online player object"
  [^String name]
  (Bukkit/getPlayer name))

(defn get-player-exact
  "Gets online player object by their exact name"
  [^String name]
  (Bukkit/getPlayerExact name))

(defn all-players
  "Gets all online players"
  []
  (Bukkit/getOnlinePlayers))

(defn bukkitclj-instance
  "Gets BukkitClj plugin instance"
  []
  (BukkitClj/getInstance))

(defmacro on
  "Adds an event listener for specified event"
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
  "Cancels cancellable event"
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
  "Defines a permission. Not usually needed unless you want to
  define who has said permission by default"
  [opts]
  `(ScriptHelper/createPermission
    ~*ns*
    (:name ~opts nil)
    (:override ~opts false)
    (:default ~opts :op)))

(defn message
  "Sends a message to CommandSender"
  [^CommandSender sender ^String message]
  (.sendMessage sender message))

(defn colorize
  "Colorizes messages using ChatColor utility"
  [^String message]
  (ChatColor/translateAlternateColorCodes \& message))

(defn has-perm
  "Checks if given CommandSender has permission"
  [^CommandSender sender ^String node]
  (.hasPermission sender node))

;(defn schedule
;  "Schedules a task"
;  [options func]
;  ())
