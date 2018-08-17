(ns template.util
  "Namespace for utilities"
  (:require [clojure.string :as s]))

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))

(def date-string "yyyy-MM-dd")

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


(defn fmt-today [] (fmt-date (java.time.LocalDateTime/now)))
