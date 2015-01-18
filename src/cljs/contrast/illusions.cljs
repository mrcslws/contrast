(ns contrast.illusions
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.components.slider :refer [slider]]
            [contrast.components.canvas :as cnv])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]
                   [contrast.macros :refer [forloop]]))

;; TODO decomplect vertical `progress->color` from horizontal
(defn two-sides-paint [progress->color]
  (fn [{:keys [transition-radius]} cnv]
    (let [ctx (.getContext cnv "2d")
          width (.-width cnv)
          height (.-height cnv)
          imagedata (.createImageData ctx width height)
          data (.-data imagedata)
          start 0
          end 255
          middle 127
          lx 0
          tx (- (/ width 2) transition-radius)
          tw (* 2 transition-radius)
          rx (+ tx tw)]
      (forloop [(row 0) (< row height) (inc row)]
               (let [lcolor (progress->color middle start (/ row height))
                     rcolor (progress->color middle end (/ row height))]
                 ;; TODO make more declarative
                 (forloop [(col lx) (< col tx) (inc col)]
                          (let [base (-> row (* width) (+ col) (* 4))]
                            (doto data
                              (aset base lcolor)
                              (aset (+ base 1) lcolor)
                              (aset (+ base 2) lcolor)
                              (aset (+ base 3) 255))))
                 (forloop [(col rx) (< col width) (inc col)]
                          (let [base (-> row (* width) (+ col) (* 4))]
                            (doto data
                              (aset base rcolor)
                              (aset (+ base 1) rcolor)
                              (aset (+ base 2) rcolor)
                              (aset (+ base 3) 255))))
                 (forloop [(col tx) (< col rx) (inc col)]
                          (let [base (-> row (* width) (+ col) (* 4))
                                c (progress->color lcolor rcolor (/ (- col tx) tw))]
                            (doto data
                              (aset base c)
                              (aset (+ base 1) c)
                              (aset (+ base 2) c)
                              (aset (+ base 3) 255))))))
      (.putImageData ctx imagedata 0 0)
      imagedata)))

(defn two-sides-component [progress->color]
  (fn [config owner {:keys [subscriber]}]
    (reify
      om/IRender
      (render [_]
        (om/build cnv/canvas config {:opts {:subscriber subscriber
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
