(ns contrast.slider
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [timeout <!]]
            [goog.string :as gstring]
            [goog.string.format]
            [contrast.dom :as domh]
            [contrast.common :refer [wide-background-image background-image
                                     ->tracking-area]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def wknob 13)
(def wmarker 8)
(def wlabel 100)

(defn slider-value [x owner]
  (let [{:keys [data-width data-max data-min data-interval]} (om/get-state
                                                              owner)]
    (-> x
        (/ data-width)
        (* (- data-max data-min))
        (cond-> data-interval
                (-> (/ data-interval)
                    js/Math.round
                    (* data-interval)))
        (+ data-min)
        (max data-min)
        (min data-max))))

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

(defn on-move [data owner]
  (fn [content-x _]
    (om/update! data (om/get-state owner :data-key)
                (slider-value content-x owner))))

(defn on-exit [data owner]
  (fn [_ _]
    (om/update! data (om/get-state owner :data-key)
                (om/get-state owner :locked-value))))

(defn on-click [data owner]
  (fn []
    (om/set-state! owner :locked-value
                   (get @data (om/get-state owner :data-key)))
    (bounce owner)))

(defn slider-left [data-value owner]
  (let [{:keys [data-max data-min data-width]} (om/get-state owner)]
    (-> data-value
        (/ (- data-max data-min))
        (* data-width)
        js/Math.round)))

;; TODO:
;; - Discover what's reusable between this and row-probe
;; - Touch compatibility
;; - Keyboard compatibility
;; - Text styling extension point?
;; - :pre verification of state
(defn slider [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:knob-offset 0})

    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :locked-value (get data
                                              (om/get-state owner :data-key))))

    om/IRenderState
    (render-state [_ {:keys [data-key data-width data-min data-max data-format
                             data-interval]}]
      (->tracking-area
       (:slider data)
       {:on-move (on-move data owner)
        :on-exit (on-exit data owner)
        :on-click (on-click data owner)
        :underlap-x 40
        :underlap-y 10}
       (dom/div #js {:style #js {;; Establish the size of the content
                                 :width data-width}}
                (apply dom/div #js {:style #js {:position "absolute"
                                                :zOrder 0
                                                :height 4
                                                :width data-width}}
                       (wide-background-image "/images/SliderLeft.png" 8
                                              "/images/SliderCenter.png"
                                              "/images/SliderRight.png" 8
                                              4))

                (dom/div #js {:style
                              #js {:position "absolute"
                                   :zOrder 1
                                   :left (- (slider-left
                                             (om/get-state
                                              owner :locked-value) owner)
                                            (quot wmarker 2))}}
                         (background-image "/images/SliderMarker.png"
                                           wmarker 4))
                (dom/div #js {:style
                              #js {:position "absolute"
                                   :zOrder 2
                                   ;; I apologize for the magic number.
                                   :top (- -4
                                           (om/get-state owner :knob-yoffset))
                                   :left (- (slider-left
                                             (get data data-key) owner)
                                            (quot wknob 2))}}
                         (background-image "/images/SliderKnob.png" wknob 13)
                         (dom/div
                          #js {:style
                               #js {:position "relative"

                                    :width wlabel
                                    :left (- (- (quot wlabel 2)
                                                (quot wknob 2)))

                                    :top -13
                                    :height 10
                                    :display (if (:is-tracking? (:slider data))
                                               "block"
                                               "none")}}
                          (dom/div
                           #js {:style
                                #js {:font "10px Helvetica, Arial, sans-serif"
                                     :font-weight "bold"
                                     :color "#ccc"
                                     :text-align "center"}}
                           (cond->> (get data data-key)

                                    data-format
                                    (gstring/format data-format))))))))))
