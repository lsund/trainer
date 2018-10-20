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
  (first (j/query db [(str "SELECT * FROM " (name table) " WHERE id=?") id])))

(defn add-plan [db name weightlift-list cardio-list]
  (j/insert! db :plan {:name name})
  (let [plan (first (j/query db ["SELECT * FROM plan WHERE name=?" name]))]
    (doseq [eid weightlift-list]
      (add db :plannedexercise {:planid (:id plan)
                                :exerciseid eid
                                :exercisetype 1}))
    (doseq [eid cardio-list]
      (add db :plannedexercise {:planid (:id plan)
                                :exerciseid eid
                                :exercisetype 2}))))
(defn all [db table]
  (j/query db [(str "SELECT * FROM " (name table))]))

(defn all-where [db table clause]
  (j/query db [(str "SELECT * FROM " (name table) " WHERE " clause)]))

(defn all-done-weightlifts-with-name [db]
  (j/query db ["select
                doneweightlift.day, doneweightlift.sets, doneweightlift.reps,
                doneweightlift.weight, weightlift.name
                from doneweightlift
                inner join weightlift on doneweightlift.exerciseid = weightlift.id;"]))

(defn all-done-cardios-with-name [db]
  (j/query db ["select donecardio.day,
                       donecardio.duration,
                       donecardio.distance,
                       donecardio.highpulse,
                       donecardio.lowpulse,
                       donecardio.level, cardio.name
                from donecardio
                inner join cardio on donecardio.exerciseid = cardio.id;"]))

(defn weightlifts-for-plan [db id]
  (j/query db
           ["select exerciseid,
                    weightlift.name,
                    weightlift.sets,
                    weightlift.reps,
                    weightlift.weight
             from plannedexercise
             inner join weightlift
             on weightlift.id = exerciseid
             and planid = ?
             and exercisetype = 1" id]))

(defn cardios-for-plan [db id]
  (j/query db
           ["select exerciseid,
                    cardio.name,
                    cardio.duration,
                    cardio.distance,
                    cardio.highpulse,
                    cardio.lowpulse,
                    cardio.level
             from plannedexercise
             inner join cardio
             on cardio.id = exerciseid
             and planid = ?
             and exercisetype = 2" id]))

(defn cardio-ids-for-plan [db id]
  (map :exerciseid
       (j/query db
                ["SELECT exerciseid FROM plannedexercise WHERE planid=? and exercisetype=2" id])))

(defn update [db table update-map id]
  (j/update! db table update-map ["id=?" id]))

(defn increment-plan-completed-count [db id]
  (j/execute! db ["update plan set timescompleted = timescompleted + 1 WHERE id = ?" id]))
