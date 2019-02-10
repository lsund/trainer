(ns trainer.core
  "Namespace that defines the system of components."
  (:require
   [com.stuartsierra.component :as c]
   [trainer.app :as app]
   [trainer.server :as server]
   [trainer.db :as db]))

(defn new-system
  [config]
  (c/system-map :server (c/using (server/new-server (:server config))
                                 [:app])
                :app (c/using (app/new-app (:app config))
                              [])
                #_:db #_(c/using (db/new-db (:db config))
                             [])))
