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

(defn tablebody [defaults extra-cols]
  [:tbody
   (map #(into [] (concat % extra-cols)) defaults)])

(defn weightlift-tablebody [f es extra-cols extra-args]
  (tablebody
   (for [e es]
     [:tr
      (for [[_ v] (select-keys e [:name :sets :reps :weight])]
        [:td (apply f (conj extra-args v))])])
   extra-cols))

(defn cardio-tablebody [f es extra-cols extra-args]
  (tablebody
   (for [{:keys [name duration distance highpulse lowpulse level] :as e} es]
     [:tr
      [:td (apply f (conj extra-args name))]
      [:td (apply f (conj extra-args duration))]
      [:td (apply f (conj extra-args distance))]
      [:td (apply f (conj extra-args highpulse))]
      [:td (apply f (conj extra-args lowpulse))]
      [:td (apply f (conj extra-args level))]])
   extra-cols))
