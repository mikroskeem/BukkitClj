(ns bukkitclj.api
  (:import (org.bukkit Bukkit)
           (org.bukkit ChatColor)
           (org.bukkit.command CommandSender)
           (org.bukkit.entity Player)
           (eu.mikroskeem.bukkitclj BukkitClj))
  (:gen-class))

(defn get-player
  "Gets online player object"
  [^String name]
  (Bukkit/getPlayer name))

(defn get-player-exact
  "Gets online player object by their exact name"
  [^String name]
  (Bukkit/getPlayerExact name))

(defmacro on
  "Adds an event listener for specified event"
  [the-class priority func]
   `(BukkitClj/createEventListener
     ~*ns*
     (if-not (instance? Class ~the-class)
       (Class/forName (.toString ~the-class))
       ~the-class)
     ~priority
     ~func))

(defmacro def-command
  "Defines a command"
  [command-name func]
  `(BukkitClj/createCommand ~*ns* ~command-name ~func))

(defn message
  "Sends a message to CommandSender"
  [^CommandSender sender ^String message]
  (.sendMessage sender message))

(defn colorize
  "Colorizes messages using ChatColor utility"
  [^String message]
  (ChatColor/translateAlternateColorCodes
   (char (-> "&" .getBytes first)) ; Wow this is annoying
   message))

;(defn schedule
;  "Schedules a task"
;  [options func]
;  ())