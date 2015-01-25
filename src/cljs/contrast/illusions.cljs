(ns contrast.illusions
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.common :refer [wavefn hexcode->rgb average-color]])
  (:require-macros [contrast.macros :refer [dorange]]))

;; TODO decomplect vertical `progress->color` from horizontal
(defn two-sides-paint [progress->color]
  (fn [{:keys [transition-radius]} cnv]
    (let [ctx (.getContext cnv "2d")
          width (.-width cnv)
          height (.-height cnv)
          imagedata (.createImageData ctx width height)
          start 0
          end 255
          middle 127
          leftx 0
          transx (- (/ width 2) transition-radius)
          transw (* 2 transition-radius)
          rightx (+ transx transw)]

      (dotimes [row height]
        (let [lcolor (progress->color middle start (/ row height))
              rcolor (progress->color middle end (/ row height))]
          (dorange [col leftx transx]
                   (pixel/write! imagedata col row lcolor lcolor lcolor 255))
          (dorange [col rightx width]
                   (pixel/write! imagedata col row rcolor rcolor rcolor 255))
          (dorange [col transx rightx]
                   (let [c (progress->color lcolor rcolor (/ (- col transx)
                                                             transw))]
                     (pixel/write! imagedata col row c c c 255)))))
      (.putImageData ctx imagedata 0 0)
      imagedata)))

(defn two-sides-component [progress->color]
  (fn [config owner {:keys [subscriber]}]
    (reify
      om/IRender
      (render [_]
        (om/build cnv/canvas config
                  {:opts {:subscriber subscriber
                          :width (:width config)
                          :height (:height config)
                          :fpaint (two-sides-paint progress->color)}})))))

(defn linear-gradient [start end progress]
  (-> progress
      (* (- end start))
      (+ start)
      js/Math.round))

(defn linear->sinusoidal [progress]
  (-> progress
      (* js/Math.PI)
      (- (/ js/Math.PI 2))
      js/Math.sin
      inc
      (/ 2)))

(def single-linear-gradient
  (two-sides-component linear-gradient))
(def single-sinusoidal-gradient
  (two-sides-component #(linear-gradient % %2
                                         (linear->sinusoidal %3))))

;; Oscillate between 0 and 255.
;; (127.5 + contrast * sin)
;; 127.5*(1 + sin)
(defn paint-sweep-grating [{:keys [contrast]} cnv]
  (let [ctx (.getContext cnv "2d")
        width (.-width cnv)
        height (.-height cnv)
        imagedata (.createImageData ctx width height)
        tcolor 127]
    (cnv/clear ctx)
    (dotimes [col width]
      (let [bcolor (-> col
                       (/ (* 22 js/Math.PI))
                       (js/Math.pow 3)
                       js/Math.sin
                       (* (- contrast 0.5))
                       (+ 127.5)
                       js/Math.round)]
        (dotimes [row height]
          (let [c (linear-gradient tcolor bcolor (/ row height))]
            (pixel/write! imagedata col row c c c 255)))))
    (.putImageData ctx imagedata 0 0)
    imagedata))

(defn sweep-grating [config owner {:keys [subscriber]}]
  (reify
    om/IRender
    (render [_]
      (om/build cnv/canvas config
                {:opts {:subscriber subscriber
                        :width (:width config)
                        :height (:height config)
                        :fpaint paint-sweep-grating}}))))

(defn paint-harmonic-grating [{:keys [from-color to-color harmonics period
                                      wave]} cnv]
  (let [ctx (.getContext cnv "2d")
        width (.-width cnv)
        height (.-height cnv)
        imagedata (.createImageData ctx width height)
        [[fr fg fb]
         [tr tg tb]] (map hexcode->rgb [from-color to-color])
        [rcenter gcenter bcenter] (average-color [fr fg fb] [tr tg tb])
        [cfoo gfoo bfoo] (map #(- %2 %)
                              [rcenter gcenter bcenter]
                              [fr fg fb])
        ;; cursor lookups will cause delays
        harmonics (om/value harmonics)]
    (cnv/clear ctx)
    (dotimes [col width]
      (let [sum-of-sins (reduce (fn [s h]
                                  (-> col
                                      (* h)
                                      ((partial wavefn wave) period)
                                      (* (/ 1 h))
                                      (+ s)))
                                0 harmonics)
            [r g b] (map (fn [center foo]
                           (-> sum-of-sins
                               (* foo)
                               (+ center)
                               js/Math.round))
                         [rcenter gcenter bcenter]
                         [cfoo gfoo bfoo])]
        (dotimes [row height]
          (pixel/write! imagedata col row r g b 255))))
    (.putImageData ctx imagedata 0 0)
    imagedata))

(defn harmonic-grating [config owner {:keys [subscriber]}]
  (reify
    om/IRender
    (render [_]
      (om/build cnv/canvas config
                {:opts {:subscriber subscriber
                        :width (:width config)
                        :height (:height config)
                        :fpaint paint-harmonic-grating}}))))
