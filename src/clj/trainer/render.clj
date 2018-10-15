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
    (form-to [:get "/history"]
             [:input {:type :submit :value "History"}])
    [:h1 "Trainer"]
    [:p "This is a program for logging your gym results."]
    [:h2 "Add Exercise"]
    (form-to [:post "/add-exercise"]
             [:input {:name "name" :type :text :placeholder "Exercise name"}]
             [:input {:name "sets" :type :number :min "0" :placeholder "Sets"}]
             [:input {:name "reps" :type :number :min "0" :placeholder "Reps"}]
             [:input {:name "weight" :type :number :min "0" :placeholder "Weight (KG)"}]
             [:button.mui-btn "Add exercise"])
    [:h2 "Make a new plan"]
    (form-to [:post "/add-to-plan"]
             [:select {:name "exercise"}
              (for [e (db/all db :exercise)]
                [:option {:value (:id e)} (:name e)])]
             [:button.mui-btn "Add to plan"])
    [:h2 "Current plan"]
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
    [:h2 "Existing plans"]
    [:ul
     (for [p (db/all db :plan)]
       [:li
        [:h3 (str (:name p) " completed " (:timescompleted p) " times")]
        [:table
         [:thead
          [:tr
           [:th "Exercise"]
           [:th "Sets"]
           [:th "Reps"]
           [:th "Kg"]]]
         [:tbody
          (for [eid (db/exercise-ids-for-plan db (:id p))]
            ;; TODO do not do this
            (let [{:keys [id name sets reps weight] :as e} (db/get-row db :exercise eid)]
              [:tr
               [:td (:name e)]
               (form-to [:post "/update-exercise"]
                        [:input {:name "id" :type :hidden :value id}]
                        [:input.hidden {:type :submit}]
                        [:td [:input {:name "sets"
                                      :type :number
                                      :value sets
                                      :min "0"}]]
                        [:td [:input {:name "reps"
                                      :type :number
                                      :value reps
                                      :min "0"}]]
                        [:td [:input {:name "weight"
                                      :type :number
                                      :value weight
                                      :min "0"}]])]))]]])]
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
    [:title "Trainer - Complete plan"]]
   [:body
    (form-to [:post "/save-plan-instance"]
             [:input {:type :hidden :name "plan" :value id}]
             [:input {:type :date :name "day" :required "true"}]
             [:table
              [:thead
               [:tr
                [:th "Exercise"]
                [:th "Sets"]
                [:th "Reps"]
                [:th "Kg"]
                [:th "Skip?"]]]
              [:tbody
               (for [eid (db/exercise-ids-for-plan db id)]
                 ;; TODO do not do this
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
                                  :min "0"}]]
                    [:td [:input {:name (str eid "_skip")
                                  :type :checkbox}]]]))]]
             [:input {:type :submit :value "Save plan"}])]))

(defn history [{:keys [db]}]
  (html5
   [:head
    [:title "Trainer - History"]
    [:body
     (for [[day es] (->> (db/all-done-exercises-with-name db)
                        (group-by :day)
                        sort
                        reverse)]
       [:div
        [:h3 day]
        [:table
         [:thead
          [:tr
           [:th "Exercise"]
           [:th "Sets"]
           [:th "Reps"]
           [:th "Kg"]]]
         [:tbody
          (for [{:keys [name sets reps weight] :as e} es]
            [:tr
             [:td name]
             [:td sets]
             [:td reps]
             [:td weight]])]]])]]))

(def not-found (html5 "not found"))
