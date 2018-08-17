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
   [:h1 "Hello"]))

(def not-found (html5 "not found"))
