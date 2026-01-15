(ns bukkitclj.api.java
  (:import (java.util.concurrent Callable)
           (java.util.function Consumer
                               Function
                               Supplier)))

(defn ->Callable [f]
  (reify Callable
    (call [_] (f))))

(defn ->Consumer [f]
  (reify Consumer
    (accept [_ arg1] (f arg1))))

(defn ->Function [f]
  (reify Function
    (apply [_ arg1] (f arg1))))

(defn ->Runnable [f]
  (reify Runnable
    (run [_] (f))))

(defn ->Supplier [f]
  (reify Supplier
    (get [_] (f))))