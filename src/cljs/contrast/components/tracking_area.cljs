(ns contrast.components.tracking-area
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]))

;; One handler to rule them all.
;; Mouse-moves are sometimes outside the boundary because the knob follows
;; the mouse, similar to the ice staircase in "Let It Go".
(defn move-handler [evt owner]
  (let [ta (om/get-node owner "tracking-area")
        cc (om/get-node owner "content-container")
        {:keys [x y]} (domh/offset-from evt cc)
        track (and (domh/within-element? evt ta)
                   (not (and (om/get-state owner :track-border-only?)
                             (domh/within-element? evt cc))))]
    (if track
      (when-let [on-move (om/get-state owner :on-move)]
        (on-move x y))
      (when-let [on-exit (om/get-state owner :on-exit)]
        (on-exit x y)))))

(defn click-handler [evt owner]
  (move-handler evt owner)
  (when-let [on-click (om/get-state owner :on-click)]
    ;; The `on-click` should not care about the [x y].
    ;; Handle that in the `on-move`.
    (on-click)))


(defn tracking-area-component [_ owner]
  (reify

    om/IRenderState
    (render-state [_ {:keys [content underlap-x underlap-y on-click
                             determine-width-from-contents?]}]
      (dom/div #js {:ref "tracking-area"
                    :onMouseEnter #(move-handler % owner)
                    :onMouseMove #(move-handler % owner)
                    :onMouseOut #(move-handler % owner)
                    :onClick #(click-handler % owner)
                    :style #js {;; TODO convince self that this is acceptable
                                :display (if determine-width-from-contents?
                                           "inline-block"
                                           "block")


                                ;; TODO explain and test
                                :verticalAlign "top"

                                :cursor (if on-click "pointer" "auto")

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
                                :paddingRight underlap-x}}
               (apply dom/div
                      #js {:ref "content-container"
                           :style #js {;; Positioned from start of content,
                                       ;; rather than start of tracking area,
                                       ;; which is shifted due to padding.
                                       :position "relative"}}
                      content)))))

(defn tracking-area [style config & content]
  (dom/div {:style (clj->js style)}
           (om/build tracking-area-component nil
                     {:state (assoc config
                               :content content)})))
