(ns contrast.components.wave-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.common :refer [wavefn wavey->ycoord]]))

(defn idwriter [config]
  (fn [imagedata]
    (let [{:keys [wave harmonics period]} config
          width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)
          amplitude (/ height 3)
          ;; perf
          harmonics (om/value harmonics)]
      (dotimes [col width]
        (let [s (reduce (fn [s h]
                          (let [period (/ period h)
                                y (/ (wavefn wave col period) h)
                                row (wavey->ycoord y amplitude height)
                                base (pixel/base width col row)]
                            (doto d
                              (aset (+ base 3) 32))
                            (+ s y)))
                        0 harmonics)]

          (let [row (wavey->ycoord s amplitude height)
                base (pixel/base width col row)]
            (doto d
              (aset (+ base 1) 128)
              (aset (+ base 2) 128)
              (aset (+ base 3) 255))))))
    imagedata))

(defn wave-display-component [data owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "wave-display")

    om/IRender
    (render [_]
      (cnv/canvas data (:width data) 60
                  (cnv/idwriter->painter (idwriter data))))))
