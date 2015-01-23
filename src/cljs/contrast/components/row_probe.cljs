(ns contrast.components.row-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.common :refer [wide-background-image]]))

(def lens-overshot 5)
(def lens-h 3)

(defn on-move [{:keys [target schema]} owner]
  (fn [_ content-y]
    (om/set-state! owner :is-tracking? true)
    (let [ch (.-offsetHeight (om/get-node owner "content"))]
      (when (and (>= content-y 0)
                 (< content-y ch))
       (om/update! target (:key schema) content-y)
       (om/set-state! owner :lens-top content-y)))))

(defn revert-to-locked! [{:keys [target schema]} owner]
  (let [v (get-in target [:locked (:key schema)])]
    (om/update! target (:key schema) v)
    (om/set-state! owner :lens-top v)))

(defn on-exit [config owner]
  (fn [_ _]
    (om/set-state! owner :is-tracking? false)
    (revert-to-locked! config owner)))

(defn on-click [{:keys [target schema]} owner]
  (fn []
    ;; TODO the locked value really needs to be indicated
    (om/update! (:locked target) (:key schema)
                (get target (:key schema)))))

(defn row-probe-component [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:lens-top nil
       :track-border-only? false
       :is-tracking? false})

    om/IWillMount
    (will-mount [_]
      (revert-to-locked! config owner))

    om/IRenderState
    (render-state [_ {:keys [content lens-top track-border-only? is-tracking?]}]
      (tracking-area nil
       {:on-move (on-move config owner)
        :on-exit (on-exit config owner)
        :on-click (on-click config owner)
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
                                 :zIndex 0}} content)))))

(defn row-probe [target schema {:keys [track-border-only?]} content]
  (om/build row-probe-component {:target target :schema schema}
            {:init-state {:track-border-only? track-border-only?}
             :state {:content content}}))
