(ns contrast.components.wave-display
  (:require [contrast.common :refer [wavefn harmonic-adder]]
            [contrast.components.canvas :as cnv]
            [contrast.hotpath :as hotpath]
            [contrast.pixel :as pixel]
            [contrast.progress :as progress]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn zoom [progress factor]
  (-> progress
      (- 0.5)
      (* factor)
      (+ 0.5)))

(defn idwriter [config]
  (fn write-imagedata! [imagedata]
    (let [width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)

          orient (progress/orient :bottom)
          y->row #(-> %
                      (progress/n->p -1 1)
                      orient
                      (zoom 0.5)
                      (progress/p->int 0 (dec height)))

          adder (harmonic-adder config)]
      (dotimes [col width]
        (let [s (adder col
                       (fn [y]
                         (let [base (pixel/base width col (y->row y))]
                           (doto d
                             (aset (+ base 3) 32)))))
              base (pixel/base width col (y->row s))]
          (doto d
            (aset (+ base 1) 128)
            (aset (+ base 2) 128)
            (aset (+ base 3) 255)))))
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
