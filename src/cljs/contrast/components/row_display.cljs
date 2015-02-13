(ns contrast.components.row-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.common :refer [trace-rets]]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]))

(defn idwriter [data owner]
  (fn [imagedata]
    (let [{row-inspect :row-inspect graphic :graphic stalkee :imagedata} data
          sr (:probed-row row-inspect)]
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
(defn row-display-component [data owner {:keys [subscriber]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "row-display")

    om/IRender
    (render [_]
      (dom/div {:onMouseMove #(om/set-state! owner :force-update 1)}
               (cnv/canvas data (-> data :graphic :width) 40
                           (cnv/idwriter->painter (trace-rets (idwriter data owner)
                                                              subscriber)))))))

(defn row-display
  ([data imagedata]
     (row-display data imagedata nil))
  ([data imagedata subscriber]
     (om/build row-display-component
               (assoc data
                 :imagedata imagedata)
               {:opts {:subscriber subscriber}})))
