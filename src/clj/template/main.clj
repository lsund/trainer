(ns template.main
  "Namespace for running the program once"
  (:require
   [template.config :as config]
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [template.handler :refer [new-handler]]))


(defn -main [& args]
  (run-server (site #'new-handler) {:port (:port (config/load))})
  (println "Server up and running"))
