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

(defn tablebody [keyseq exercise-type f end-f es]
  [:tbody
   (for [e es]
     [:tr
      (for [[k v] (select-keys e keyseq)]
        [:td (apply f [[k v] (:exerciseid e) exercise-type] )])
      (end-f e)])])

(def cardio-tablebody
  (partial tablebody [:name :duration :distance :highpulse :lowpulse :level] :cardio))

(def weightlift-tablebody
  (partial tablebody [:name :sets :reps :weight] :weightlift))
