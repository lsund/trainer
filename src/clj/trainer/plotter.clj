(ns trainer.plotter
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [me.raynes.fs :as fs]
            [trainer.db :as db]))

(def mock {:title "Benchpress"
           :ylabel "Weight"
           :x-range {:lower "2017-10-01"
                     :upper "2018-11-01"}
           :y-range {:lower 10
                     :upper 50}})

(defn make-gnuplot-template [{:keys [title ylabel x-range y-range]}]
  (format
   "#!/usr/bin/gnuplot
reset
unset multiplot
set title \"%s\"
set xdata time
set style data lines
set terminal png size 1024, 800 font \"Hack,12\"
set xlabel \"Day\"
set ylabel \"%s\"
set output \"data/result.png\"
set datafile separator \",\"
set timefmt '%%Y-%%m-%%d'
set format x \"%%Y-%%m-%%d\"
set xrange ['%s':'%s']
set yrange [%d:%d]
plot 'data/plotdata.csv' using 1:2 t \"inbound\" w lines"
   title
   ylabel
   (:lower x-range)
   (:upper x-range)
   (:lower y-range)
   (:upper y-range)))

(defn write-csv [res]
  (with-open [writer (io/writer "data/plotdata.csv")]
    (csv/write-csv writer res)))

(defn doplot [db]
  (fs/mkdir "data")
  (let [data (map  #(vals (select-keys % [:day :weight]))
                   (sort-by :day (db/all-where db :doneweightlift "exerciseid=1")))]
    (write-csv data))
  (spit "data/plot.gnuplot" (make-gnuplot-template mock))
  (shell/sh "gnuplot" "data/plot.gnuplot"))
