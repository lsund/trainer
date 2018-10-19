(ns trainer.handler
  "Namespace for handling routes"
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]
   [clojure.string :as string]

   [ring.util.response :refer [redirect response]]
   [ring.middleware
    [session]
    [defaults :refer [site-defaults wrap-defaults]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]

   ;; Logging
   [taoensso.timbre :as logging]
   [taoensso.timbre.appenders.core :as appenders]

   [trainer.db :as db]
   [trainer.util :as util]
   [trainer.render :as render]))

(defn process-exercise-property [[k v]]
  (let [[_ id prop & _] (string/split k #"_")]
    {:id (util/parse-int id)
     :key (keyword prop)
     :value (if (= v "on")
              :true
              (util/parse-int v))}))

(defn make-exercise [id props]
  (assoc (into {} (for [prop props]
                    [(:key prop) (:value prop)]))
         :id
         id))

(defn is-weightlift-property [[s _]]
  ;; `EXERCISETYPE_ ID_NAME` Example: `2_8_reps` means: the number of repetitions (reps) of a
  ;; cardio exercise (2) of id (8)
  (and (string? s)
       (re-matches #"1_[0-9]+_.*" s)))

(defn is-cardio-property [[s _]]
  ;; `EXERCISETYPE_ ID_NAME` Example: `2_8_reps` means: the number of repetitions (reps) of a
  ;; cardio exercise (2) of id (8)
  (and (string? s)
       (re-matches #"2_[0-9]+_.*" s)))

(defn save-plan-instance [db params]
  (println params)
  (let [planid (-> params :plan util/parse-int)
        day (util/->localdate (:day params))
        weightlifts (for [[id props] (group-by :id (for [p (filter is-weightlift-property params)]
                                                     (process-exercise-property p)))]
                      (make-exercise id props))
        cardios (for [[id props] (group-by :id (for [p (filter is-cardio-property params)]
                                                 (process-exercise-property p)))]
                  (make-exercise id props))]
    (doseq [e weightlifts]
      (when-not (:skip e)
        (db/add db
                :doneweightlift
                (merge {:day day
                        :planid planid
                        :exerciseid (:id e)}
                       (select-keys e [:sets :reps :weight])))))
    (doseq [e cardios]
      (when-not (:skip e)
        (db/add db
                :donecardio
                (merge {:day day
                        :planid planid
                        :exerciseid (:id e)}
                       (select-keys e [:duration :distance :highpulse :lowpulse :level])))))
    (db/increment-plan-completed-count db planid)))

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" {:keys [session]}
        (render/index config session))
   (GET "/history" []
        (render/history config))
   (POST "/add-weightlift" [name sets reps weight]
         (db/add db :weightlift {:name name
                                 :sets (util/parse-int sets)
                                 :reps (util/parse-int reps)
                                 :weight (util/parse-int weight)})
         (redirect "/"))
   (POST "/add-cardio" [name duration distance highpulse lowpulse level type]
         (db/add db :cardio {:name name
                             :duration (util/parse-int duration)
                             :distance (util/parse-int distance)
                             :highpulse (util/parse-int highpulse)
                             :lowpulse (util/parse-int lowpulse)
                             :level (util/parse-int level)})
         (redirect "/"))
   (POST "/update-weightlift" [id sets reps weight]
         (db/update db :weightlift {:sets (util/parse-int sets)
                                    :reps (util/parse-int reps)
                                    :weight (util/parse-int weight)} (util/parse-int id))
         (redirect "/"))
   (POST "/update-cardio" [id duration distance highpulse lowpulse level]
         (db/update db :cardio     {:duration (util/parse-int duration)
                                    :distance (util/parse-int distance)
                                    :highpulse (util/parse-int highpulse)
                                    :lowpulse (util/parse-int lowpulse)
                                    :level (util/parse-int level)} (util/parse-int id))
         (redirect "/"))
   (POST "/add-weightlift-to-plan" {:keys [session params]}
         (let [weightlifts (:weightlift-list session [])
               session (assoc session :weightlift-list (conj weightlifts (:weightlift params)))]
           (-> (redirect "/")
               (assoc :session session))))
   (POST "/add-cardio-to-plan" {:keys [session params]}
         (let [cardios (:cardio-list session [])
               session (assoc session :cardio-list (conj cardios (:cardio params)))]
           (-> (redirect "/")
               (assoc :session session))))
   (POST "/save-plan" {:keys [session params]}
         (db/add-plan db
                      (:name params)
                      (map util/parse-int (:weightlift-list session))
                      (map util/parse-int (:cardio-list session)))
         (-> (redirect "/")
             (assoc :session nil)))
   (POST "/save-plan-instance" {:keys [params]}
         (save-plan-instance db params)
         (-> (redirect "/")
             (assoc :session nil)))
   (GET "/complete-plan" [plan]
        (render/complete-plan config (util/parse-int plan)))
   (r/resources "/")
   (r/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
