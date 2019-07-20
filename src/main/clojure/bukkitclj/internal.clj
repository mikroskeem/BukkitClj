(ns bukkitclj.internal
  (:gen-class))

; Thanks to @eallik for this :)
(defn get-file-ns [file-path]
  (let [sexps (load-string (str "'(" (slurp file-path) ")"))]
    (second (first sexps))))