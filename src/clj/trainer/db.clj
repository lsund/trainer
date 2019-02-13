(ns trainer.db
  "Namespace for database interfacing"
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [trainer.util :as util]
            [trainer.config :as config]
            [jdbc.pool.c3p0 :as pool]))

(def db-uri
  (java.net.URI. (or
    (System/getenv "DATABASE_URL")
    "postgresql://localhost:5432/trainer")))

(def user-and-password
  (if (nil? (.getUserInfo db-uri))
    nil
    (clojure.string/split (.getUserInfo db-uri) #":")))

(def spec
  (pool/make-datasource-spec
    {:classname "org.postgresql.Driver"
    :subprotocol "postgresql"
    :user (get user-and-password 0)
    :password (get user-and-password 1)
    :subname (if (= -1 (.getPort db-uri))
                (format "//%s%s" (.getHost db-uri) (.getPath db-uri))
                (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri) (.getPath db-uri)))}))

(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})

(def pg-uri
  {:dbtype "postgresql"
   :connection-uri "postgresql://localhost:5432/trainer"})

(def pg-db-val (pg-db {:name "trainer"}))

(defrecord Db [db db-config]
  c/Lifecycle
  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (pg-db db-config)))
  (stop [component]
    (println ";; [Db] Stopping database")
    component))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util

(defn new-db
  [config]
  (map->Db {:db-config config}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Insert

(defn add [& args]
  (apply jdbc/insert! args))

(defn add-plan [db name weightlift-list cardio-list]
  (jdbc/insert! db :plan {:name name})
  (let [plan (first (jdbc/query db ["SELECT * FROM plan WHERE name=?" name]))]
    (doseq [eid weightlift-list]
      (add db :plannedexercise {:planid (:id plan)
                                :exerciseid eid
                                :exercisetype 1}))
    (doseq [eid cardio-list]
      (add db :plannedexercise {:planid (:id plan)
                                :exerciseid eid
                                :exercisetype 2}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update

(defn update-row [db table update-map id]
  (jdbc/update! db table update-map ["id=?" id]))

(defn increment-plan-completed-count [db id]
  (jdbc/execute! db ["update plan set timescompleted = timescompleted + 1 WHERE id = ?" id]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query

(defn element [db table id]
  (first (jdbc/query db [(str "SELECT * FROM " (name table) " WHERE id=?") id])))

(defn all [db table]
  (jdbc/query db [(str "SELECT * FROM " (name table))]))

(defn all-where [db table clause]
  (jdbc/query db [(str "SELECT * FROM " (name table) " WHERE " clause)]))

(defn- vec->sql-list [v]
  (str "(" (string/join "," v) ")"))

(defn subset [db table ids]
  (if (not-empty ids)
    (jdbc/query db [(str "SELECT * FROM " (name table) " WHERE id IN " (vec->sql-list ids))])))

(defn value-span [db table column eid]
  (first (jdbc/query db
                  [(str "SELECT min(" (name column) "),
                                max(" (name column) ")
                         FROM " (name table) "
                         WHERE exerciseid=?") eid])))

(defn all-done-weightlifts-with-name [db]
  (jdbc/query db ["select
                doneweightlift.day, doneweightlift.sets, doneweightlift.reps,
                doneweightlift.weight, weightlift.name
                from doneweightlift
                inner join weightlift on doneweightlift.exerciseid = weightlift.id;"]))

(defn all-done-cardios-with-name [db]
  (jdbc/query db ["select donecardio.day,
                       donecardio.duration,
                       donecardio.distance,
                       donecardio.highpulse,
                       donecardio.lowpulse,
                       donecardio.level, cardio.name
                from donecardio
                inner join cardio on donecardio.exerciseid = cardio.id;"]))

(defn weightlifts-for-plans [db ids]
  (jdbc/query db
           [(str "SELECT plan.id AS planid,
                         exerciseid,
                         weightlift.name,
                         weightlift.sets,
                         weightlift.reps,
                         weightlift.weight
                  FROM plannedexercise
                  INNER JOIN weightlift
                  ON weightlift.id = exerciseid
                  AND planid IN "
                 (vec->sql-list ids)
                 " AND exercisetype = 1
                  INNER JOIN plan
                  ON plan.id = planid")]))

(defn cardios-for-plans [db ids]
  (jdbc/query db
           [(str "SELECT plan.id AS planid,
                         exerciseid,
                         cardio.name,
                         cardio.duration,
                         cardio.distance,
                         cardio.highpulse,
                         cardio.lowpulse,
                         cardio.level
                  FROM plannedexercise
                  INNER JOIN cardio
                  ON cardio.id = exerciseid
                  AND planid IN "
                 (vec->sql-list ids)
                 " AND exercisetype = 2
                  INNER JOIN plan
                  ON plan.id = planid")]))

(defn squash-results [db]
  (jdbc/query db
           ["SELECT name,
                    squashresult.day,
                    squashresult.myscore,
                    squashresult.opponentscore
             FROM squashresult
             INNER JOIN squashopponent
             ON squashresult.opponentid = squashopponent.id
             ORDER BY day DESC"]))

(defn active-plans [db]
  (jdbc/query db ["select * from plan where active = 't' order by timescompleted asc"]))

(defn cardio-ids-for-plan [db id]
  (map :exerciseid
       (jdbc/query db
                ["SELECT exerciseid FROM plannedexercise WHERE planid=? and exercisetype=2" id])))

(defn timeline [db column exerciseid]
  (let [table (if (some #{column} [:sets :reps :weight]) "doneweightlift" "donecardio")]
    (println column table exerciseid)
    (jdbc/query db [(str "SELECT day," (name column) " FROM " table " WHERE exerciseid = ?")  exerciseid])))
