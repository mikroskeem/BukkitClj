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

(defn on
  "Adds an event listener for specified event"
  [the-class, func]
  (let [c (if-not (instance? the-class Class)
            (Class/forName (.toString the-class))
            the-class)]
    (println *ns*)
    (eu.mikroskeem.bukkitclj.BukkitClj/createEventListener c func)))


