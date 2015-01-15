(ns contrast.slider
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [timeout <!]]
            [goog.string :as gstring]
            [goog.string.format]
            [contrast.dom :as domh]
            [contrast.common :refer [wide-background-image background-image]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def underlap 40)
(def wside 8)
(def wknob 13)
(def wmarker 8)
(def wlabel 100)

(defn slider-value [evt owner]
  (let [{:keys [data-width data-max data-min data-interval]} (om/get-state owner)]
   (-> evt
       (domh/offset-from (om/get-node owner "tracking-area"))
       :x
       (- underlap)
       (/ data-width)
       (* (- data-max data-min))
       (cond-> data-interval
               (-> (/ data-interval)
                   js/Math.round
                   (* data-interval)))
       (+ data-min)
       (max data-min)
       (min data-max))))

;; One handler to rule them all.
;; Mouse-moves are sometimes outside the boundary because the knob follows
;; the mouse, similar to a magic carpet.
(defn on-move [evt data owner]
  (let [ta (om/get-node owner "tracking-area")
        {:keys [data-key]} (om/get-state owner)]
    (if (domh/within-element? evt ta)
      (do
        (om/set-state! owner :is-tracking? true)
        (om/update! data data-key (slider-value evt owner)))
      (do
        (om/set-state! owner :is-tracking? false)
        (om/update! data data-key (om/get-state owner :locked-value))))))

(defn slider-left [data-value owner]
  (let [{:keys [data-max data-min data-width]} (om/get-state owner)]
    (-> data-value
        (/ (- data-max data-min))
        (* data-width)
        (- (/ wmarker 2))
        js/Math.round)))

(defn bounce [owner]
  (let [start (js/Date.now)
        bounce-duration 500
        b1height 15
        b2height 4]
    (go-loop []
      (<! (timeout 20))
      (let [p (-> (js/Date.now)
                  (- start)
                  (/ bounce-duration)
                  (min 1))
            b2 (> p 0.5)
            y (* (if b2
                   b2height
                   b1height)
                 (- 1 (-> p
                          (cond-> b2
                                  (- 0.5))
                          (* 4)
                          (- 1)
                          (js/Math.pow 2))))]

        (om/set-state! owner :knob-yoffset y)
        (when (< p 1)
          (recur))))))

;; TODO:
;; - 80 per line
;; - Discover what's reusable between this and row-probe
;; - Touch compatibility
;; - Keyboard compatibility
;; - Text styling extension point?
;; - :pre verification of state
(defn slider [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:is-tracking? false
       :knob-offset 0})

    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :locked-value (get data
                                              (om/get-state owner :data-key))))

    om/IRenderState
    (render-state [_ {:keys [data-key data-width data-min data-max data-format
                             data-interval]}]
      (dom/div #js {:ref "tracking-area"
                    :onMouseEnter #(on-move % data owner)
                    :onMouseMove #(on-move % data owner)
                    :onClick (fn [evt]
                               (on-move evt data owner)
                               (om/set-state! owner :locked-value
                                              (get @data data-key))
                               (bounce owner))
                    :onMouseOut #(on-move % data owner)
                    :style #js {;; Expand to the content,
                                ;; rather than the available space.
                                :display "inline-block"
                                :width data-width

                                :marginTop (- underlap)
                                :marginBottom (- underlap)
                                :paddingTop underlap
                                :paddingBottom underlap

                                :marginLeft (- underlap)
                                :marginRight (- underlap)
                                :paddingLeft underlap
                                :paddingRight underlap}}
               (dom/div #js {:id "this"
                             :style #js {;; Positioned from start of content,
                                         ;; rather than start of tracking area,
                                         ;; which is shifted due to padding.
                                         :position "relative"}}
                        (apply dom/div #js {:style #js {:position "absolute"
                                                        :zOrder 0
                                                        :height 4
                                                        :width data-width}}
                               (wide-background-image "/images/SliderLeft.png" wside
                                                      "/images/SliderCenter.png"
                                                      "/images/SliderRight.png" wside
                                                      4))

                        (dom/div #js {:style #js {:position "absolute"
                                                  :zOrder 1
                                                  :left (- (slider-left (om/get-state owner :locked-value) owner)
                                                           (quot wmarker 2))}}
                                 (background-image "/images/SliderMarker.png" wmarker 4))
                        (dom/div #js {:style #js {:position "absolute"
                                                  :zOrder 2
                                                  ;; I apologize for the magic number.
                                                  :top (- -4 (om/get-state owner :knob-yoffset))
                                                  :left (- (slider-left (get data data-key) owner)
                                                           (quot wknob 2))}}
                                 (background-image "/images/SliderKnob.png" wknob 13)
                                 (dom/div #js {:style #js {:position "relative"

                                                           :width wlabel
                                                           :left (- (- (quot wlabel 2) (quot wknob 2)))

                                                           :top -13
                                                           :height 10
                                                           :display (if (om/get-state owner :is-tracking?)
                                                                      "block"
                                                                      "none")}}
                                          (dom/div #js {:style #js {:font "10px Helvetica, Arial, sans-serif"
                                                                    :font-weight "bold"
                                                                    :color "#ccc"
                                                                    :text-align "center"}}
                                                   (cond->> (get data data-key)

                                                            data-format
                                                            (gstring/format data-format))))))))))
