(ns contrast.illusions
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel])
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
