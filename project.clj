(defproject trainer "0.1.0-SNAPSHOT"
  :description "TODO"
  :url "https://github.com/lsund/trainer"


  :min-lein-version "2.7.0"

  :dependencies [;; Commons
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.4.474"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/data.csv "0.1.4"]

                 ;; Database
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.postgresql/postgresql "42.2.2"]

                 ;; Web
                 [http-kit "2.2.0"]
                 [ring/ring-defaults "0.3.0"]
                 [compojure "1.6.1"]
                 [reagent "0.8.0"]
                 [hiccup "1.0.5"]

                 ;; Other
                 [com.taoensso/timbre "4.10.0"]
                 [io.aviso/pretty "0.1.34"]
                 [me.raynes/fs "1.4.6"]

                 ,,,]

  :plugins [[lein-figwheel "0.5.15"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [io.aviso/pretty "0.1.34"]]

  :source-paths ["src/clj" "src/cljs"]

  :ring
  {:handler trainer.core/new-handler}

  :main
  trainer.main

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/cljs"]

                :figwheel {:on-jsload "trainer.core/on-js-reload"
                           :open-urls ["http://localhost:3449/index.html"]}

                :compiler {:main                 trainer.core
                           :asset-path           "js/compiled/out"
                           :output-to            "resources/public/js/compiled/trainer.js"
                           :output-dir           "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads             [devtools.preload]}}
               {:id           "min"
                :source-paths ["src/cljs"]
                :compiler     {:output-to     "resources/public/js/compiled/trainer.js"
                               :main          trainer.core
                               :optimizations :advanced
                               :pretty-print  false}}]}

  :figwheel
  {:css-dirs ["resources/public/css"]}

  :repl-options
  {:init-ns user}

  :profiles {:dev {:dependencies  [[binaryage/devtools "0.9.9"]
                                   [figwheel-sidecar "0.5.15"]
                                   [com.cemerick/piggieback "0.2.2"]]
                   :source-paths  ["src/clj" "src/cljs" "dev"]
                   :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
