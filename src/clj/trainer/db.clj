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

(defn get-row [db table id]
  (first (j/query db [(str "select * from " (name table) " where id=?") id])))

(defn add-plan [db name eids]
  (j/insert! db :plan {:name name})
  (let [plan (first (j/query db ["select * from plan where name=?" name]))]
    (doseq [eid eids]
      (add db :planexercise {:planid (:id plan)
                             :exerciseid eid}))))
(defn all [db table]
  (j/query db [(str "select * from " (name table))]))

(defn all-done-exercises-with-name [db]
  (j/query db ["SELECT
                DoneExercise.day, DoneExercise.sets, DoneExercise.reps,
                Doneexercise.weight, exercise.name
                FROM DoneExercise
                INNER JOIN exercise on DoneExercise.exerciseId = exercise.id;"]))

(defn exercise-ids-for-plan [db id]
  (map :exerciseid (j/query db ["select exerciseid from planexercise where planid=?" id])))

(defn update [db table update-map id]
  (j/update! db table update-map ["id=?" id]))
