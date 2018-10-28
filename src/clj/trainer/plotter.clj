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
plot 'data/%s.csv' using 1:2 t \"%s\" lw 5"
   title
   ylabel
   uuid
   (-> x-range :min util/fmt-date)
   (-> x-range :max util/fmt-date)
   (first (padding (:min y-range) (:max y-range)))
   (second (padding (:min y-range) (:max y-range)))
   uuid
   mode))

(defn write-csv [res uuid]
  (with-open [writer (io/writer (str "data/" uuid ".csv"))]
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

(defn- mode->ylabel [etype mode]
  (cond
    (= mode :both) "Score"
    (and (= mode first) (= etype :weightlift)) "Kg"
    (and (= mode first) (= etype :cardio)) "Time"
    (and (= mode second) (= etype :weightlift)) "Number"
    (and (= mode second) (= etype :cardio)) "Level"))

(defn- generate-aux [etype {:keys [db eid mode]}]
  (util/clear-dir "resources/public/img")
  (util/clear-dir "data")
  (fs/mkdir "data")
  (let [[fst snd] (case etype
                    :weightlift [:weight :reps]
                    :cardio [:duration :level]
                    nil)
        rows (sort-by :day (db/all-where db
                                         (keyword (str "done" (name etype)))
                                         (str "exerciseid=" eid)))
        fst-data (map #(vals (select-keys % [:day fst])) rows)
        snd-data (map #(vals (select-keys % [:day snd])) rows)
        data (case mode
               :both (filter some? (map score fst-data snd-data))
               :fst (filter (comp some? second) fst-data)
               :snd (filter (comp some? second) snd-data))
        ylabel (mode->ylabel etype mode)
        uuid (util/uuid)]
    (write-csv data uuid)
    (spit (str "data/" uuid ".gnuplot")
          (make-gnuplot-template {:uuid uuid
                                  :title (:name (db/element db etype eid))
                                  :ylabel ylabel
                                  :x-range (db/range db (keyword (str "done" (name etype))) :day eid)
                                  :y-range (data->range data)
                                  :mode mode}))
    (shell/sh "gnuplot" (str "data/" uuid ".gnuplot"))
    uuid))

(defmulti generate
  (fn [params]
    (:type params)))

(defmethod generate :weightlift [params]
  (generate-aux :weightlift params))

(defmethod generate :cardio [params]
  (generate-aux :cardio params))
