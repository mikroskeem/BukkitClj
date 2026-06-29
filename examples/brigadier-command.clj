(ns brigadier-command
  "Demonstrates def-brigadier-command: a Paper Brigadier command defined from a script.

   Brigadier commands are registered through Paper's command lifecycle (LifecycleEvents.COMMANDS),
   not the legacy command map. BukkitClj drains every loaded script's nodes into the registrar when
   that event fires, and re-fires it (via Bukkit.reloadData()) after a runtime script (re)load, so
   these hot-reload just like def-command commands.

   This example uses Paper's native builder API directly so it stands alone; in practice you'd
   likely wrap it in a friendlier DSL."
  (:require [bukkitclj.api :refer [def-brigadier-command]]
            [bukkitclj.api.logger :as log])
  (:import (com.mojang.brigadier Command)
           (com.mojang.brigadier.arguments StringArgumentType)
           (io.papermc.paper.command.brigadier Commands)
           (org.bukkit.entity Player)))

(defn- executes [builder f]
  (.executes builder (reify Command
                       (run [_ ctx]
                         (f ctx)
                         Command/SINGLE_SUCCESS))))

(defn- greet [ctx]
  (let [source (.getSource ctx)
        sender (.getSender source)
        name (try (StringArgumentType/getString ctx "name")
                  (catch IllegalArgumentException _ "world"))]
    (.sendMessage sender (str "Hello, " name "!"))
    (when (instance? Player sender)
      (log/info "{} greeted {}" (.getName ^Player sender) name))))

;; /greet            -> "Hello, world!"
;; /greet <name>     -> "Hello, <name>!"
;; aliases: /hello, /hi
(def-brigadier-command {:description "Greets someone"
                        :aliases ["hello" "hi"]}
  (-> (Commands/literal "greet")
      (executes greet)
      (.then (-> (Commands/argument "name" (StringArgumentType/word))
                 (.suggests (reify com.mojang.brigadier.suggestion.SuggestionProvider
                              (getSuggestions [_ _ctx builder]
                                (doto builder
                                  (.suggest "friend")
                                  (.suggest "stranger"))
                                (.buildFuture builder))))
                 (executes greet)))))
