(ns trainer.render
  "Namespace for rendering hiccup"
  (:require
   [trainer.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [trainer.util :as util]
   [trainer.html :as html]))

(defn id->name [db table id]
  (->> id util/parse-int (db/get-row db table) :name))

(defn index
  [{:keys [db] :as config} {:keys [weightlift-list cardio-list] :as session}]
  (html5
   [:head
    [:title "Trainer"]]
   [:body
    (form-to [:get "/history"]
             [:input {:type :submit :value "History"}])
    [:h1 "Trainer"]
    [:p "This is a program for logging your gym results."]
    [:h2 "Add Weightlift"]
    (form-to [:post "/add-weightlift"]
             [:input {:name "name" :type :text :placeholder "weightlift name"}]
             [:input {:name "sets" :type :number :min "0" :placeholder "Sets"}]
             [:input {:name "reps" :type :number :min "0" :placeholder "Reps"}]
             [:input {:name "weight" :type :number :min "0" :placeholder "Weight (KG)"}]
             [:input {:name "type" :type :hidden :value "1"}]
             [:button.mui-btn "Add Weightlift"])
    [:h2 "Add Cardio"]
    (form-to [:post "/add-cardio"]
             [:input {:name "name" :type :text :placeholder "cardio name"}]
             [:input {:name "duration" :type :number :min "0" :placeholder "Duration"}]
             [:input {:name "distance" :type :number :min "0" :placeholder "Distance"}]
             [:input {:name "highpulse" :type :number :min "0" :placeholder "High Pulse"}]
             [:input {:name "lowpulse" :type :number :min "0" :placeholder "Low Pulse"}]
             [:input {:name "level" :type :number :min "0" :placeholder "Level"}]
             [:input {:name "type" :type :hidden :value "2"}]
             [:button.mui-btn "Add Cardio"])
    [:h2 "Make a new plan"]
    (form-to [:post "/add-weightlift-to-plan"]
             [:select {:name "weightlift"}
              (for [e (db/all db :weightlift)]
                [:option {:value (:id e)} (:name e)])]
             [:button.mui-btn "Add to plan"])
    (form-to [:post "/add-cardio-to-plan"]
             [:select {:name "cardio"}
              (for [e (db/all db :cardio)]
                [:option {:value (:id e)} (:name e)])]
             [:button.mui-btn "Add to plan"])
    [:h2 "Current plan"]
    [:table
     [:thead
      [:tr
       [:th "Name"]]]
     [:tbody
      (for [id weightlift-list]
        [:tr
         [:td (id->name db :weightlift id)]])
      (for [id cardio-list]
        [:tr
         [:td (id->name db :cardio id)]])]]
    (form-to [:post "/save-plan"]
             [:input {:name "name" :type :text :placeholder "Plan name"}]
             [:button.mui-btn "Save plan"])
    [:h2 "Existing plans"]
    [:ul
     (for [p (db/all db :plan)]
       [:li
        [:h3 (str (:name p) " completed " (:timescompleted p) " times")]
        [:table
         (html/cardio-tablehead)
         [:tbody
          (for [{:keys [exerciseid name duration distance highpulse lowpulse level]}
                (db/cardio-ids-for-plan db (:id p))]
            [:tr
             [:td name]
             (form-to [:post "/update-cardio"]
                      [:input {:name "id" :type :hidden :value exerciseid}]
                      [:input.hidden {:type :submit}]
                      [:td [:input {:name "duration"
                                    :type :number
                                    :value duration
                                    :min "0"}]]
                      [:td [:input {:name "distance"
                                    :type :number
                                    :value distance
                                    :min "0"}]]
                      [:td [:input {:name "highpulse"
                                    :type :number
                                    :value highpulse
                                    :min "0"}]]
                      [:td [:input {:name "lowpulse"
                                    :type :number
                                    :value lowpulse
                                    :min "0"}]]
                      [:td [:input {:name "level"
                                    :type :number
                                    :value level
                                    :min "0"}]])])]]
        [:table
         (html/weightlift-tablehead)
         [:tbod\
          (for [{:keys [exerciseid name sets reps weight]} (db/weightlifts-for-plan db (:id p))]
            [:tr
             [:td name]
             (form-to [:post "/update-weightlift"]
                      [:input {:name "id" :type :hidden :value exerciseid}]
                      [:input.hidden {:type :submit}]
                      [:td [:input {:name "sets"
                                    :type :number
                                    :value sets
                                    :min "0"}]]
                      [:td [:input {:name "reps"
                                    :type :number
                                    :value reps
                                    :min "0"}]]
                      [:td [:input {:name "weight"
                                    :type :number
                                    :value weight
                                    :min "0"}]])])]]])]
    [:h3 "Complete a plan"]
    (form-to [:get "/complete-plan"]
             [:select {:name "plan"}
              (for [e (db/all db :plan)]
                [:option {:value (:id e)} (:name e)])]
             [:button.mui-btn "Start"])
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn number-input-if-number [exerciseid exercisetype v]
  (cond (util/parse-int v) [:input {:name (str (if (= exercisetype :weightlift) "_1" "_2")
                                               exerciseid
                                               (str "_" 'v))
                                    :type :number
                                    :value v
                                    :min "0"}]
        (nil? v) "N/A"
        :default v))

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
              (let [es (db/cardios-for-plan db id)]
                (html/cardio-tablebody number-input-if-number
                                       es
                                       [[:td [:input {:name (str "2_" (:exerciseid es) "_skip")
                                                       :type :checkbox}]]]
                                       [(:exerciseid es)
                                        :cardio]))]
             [:table
              (html/weightlift-tablehead "Skip?")
              (let [es (db/weightlifts-for-plan db id)]
                (html/weightlift-tablebody number-input-if-number
                                           es
                                           [[:td [:input {:name (str "1_" (:exerciseid es) "_skip")
                                                           :type :checkbox}]]]
                                           [(:exerciseid es)
                                            :weightlift]))]
             [:input {:type :submit :value "Save plan"}])]))

(defn- value-or-na [v]
  (if v v "N/A"))

(defn history [{:keys [db]}]
  (html5
   [:head
    [:title "Trainer - History"]
    [:body
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
               (html/cardio-tablebody value-or-na cardios nil nil)])
            (when weightlifts
              [:table
               (html/weightlift-tablehead)
               (html/weightlift-tablebody value-or-na weightlifts nil nil)])])))]]))

(def not-found (html5 "not found"))
