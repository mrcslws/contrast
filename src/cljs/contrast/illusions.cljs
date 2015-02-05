(ns contrast.illusions
  (:require [om.core :as om :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.common :refer [wavefn spectrum-dictionary]])
  (:require-macros [contrast.macros :refer [dorange]]))

;; TODO decomplect vertical `progress->color` from horizontal
(defn two-sides-painter [config interval-lookup]
  (fn [cnv]
    (let [{:keys [transition-radius]} config
          ctx (.getContext cnv "2d")
          width (.-width cnv)
          height (.-height cnv)
          imagedata (.createImageData ctx width height)
          start 0
          end 255
          middle 127
          leftx 0
          transx (- (/ width 2) transition-radius)
          transw (* 2 transition-radius)
          rightx (+ transx transw)
          lprogress->color (interval-lookup middle start)
          rprogress->color (interval-lookup middle end)]

      (dotimes [row height]
        (let [lcolor (lprogress->color (/ row height))
              rcolor (rprogress->color (/ row height))
              progress->color (interval-lookup lcolor rcolor)]
          (dorange [col leftx transx]
                   (pixel/write! imagedata col row lcolor lcolor lcolor 255))
          (dorange [col rightx width]
                   (pixel/write! imagedata col row rcolor rcolor rcolor 255))
          (dorange [col transx rightx]
                   (let [c (progress->color (/ (- col transx) transw))]
                     (pixel/write! imagedata col row c c c 255)))))
      (.putImageData ctx imagedata 0 0)
      imagedata)))

(defn number-line-maker [ptransform]
  (fn [start end]
    (let [l (- end start)]
      (fn [p]
        (-> p
            ptransform
            (* l)
            (+ start)
            js/Math.round)))))

(defn linear->sinusoidal [progress]
  (-> progress
      (* js/Math.PI)
      (- (/ js/Math.PI 2))
      js/Math.sin
      inc
      (/ 2)))

(defn single-sinusoidal-gradient-painter [config]
  (two-sides-painter config (number-line-maker linear->sinusoidal)))

(defn accelerating-sine-wave [{:keys [contrast]}]
  (fn [x]
    (-> x
        (/ (* 22 js/Math.PI))
        (js/Math.pow 3)
        js/Math.sin)))

(defn sweep-grating-painter [config]
  (cnv/gradient-vertical-stripe-painter
   (comp
    (spectrum-dictionary (:spectrum config))
    (fn [_] 0))
   (comp
    (spectrum-dictionary (:spectrum config))
    (accelerating-sine-wave config))))

(defn sums-of-harmonics [{:keys [harmonics period wave]}]
  (let [harray (clj->js harmonics)
        c (count harmonics)
        wfn (partial (get-method wavefn wave) wave)]
    (fn [x]
      (loop [s 0
             i 0]
        (if-not (< i c)
          s
          (let [h (aget harray i)]
              (recur (-> x
                      (* h)
                      (wfn period)
                      (* (/ 1 h))
                      (+ s))
                  (inc i))))))))

(defn harmonic-grating-painter [config]
  (cnv/solid-vertical-stripe-painter (comp
                                      (spectrum-dictionary (:spectrum config))
                                      (sums-of-harmonics config))))
