(ns bukkitclj.api.logger
  (:import (org.slf4j Logger)
           (eu.mikroskeem.bukkitclj BukkitClj))
  (:gen-class))

(defn- get-logger []
  (.getSLF4JLogger (BukkitClj/getInstance)))

(defn debug [^String fmt & args]
  (.error (get-logger) fmt args))

(defn info [^String fmt & args]
  (.info (get-logger) fmt args))

(defn warn [^String fmt & args]
  (.warn (get-logger) fmt args))

(defn error [^String fmt & args]
  (.error (get-logger) fmt args))