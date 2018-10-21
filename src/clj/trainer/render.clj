(ns trainer.render
  "Namespace for rendering hiccup"
  (:require
   [trainer.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [trainer.util :as util]
   [trainer.html :as html]))

(defn- layout
  [config title content]
  (html5
   [:head
    [:title (str "Trainer - " title)]]
   [:body.mui-container
    content
    (apply include-css (:styles config))
    (apply include-js (:javascripts config))]))

(defn- existing-plans [{:keys [db] :as config}]
  [:ul
   (for [p (db/all-where db :plan "active = 't'")]
     [:li
      [:h3 (str (:name p) " completed " (:timescompleted p) " times")]
      [:table
       (html/cardio-tablehead)
       [:tbody
        (for [e (db/cardios-for-plan db (:id p))]
          (html/update-form :cardio
                            e
                            [:exerciseid :name :duration :distence :lowpulse :level]))]]
      [:table
       (html/weightlift-tablehead)
       [:tbody
        (for [e (db/weightlifts-for-plan db (:id p))]
          (html/update-form :weightlift e [:reps :sets :weight]))]]])])

(defn index
  [{:keys [db] :as config} {:keys [weightlift-list cardio-list] :as session }]
  (layout config
          "Overview"
          [:div
           [:h1 "Trainer"]
           [:p "This is a program for logging your gym results."]
           (html/add-exercise-form :weightlift [:sets :reps :weight])
           (html/add-exercise-form :cardio [:name :duration :distance :highpulse :lowpulse :level])
           [:h2 "Existing plans"]
           (existing-plans config)
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
             (for [id cardio-list]
               [:tr
                [:td (id->name db :cardio id)]])]]
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
                    [:input {:type :submit :value "History"}])]))

(defn- modifiable-if-number [[k v] exerciseid etype]
  (cond (util/parse-int v) [:input {:name (str (db/exercise-type->id etype) "_"
                                               exerciseid "_"
                                               (name k))
                                    :type :number
                                    :value v
                                    :min "0"}]
        (nil? v) "N/A"
        :default v))

(defn- skip-optionally [e]
  [:td [:input {:name (str "2_" (:exerciseid e) "_skip")
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
                     (html/cardio-tablebody value-or-na (fn [_] nil) cardios)])
                  (when weightlifts
                    [:table
                     (html/weightlift-tablehead)
                     (html/weightlift-tablebody value-or-na (fn [_] nil) weightlifts)])])))]))

(def not-found (html5 "not found"))
