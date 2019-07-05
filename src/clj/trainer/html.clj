(ns trainer.html
  "Namespace for HTML components"
  (:require
   [hiccup.form :refer [form-to]]))

(defn navbar []
  [:div.mui-appbar
   [:table {:width "100%"}
    [:tr {:style "vertical-align:middle;"}
     [:td.mui--appbar-height
      (form-to [:get "/"]
               [:input {:type :submit :value "Index"}])]
     [:td.mui--appbar-height
      (form-to [:get "/plot/cardio"]
               [:input {:type :submit :value "Plot Cardio"}])]
     [:td.mui--appbar-height
      (form-to [:get "/plot/weightlift"]
               [:input {:type :submit :value "Plot Weightlift"}])]
     [:td.mui--appbar-height
      (form-to [:get "/squash"]
               [:input {:type :submit :value "Squash Results"}])]
     [:td.mui--appbar-height
      (form-to [:get "/history"]
               [:input {:type :submit :value "Training History"}])]]]])

(defn tablehead [defaults extra-cols]
  [:thead
   (into [] (concat defaults (map #(conj [:th] %) extra-cols)))])

(defn weightlift-tablehead [& extra-cols]
  (tablehead [:tr
              [:th "Weightlift"]
              [:th "Sets"]
              [:th "Reps"]
              [:th "Kg"]]
             extra-cols))

(defn cardio-tablehead [& extra-cols]
  (tablehead [:tr
              [:th "Cardio"]
              [:th "Duration"]
              [:th "Distance"]
              [:th "High Pulse"]
              [:th "Low Pulse"]
              [:th "Level"]]
             extra-cols))

(defn tablebody [keyseq etype f end-fs es]
  [:tbody
   (for [e es]
     [:tr
      (for [[k v] (select-keys e keyseq)]
        [:td (apply f [[k v]
                       (if (:exerciseid e)
                         (:exerciseid e)
                         (:opponentid e))
                       etype])])
      (for [f end-fs]
        (f e etype))])])

(def cardio-tablebody
  (partial tablebody [:name :duration :distance :highpulse :lowpulse :level] :cardio))

(def weightlift-tablebody
  (partial tablebody [:name :sets :reps :weight] :weightlift))

(defn add-exercise-form [etype props]
  (form-to [:post (str "/add-" (name etype))]
           [:input {:name "name" :type :text :placeholder "Name"}]
           [:input.hidden {:type :submit}]
           [:div
            (for [prop props]
              (if (= :duration prop)
                [:input {:name prop :type :text :placeholder prop}]
                [:input {:name prop :type :number :min "0" :placeholder prop}]))]
           [:button.mui-btn "Add " (name etype)]))

(defn update-form [type e props]
  (form-to [:post (str "/update-" (name type))]
           [:tr
            [:td (:name e)]
            [:input {:name "id" :type :hidden :value (:exerciseid e)}]
            [:input.hidden {:type :submit}]
            (for [[k v] (select-keys e props)]
              (if (= :duration k)
                [:td [:input {:name k :type :text :value v}]]
                [:td [:input {:name k :type :number :value v :min "0"}]]))]))

(defn add-to-plan-form [etype es]
  (form-to [:post (str "/add-" (name etype) "-to-plan")]
           [:select {:name etype}
            (for [e es]
              [:option {:value (:id e)} (:name e)])]
           [:button.mui-btn "Add to current plan"]))
