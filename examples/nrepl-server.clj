(ns nrepl-server
  (:require [bukkitclj.api :refer [def-command def-command-completion message]]
            [bukkitclj.api.logger :as log]
            [cemerick.pomegranate :as pom]
            [cemerick.pomegranate.aether :as aether]))

;; State
(defonce nrepl-classloader (atom nil))
(defonce ^:private server (atom nil))

(def listen-host "0.0.0.0")

(defn- setup-classloader! []
  (or @nrepl-classloader
      (let [cl (-> (Thread/currentThread) .getContextClassLoader)]
        (reset! nrepl-classloader cl)
        cl)))

(defn- load-nrepl-deps! []
  (let [cl (setup-classloader!)]
    (log/info "Loading nREPL dependencies into classloader {}" cl)
    (pom/add-dependencies
     :coordinates '[[nrepl/nrepl "1.5.2" :exclusions [[org.clojure]]]
                    [cider/cider-nrepl "0.58.0" :exclusions [[org.clojure]]]]
     :classloader cl
     :repositories (merge aether/maven-central
                          {"clojars" "https://clojars.org/repo/"}))
    (log/info "nREPL dependencies loaded")))

(defn start-server!
  ([] (start-server! 7888))
  ([port]
   (if @server
     (log/warn "nREPL server already running on {}:{}" listen-host (:port @server))
     (let [cl (setup-classloader!)
           current-ccl (-> (Thread/currentThread) .getContextClassLoader)]
       (.setContextClassLoader (Thread/currentThread) cl)
       (try
         (with-bindings {clojure.lang.Compiler/LOADER cl}
           (load-nrepl-deps!)
           (let [start-server (requiring-resolve 'nrepl.server/start-server)
                 cider-nrepl-handler (requiring-resolve 'cider.nrepl/cider-nrepl-handler)
                 wrapped-handler (fn [msg]
                                   (let [old-ccl (-> (Thread/currentThread) .getContextClassLoader)]
                                     (.setContextClassLoader (Thread/currentThread) cl)
                                     (push-thread-bindings {clojure.lang.Compiler/LOADER cl})
                                     (try
                                       (cider-nrepl-handler msg)
                                       (finally
                                         (pop-thread-bindings)
                                         (.setContextClassLoader (Thread/currentThread) old-ccl)))))
                 srv (start-server :bind listen-host :port port :handler wrapped-handler)]
             (reset! server srv)
             (log/info "nREPL server started on {}:{}" listen-host port)))
         (finally
           (.setContextClassLoader (Thread/currentThread) current-ccl)))))))

(defn stop-server! []
  (if-let [srv @server]
    (let [cl (setup-classloader!)]
      (with-bindings {clojure.lang.Compiler/LOADER cl}
        (let [stop-server (requiring-resolve 'nrepl.server/stop-server)]
          (stop-server srv)))
      (reset! server nil)
      (log/info "nREPL server stopped"))
    (log/warn "No nREPL server running")))

(def-command {:name "nrepl"}
  (fn [sender _ args]
    (let [subcmd (first args)
          port (when (second args)
                 (try (Integer/parseInt (second args))
                      (catch NumberFormatException _ nil)))]
      (case subcmd
        "start" (do
                  (if port
                    (start-server! port)
                    (start-server!))
                  (message sender "nREPL server starting..."))
        "stop"  (do
                  (stop-server!)
                  (message sender "nREPL server stopped"))
        "status" (message sender (if @server
                                   (str "nREPL running on " listen-host ":" (:port @server))
                                   "nREPL not running"))
        (message sender "Usage: /nrepl <start [port]|stop|status>")))))

(def-command-completion {:name "nrepl"}
  (fn [_ _ args]
    (when (= 1 (count args))
      (filter #(.startsWith ^String % (first args))
              ["start" "stop" "status"]))))

;; Lifecycle
(defn script-init []
  (setup-classloader!))

(defn script-deinit []
  (when @server
    (stop-server!))
  (reset! nrepl-classloader nil))