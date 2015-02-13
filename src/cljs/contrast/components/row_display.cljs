(ns contrast.components.row-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.common :refer [trace-rets]]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.state :as state]))

(defn idwriter [row-inspect stalkee]
  (fn [imagedata]
    (let [sr (:probed-row row-inspect)]
      (when (and sr stalkee)
        (let [width (.-width imagedata)
              height (.-height imagedata)
              sw (.-width stalkee)
              sh (.-height stalkee)
              sd (.-data stalkee)]
          ;; TODO handle the case where this assert fails
          (assert (= sw width))

          (assert (< sr sh))

          (dotimes [row height]
            (dotimes [col width]
              (pixel/copy! imagedata col row
                           stalkee col sr))))))
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
