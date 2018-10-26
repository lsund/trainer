(ns trainer.util
  "Namespace for utilities"
  (:require [clojure.string :as s]))

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [x]
  (cond
    (= (type x) java.lang.Integer) x
    (= (type x) java.lang.String) (try (Integer. (re-find #"\d+" x))
                                       (catch NumberFormatException _ nil))
    :default nil))

(defn string->localdate [s]
  (java.time.LocalDate/parse s (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")))

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
