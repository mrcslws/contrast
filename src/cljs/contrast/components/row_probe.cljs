(ns contrast.components.row-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.common :refer [wide-background-image]]
            [contrast.state :as state]))

(def lens-overshot 5)
(def lens-h 3)

;; TODO - bug: when the row probe is not tracking, you can still hover
;; over it, blocking you from hovering over the content.
(defn on-move [row-inspect owner]
  (fn [_ content-y]
    (om/set-state! owner :is-tracking? true)
    (let [schema (om/get-state owner :schema)
          ch (.-offsetHeight (om/get-node owner "content"))]
      (when (and (>= content-y 0)
                 (< content-y ch))
       (om/update! row-inspect (:key schema) content-y)
       (om/set-state! owner :lens-top content-y)))))

(defn revert-to-locked! [row-inspect owner]
  (let [schema (om/get-state owner :schema)
        v (get-in row-inspect [:locked (:key schema)])]
    (om/update! row-inspect (:key schema) v)
    (om/set-state! owner :lens-top v)))

(defn on-exit [row-inspect owner]
  (fn [_ _]
    (om/set-state! owner :is-tracking? false)
    (revert-to-locked! row-inspect owner)))

(defn on-click [row-inspect owner]
  (fn []
    ;; TODO the locked value really needs to be indicated
    (let [key (:key (om/get-state owner :schema))]
        (om/update! (:locked row-inspect) key
                 (get row-inspect key)))))

(defn row-probe-component [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "row-probe")

    om/IInitState
    (init-state [_]
      {:lens-top nil
       :track-border-only? false
       :is-tracking? false})

    om/IWillMount
    (will-mount [_]
      (revert-to-locked! (om/observe owner (state/row-inspect k)) owner))

    om/IRenderState
    (render-state [_ {:keys [content lens-top track-border-only? is-tracking?]}]

      (let [row-inspect (om/observe owner (state/row-inspect k))]
        (tracking-area nil
                       {:on-move (on-move row-inspect owner)
                        :on-exit (on-exit row-inspect owner)
                        :on-click (on-click row-inspect owner)
                        :underlap-x 40
                        :track-border-only? track-border-only?
                        :determine-width-from-contents? true}
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
                                         (when is-tracking?
                                           (dom/div #js {:style
                                                         #js {:position "absolute"
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
                       (dom/div #js {:ref "content"
                                     :style #js {:position "relative"
                                                 :zIndex 0}} content))))))

(defn row-probe [k schema {:keys [track-border-only?]} content]
  (om/build row-probe-component k
            {:init-state {:track-border-only? track-border-only?}
             :state {:content content
                     :schema schema}}))
