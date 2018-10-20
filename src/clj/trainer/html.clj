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

(defn tablebody [f end-f es keyseq exercise-type]
  [:tbody
   (for [e es]
     [:tr
      (for [[k v] (select-keys e keyseq)]
        [:td (apply f [[k v] (:exerciseid e) exercise-type] )])
      (end-f e)])])


(defn cardio-tablebody [f end-f es]
  (tablebody f end-f es [:name :duration :distance :highpulse :lowpulse :level] :cardio))

(defn weightlift-tablebody [f end-f es]
  (tablebody f end-f es [:name :sets :reps :weight] :weightlift))

(defmacro defcurry [name fn-to-call & args]
  `(defn ~name [args]
     (partial (apply ~fn-to-call args))))

;; (defcurry print-hello println "hello")

;; TODO  Save plan does not work.

;; (defcurry weightlift-tablebody tablebody [:name :sets :refs :weight])

;; (defcurry cardio-tablebody tablebody [:name :duration :distance :highpulse :lowpulse :level])
