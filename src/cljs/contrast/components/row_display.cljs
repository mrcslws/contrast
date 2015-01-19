(ns contrast.components.row-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]))

(defn paint [owner]
  (fn [{target :target schema :schema stalkee :imagedata}  cnv]
    (let [ctx (.getContext cnv "2d")
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

(defn row-display-component [config owner {:keys [subscriber]}]
  (reify
    om/IRender
    (render [_]
      (dom/div {:onMouseMove #(om/set-state! owner :force-update 1)}
               (om/build cnv/canvas config
                         {:opts {:width 600 :height 50
                                 :fpaint (paint owner)
                                 :subscriber subscriber}})))))

(defn row-display [target schema imagedata {:keys [subscriber]}]
  (om/build row-display-component
            {:target target :schema schema
             :imagedata imagedata}
            {:opts {:subscriber subscriber}}))
