(ns contrast.components.slider
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [timeout <!]]
            [goog.string :as gstring]
            [goog.string.format]
            [contrast.dom :as domh]
            [contrast.common :refer [wide-background-image background-image]]
            [contrast.components.tracking-area :refer [tracking-area]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def wknob 13)
(def wmarker 8)
(def wlabel 100)

(defn slider-value [x schema owner]
  (let [{vmax :max vmin :min interval :interval} schema]
    (-> x
        (/ (.-offsetWidth (om/get-node owner "slider")))
        (* (- vmax vmin))
        (cond-> interval
                (-> (/ interval)
                    js/Math.round
                    (* interval)))
        (+ vmin)
        (max vmin)
        (min vmax))))

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

(defn on-move [target schema owner]
  (fn [content-x _]
    (om/set-state! owner :is-tracking? true)
    (om/update! target (:key schema) (slider-value content-x schema owner))))

(defn on-exit [target schema owner]
  (fn [_ _]
    (om/set-state! owner :is-tracking? false)
    (om/update! target (:key schema) (om/get-state owner :locked-value))))

(defn on-click [target schema owner]
  (fn []
    (om/set-state! owner :locked-value (get target (:key schema)))
    (bounce owner)))

(defn slider-left [value schema]
  (-> value
      (/ (- (:max schema) (:min schema)))
      (* 100)
      js/Math.round
      (str "%")))

;; TODO:
;; - Discover what's reusable between this and row-probe
;; - Touch compatibility
;; - Keyboard compatibility
;; - Text styling extension point?
;; - :pre verification of state
(defn slider-component [{:keys [target schema]} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:knob-offset 0
       :is-tracking? false})

    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :locked-value (get target (:key schema))))

    om/IRenderState
    (render-state [_ {:keys [is-tracking?]}]
      (tracking-area
       nil
       {:on-move (on-move target schema owner)
        :on-exit (on-exit target schema owner)
        :on-click (on-click target schema owner)
        :underlap-x 40
        :underlap-y 0}
       (dom/div #js {:ref "slider"
                     :style #js {;; Make room for the bounce.
                                 :margin-top 20
                                 :margin-bottom 8
                                 :height 4}}
                (apply dom/div #js {:style #js {:position "absolute"
                                                :zIndex 0
                                                :height 4
                                                :width "100%"}}
                       (wide-background-image "/images/SliderLeft.png" 8
                                              "/images/SliderCenter.png"
                                              "/images/SliderRight.png" 8
                                              4))
                (dom/div #js {:style
                              #js {:position "absolute"
                                   :zIndex 1
                                   :left (slider-left (om/get-state
                                                       owner :locked-value)
                                                      schema)}}
                         (dom/div #js {:style #js {:position "absolute"
                                                   :left (- (quot wmarker 2))}}
                                  (background-image "/images/SliderMarker.png"
                                                    wmarker 4)))
                (dom/div #js {:style
                              #js {:position "absolute"
                                   :zIndex 2
                                   ;; I apologize for the magic number.
                                   :top (- -4
                                           (om/get-state owner :knob-yoffset))
                                   :left (slider-left (get target
                                                           (:key schema))
                                                      schema)}}
                         (dom/div #js {:style #js {:position "absolute"
                                                   :left (- (quot wknob 2))}}
                                  (background-image "/images/SliderKnob.png"
                                                    wknob 13))
                         (dom/div
                          #js {:style
                               #js {:position "relative"

                                    :width wlabel
                                    :left (- (- (quot wlabel 2)
                                                (quot wknob 2)))

                                    :top -13
                                    :height 10
                                    :display (if is-tracking? "block" "none")}}
                          (dom/div
                           #js {:style
                                #js {:font "10px Helvetica, Arial, sans-serif"
                                     :fontWeight "bold"
                                     :color "#ccc"
                                     :text-align "center"}}
                           (cond->> (get target (:key schema))

                                    (contains? schema :str-format)
                                    (gstring/format (:str-format
                                                     schema)))))))))))

(defn slider [style target schema]
  (dom/div #js {:style (clj->js style)}
           (om/build slider-component {:target target :schema schema})))
