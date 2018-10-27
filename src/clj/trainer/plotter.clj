(ns trainer.plotter
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [me.raynes.fs :as fs]
            [trainer.db :as db]))


(defn write-csv [db]
  (let [res (map  #(vals (select-keys % [:day :weight]))
                  (sort-by :day (db/all-where db :doneweightlift "exerciseid=1")))]
    (with-open [writer (io/writer "data/plotdata.csv")]
      (csv/write-csv writer res)))
  )

(defn doplot [db]
  (fs/mkdir "data")
  (write-csv db)
  (shell/sh "gnuplot" "data/plot.gnuplot"))
