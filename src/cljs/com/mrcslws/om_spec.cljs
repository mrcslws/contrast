(ns com.mrcslws.om-spec
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.common :refer [display-name]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(defn drop-grandchildren [children]
  (mapv #(dissoc % :children) children))

(defmulti render-child
  (fn [c]
    (cond
     (contains? c :f)
     :f)))

(defmethod render-child :f [{:keys [f props m children-chan children]}]
  (om/build f props
            (-> m
                (assoc-in [:state ::children-chan]
                          children-chan)
                (assoc-in [:init-state ::initial-children]
                          children))))

(defmethod render-child nil [c]
  ;; Assume raw content.
  c)

;; Could do a defmethod for :reactf, allowing dom/div, etc.  The
;; difficulty here is that it will need a children-host to catch child
;; changes, since we can't rely on the component to pick up the
;; ::children-chan. This children-host needs to return a ReactElement
;; (not just text) so it puts strange requirements on what can be
;; hosted in a :reactf -- usually you can pass text into dom/div,
;; etc. It'd require some rework to make this not-awkward, so I'm
;; waiting and seeing if there's demand for this feature.


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
      (go-loop []
        (let [{:keys [teardown check-your-channels children-chan]}
              (om/get-state owner)]
          (when (alt!
                  teardown false
                  check-your-channels true

                  children-chan
                  ([v]
                     (when (not= (drop-grandchildren v)
                                 (drop-grandchildren (om/get-state
                                                      owner :children)))
                       (om/set-state! owner :children v))
                     true))

            (recur)))))

    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :teardown) true))

    om/IDidUpdate
    (did-update [_ _ _]
      (put! (om/get-state owner :check-your-channels) true))

    om/IRenderState
    (render-state [_ {:keys [children]}]
      (let [[first-element & remaining :as all] (map render-child children)]
        (if remaining
          (apply dom/div nil all)
          first-element)))))

(defn children-in-div
  ([owner]
     (children-in-div (om/get-state owner ::children-chan)
                      (om/get-state owner ::initial-children)
                      (display-name owner)))
  ([children-chan initial-children parent-name]
     (om/build children-host-component nil
               {:state {:parent-display-name parent-name
                        :children-chan children-chan}
                :init-state {:children initial-children}})))

;; For forwarding a component's children into another component.
(defn children-in-div-spec [owner]
  {:f children-host-component
   :m {:state {:parent-display-name (display-name owner)
               :children-chan (om/get-state owner ::children-chan)}
       :init-state {:children (om/get-state owner ::initial-children)}}})

(defn inc-last [v]
  (update-in v [(dec (count v))]
             inc))

(def actual-path (comp vec (partial interleave (repeat :children))))

(defn traverse [children f]
  (loop [sofar children
         path []]
    (let [actual (actual-path path)]
      (if (get-in children actual)
        (recur (f sofar actual)
               (conj path 0))
        (if (> (count path) 1)
          (recur sofar
                 (-> path pop inc-last))
          sofar)))))

(defn absorb-spec [spec owner]
  (let [;; Host the root as a child.
        ;; Store a spec because we want to store a :children-chan for the root.
        spec {:children [spec]}
        previous (om/get-state owner :absorbed-spec)
        new (traverse spec
                      (fn [sofar path]
                        (let [children-path (conj path :children)
                              channel-path (conj path :children-chan)]
                            (cond-> sofar

                                 (get-in spec children-path)
                                 (assoc-in channel-path
                                           (or (get-in previous channel-path)
                                               (chan)))))))]
    (om/set-state-nr! owner :absorbed-spec new)))

(defn spec-renderer-component [spec owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "spec-renderer")

    om/IWillMount
    (will-mount [_]
      (absorb-spec spec owner))

    om/IWillReceiveProps
    (will-receive-props [_ spec]
      (absorb-spec spec owner))

    om/IDidUpdate
    (did-update [_ _ _]
      (let [absorbed-spec (om/get-state owner :absorbed-spec)]
        (traverse absorbed-spec
                  (fn [_ path]
                    (when-let [channel (get-in absorbed-spec
                                               (conj path :children-chan))]
                      (put! channel
                            (get-in absorbed-spec (conj path :children))))))))

    om/IRenderState
    (render-state [this {:keys [absorbed-spec]}]
      (om/build children-host-component nil
                {:state {:parent-display-name (om/display-name this)
                         :children-chan (:children-chan absorbed-spec)}
                :init-state {:children (:children absorbed-spec)}}))))

(defn render [s]
  (om/build spec-renderer-component s))
