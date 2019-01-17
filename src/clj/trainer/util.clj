(ns trainer.util
  "Namespace for utilities"
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]))

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [x]
  (cond
    (= (type x) java.lang.Integer) x
    (= (type x) java.lang.String) (try (Integer. (re-find #"\d+" x))
                                       (catch NumberFormatException _ nil))
    :default nil))

(def ^:private date-string "yyyy-MM-dd")

(defn string->localdate [s]
  (java.time.LocalDate/parse s (java.time.format.DateTimeFormatter/ofPattern date-string)))

(defn ->localdate
  [date]
  (cond (= (type date) java.sql.Timestamp) (.. date toLocalDateTime toLocalDate)
        (= (type date) java.sql.Date) (.toLocalDate date)
        (= (type date) java.time.LocalDate) date
        (= (type date) java.time.LocalDateTime) date
        (= (type date) java.lang.String) (string->localdate date)
        (nil? date) (throw (Exception.  "Nil argument to localdate"))
        :default (throw (Exception. (str "Unknown date type: " (type date))))))

(defn fmt-date [d]
  (.format (java.time.format.DateTimeFormatter/ofPattern date-string)
           (->localdate d)))

(defn today [] (java.time.LocalDateTime/now))

(defn fmt-today [] (fmt-date (java.time.LocalDateTime/now)))

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn clear-dir [dirname]
  (doseq [file (fs/list-dir dirname)]
    (fs/delete file)))

(defn duration-str->int [x]
  (if-let [[_ minutes _ seconds _] (re-matches #"(\d+)(m)(\d+)(s)" x)]
    (+ (* (parse-int minutes) 60) (parse-int seconds))
    (when-let [[_ number unit] (re-matches #"(\d+)([ms])" x)]
      (case unit
        "m" (* (parse-int number) 60)
        "s" (parse-int number)))))

(defn int->duration-str [x]
  (let [q (quot x 60)
        r (rem x 60)]
    (if (zero? r)
      (str q "m")
      (str q "m" r "s"))))
