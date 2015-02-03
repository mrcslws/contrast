(ns contrast.components.row-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]))

(defn painter [data owner]
  (fn [cnv]
    (let [{target :target schema :schema stalkee :imagedata} data
          ctx (.getContext cnv "2d")
          sr (get target (:key schema))]
      (cnv/clear ctx)
      (when (and sr stalkee)
        (let [width (.-width cnv)
              height (.-height cnv)
              imagedata (.createImageData ctx width height)
              sw (.-width stalkee)
              sh (.-height stalkee)
              sd (.-data stalkee)
              sr (get target (:key schema))]
          ;; TODO handle the case where this assert fails
          (assert (= sw width))

          (assert (< sr sh))

          (dotimes [row height]
            (dotimes [col width]
              (pixel/copy! imagedata col row
                           stalkee col sr)))
          (.putImageData ctx imagedata 0 0))))))

;; TODO display a red border when the illusion's tracking area is tracking.
;; This will involve channels.
(defn row-display-component [config owner {:keys [subscriber]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "row-display")

    om/IRender
    (render [_]
      (dom/div {:onMouseMove #(om/set-state! owner :force-update 1)}
               (cnv/canvas config 600 40 (painter config owner) subscriber)))))

(defn row-display
  ([target schema imagedata]
     (row-display target schema imagedata nil))
  ([target schema imagedata subscriber]
     (om/build row-display-component
               {:target target :schema schema
                :imagedata imagedata}
               {:opts {:subscriber subscriber}})))
