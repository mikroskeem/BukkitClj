(ns bukkitclj.api.data
  (:require [clojure.java.io :as io])
  (:import (eu.mikroskeem.bukkitclj ScriptHelper)
           (java.io File))
  (:gen-class))

(defmacro get-data-file
  "Gets script specific data file"
  []
  `(ScriptHelper/getScriptDataFile ~*ns*))

(defn load-edn
  "Loads edn from file or returns nil"
  [^File file]
  (if (.exists file)
    (with-open [r (java.io.PushbackReader. (io/reader file))]
      (binding [*read-eval* false]
        (read r false nil)))
    nil))

(defn dump-edn
  "Dumps Clojure objects into edn file"
  [^File file data]
  (with-open [w (io/writer file)]
    (binding [*out* w]
      (pr data))))