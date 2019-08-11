(ns trainer.db
  "Namespace for database interfacing"
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as jdbc]
            [medley.core :refer [map-vals]]
            [clojure.string :as string]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [trainer.util :as util]
            [trainer.config :as config]
            [jdbc.pool.c3p0 :as pool]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Make DB Spec

;; Heroku DB Spec
(def db-uri
  (java.net.URI. (or
                  (env :heroku-postgresql-olive-url)
                  "postgresql://localhost:5432/trainer")))

(def user-and-password
  (if (nil? (.getUserInfo db-uri))
    nil
    (clojure.string/split (.getUserInfo db-uri) #":")))

(defn make-db-spec []
  (pool/make-datasource-spec
   {:classname "org.postgresql.Driver"
    :subprotocol "postgresql"
    :user (get user-and-password 0)
    :password (get user-and-password 1)
    :subname (if (= -1 (.getPort db-uri))
               (format "//%s%s" (.getHost db-uri) (.getPath db-uri))
               (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri) (.getPath db-uri)))}))

;; Local DB Spec
(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})

(def pg-uri
  {:dbtype "postgresql"
   :connection-uri "postgresql://localhost:5432/trainer"})

(def pg-db-val (pg-db {:name "trainer"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API

(defrecord Db [db db-config]
  c/Lifecycle
  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (make-db-spec)))
  (stop [component]
    (println ";; [Db] Stopping database")
    component))

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

(defmulti nudge-weight
  (fn [_ op _] op))

(defmethod nudge-weight :inc [db _ {:keys [id reps weight highreps delta]}]
  (let [maxreps (if highreps 24 12)
        minreps (if highreps 20 10)
        update-map (if (>= reps maxreps)
                     {:reps minreps
                      :weight (+ weight delta)}
                     {:reps (inc reps)})]
    (update-row db :weightlift update-map id)))

(defmethod nudge-weight :dec [db _ {:keys [id reps weight highreps delta]}]
  (let [maxreps (if highreps 24 12)
        minreps (if highreps 20 10)
        update-map (if (<= reps minreps)
                     {:reps maxreps
                      :weight (- weight delta)}
                     {:reps (dec reps)})]
    (update-row db :weightlift update-map id)))

(defn increment-plan-completed-count [db id]
  (jdbc/execute! db ["update plan set timescompleted = timescompleted + 1 WHERE id = ?" id]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query

(defn row [db table id]
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

(defn squash-results
  ([db]
   (squash-results db nil))
  ([db id]
   (let [sql (if id
               ["SELECT name,
                           squashresult.day,
                           squashresult.myscore,
                           squashresult.opponentscore,
                           squashresult.opponentid
                   FROM squashresult
                   INNER JOIN squashopponent
                   ON squashresult.opponentid = squashopponent.id
                   AND squashresult.opponentid = ?
                   ORDER BY day DESC" id]
               ["SELECT name,
                           squashresult.day,
                           squashresult.myscore,
                           squashresult.opponentscore,
                           squashresult.opponentid
                   FROM squashresult
                   INNER JOIN squashopponent
                   ON squashresult.opponentid = squashopponent.id
                   ORDER BY day DESC"])]
     (jdbc/query db sql))))

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

(defn squash-statistics
  ([db]
   (squash-statistics db nil))
  ([db opponent-id]
   (let [sql
         (if opponent-id
           ["SELECT myscore,opponentscore FROM squashresult WHERE opponentid=?"
            opponent-id]
           ["SELECT myscore,opponentscore FROM squashresult"])
         results (jdbc/query db sql)
         compare-score-by #(fn [{:keys [myscore opponentscore]}]
                             (% myscore opponentscore))]
     (map-vals count {:wins (filter (compare-score-by >) results)
                      :losses (filter (compare-score-by <) results)
                      :draws (filter (compare-score-by =) results)}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; serialize

(defn serialize-done-weightlift [db]
  (->>
   (jdbc/query db ["select doneweightlift.day, doneweightlift.sets,
                   doneweightlift.reps, doneweightlift.weight,
                   weightlift.name from doneweightlift inner join
                   weightlift on weightlift.id = doneweightlift.exerciseid;"])
   (apply list)
   (spit "/home/lsund/test.edn")))

(defn serialize-done-cardio [db]
  (->>
   (jdbc/query db ["select donecardio.day, donecardio.duration,
                   donecardio.distance, donecardio.highpulse,
                   donecardio.lowpulse, donecardio.level,
                   cardio.name from donecardio inner join
                   cardio on cardio.id = donecardio.exerciseid;"])
   (apply list)
   (spit "/home/lsund/test.edn")))
