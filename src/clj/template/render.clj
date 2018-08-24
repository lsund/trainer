(ns myblog.render
  "Namespace for rendering hiccup"
  (:require
   [myblog.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [myblog.util :as util]
   [myblog.html :as html]))


(defn index
  [config]
  (html5
   [:h1 "Hello. Fixme"]))

(def not-found (html5 "not found"))
