(ns contrast.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close!]]
            [contrast.slider :refer [slider]]
            [contrast.layeredcanvas :refer [layered-canvas]]))

(defonce app-state (atom {:width 600
                          :height 600
                          :transition-radius 50}))

(defn clear [ctx]
  (.save ctx)
  (.setTransform ctx 1 0 0 1 0 0)
  (.clearRect ctx 0 0 (.. ctx -canvas -width) (.. ctx -canvas -height))
  (.restore ctx))

(defn fill-rect [ctx x y width height fill]
  (set! (.-fillStyle ctx) fill)
  (.fillRect ctx x y width height))

(defn grayscale [i]
  (str "rgb(" i "," i "," i ")"))

(defn linear-gradient [ctx x0 y0 x1 y1 & stops]
  (let [g (.createLinearGradient ctx x0 y0 x1 y1)]
    (doseq [[offset color] stops]
      (.addColorStop g offset color))
    g))

(defn offset-from-target [evt]
  {:x (- (.-pageX evt)
      (-> evt .-target .getBoundingClientRect .-left))
   :y (- (.-pageY evt)
      (-> evt .-target .getBoundingClientRect .-top))})

(defn paint-two-halves [_ cnv]
  (let [ctx (.getContext cnv "2d")
        width (.-width cnv)
        height (.-height cnv)
        w (/ width 2)]
    (clear ctx)
    (fill-rect ctx 0 0 w height
               (linear-gradient ctx 0 0 0 height
                                [0 (grayscale 127)]
                                [1 (grayscale 0)]))
    (fill-rect ctx w 0 w height
               (linear-gradient ctx 0 0 0 height
                                [0 (grayscale 127)]
                                [1 (grayscale 255)]))))

(defn paint-transition [{:keys [transition-radius]} cnv]
  (let [ctx (.getContext cnv "2d")
        width (.-width cnv)
        height (.-height cnv)
        tx (- (/ width 2) transition-radius)
        tw (* 2 transition-radius)]
    (clear ctx)
    (doseq [row (range height)
            :let [left (grayscale (js/Math.round (* 128 (- 1 (/ row height)))))
                  right (grayscale (js/Math.round (* 128 (+ 1 (/ row height)))))]]
      (fill-rect ctx tx row tw 1
                 (linear-gradient ctx tx 0 (+ tx tw) 0
                                  [0 left]
                                  [1 right])))))

(defn paint-redline [{:keys [spotlight-row]} cnv]
  (let [ctx (.getContext cnv "2d")
        width (.-width cnv)]
    (clear ctx)
    (when [spotlight-row]
      (fill-rect ctx 0 spotlight-row width 1 "red"))))

(defn illusion [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:pixels-chan (chan)})

    om/IRenderState
    (render-state [_ {:keys [pixels-chan]}]
      (dom/div nil
               (let [common {:width (:width app)
                             :height (:height app)}
                     graphic-opts (assoc common
                                    :layers [{:fpaint paint-two-halves
                                              :fdata #(select-keys % [])}
                                             {:fpaint paint-transition
                                              :fdata #(select-keys % [:transition-radius])}])
                     graphic (om/build layered-canvas app {:init-state {:pixels-chan pixels-chan}
                                                           :opts graphic-opts})
                     probe-opts (assoc common
                                  :layers [graphic
                                           {:fpaint paint-redline
                                            :fdata #(select-keys % [:spotlight-row])
                                            :additional {:onMouseLeave
                                                         #(om/update! app :spotlight-row nil)
                                                         :onMouseMove
                                                         #(om/update!
                                                           app :spotlight-row
                                                           (-> % offset-from-target :y))}}])]
                 (om/build layered-canvas app {:opts probe-opts}))
               (om/build slider app {:opts {:value-key :transition-radius}})))))

(defn main []
  (om/root illusion
           app-state
           {:target (. js/document (getElementById "app"))}))
