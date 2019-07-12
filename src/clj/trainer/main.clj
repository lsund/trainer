(ns trainer.main
  "Main entry point"
  (:require
   [trainer.config :as config]
   [trainer.core :refer [new-system]]
   [com.stuartsierra.component :as c])
  (:gen-class))

(defn -main [& args]
  (c/start (new-system (config/load-local)))
  (println "Server up and running"))
