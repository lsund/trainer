(ns trainer.plotter
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [me.raynes.fs :as fs]
            [trainer.db :as db]
            [trainer.util :as util]))

(defn score [weight reps] (* weight reps))

(defn padding [x y]
  (let [sum (+ x y)]
    [(- x (quot sum 5))
     (+ y (quot sum 5))]))

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
plot 'data/plotdata.csv' using 1:2 t \"%s\" lw 3"
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

(defn generate [db eid props]
  (fs/mkdir "data")
  (let [use-weight (= (:weight props) "on")
        use-reps (= (:reps props) "on")
        mode (cond
               (and use-weight use-reps) :both
               use-weight :weight
               use-reps :reps
               :default :none)
        data (map  #(vals (select-keys % [:day mode]))
                   (sort-by :day (db/all-where db :doneweightlift (str "exerciseid=" eid))))]
    (write-csv data)
    (spit "data/plot.gnuplot"
          (make-gnuplot-template {:title (:name (db/element db :weightlift eid))
                                  :ylabel (string/capitalize (name mode))
                                  :x-range (db/range db :doneweightlift :day eid)
                                  :y-range (db/range db :doneweightlift mode eid)
                                  :mode mode})))
  (shell/sh "gnuplot" "data/plot.gnuplot"))
