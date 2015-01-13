(ns contrast.slider
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [timeout <!]]
            [goog.string :as gstring]
            [goog.string.format]
            [contrast.dom :as domh])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def underlap 40)
(def wside 8)
(def wknob 13)
(def wmarker 8)

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

        (om/set-state! owner :knob-yoffset y )
        (when (< p 1)
          (recur))))))

;; TODO:
;; - Touch compatibility
;; - Keyboard compatibility
;; - Text styling extension point?
;; - Use state instead of opts?
(defn slider [data owner {:keys [data-key
                                 data-width data-min data-max
                                 data-interval data-format]}]
  (letfn [(slider-value [evt]
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
                (min data-max)))
          (is-in-tracking-area? [evt]
            (let [r (-> (om/get-node owner "tracking-area")
                           .getBoundingClientRect)]
              (and (>= (.-pageX evt) (.-left r))
                   (<= (.-pageX evt) (.-right r))
                   (>= (.-pageY evt) (.-top r))
                   (<= (.-pageY evt) (.-bottom r)))))]
    (reify
      om/IInitState
      (init-state [_]
        {:locked-value (get data data-key)
         :is-tracking? false
         :knob-offset 0})

      om/IRender
      (render [_]
        (dom/div #js {:class "Slider"
                      :style #js {:display "inline-block"
                                  :position "relative"
                                  :top -7
                                  :left 126}}
                 (dom/div #js
                          {:ref "tracking-area"
                           :onMouseEnter #(om/set-state! owner :is-tracking? true)
                           :onMouseMove #(om/update! data data-key
                                                     (slider-value %))
                           :onClick (fn [evt]
                                      (om/update! data data-key
                                                  (slider-value evt))
                                      (om/set-state! owner :locked-value
                                                     (get @data data-key))
                                      (bounce owner))
                           :onMouseOut (fn [evt]
                                         ;; This might just be the mouse going over the knob
                                         (if (is-in-tracking-area? evt)
                                           (om/update! data data-key (slider-value evt))
                                           (do
                                             (om/update! data data-key (om/get-state owner :locked-value))
                                             (om/set-state! owner :is-tracking? false))))

                           :style #js {:position "absolute"
                                       :z-index 0
                                       :left (- underlap)
                                       :top 0
                                       :width (+ data-width (* 2 underlap))
                                       :height 28
                                       :cursor "pointer"
                                       :backgroundSize "100% 100%"
                                       :backgroundRepeat "no-repeat"}}
                          (dom/div #js {:style #js {:position "absolute"
                                                    :zIndex 0
                                                    :left underlap
                                                    :top 14
                                                    :width wside
                                                    :height 4
                                                    :backgroundImage "url(/images/SliderLeft.png)"
                                                    :backgroundSize "100% 100%"
                                                    :backgroundRepeat "no-repeat"}})
                          (dom/div #js {:style #js {:position "absolute"
                                                    :zIndex 0
                                                    :left (-> data-width
                                                              (+ underlap)
                                                              (- wside))
                                                    :top 14
                                                    :width wside
                                                    :height 4
                                                    :backgroundImage "url(/images/SliderRight.png)"
                                                    :backgroundSize "100% 100%"
                                                    :backgroundRepeat "no-repeat"}})
                          (dom/div #js {:style #js {:position "absolute"
                                                    :zIndex 0
                                                    :left (+ underlap wside)
                                                    :top 14
                                                    :width (- data-width (* 2 wside))
                                                    :height 4
                                                    :backgroundImage "url(/images/SliderCenter.png)"
                                                    :backgroundSize "100% 100%"
                                                    :backgroundRepeat "no-repeat"}})
                          (dom/div #js {:style #js {:position "absolute"
                                                    :zIndex 0
                                                    :left (-> (om/get-state owner :locked-value)
                                                              (/ (- data-max data-min))
                                                              (* data-width)
                                                              (- (/ wmarker 2))
                                                              (+ underlap)
                                                              js/Math.round)
                                                    :top 14
                                                    :width wmarker
                                                    :height 4
                                                    :backgroundImage "url(/images/SliderMarker.png)"
                                                    :backgroundSize "100% 100%"
                                                    :backgroundRepeat "no-repeat"}})
                          (dom/div #js {:style #js {:position "absolute"
                                                    :zIndex 0
                                                    :left (-> (get data data-key)
                                                              (/ (- data-max data-min))
                                                              (* data-width)
                                                              (- (/ wknob 2))
                                                              (+ underlap)
                                                              js/Math.round)
                                                    :top (- 10 (om/get-state owner :knob-yoffset))
                                                    :width wknob
                                                    :height 13
                                                    :backgroundImage "url(/images/SliderKnob.png)"
                                                    :backgroundSize "100% 100%"
                                                    :backgroundRepeat "no-repeat"}}
                                   (dom/div #js {:style #js {:position "absolute"
                                                             :zIndex 0
                                                             :left -43
                                                             :top -13
                                                             :width 100
                                                             :height 10
                                                             :display (if (om/get-state owner :is-tracking?)
                                                                        "block"
                                                                        "none")
                                                             :backgroundSize "100% 100%"
                                                             :backgroundRepeat "no-repeat"}}
                                            (dom/div #js {:style #js {:font "10px Helvetica, Arial, sans-serif"
                                                                      :font-weight "bold"
                                                                      :color "#ccc"
                                                                      :text-align "center"}}
                                                     (cond->> (get data data-key)

                                                              data-format
                                                              (gstring/format data-format)))))))))))
