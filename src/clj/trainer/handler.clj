(ns trainer.handler
  "Namespace for handling routes"
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [cheshire.core :refer [generate-string]]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [taoensso.timbre :as logging]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect]]
            [trainer.db :as db]
            [trainer.render :as render]
            [trainer.util :as util]))

(defn process-exercise-property [[k v]]
  (let [[_ id prop & _] (string/split k #"_")]
    {:id (util/parse-int id)
     :key (keyword prop)
     :value (if (= v "on")
              :true
              (if-not (= (keyword prop) :duration)
                (util/parse-int v)
                v))}))

(defn make-exercise [id props]
  (assoc (into {} (for [prop props]
                    [(:key prop) (:value prop)]))
         :id
         id))

(defn is-weightlift-property [[s _]]
  (and (string? s)
       (re-matches #"1_[0-9]+_.*" s)))

(defn is-cardio-property [[s _]]
  (and (string? s)
       (re-matches #"2_[0-9]+_.*" s)))

(defn save-plan-instance [db params]
  (let [planid (-> params :plan util/parse-int)
        day (util/->localdate (:day params))
        weightlifts (for [[id props]
                          (group-by :id (for [p (filter is-weightlift-property
                                                        params)]
                                          (process-exercise-property p)))]
                      (make-exercise id props))
        cardios (for [[id props] (group-by :id
                                           (for [p (filter is-cardio-property
                                                           params)]
                                             (process-exercise-property p)))]
                  (make-exercise id props))]
    (doseq [e weightlifts]
      (when-not (:skip e)
        (db/add db
                :doneweightlift
                (merge {:day day
                        :planid planid
                        :exerciseid (:id e)}
                       (select-keys e [:sets :reps :weight])))
        (cond (:increment e) (db/nudge-weight db
                                              :inc
                                              (db/row db :weightlift (:id e)))
              (:decrement e) (db/nudge-weight db
                                              :dec
                                              (db/row db :weightlift (:id e))))))
    (doseq [cardio cardios]
      (when-not (:skip cardio)
        (db/add db
                :donecardio
                (merge {:day day
                        :planid planid
                        :exerciseid (:id cardio)}
                       (select-keys cardio
                                    [:duration
                                     :distance
                                     :highpulse
                                     :lowpulse
                                     :level])))))
    (db/increment-plan-completed-count db planid)))

(defn- increment-goal-task []
  (try
    (client/post "http://localhost:3007/nudge/at/task"
                 {:form-params {"id" 20
                                "url" "/"}})
    (catch Exception e
      (logging/info (str "Could not make request to goal-tracker " (.getMessage e))))))

(defn exercises-for-plans [db ids]
  {:weightlifts-for-plans (group-by :planid (db/weightlifts-for-plans db ids))
   :cardios-for-plans (group-by :planid (db/cardios-for-plans db ids))})

(defn index [db config weightlift-list cardio-list]
  (let [plans (db/active-plans db)
        params (merge {:weightlifts (db/all db :weightlift)
                       :cardios (db/all db :cardio)
                       :current-plan-weightlifts (db/subset db
                                                            :weightlift
                                                            (mapv util/parse-int weightlift-list))
                       :current-plan-cardios (db/subset db
                                                        :cardio
                                                        (mapv util/parse-int cardio-list))
                       :plans plans}
                      (exercises-for-plans db (map :id plans)))]
    (render/index config params)))

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" {:keys [session]}
        (index db config (:weightlift-list session) (:cardio-list session)))
   (GET "/history" []
        (render/history config {:done-cardios (db/all-done-cardios-with-name db)
                                :done-weightlifts (db/all-done-weightlifts-with-name db)}))
   (GET "/squash" []
        (render/squash config {:statistics (db/squash-statistics db nil)
                               :opponents (sort-by :name (db/all db :squashopponent))
                               :results (db/squash-results db)}))
   (GET "/squash-opponent" [id]
        (render/squash-opponent config
                                {:statistics
                                 (db/squash-statistics db (util/parse-int id))

                                 :results
                                 (db/squash-results db (util/parse-int id))}))
   (GET "/plot/:etype" [eid fst snd etype]
        (render/plot (keyword etype)
                     config
                     {:eid (util/parse-int eid)
                      :fst fst
                      :snd snd}))
   (POST "/add-weightlift" [name sets reps weight]
         (db/add db :weightlift {:name name
                                 :sets (util/parse-int sets)
                                 :reps (util/parse-int reps)
                                 :weight (util/parse-int weight)})
         (redirect "/"))
   (POST "/add-cardio" [name duration distance highpulse lowpulse level type]
         (db/add db :cardio {:name name
                             :duration (util/parse-int-or-nil duration)
                             :distance (util/parse-int-or-nil distance)
                             :highpulse (util/parse-int-or-nil highpulse)
                             :lowpulse (util/parse-int-or-nil lowpulse)
                             :level (util/parse-int-or-nil level)})
         (redirect "/"))
   (POST "/update-weightlift" [id sets reps weight]
         (db/update-row db :weightlift {:sets (util/parse-int sets)
                                        :reps (util/parse-int reps)
                                        :weight (util/parse-int weight)} (util/parse-int id))
         (redirect "/"))
   (POST "/update-cardio" [id duration distance highpulse lowpulse level]
         (db/update-row db :cardio     {:duration duration
                                        :distance (util/parse-int-or-nil distance)
                                        :highpulse (util/parse-int-or-nil highpulse)
                                        :lowpulse (util/parse-int-or-nil lowpulse)
                                        :level (util/parse-int-or-nil level)} (util/parse-int id))
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
         (increment-goal-task)
         (-> (redirect "/")
             (assoc :session nil)))
   (GET "/complete-plan" [plan]
        (render/complete-plan (assoc (exercises-for-plans db [plan]) :id (util/parse-int plan))))
   (POST "/add-squash-result" [day opponentid myscore opponentscore]
         (db/add db
                 :squashresult
                 {:day (util/->localdate day)
                  :opponentid (util/parse-int opponentid)
                  :myscore (util/parse-int myscore)
                  :opponentscore (util/parse-int opponentscore)})
         (redirect "/squash"))
   (POST "/add-squash-opponent" [name]
         (db/add db
                 :squashopponent
                 {:name name})
         (redirect "/squash"))
   (route/resources "/")
   (GET "/export-squash-result" []
        (generate-string (db/export-squash-result db)))
   (GET "/export-done-cardio" []
        (generate-string (db/export-done-cardio db)))
   (GET "/export-done-weightlift" []
        (generate-string (db/export-done-weightlift db)))

   (route/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
