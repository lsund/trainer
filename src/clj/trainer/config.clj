(ns trainer.config
  "Namespace for loading configuration"
  (:require [clojure.edn :as edn]))

(defn load-local
  []
  (edn/read-string (slurp "resources/edn/config.edn")))
