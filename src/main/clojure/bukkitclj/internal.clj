(ns bukkitclj.internal
  (:gen-class))

(defn get-file-ns
  "Reads the namespace symbol from a Clojure script without evaluating it."
  [file-path]
  (let [form (binding [*read-eval* false]
               (read-string (slurp file-path)))]
    (when-not (and (seq? form) (= 'ns (first form)))
      (throw (ex-info "Script must begin with an (ns ...) form" {:file file-path})))
    (second form)))

(defn get-clojure-class-loader
  "Returns the root Clojure class loader shared by all scripts"
  []
  eu.mikroskeem.bukkitclj.BukkitClj/clojureClassLoader)