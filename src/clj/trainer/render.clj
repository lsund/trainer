(ns trainer.render
  "Namespace for rendering hiccup"
  (:require
   [trainer.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [trainer.util :as util]
   [trainer.html :as html]
   [trainer.plotter :as plotter]
   [slingshot.slingshot :refer [try+]]))

(defn- layout
  [config title content]
  (html5
   [:head
    [:title (str "Trainer - " title)]]
   [:body.mui-container
    (html/navbar)
    content
    (apply include-css (:styles config))
    (apply include-js (:javascripts config))]))

(defn- existing-plans [{:keys [db] :as config}]
  [:ul
   (for [p (db/all db :plan) #_(db/all-where db :plan "active = 't'")]
     [:li
      [:h3 (str (:name p) " completed " (:timescompleted p) " times")]
      [:table
       (html/cardio-tablehead)
       [:tbody
        (for [e (db/cardios-for-plan db (:id p))]
          (html/update-form :cardio
                            e
                            [:duration :distance :highpulse :lowpulse :level]))]]
      [:table
       (html/weightlift-tablehead)
       [:tbody
        (for [e (db/weightlifts-for-plan db (:id p))]
          (html/update-form :weightlift e [:sets :reps :weight]))]]])])

(defn index
  [{:keys [db] :as config} {:keys [weightlift-list cardio-list] :as session }]
  (layout config
          "Overview"
          [:div
           [:h1 "Trainer"]
           [:p "This is a program for logging your gym results."]
           (html/add-exercise-form :weightlift [:sets :reps :weight])
           (html/add-exercise-form :cardio [:duration :distance :highpulse :lowpulse :level])
           [:h2 "Make a new plan"]
           (html/add-to-plan-form :weightlift (db/all db :weightlift))
           (html/add-to-plan-form :cardio (db/all db :cardio))
           [:h2 "Current plan"]
           [:table
            [:thead
             [:tr
              [:th "Name"]]]
            [:tbody
             (for [e (db/subset db :weightlift (mapv util/parse-int weightlift-list))]
               [:tr
                [:td (:name e)]])
             (for [e (db/subset db :cardio (mapv util/parse-int cardio-list))]
               [:tr
                [:td (:name e)]])]]
           (form-to [:post "/save-plan"]
                    [:input {:name "name" :type :text :placeholder "Plan name"}]
                    [:button.mui-btn "Save plan"])
           [:h3 "Complete a plan"]
           (form-to [:get "/complete-plan"]
                    [:select {:name "plan"}
                     (for [e (db/all db :plan)]
                       [:option {:value (:id e)} (:name e)])]
                    [:button.mui-btn "Start"])
           (form-to [:get "/history"]
                    [:input {:type :submit :value "History"}])
           (form-to [:get "/squash"]
                    [:input {:type :submit :value "Squash Results"}])
           [:h2 "Existing plans"]
           (existing-plans config)]))

(defn- modifiable-if-number [[k v] exerciseid etype]
  (cond (util/parse-int v) [:input {:name (str (db/exercise-type->id etype) "_"
                                               exerciseid "_"
                                               (name k))
                                    :type :number
                                    :value v
                                    :min "0"}]
        (nil? v) "N/A"
        :default v))

(defn- skip-optionally [e etype]
  [:td [:input {:name (str (db/exercise-type->id etype) "_" (:exerciseid e) "_skip")
                :type :checkbox}]])

(defn complete-plan [{:keys [db]} id]
  (html5
   [:head
    [:title "Trainer - Complete plan"]]
   [:body
    (form-to [:post "/save-plan-instance"]
             [:input {:type :hidden :name "plan" :value id}]
             [:input {:type :date :name "day" :required "true"}]
             [:table
              (html/cardio-tablehead "Skip?")
              (html/cardio-tablebody modifiable-if-number
                                     skip-optionally
                                     (db/cardios-for-plan db id))]
             [:table
              (html/weightlift-tablehead "Skip?")
              (html/weightlift-tablebody modifiable-if-number
                                         skip-optionally
                                         (db/weightlifts-for-plan db id))]
             [:input {:type :submit :value "Save plan"}])]))

(defn- value-or-na [[_ v] _ _]
  (if v v "N/A"))

(defn- const [x & _] x)

(defn- const-nil [& _] nil)

(defn history [{:keys [db] :as config}]
  (layout config
          "History"
          [:div
           (let [cardio-map (->> (map #(assoc % :collection :cardios)
                                      (db/all-done-cardios-with-name db))
                                 (group-by :day))
                 weightlift-map (->> (map #(assoc % :collection :weightlifts)
                                          (db/all-done-weightlifts-with-name db))
                                     (group-by :day))]

             (for [[day e] (-> (merge-with concat weightlift-map cardio-map) sort reverse)]
               (let [{:keys [cardios weightlifts]} (group-by :collection e)]
                 [:div
                  [:h3 day]
                  (when cardios
                    [:table
                     (html/cardio-tablehead)
                     (html/cardio-tablebody value-or-na const-nil cardios)])
                  (when weightlifts
                    [:table
                     (html/weightlift-tablehead)
                     (html/weightlift-tablebody value-or-na const-nil weightlifts)])])))]))

(defn squash [{:keys [db] :as config}]
  (layout config
          "Squash"
          [:div
           [:h3 "Add Squash Result"]
           (form-to [:post "/add-squash-result"]
                    [:input {:name "day" :type :date}]
                    [:select {:name "opponentid"}
                     (for [e (db/all db :squashopponent)]
                       [:option {:value (:id e)} (:name e)])]
                    [:input {:name "myscore" :type :number}]
                    [:input {:name "opponentscore" :type :number}]
                    [:input.hidden {:type :submit}])
           [:table
            (html/tablehead [:tr
                             [:th "Day"]
                             [:th "Opponent"]
                             [:th "---"]
                             [:th " "]])
            (html/tablebody [:day :name :myscore :opponentscore]
                            nil
                            const
                            const-nil
                            (reverse (sort-by :day (db/squash-results db))))]]))

(defn- maybe-regenerate-plot [db eid etype fst snd]
  (let [use-fst (= fst "on")
        use-snd (= snd "on")
        mode (cond
               (and use-fst use-snd) :both
               use-fst :fst
               use-snd :snd
               :default :none)]
    (when (not= mode :none)
      (plotter/generate
       {:db db
        :eid (util/parse-int eid)
        :type etype
        :mode mode}))))

(defn- plot-aux [etype {:keys [config eid fst snd]}]
  (let [plot-result (try+
                     {:uuid (maybe-regenerate-plot (:db config) eid etype fst snd)}
                     (catch [:type :trainer.plotter/empty-data] {:keys [data]}
                       {:emsg "There was an error generating the plot"}))]
    (layout config
            (str (name etype) " Plotter")
            (let [[fst-title snd-title] (case etype
                                          :weightlift ["Weight" "Reps"]
                                          :cardio ["Duration" "Level"]
                                          nil)]
              [:div
               [:div.error (:emsg plot-result)]
               [:h3 (str  "Plot " (name etype) " exercise")]
               (form-to [:get (str "/plot/" (name etype))]
                        [:select {:name "eid"}
                         [:div
                          (for [e (db/all (:db config) etype)]
                            (if (= (:id e) eid)
                              [:option {:value (:id e) :selected "selected"} (:name e)]
                              [:option {:value (:id e)} (:name e)]))]]

                        [:div
                         [:input {:name :fst
                                  :type :checkbox
                                  :checked (when (= "on" fst) "true")} fst-title]]
                        [:div
                         [:input {:name :snd
                                  :type :checkbox
                                  :checked (when (= "on" snd) "true")} snd-title]]
                        [:input.hidden {:type :submit}]
                        [:button.mui-btn "Generate plot"])
               [:img {:src (str "/img/" (:uuid plot-result) ".png")}]]))))

(defmulti plot
  (fn [params]
    (get-in params [:config :etype])))

(defmethod plot :weightlift [params]
  (plot-aux :weightlift params))

(defmethod plot :cardio [params]
  (plot-aux :cardio params))

(def not-found (html5 "not found"))
