(ns trainer.render
  "Namespace for rendering hiccup"
  (:require
   [trainer.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [trainer.util :as util]
   [trainer.html :as html]))

(defn index
  [{:keys [db] :as config} exercise-list]
  (html5
   [:head
    [:title "Trainer"]]
   [:body
    [:h1 "Trainer"]
    [:p "This is a program for logging your gym results."]
    [:h3 "Add Exercise"]
    (form-to [:post "/add-exercise"]
             [:input {:name "name" :type :text :placeholder "Exercise name"}]
             [:button.mui-btn "Add exercise"])
    [:h3 "Make a new plan"]
    (form-to [:post "/add-to-plan"]
             [:select {:name "exercise"}
              (for [e (db/all db :exercise)]
                [:option {:value (:id e)} (:name e)])]
             [:button.mui-btn "Add to plan"])
    [:h3 "Current plan"]
    [:table
     [:thead
      [:tr
       [:th "Name"]]]
     [:tbody
      (for [id exercise-list]
        [:tr
         [:td id]])]]
    (form-to [:post "/save-plan"]
             [:input {:name "name" :type :text :placeholder "Plan name"}]
             [:button.mui-btn "Save plan"])
    [:h3 "Existing plans"]
    [:ul
     (for [p (db/all db :plan)]
       [:li
        [:div (:name p)]
        [:div
         [:ul
          (for [t (db/tasks-for-plan db (:id p))]
            [:li (:name (db/id->exercise db (:exerciseid t)))])]]])]

    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(def not-found (html5 "not found"))
