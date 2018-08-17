(ns template.config
  "Namespace for loading configuration"
  (:require [clojure.edn :as edn]))

(defn load
  []
  (edn/read-string (slurp "resources/edn/config.edn")))
