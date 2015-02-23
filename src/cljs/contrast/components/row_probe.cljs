(ns contrast.components.row-probe
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.common :refer [wide-background-image]]
            [contrast.components.tracking-area :refer [tracking-area-component]]
            [contrast.state :as state]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [com.mrcslws.om-spec :as spec])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def lens-overshot 5)
(def lens-h 3)

(defn row-probe-ui [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "row-probe-ui")

    om/IRender
    (render [_]
      (let [row-inspect (om/observe owner (state/row-inspect k))
            lens-top (:probed-row row-inspect)]
        (dom/div
         nil
         (dom/div #js {:style #js {:position "relative"
                                   :zIndex 1
                                   :height 0}}

                  (dom/div #js {:style
                                #js {:display (if (nil? lens-top)
                                                "none" "block")

                                     :top (- lens-top (quot lens-h 2))
                                     :height lens-h

                                     ;; Fill the positioned container
                                     :position "absolute"
                                     :width "100%"

                                     ;; Lengthen and center
                                     :paddingLeft lens-overshot
                                     :paddingRight lens-overshot
                                     :left (- lens-overshot)}}

                           (dom/div #js {:style
                                         #js {:position "absolute"
                                              :width lens-overshot
                                              :height lens-h
                                              :backgroundColor "red"
                                              :left 0}})
                           (when (:is-tracking? row-inspect)
                             (dom/div
                              #js {:style #js {:position "absolute"
                                               :left lens-overshot
                                               :right lens-overshot
                                               :height 1
                                               :borderTop "1px solid red"
                                               :borderBottom "1px solid red"}}))
                           (dom/div #js {:style
                                         #js {:position "absolute"
                                              :width lens-overshot
                                              :height lens-h
                                              :backgroundColor "red"
                                              :right 0}})))
         (dom/div #js {:style #js {:position "relative"
                                   :zIndex 0}}
                  (spec/children-in-div owner)))))))

(defn revert-to-locked! [row-inspect schema]
  (let [v (get-in @row-inspect [:locked (:key schema)])]
    (om/update! row-inspect (:key schema) v)))

(defn row-probe-component [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "row-probe")

    om/IInitState
    (init-state [_]
      {:moves (chan)
       :clicks (chan)
       :exits (chan)})

    om/IWillMount
    (will-mount [_]
      (let [row-inspect (state/row-inspect k)]
        ;; TODO I hate this.
        (revert-to-locked! row-inspect (om/get-state owner :schema))

        ;; TODO - bug: when the row probe is not tracking, you can still hover
        ;; over it, blocking you from hovering over the content.
        (go-loop []
          (let [[_ content-y] (<! (om/get-state owner :moves))
                {:keys [schema]} (om/get-state owner)]
            (om/update! row-inspect :is-tracking? true)
            (om/update! row-inspect (:key schema) content-y)
            (recur)))

        (go-loop []
          (let [_ (<! (om/get-state owner :exits))
                {:keys [schema]} (om/get-state owner)]
            (om/update! row-inspect :is-tracking? false)
            (revert-to-locked! row-inspect schema)
            (recur)))

        (go-loop []
          (let [_ (<! (om/get-state owner :clicks))
                {:keys [schema]} (om/get-state owner)]
            ;; TODO the locked value really needs to be indicated
            (let [key (:key schema)]
              (om/update! (:locked row-inspect) key
                          (get @row-inspect key)))
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [track-border-only? moves clicks exits]}]
      (spec/render
       {:f tracking-area-component
        :m {:state {:moves moves
                    :exits exits
                    :clicks clicks
                    :underlap-x 40
                    :track-border-only? track-border-only?
                    :determine-width-from-contents? true}}
        :children [(spec/children-in-div-spec owner)]}))))

(defn row-probe-spec [k schema track-border-only? children]
  {:f row-probe-component
   :props k
   :m {:state {:track-border-only? track-border-only?
               :schema schema}}
   :children [{:f row-probe-ui
               :props k
               :children children}]})
