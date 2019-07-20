(ns bukkitclj.api
  (:import (org.bukkit Bukkit)
          (org.bukkit.entity Player))
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
   `(eu.mikroskeem.bukkitclj.BukkitClj/createEventListener
     ~*ns*
     (if-not (instance? Class ~the-class)
       (Class/forName (.toString ~the-class))
       ~the-class)
     ~priority
     ~func))