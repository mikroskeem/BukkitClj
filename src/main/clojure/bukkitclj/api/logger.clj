(ns bukkitclj.api.logger
  (:import (eu.mikroskeem.bukkitclj.wrappers LoggerHelper)
           (org.slf4j.event Level))
  (:gen-class))

(defmacro get-logger []
  `(LoggerHelper/get ~*ns*))

(defmacro trace [^String fmt & args]
  `(LoggerHelper/log Level/TRACE (get-logger) ~fmt (into-array Object [~@args])))

(defmacro debug [^String fmt & args]
  `(LoggerHelper/log Level/DEBUG (get-logger) ~fmt (into-array Object [~@args])))

(defmacro info [^String fmt & args]
  `(LoggerHelper/log Level/INFO (get-logger) ~fmt (into-array Object [~@args])))

(defmacro warn [^String fmt & args]
  `(LoggerHelper/log Level/WARN (get-logger) ~fmt (into-array Object [~@args])))

(defmacro error [^String fmt & args]
  `(LoggerHelper/log Level/ERROR (get-logger) ~fmt (into-array Object [~@args])))