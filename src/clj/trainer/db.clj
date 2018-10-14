(ns trainer.db
  "Namespace for database interfacing"
  (:require [clojure.java.jdbc :as j]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [trainer.util :as util]
            [trainer.config :as config]))


(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})


(defrecord Db [db db-config]
  c/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (pg-db db-config)))

  (stop [component]
    (println ";; [Db] Stopping database")
    component))

(defn new-db
  [config]
  (map->Db {:db-config config}))

(defn add [& args]
  (apply j/insert! args))

(defn add-plan [db name eids]
  (j/insert! db :plan {:name name})
  (let [plan (first (j/query db ["select * from plan where name=?" name]))]
    (doseq [eid eids]
      (add db :task {:planid (:id plan)
                     :exercisid eid}))))
(defn all [db table]
  (j/query db [(str "select * from " (name table))]))

(defn tasks-for-plan [db plan-id]
  (j/query db ["select * from task where planid=?" plan-id]))

(defn id->exercise [db id]
  (first (j/query db ["select * from exercise where id=?" id])))
