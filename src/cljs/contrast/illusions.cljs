(ns contrast.illusions
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.slider :refer [slider]]
            [contrast.canvas :as cnv]
            [contrast.layeredcanvas :refer [layered-canvas]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn grayscale [i]
  (str "rgb(" i "," i "," i ")"))

(defn paint-two-halves [_ cnv]
  (let [ctx (.getContext cnv "2d")
        width (.-width cnv)
        height (.-height cnv)
        w (/ width 2)]
    (cnv/clear ctx)
    (cnv/fill-rect ctx 0 0 w height
                   (cnv/linear-gradient ctx 0 0 0 height
                                        [0 (grayscale 127)]
                                        [1 (grayscale 0)]))
    (cnv/fill-rect ctx w 0 w height
                   (cnv/linear-gradient ctx 0 0 0 height
                                        [0 (grayscale 127)]
                                        [1 (grayscale 255)]))))

(defn paint-transition [{:keys [transition-radius]} cnv]
  (let [ctx (.getContext cnv "2d")
        width (.-width cnv)
        height (.-height cnv)
        tx (- (/ width 2) transition-radius)
        tw (* 2 transition-radius)]
    (cnv/clear ctx)
    (doseq [row (range height)
            :let [left (grayscale (js/Math.round
                                   (* 128 (- 1 (/ row height)))))
                  right (grayscale (js/Math.round
                                    (* 128 (+ 1 (/ row height)))))]]
      (cnv/fill-rect ctx tx row tw 1
                     (cnv/linear-gradient ctx tx 0 (+ tx tw) 0
                                          [0 left]
                                          [1 right])))))

(defn single-linear-gradient [config owner {:keys [pixel-requests]}]
  (reify
    om/IRender
    (render [_]
      (om/build layered-canvas (select-keys config [:transition-radius])
                {:opts {:pixel-requests pixel-requests
                        :width (:width config)
                        :height (:height config)
                        :layers [{:fpaint paint-two-halves
                                  :fdata #(select-keys % [])}
                                 {:fpaint paint-transition
                                  :fdata #(select-keys % [:transition-radius])}
                                 ]}}))))
