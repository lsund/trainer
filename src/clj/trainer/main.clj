(ns trainer.main
  "Namespace for running the program once"
  (:require
   [trainer.config :as config]
   [trainer.core :refer [new-system]]
   [com.stuartsierra.component :as c])
  (:gen-class))

(defn -main [& args]
  (println "Server up and running"))

;; (defn -main [& args]
;;   (c/start (new-system (config/load)))
;;   (println "Server up and running"))
