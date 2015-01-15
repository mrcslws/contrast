(ns contrast.row-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]
            [contrast.common :refer [wide-background-image]]))

(def tracking-area-underlap 40)

(def lens-overshot 10)
(def lens-h 11)
(def lens-side-w 6)

;; One handler to rule them all.
;; Mouse-moves are sometimes outside the boundary because the lens follows
;; the mouse, similar to the ice staircase in "Let It Go".
(defn on-move [evt config owner]
  (om/set-state!
   owner :lens-top
   (let [ta (om/get-node owner "tracking-area")]
     (when (and (domh/within-element? evt ta)
                (not (and (om/get-state owner :track-border-only)
                          (domh/within-element?
                           evt (om/get-node owner "content-container")))))
       (:y (domh/offset-from evt ta))))))

(defn row-probe [config owner]
  (reify
    om/IInitState
    (init-state [_]
      {:lens-top nil
       :track-border-only false})

    om/IRenderState
    (render-state [_ {:keys [content data-key data-width data-min data-max
                             data-interval lens-top]}]

      (dom/div #js {:onMouseOut #(on-move % config owner)
                    :onMouseMove #(on-move % config owner)
                    :onMouseEnter #(on-move % config owner)
                    :ref "tracking-area"
                    :style #js {;; Expand to the content,
                                ;; rather than the available space.
                                :display "inline-block"

                                :marginLeft (- tracking-area-underlap)
                                :marginRight (- tracking-area-underlap)
                                :paddingLeft tracking-area-underlap
                                :paddingRight tracking-area-underlap}}
               (dom/div #js {:style #js {;; Positioned from start of content,
                                         ;; rather than start of tracking area,
                                         ;; which is shifted due to padding.
                                         :position "relative"
                                         :zIndex 1}}
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
                               (wide-background-image
                                "images/RowLensLeft.png" 6
                                "images/RowLensCenter.png"
                                "images/RowLensRight.png" 6
                                lens-h)))
               (dom/div #js {:ref "content-container"
                             :style #js {:position "relative"
                                         :zIndex 0}} content)))))
