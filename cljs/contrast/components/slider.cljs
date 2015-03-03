(ns contrast.components.slider
  (:require [cljs.core.async :refer [chan timeout <!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [wide-background-image background-image]]
            [contrast.components.tracking-area :refer [tracking-area-component]]
            [contrast.dom :as domh]
            [goog.string :as gstring]
            [goog.string.format]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def wknob 13)
(def wmarker 8)
(def wlabel 100)
(def bounce-duration 500)
(def b1height 15)
(def b2height 4)

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

(defn knob-yoffset [bounce-start]
  (let [p (-> (js/Date.now)
              (- bounce-start)
              (/ bounce-duration)
              (min 1))
        b2 (> p 0.5)]
    (* (if b2
         b2height
         b1height)
       (- 1 (-> p
                (cond-> b2
                        (- 0.5))
                (* 4)
                (- 1)
                (js/Math.pow 2))))))

(defn slider-left [value schema]
  (-> value
      (/ (- (:max schema) (:min schema)))
      (* 100)
      js/Math.round
      (str "%")))

(defn slider-unchanging-background [_ owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:style #js {:position "absolute"
                                      :zIndex 0
                                      :height 4
                                      :width "100%"}}
             (wide-background-image "images/SliderLeft.png" 8
                                    "images/SliderCenter.png"
                                    "images/SliderRight.png" 8
                                    4)))))

(defn slider-component-internal [{:keys [target schema]} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "slider-internal")

    om/IInitState
    (init-state [_]
      {:is-tracking? false
       :bounce-start 0})

    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :locked-value (get target (:key schema)))

      (go-loop []
        (let [[content-x _] (<! (om/get-state owner :moves))]
          (om/set-state! owner :is-tracking? true)
          (om/update! target (:key schema) (slider-value content-x schema owner))
          (recur)))

      (go-loop []
        (let [_ (<! (om/get-state owner :exits))]
          (om/set-state! owner :is-tracking? false)
          (om/update! target (:key schema) (om/get-state owner :locked-value))
          (recur)))

      (go-loop []
        (let [_ (<! (om/get-state owner :clicks))]
          (om/set-state! owner :locked-value (get @target (:key schema)))
          (om/set-state! owner :bounce-start (js/Date.now))
          (recur))))

    om/IDidUpdate
    (did-update [_ _ _]
      (when (<= (- (js/Date.now) (om/get-state owner :bounce-start))
                bounce-duration)
        (om/refresh! owner)))

    om/IRenderState
    (render-state [_ {:keys [is-tracking? locked-value bounce-start]}]
      (dom/div #js {:ref "slider"
                    :style #js {;; Make room for the bounce.
                                ;; It's a bit gross that the slider is, by
                                ;; default, unaligned with text next to it, but
                                ;; it's good to force the consumer to make room
                                ;; for the animation (or choose not to).
                                :marginTop 26
                                :marginBottom 4
                                :height 4}}
               (om/build slider-unchanging-background nil)
               (dom/div #js {:style
                             #js {:position "absolute"
                                  :zIndex 1
                                  :left (slider-left locked-value schema)}}
                        (dom/div #js {:style #js {:position "absolute"
                                                  :left (- (quot wmarker 2))}}
                                 (background-image "images/SliderMarker.png"
                                                   wmarker 4)))
               (dom/div #js {:style
                             #js {:position "absolute"
                                  :zIndex 2
                                  ;; I apologize for the magic number.
                                  :top (- -4
                                          (knob-yoffset bounce-start))
                                  :left (slider-left (get target
                                                          (:key schema))
                                                     schema)}}
                        (dom/div #js {:style #js {:position "absolute"
                                                  :left (- (quot wknob 2))}}
                                 (background-image "images/SliderKnob.png"
                                                   wknob 13))
                        (dom/div
                         #js {:style
                              #js {:position "relative"

                                   :width wlabel
                                   :left (- (- (quot wlabel 2)
                                               (quot wknob 2)))

                                   :top -13
                                   :height 10
                                   :display (if is-tracking?
                                              "block" "none")}}
                         (dom/div
                          #js {:style
                               #js {:font "10px Helvetica, Arial, sans-serif"
                                    :fontWeight "bold"
                                    :color "#ccc"
                                    :textAlign "center"}}
                          (cond->> (get target (:key schema))

                                   (contains? schema :str-format)
                                   (gstring/format (:str-format
                                                    schema))))))))))

;; TODO:
;; - Touch compatibility
;; - Keyboard compatibility
;; - Text styling extension point?
;; - :pre verification of state
(defn slider-component [v owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "slider")

    om/IInitState
    (init-state [_]
      {:moves (chan)
       :clicks (chan)
       :exits (chan)})

    om/IRenderState
    (render-state [_ {:keys [moves clicks exits]}]
      (spec/render
       {:f tracking-area-component
        :m {:state {:moves moves
                    :clicks clicks
                    :exits exits
                    :underlap-x 10}}
        :children [{:f slider-component-internal
                    :props v
                    :m {:state {:moves moves
                                :clicks clicks
                                :exits exits}}}]}))))

(defn slider [style target schema]
  (dom/div #js {:style (clj->js (merge {:display "inline-block"}
                                       style))}
           (om/build slider-component {:target target :schema schema})))
