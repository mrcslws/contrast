(ns contrast.instrumentation
  (:require [om.core :as om :include-macros true]))

(defn uncursor [v]
  (cond-> v
          (om/cursor? v)
          om/value))

(defn updates [events]
  (->> events
       (filter (fn [[name _ _]]
                 (or (= name :will-update)
                     (= name :did-update))))
       (partition 2)
       (map (fn [[[name1 start1 finish1] [name2 start2 finish2]]]
              (assert (or (= name1 :will-update)))
              (assert (= name2 :did-update))
              {:time finish2
               :duration (- finish2 start1)}))))

(defn format-display-name [s]
  (or s "Unknown"))

(defn format-timestamp [date]
  (str (-> (str "0" (.getHours date))
           (.slice -2))
       ":"
       (-> (str "0" (.getMinutes date))
           (.slice -2))
       ":"
       (-> (str "0" (.getSeconds date))
           (.slice -2))
       ":"
       (-> (str "00" (.getMilliseconds date))
           (.slice -3))))

(defn format-duration [duration]
  (str (.toFixed duration 1) "ms"))

(defn updates-table [d]
  {:cols ["component" "last update" "duration"]
   :rows (->> d
              uncursor
              ;; {:react-id1 {:display-name "foo"
              ;;              :events [[:will-update Date Date]
              ;;                       [:render :who :knows]
              ;;                       [:did-update Date Date]]}
              (mapcat (fn [[_ {:keys [display-name events]}]]

                        (->> (updates events)
                             (map (fn [{:keys [time duration]}]
                                    [display-name time duration])))))
              ;; [["foo" Date 4]]
              (sort-by (fn [[_ finish _]]
                         finish))

              (map (fn [[display-name update-end duration]]
                     [(format-display-name display-name)
                      (format-timestamp update-end)
                      (format-duration duration)])))})

(defn mean [coll]
  (/ (reduce + coll)
     (count coll)))

(defn std-dev [coll]
  (let [a (mean coll)]
    (Math/sqrt (mean (map #(Math/pow (- % a) 2) coll)))))

(defn aggregate-update-times [d]
  {:cols ["component" "count" "total duration" "last update" "last duration"
          "average duration" "max duration" "min duration" "stdev duration"]
   :rows (->> d
              uncursor
              (map (fn [[id {:keys [display-name events]}]]
                     (let [ups (updates events)]
                       (when-not (empty? ups)
                         (let [{last-time :time
                                last-duration :duration} (apply max-key :time ups)
                                [sum a sd m+ m-] ((juxt (partial apply +)
                                                        mean
                                                        std-dev
                                                        (partial apply max)
                                                        (partial apply min)) (map :duration ups))]
                           {:id id
                            :display-name display-name
                            :count (count ups)
                            :total-duration sum
                            :last-time last-time
                            :last-duration last-duration
                            :mean a
                            :stdev sd
                            :max m+
                            :min m-})))))
              (remove nil?)
              (sort-by :last-time >)
              (map (juxt (comp format-display-name :display-name)
                         :count
                         (comp format-duration :total-duration)
                         (comp format-timestamp :last-time)
                         (comp format-duration :last-duration)
                         (comp format-duration :mean)
                         (comp format-duration :max)
                         (comp format-duration :min)
                         (comp format-duration :stdev))))})

(defn component-id [x]
  ;; If a component contains a component, and this parent has no actual DOM
  ;; nodes, then they share a `rootNodeID` but their depths differ.
  (let [id (aget x "_rootNodeID")
        depth (aget x "_mountDepth")]
    (assert id)
    (assert depth)
    [id depth]))

(defn display-name [c]
  ((aget c "getDisplayName")))

(defn log-times [data event-name this f]
  (let [start (js/Date.)
        r (f)
        finish (js/Date.)]
    (swap! data update-in [(component-id this)]
           (fn [{:keys [events]}]
             {:display-name (display-name this)
              :events (-> events
                          (conj [event-name start finish])
                          vec)}))
    r))

(defn instrument-methods [methods data]
  (-> methods
      (update-in [:componentWillMount]
                 (fn [f]
                   (fn []
                     (this-as this
                              (log-times data :will-mount this
                                         #(.call f this))))))
      (update-in [:componentDidMount]
                 (fn [f]
                   (fn []
                     (this-as this
                              (log-times data :did-mount this
                                         #(.call f this))))))
      (update-in [:componentWillUnmount]
                 (fn [f]
                   (fn []
                     (this-as this
                              (log-times data :will-unmount this
                                         #(.call f this))))))
      (update-in [:componentWillUpdate]
                 (fn [f]
                   (fn [next-props next-state]
                     (this-as this
                              (log-times data :will-update this
                                         #(.call f this next-props next-state))))))
      (update-in [:componentDidUpdate]
                 (fn [f]
                   (fn [prev-props prev-state]
                     (this-as this
                              (log-times data :did-update this
                                         #(.call f this prev-props prev-state))))))
      (update-in [:render]
                 (fn [f]
                   (fn []
                     (this-as this
                              (log-times data :render this
                                         #(.call f this))))))))
