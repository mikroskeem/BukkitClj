(ns bukkitclj.api.scheduler
  (:require [bukkitclj.api :refer [bukkitclj-instance]]
            [bukkitclj.api.java :refer [->Supplier]])
  (:import (java.util.concurrent CompletableFuture
                                 Executor)
           (org.bukkit Bukkit)))

(def ^Executor executor
  (-> (Bukkit/getScheduler)
      (.getMainThreadExecutor (bukkitclj-instance))))

;; NOTE: deliberately using CompletableFuture/supplyAsync instead of Scheduler/callSyncMethod
(defn run-sync! [f]
  (CompletableFuture/supplyAsync
   (->Supplier f)
   executor))