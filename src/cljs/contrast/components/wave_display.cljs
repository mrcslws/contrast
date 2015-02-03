(ns contrast.components.wave-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.common :refer [wavefn wavey->ycoord]]))

(defn painter [config]
  (fn [cnv]
    (let [{:keys [wave harmonics period]} config
          ctx (.getContext cnv "2d")
          width (.-width cnv)
          height (.-height cnv)
          imagedata (.createImageData ctx width height)
          amplitude (/ height 3)
          ;; perf
          harmonics (om/value harmonics)]
      (cnv/clear ctx)
      (dotimes [col width]
        (let [s (reduce (fn [s h]
                          (let [period (/ period h)
                                y (/ (wavefn wave col period) h)]
                            (pixel/write! imagedata col (wavey->ycoord y amplitude height)
                                          0 0 0 32)
                            (+ s y)))
                        0 harmonics)]
          (pixel/write! imagedata col (wavey->ycoord s amplitude height)
                        0 128 128 255)))
      (.putImageData ctx imagedata 0 0))))

(defn wave-display-component [data owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "wave-display")

    om/IRender
    (render [_]
      (cnv/canvas data (:width data) 60 (painter data)))))
