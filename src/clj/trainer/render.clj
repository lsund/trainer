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
  [{:keys [db] :as config}]
  (html5
   [:head
    [:title "Trainer"]]
   [:body
    [:h1 "Trainer"]
    [:p "This is a program for logging your gym results."]
    [:h3 "Add Exercise"]
    (form-to [:post "/add-exercise"]
             [:input
              {:name "name" :type :text :placeholder "Exercise name"}]
             [:button.mui-btn "Add exercise"])
    [:h3 "Make a new plan"]
    (form-to [:post "/add-to-plan"]
             [:select {:name "exercise"}
              (for [e (db/all-exercises db)]
                [:option {:value (:name e)} (:name e)])]
             [:button.mui-btn "Add to plan"])
    (form-to [:post "/save-plan"]
             [:button.mui-btn "Save plan"])
    #_[:h3 "Existing exercises"]
    #_[:table
       [:thead
        [:tr
         [:th "Name"]]]
       [:tbody
        (for [e (db/all-exercises db)]
          [:tr
           [:td (:name e)]])]]

    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(def not-found (html5 "not found"))
