(ns com.mrcslws.om-spec
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.common :refer [display-name]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(defn drop-grandchildren [children]
  (mapv #(cond-> % (associative? %) (dissoc % :children)) children))

(defn children-host-component [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      (str (or (om/get-state owner :parent-display-name)
               "Unnamed component's")
           " "
           "children-host"))


    om/IInitState
    (init-state [_]
      {:check-your-channels (chan)
       :teardown (chan)})

    om/IWillMount
    (will-mount [_]
      (let [;; Caching avoids recalculation and it makes calls to `=` faster.
            cached-comparee (atom (drop-grandchildren
                                   (om/get-state owner :children)))]
       (go-loop []
         (let [{:keys [teardown check-your-channels channels path]}
               (om/get-state owner)
               children-chan (get @channels path)]
           (when (alt!
                   children-chan
                   ([v]
                      (let [comparee (drop-grandchildren v)]
                        (when (not= comparee
                                    @cached-comparee)
                          (om/set-state! owner :children v)
                          (reset! cached-comparee comparee)))
                      true)

                   teardown false
                   check-your-channels true)

             (recur))))))

    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :teardown) true))

    om/IDidUpdate
    (did-update [_ _ _]
      (put! (om/get-state owner :check-your-channels) true))

    om/IRenderState
    (render-state [_ {:keys [children path channels]}]
      (let [[first-element & remaining :as all]
            (loop [i 0
                   sofar []]
              (if (< i (count children))
                (let [child (nth children i)]
                    (recur (inc i)
                           (conj sofar
                                 (if (associative? child)
                                   (let [{:keys [f props m children]} child]
                                     (om/build f props
                                               (-> m
                                                   (assoc-in [:state ::path]
                                                             (conj path i))
                                                   (assoc-in [:state ::channels]
                                                             channels)
                                                   (assoc-in [:init-state
                                                              ::initial-children]
                                                             children))))
                                   (do
                                     (when (aget child "_isReactElement")
                                       (js/console.warn
                                        (str "Passing a ReactElement as a child "
                                             "will force a render.")))
                                     child)))))
                sofar))]
        (if remaining
          (apply dom/div nil all)
          first-element)))))

(defn children-in-div
  ([owner]
     (children-in-div (om/get-state owner ::path)
                      (om/get-state owner ::initial-children)
                      (om/get-state owner ::channels)
                      (display-name owner)))
  ([path initial-children channels parent-name]
     (om/build children-host-component nil
               {:state {:parent-display-name parent-name
                        :channels channels
                        :path path}
                :init-state {:children initial-children}})))

;; For forwarding a component's children into another component.
(defn children-in-div-spec [owner]
  {:f children-host-component
   :m {:state {:parent-display-name (display-name owner)
               :path (om/get-state owner ::path)
               :channels (om/get-state owner ::channels)}
       :init-state {:children (om/get-state owner ::initial-children)}}})

(defn inc-last [v]
  (update-in v [(dec (count v))]
             inc))

(defn traverse
  ([spec f]
     (traverse spec f []))
  ([spec f path]
     (when spec
       (f spec path)
       (when-let [children (:children spec)]
         (dotimes [i (count children)]
           (traverse (nth children i) f (conj path i)))))))


(defn absorb-spec [spec owner]
  (let [;; Host the root as a child.
        ;; Store a spec because we want to store a channel for the root.
        spec {:children [spec]}
        {previous :absorbed-spec channels :channels} (om/get-state owner)]
    (traverse spec
              (fn [node
                   path]
                (when (and (contains? node :children)
                           (not (contains? @channels path)))
                  (swap! channels assoc path (chan)))))
    (om/set-state-nr! owner :absorbed-spec spec)))

(defn spec-renderer-component [spec owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "spec-renderer")

    om/IInitState
    (init-state [_]
      {:channels (atom {})})

    om/IWillMount
    (will-mount [_]
      (absorb-spec spec owner))

    om/IWillReceiveProps
    (will-receive-props [_ spec]
      (absorb-spec spec owner))

    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [absorbed-spec channels]} (om/get-state owner)
            channels @channels]
        (traverse absorbed-spec
                  (fn [node path]
                    (when-let [channel (get channels path)]
                      (put! channel (:children node)))))))

    om/IRenderState
    (render-state [this {:keys [absorbed-spec channels]}]
      (om/build children-host-component nil
                {:state {:parent-display-name (om/display-name this)
                         :path []
                         :channels channels}
                 :init-state {:children (:children absorbed-spec)}}))))

(defn render [s]
  (om/build spec-renderer-component s))
