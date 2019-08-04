(ns bukkitclj.api.player
  (:require [bukkitclj.api :refer [bukkitclj-instance]])
  (:import (org.bukkit.entity Player))
  (:gen-class))

(defn hide-player
  "Hides x from y"
  [^Player x ^Player y]
  (.hidePlayer y (bukkitclj-instance) x))

(defn show-player
  "Allows x to see y who was previously hidden"
  [^Player x ^Player y]
  (.showPlayer x (bukkitclj-instance) y))

(defn can-see
  "Returns whether x can see y"
  [^Player x ^Player y]
  (.canSee x y))