(ns contrast.components.row-display
  (:require [contrast.common :refer [trace-rets]]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.state :as state]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn idwriter [row-inspect stalkee]
  (fn [imagedata]
    (let [sr (:probed-row row-inspect)]
      (when (and sr stalkee)
        (let [width (.-width imagedata)
              height (.-height imagedata)
              d (.-data imagedata)
              sw (.-width stalkee)
              sh (.-height stalkee)
              sd (.-data stalkee)]
          ;; (assert (= sw width))
          ;; (assert (< sr sh))
          (dotimes [row height]
            (dotimes [col width]
              (let [base (pixel/base width col row)
                    sbase (pixel/base width col sr)]
                (doto d
                  (aset base (aget sd sbase))
                  (aset (+ base 1) (aget sd (+ sbase 1)))
                  (aset (+ base 2) (aget sd (+ sbase 2)))
                  (aset (+ base 3) (aget sd (+ sbase 3))))))))))
    imagedata))

;; TODO display a red border when the illusion's tracking area is tracking.
;; This will involve channels.
(defn row-display-component [k owner {:keys [subscriber]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "row-display")

    om/IRenderState
    (render-state [_ {:keys [stalkee]}]
      (let [row-inspect (om/observe owner (state/row-inspect k))
            figure (om/observe owner (state/figure k))]
        (dom/div {:onMouseMove #(om/set-state! owner :force-update 1)}
                 (cnv/canvas [row-inspect figure stalkee] (:width figure) 40
                             (cnv/idwriter->painter (trace-rets (idwriter row-inspect stalkee)
                                                                subscriber))))))))

(defn row-display
  ([k stalkee]
     (row-display k stalkee nil))
  ([k stalkee subscriber]
     (om/build row-display-component k {:state {:stalkee stalkee}
                                        :opts {:subscriber subscriber}})))
