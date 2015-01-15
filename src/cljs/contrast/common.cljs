(ns contrast.common
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]))

;; Common components

(defn css-url [url]
  (str "url(" url ")"))

(defn background-image [img w h]
  (dom/div #js {:style #js {:position "absolute"
                            :backgroundSize "100% 100%"
                            :backgroundRepeat "no-repeat"
                            :backgroundImage (css-url img)
                            :width w
                            :height h}}))

(defn wide-background-image [left leftw center right rightw h]
  (let [common {:position "absolute"
                :backgroundSize "100% 100%"
                :backgroundRepeat "no-repeat"
                :height h}]
    (map #(dom/div #js {:style (clj->js (apply assoc common %))})
         [[:left 0
           :backgroundImage (css-url left)
           :width leftw]
          [:left leftw
           :right rightw
           :backgroundImage (css-url center)]
          [:right 0
           :backgroundImage (css-url right)
           :width rightw]])))



;; TRACKING AREA

;; One handler to rule them all.
;; Mouse-moves are sometimes outside the boundary because the knob follows
;; the mouse, similar to the ice staircase in "Let It Go".
(defn move-handler [evt data owner]
  (let [ta (om/get-node owner "tracking-area")
        cc (om/get-node owner "content-container")
        {:keys [x y]} (domh/offset-from evt cc)
        track (and (domh/within-element? evt ta)
                   (not (and (om/get-state owner :track-border-only?)
                             (domh/within-element? evt cc))))]
    (when data (om/update! data :is-tracking? track))
    (if track
      (when-let [on-move (om/get-state owner :on-move)]
        (on-move x y))
      (when-let [on-exit (om/get-state owner :on-exit)]
        (on-exit x y)))))

(defn click-handler [evt data owner]
  (move-handler evt data owner)
  (when-let [on-click (om/get-state owner :on-click)]
    ;; The `on-click` should not care about the [x y].
    ;; Handle that in the `on-move`.
    (on-click)))

(defn tracking-area [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when data (om/update! data :is-tracking? false)))

    om/IRenderState
    (render-state [_ {:keys [content underlap-x underlap-y on-click]}]
      (dom/div #js {:ref "tracking-area"
                    :onMouseEnter #(move-handler % data owner)
                    :onMouseMove #(move-handler % data owner)
                    :onMouseOut #(move-handler % data owner)
                    :onClick #(click-handler % data owner)
                    :style #js {;; Expand to the content,
                                ;; rather than the available space.
                                :display "inline-block"
                                :cursor (if on-click "pointer" "auto")

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

(defn ->tracking-area [data optional-state & content]
  (om/build tracking-area data
            {:state (assoc optional-state
                      :content content)}))
