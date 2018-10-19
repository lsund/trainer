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

(defn weightlift-tablebody [f es extra-col & extra-args]
  [:tbody
   (for [{:keys [name sets reps weight] :as e} es]
     [:tr
      [:td (apply f (conj extra-args name))]
      [:td (apply f (conj extra-args sets))]
      [:td (apply f (conj extra-args reps))]
      [:td (apply f (conj extra-args weight))]
      extra-col])])

(defn cardio-tablebody [f es extra-col & extra-args]
  [:tbody
   (for [{:keys [name duration distance highpulse lowpulse level] :as e} es]
     [:tr
      [:td (apply f (conj extra-args name))]
      [:td (apply f (conj extra-args duration))]
      [:td (apply f (conj extra-args distance))]
      [:td (apply f (conj extra-args highpulse))]
      [:td (apply f (conj extra-args lowpulse))]
      [:td (apply f (conj extra-args level))]
      extra-col])])
