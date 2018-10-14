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
  (println k v)
  (let [[id prop & _] (string/split k #"_")]
    {:id (util/parse-int id)
     :key (keyword prop)
     :value (util/parse-int v)}))

(defn make-exercise [id props]
  (assoc (into {} (for [prop props]
                    [(:key prop) (:value prop)]))
         :id
         id))

(defn is-exercise-property [[s _]]
  (and (string? s)
       (re-matches #"[0-9]+_.*" s)))

(defn save-plan-instance [db params]
  (let [planid (-> params :plan util/parse-int)
        day (util/->localdate (:day params))
        es
        (for [[id props] (group-by :id (for [p (filter is-exercise-property params)]
                                         (process-exercise-property p)))]
          (make-exercise id props))]
    (doseq [e es]
      (db/add db :doneexercise {:day day
                                :planid planid
                                :exerciseid (:id e)})
      (db/update db :exercise (select-keys e [:sets :reps :weight]) (:id e)))))

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" {:keys [session]}
        (render/index config (:exercise-list session)))
   (POST "/add-exercise" [name sets reps weight]
         (db/add db :exercise {:name name
                               :sets (util/parse-int sets)
                               :reps (util/parse-int reps)
                               :weight (util/parse-int weight)})
         (redirect "/"))
   (POST "/add-to-plan" {:keys [session params]}
         (let [exercises (:exercise-list session [])
               session (assoc session :exercise-list (conj exercises (:exercise params)))]
           (-> (redirect "/")
               (assoc :session session))))
   (POST "/save-plan" {:keys [session params]}
         (db/add-plan db (:name params) (map util/parse-int (:exercise-list session)))
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
