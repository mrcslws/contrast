(ns contrast.instrumentation
  (:require [om.core :as om :include-macros true]))

(defn updates-table [d]
  {:cols [:component :last-update :duration]
   :rows (->> d
              ;; {:react-id1 {:display-name "foo"
              ;;              :events [[:will-update Date Date]
              ;;                       [:render :who :knows]
              ;;                       [:did-update Date Date]]}
              (mapcat (fn [[_ {:keys [display-name events]}]]
                        (->> events
                             (filter (fn [[name _ _]]
                                       (or (= name :will-update)
                                           (= name :did-update))))
                             (partition 2)
                             (map (fn [[[name1 start1 finish1] [name2 start2 finish2]]]
                                    (assert (or (= name1 :will-update)))
                                    (assert (= name2 :did-update))
                                    [display-name finish2 (- finish2 start1)])))))
              ;; [["foo" Date 4]]
              (sort-by (fn [[_ finish _]]
                         finish))

              (map (fn [[display-name update-end duration]]
                     {:component (or display-name "Unknown")
                      :last-update (str (-> (str "0" (.getHours update-end))
                                            (.slice -2))
                                        ":"
                                        (-> (str "0" (.getMinutes update-end))
                                            (.slice -2))
                                        ":"
                                        (-> (str "0" (.getSeconds update-end))
                                            (.slice -2))
                                        ":"
                                        (-> (str "00" (.getMilliseconds update-end))
                                            (.slice -3)))
                      :duration (str duration "ms")})))})

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
