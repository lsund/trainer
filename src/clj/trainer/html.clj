(ns trainer.html
  "Namespace for HTML components"
  (:require
   [hiccup.form :refer [form-to]]))

(defn tablehead [defaults & extra-cols]
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

(defn tablebody [keyseq etype f end-f es]
  [:tbody
   (for [e es]
     [:tr
      (for [[k v] (select-keys e keyseq)]
        [:td (apply f [[k v] (:exerciseid e) etype] )])
      (end-f e)])])

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
              [:input {:name prop :type :number :min "0" :placeholder prop}])]
           [:button.mui-btn "Add " (name etype)]))

(defn update-form [type e keyseq]
  (form-to [:post (str "/update-" (name type))]
           [:tr
            [:td (:name e)]
            [:input {:name "id" :type :hidden :value (:exerciseid e)}]
            [:input.hidden {:type :submit}]
            (for [[k v] (select-keys e keyseq)]
              [:td [:input {:name k
                            :type :number
                            :value v
                            :min "0"}]])]))

(defn add-to-plan-form [etype es]
  (form-to [:post (str "/add-" etype "-to-plan")]
           [:select {:name etype}
            (for [e es]
              [:option {:value (:id e)} (:name e)])]
           [:button.mui-btn "Add to plan"]))
