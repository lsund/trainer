(ns trainer.render
  "Namespace for rendering hiccup"
  (:require
   [trainer.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [trainer.util :as util]
   [trainer.html :as html]))

(defn id->exercise-name [db id]
  (->> id util/parse-int (db/get-row db :exercise) :name))

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
             [:input {:name "sets" :type :number :min "0" :placeholder "Sets"}]
             [:input {:name "reps" :type :number :min "0" :placeholder "Reps"}]
             [:input {:name "weight" :type :number :min "0" :placeholder "Weight (KG)"}]
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
         [:td (id->exercise-name db id)]])]]
    (form-to [:post "/save-plan"]
             [:input {:name "name" :type :text :placeholder "Plan name"}]
             [:button.mui-btn "Save plan"])
    [:h3 "Existing plans"]
    [:ul
     (for [p (db/all db :plan)]
       [:li
        [:div (:name p)]
        [:table
         [:thead
          [:tr
           [:th "Exercise"]]]
         [:tbody
          (for [t (db/exercises-for-plan db (:id p))]
            [:tr
             [:td (id->exercise-name db (:exerciseid t))]])]]])]
    [:h3 "Complete a plan"]
    (form-to [:get "/complete-plan"]
             [:select {:name "plan"}
              (for [e (db/all db :plan)]
                [:option {:value (:id e)} (:name e)])]
             [:button.mui-btn "Start"])
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn complete-plan [{:keys [db]} id]
  (html5
   [:head
    [:title "Complete plan"]]
   [:body
    (form-to [:post "/save-plan-instance"]
             [:input {:type :hidden :name "plan" :value id}]
             [:table
              [:thead
               [:tr
                [:th "Exercise"]
                [:th "Sets"]
                [:th "Reps"]
                [:th "Kg"]]]
              [:tbody
               (for [eid (map :exerciseid (db/exercises-for-plan db id))]
                 (let [e (db/get-row db :exercise eid)]
                   [:tr
                    [:td (:name e)]
                    [:td [:input {:name (str eid "_sets")
                                  :type :number
                                  :value (:sets e)
                                  :min "0"}]]
                    [:td [:input {:name (str eid "_reps")
                                  :type :number
                                  :value (:reps e)
                                  :min "0"}]]
                    [:td [:input {:name (str eid "_weight")
                                  :type :number
                                  :value (:weight e)
                                  :min "0"}]]]))]]
             [:input {:type :submit :value "Save plan"}])]))

(def not-found (html5 "not found"))
