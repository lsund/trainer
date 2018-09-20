(ns template.render
  "Namespace for rendering hiccup"
  (:require
   [template.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [template.util :as util]
   [template.html :as html]))


(defn index
  [config]
  (html5
   [:head
    [:title "Fixme"]]
   [:body
    [:h1 "Hello. Fixme"]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(def not-found (html5 "not found"))
