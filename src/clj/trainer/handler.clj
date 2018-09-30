(ns trainer.handler
  "Namespace for handling routes"
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]


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

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (GET "/" {:keys [session]}
        (render/index config (:exercise-list session)))
   (POST "/add-to-plan" {:keys [session params]}
         (let [exercises (:exercise-list session [])
               session (assoc session :exercise-list (conj exercises (:exercise params)))]
           (-> (redirect "/")
               (assoc :session session))))
   (POST "/save-plan" {:keys [session params]}
         (db/add-plan db (:name params) (map util/parse-int (:exercise-list session)))
         (-> (redirect "/")
             (assoc :session nil)))
   (POST "/add-exercise" [name]
         (db/add db :exercise {:name name})
         (redirect "/"))
   (r/resources "/")
   (r/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
