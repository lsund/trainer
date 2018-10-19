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

(defn weightlift-tablebody [f es]
  [:tbody
   (for [{:keys [name sets reps weight] :as e} es]
     [:tr
      [:td (f name)]
      [:td (f sets)]
      [:td (f reps)]
      [:td (f weight)]])])

(defn cardio-tablebody [f es]
  [:tbody
   (for [{:keys [name duration distance highpulse lowpulse level] :as e} es]
     [:tr
      [:td (f name)]
      [:td (f duration)]
      [:td (f distance)]
      [:td (f highpulse)]
      [:td (f lowpulse)]
      [:td (f level)]])])
