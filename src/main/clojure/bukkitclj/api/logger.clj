(ns bukkitclj.api.logger
  (:import (eu.mikroskeem.bukkitclj.wrappers LoggerHelper))
  (:gen-class))

(defmacro get-logger []
  `(LoggerHelper/get ~*ns*))

(defmacro debug [^String fmt & args]
  `(LoggerHelper/log "debug" (get-logger) ~fmt (into-array Object [~@args])))

(defmacro info [^String fmt & args]
  `(LoggerHelper/log "info" (get-logger) ~fmt (into-array Object [~@args])))

(defmacro warn [^String fmt & args]
  `(LoggerHelper/log "warn" (get-logger) ~fmt (into-array Object [~@args])))

(defmacro error [^String fmt & args]
  `(LoggerHelper/log "error" (get-logger) ~fmt (into-array Object [~@args])))