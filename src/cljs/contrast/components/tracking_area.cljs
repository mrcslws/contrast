(ns contrast.components.tracking-area
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.dom :as domh]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; pnpoly - http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
;; Note that points must be ordered! Clockwise or counterclockwise, doesn't matter.
(defn poly-contains? [corners x y]
  (loop [i 0
         j (-> corners count dec)
         c? false]
    (if (< i (count corners))
      (recur (inc i)
             i
             (let [[ix iy] (nth corners i)
                   [jx jy] (nth corners j)]
               (if (and (not= (> iy y)
                              (> jy y))
                        (< x
                           (+ ix
                              (* (- jx ix)
                                 (/ (- y iy)
                                    (- jy iy))))))
                 (not c?)
                 c?)))
      c?)))

(defn coordinate [el]
  (let [r (.getBoundingClientRect el)]
    [(.-left r)
     (.-top r)]))

;; One handler to rule them all.
;; Mouse-moves are sometimes outside the boundary because the knob follows
;; the mouse, similar to the ice staircase in "Let It Go".
(defn move-handler [evt owner]
  (let [;; Rather than calculating via DOM offsets, hit-test the quadrilateral
        ;; formed by the corners of the element.
        ;; The DOM offset approach doesn't work with CSS transforms.
        [cc ta] (map (fn [refs]
                       (map #(-> (om/get-node owner %) coordinate)
                            refs))
                     [["nw" "ne" "se" "sw"]
                      ["tnw" "tne" "tse" "tsw"]])
        within-ta? (poly-contains? ta (.-clientX evt) (.-clientY evt))
        within-content? (poly-contains? cc (.-clientX evt) (.-clientY evt))

        ;; TODO calculate the [x y] within the untransformed element.
        {:keys [x y]} (domh/offset-from evt (om/get-node owner "content-container"))]
    (if (and within-ta?
             (not (and (om/get-state owner :track-border-only?)
                       within-content?)))
      (do
        (om/set-state-nr! owner :entered? true)
        (when-let [moves (om/get-state owner :moves)]
          (put! moves [x y])))
      (when (om/get-state owner :entered?)
        (om/set-state-nr! owner :entered? false)
        (when-let [exits (om/get-state owner :exits)]
          (put! exits [x y])))))
  nil)

(defn click-handler [evt owner]
  (move-handler evt owner)
  (when-let [clicks (om/get-state owner :clicks)]
    ;; The `on-click` should not care about the [x y].
    ;; Handle that in the `on-move`.
    (put! clicks :click))
  nil)

(defn reference-point [ref style]
  (dom/div #js {:ref ref
                :style (clj->js (assoc style
                                  :height 0
                                  :width 0
                                  :position "absolute"))}))

(defn tracking-area-component [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "tracking-area")

    om/IRenderState
    (render-state [_ {:keys [underlap-x underlap-y clicks
                             determine-width-from-contents?]}]
      (dom/div #js {:ref "tracking-area"
                    :className "TrackingArea"
                    :onMouseEnter #(move-handler % owner)
                    :onMouseMove #(move-handler % owner)
                    :onMouseOut #(move-handler % owner)
                    :onClick #(click-handler % owner)
                    :style #js {;; TODO convince self that this is acceptable
                                :display (if determine-width-from-contents?
                                           "inline-block"
                                           "block")
                                :height "100%"


                                ;; TODO explain and test
                                :verticalAlign "top"

                                :cursor (if clicks "pointer" "auto")

                                ;; Include child element margins.
                                ;; Otherwise they won't be included in cases
                                ;; where underlap is 0.
                                :overflow "auto"

                                :marginTop (- underlap-y)
                                :marginBottom (- underlap-y)
                                :paddingTop underlap-y
                                :paddingBottom underlap-y

                                :marginLeft (- underlap-x)
                                :marginRight (- underlap-x)
                                :paddingLeft underlap-x
                                :paddingRight underlap-x

                                :position "relative"}}
               (reference-point "tnw" {:left 0 :top 0})
               (reference-point "tne" {:right 0 :top 0})
               (reference-point "tsw" {:left 0 :bottom 0})
               (reference-point "tse" {:right 0 :bottom 0})
               (dom/div #js {:ref "content-container"
                             :className "ContentContainer"
                             :style #js {;; Positioned from start of content,
                                         ;; rather than start of tracking area,
                                         ;; which is shifted due to padding.
                                         :position "relative"}}
                        (reference-point "nw" {:left 0 :top 0})
                        (reference-point "ne" {:right 0 :top 0})
                        (reference-point "sw" {:left 0 :bottom 0})
                        (reference-point "se" {:right 0 :bottom 0})
                        (spec/children-in-div owner))))))
