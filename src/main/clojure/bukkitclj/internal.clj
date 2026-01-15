(ns bukkitclj.internal
  (:gen-class))

; Thanks to @eallik for this :)
(defn get-file-ns [file-path]
  (let [sexps (load-string (str "'(" (slurp file-path) ")"))]
    (second (first sexps))))

(defn get-clojure-class-loader
  "Returns the root Clojure class loader shared by all scripts"
  []
  eu.mikroskeem.bukkitclj.BukkitClj/clojureClassLoader)