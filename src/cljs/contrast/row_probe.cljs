(ns contrast.row-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]
            [contrast.common :refer [wide-background-image
                                     tracking-area]]))

(def lens-overshot 10)
(def lens-h 11)

(defn on-move [_ owner]
  (fn [_ content-y]
    (om/set-state! owner :lens-top content-y)))

(defn on-exit [data owner]
  (fn [_ _]
    (om/set-state! owner :lens-top nil)))

(defn row-probe [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:lens-top nil
       :track-border-only? false})

    om/IRenderState
    (render-state [_ {:keys [content data-key data-width data-min data-max
                             data-interval lens-top track-border-only?]}]
      (tracking-area {:display "inline-block"}
       {:on-move (on-move config owner)
        :on-exit (on-exit config owner)
        :underlap-x 40
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
       (dom/div #js {:style #js {:position "relative"
                                 :zIndex 0}} content)))))
