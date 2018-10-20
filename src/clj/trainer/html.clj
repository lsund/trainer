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

(defn tablebody [f extra-f es exercise-type keyseq]
  [:tbody
   (for [e es]
     [:tr
      (for [[k v] (select-keys e keyseq)]
        [:td (apply f [exercise-type [k v] (:exerciseid e)] )])
      (extra-f e)])])


(defn cardio-tablebody [f extra-f es]
  (tablebody f extra-f es :cardio [:name :duration :distance :highpulse :lowpulse :level]))

(defn weightlift-tablebody [f extra-f es]
  (tablebody f extra-f es :weightlift [:name :sets :reps :weight]))

(defmacro defcurry [name fn-to-call & args]
  `(defn ~name [args]
     (partial (apply ~fn-to-call args))))

;; (defcurry print-hello println "hello")

;; TODO  Save plan does not work.

;; (defcurry weightlift-tablebody tablebody [:name :sets :refs :weight])

;; (defcurry cardio-tablebody tablebody [:name :duration :distance :highpulse :lowpulse :level])
