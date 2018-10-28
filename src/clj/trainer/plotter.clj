(ns trainer.plotter
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [me.raynes.fs :as fs]
            [trainer.db :as db]
            [trainer.util :as util]
            [slingshot.slingshot :refer [throw+]]))

(defn padding [x y]
  (let [sum (+ x y)]
    [(- x (quot sum 5))
     (+ y (quot sum 5))]))

(defn make-gnuplot-template [{:keys [uuid title ylabel x-range y-range mode]}]
  (format
   "#!/usr/bin/gnuplot
reset
unset multiplot
set title \"%s\"
set xdata time
set style data linespoints
set terminal png size 1024, 800 font \"Hack,12\"
set xlabel \"Day\"
set ylabel \"%s\"
set output \"resources/public/img/%s.png\"
set datafile separator \",\"
set timefmt '%%Y-%%m-%%d'
set format x \"%%Y-%%m-%%d\"
set xrange ['%s':'%s']
set yrange [%d:%d]
set xtics 7000000
plot 'data/plotdata.csv' using 1:2 t \"%s\" lw 5"
   title
   ylabel
   uuid
   (-> x-range :min util/fmt-date)
   (-> x-range :max util/fmt-date)
   (first (padding (:min y-range) (:max y-range)))
   (second (padding (:min y-range) (:max y-range)))
   mode))

(defn write-csv [res]
  (with-open [writer (io/writer "data/plotdata.csv")]
    (csv/write-csv writer res)))

(defn- score [[k1 fst] [k2 snd]]
  (when (and (= k1 k2) fst snd)
    [k1 (+ fst snd)]))

(defn- data->range [data]
  (when (empty? data)
    (throw+ {:type ::empty-data :data data}))
  (let [max (second (apply max-key second data))
        min (second (apply min-key second data))]
    {:max max
     :min min}))

(defn- mode->ylabel [mode]
  (case mode
    :both "Score"
    :weight "Kg"
    :duration "Time"
    :level "Level"
    :reps "Number"))

(defmulti generate
  (fn [params]
    (:type params)))

(defmethod generate :weightlift [{:keys [db eid mode]}]
  (fs/mkdir "data")
  (let [rows (sort-by :day (db/all-where db :doneweightlift (str "exerciseid=" eid)))
        weight-data (map #(vals (select-keys % [:day :weight])) rows)
        reps-data (map #(vals (select-keys % [:day :reps])) rows)
        data (case mode
               :both (filter some? (map score weight-data reps-data))
               :weight weight-data
               :reps reps-data)
        ylabel (mode->ylabel mode)
        uuid (util/uuid)]
    (write-csv data)
    (spit "data/plot.gnuplot"
          (make-gnuplot-template {:uuid uuid
                                  :title (:name (db/element db :weightlift eid))
                                  :ylabel ylabel
                                  :x-range (db/range db :doneweightlift :day eid)
                                  :y-range (data->range data)
                                  :mode mode}))
    (shell/sh "gnuplot" "data/plot.gnuplot")
    uuid))

(defmethod generate :cardio [{:keys [db eid mode]}]
  (fs/mkdir "data")
  (let [rows (sort-by :day (db/all-where db :donecardio (str "exerciseid=" eid)))
        duration-data (map #(vals (select-keys % [:day :duration])) rows)
        level-data (map #(vals (select-keys % [:day :level])) rows)
        data (case mode
               :both (filter some? (map score duration-data level-data))
               :duration (filter (comp some? second) duration-data)
               :level (filter (comp some? second) level-data))
        ylabel (mode->ylabel mode)
        uuid (util/uuid)]

    (write-csv data)
    (spit "data/plot.gnuplot"
          (make-gnuplot-template {:uuid uuid
                                  :title (:name (db/element db :cardio eid))
                                  :ylabel ylabel
                                  :x-range (db/range db :donecardio :day eid)
                                  :y-range (data->range data)
                                  :mode mode}))
    (shell/sh "gnuplot" "data/plot.gnuplot")
    uuid))
