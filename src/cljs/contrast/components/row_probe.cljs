(ns contrast.components.row-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.common :refer [wide-background-image]]))

(def lens-overshot 10)
(def lens-h 11)

(defn on-move [{:keys [target schema]} owner]
  (fn [_ content-y]
    (let [ch (.-offsetHeight (om/get-node owner "content"))
          y (-> content-y
                (max 0)
                (min (dec ch)))]
      (om/update! target (:key schema) y)
      (om/set-state! owner :lens-top y))))

(defn revert-to-locked! [{:keys [target schema]} owner]
  (let [v (get-in target [:locked (:key schema)])]
    (om/update! target (:key schema) v)
    (om/set-state! owner :lens-top v)))

(defn on-exit [config owner]
  (fn [_ _]
    (revert-to-locked! config owner)))

(defn on-click [{:keys [target schema]} owner]
  (fn []
    ;; TODO the locked value really needs to be indicated
    (om/update! (:locked target) (:key schema)
                (get target (:key schema)))))

;; TODO: bug -- moving the cursor up the top of the image
;; causes the probe to show up when content should be ignored
(defn row-probe-component [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:lens-top nil
       :track-border-only? false})

    om/IWillMount
    (will-mount [_]
      (revert-to-locked! config owner))

    om/IRenderState
    (render-state [_ {:keys [content data-key data-width data-min data-max
                             data-interval lens-top track-border-only?]}]
      (tracking-area nil
       {:on-move (on-move config owner)
        :on-exit (on-exit config owner)
        :on-click (on-click config owner)
        :underlap-x 40
        :underlap-y 10
        :track-border-only? track-border-only?
        :determine-width-from-contents? true}
       (dom/div #js {:style #js {:position "relative"
                                 :zIndex 1
                                 :height 0}}
                (apply dom/div #js {:style
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
                       (wide-background-image "images/RowLensLeft.png" 6
                                              "images/RowLensCenter.png"
                                              "images/RowLensRight.png" 6
                                              lens-h)))
       (dom/div #js {:ref "content"
                     :style #js {:position "relative"
                                 :zIndex 0}} content)))))

(defn row-probe [target schema {:keys [track-border-only?]} content]
  (om/build row-probe-component {:target target :schema schema}
            {:init-state {:track-border-only? track-border-only?}
             :state {:content content}}))
