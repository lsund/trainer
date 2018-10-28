(ns trainer.plotter
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [me.raynes.fs :as fs]
            [trainer.db :as db]
            [trainer.util :as util]))

(defn padding [x y]
  (let [sum (+ x y)]
    [(- x (quot sum 5))
     (let [y* (+ y (quot sum 5))]
       (if (zero? y*) 1 y*))]))

(defn make-gnuplot-template [{:keys [title ylabel x-range y-range mode]}]
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
set output \"resources/public/img/result.png\"
set datafile separator \",\"
set timefmt '%%Y-%%m-%%d'
set format x \"%%Y-%%m-%%d\"
set xrange ['%s':'%s']
set yrange [%d:%d]
set xtics 7000000
plot 'data/plotdata.csv' using 1:2 t \"%s\" lw 5"
   title
   ylabel
   (-> x-range :min util/fmt-date)
   (-> x-range :max util/fmt-date)
   (first (padding (:min y-range) (:max y-range)))
   (second (padding (:min y-range) (:max y-range)))
   mode))

(defn write-csv [res]
  (with-open [writer (io/writer "data/plotdata.csv")]
    (csv/write-csv writer res)))

(defn- score [[d1 weight] [d2 reps]]
  (if (= d1 d2)
    [d1 (+ weight reps)]
    nil))

(defn- data->range [data]
  {:max (second (apply max-key second data))
   :min (second (apply min-key second data))})

(defn- mode->ylabel [mode]
  (case mode
    :both "Score"
    :weight "Kg"
    :reps "Number"))

(defn generate [db eid mode]
  (fs/mkdir "data")
  (let [rows (sort-by :day (db/all-where db :doneweightlift (str "exerciseid=" eid)))
        weight-data (map #(vals (select-keys % [:day :weight])) rows)
        reps-data (map #(vals (select-keys % [:day :reps])) rows)
        data (case mode
               :both (filter some? (map score weight-data reps-data))
               :weight weight-data
               :reps reps-data)
        ylabel (mode->ylabel mode)]
    (write-csv data)
    (spit "data/plot.gnuplot"
          (make-gnuplot-template {:title (:name (db/element db :weightlift eid))
                                  :ylabel ylabel
                                  :x-range (db/range db :doneweightlift :day eid)
                                  :y-range (data->range data)
                                  :mode mode})))
  (shell/sh "gnuplot" "data/plot.gnuplot"))
