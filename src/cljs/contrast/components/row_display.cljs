(ns contrast.components.row-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv])
  (:require-macros [contrast.macros :refer [forloop]]))

(defn paint [owner]
  (fn [{target :target schema :schema stalkee :imagedata}  cnv]
    (let [ctx (.getContext cnv "2d")
          sr (get target (:key schema))]
      (cnv/clear ctx)
      (when (and sr stalkee)
        (let [width (.-width cnv)
              height (.-height cnv)
              imagedata (.createImageData ctx width height)
              data (.-data imagedata)
              sw (.-width stalkee)
              sh (.-height stalkee)
              sd (.-data stalkee)
              sr (get target (:key schema))]
          ;; TODO handle the case where this assert fails
          (assert (= sw width))

          (assert (< sr sh))

          (forloop [(row 0) (< row height) (inc row)]
                   (forloop [(col 0) (< col width) (inc col)]
                            (let [sbase (-> sr (* sw) (+ col) (* 4))
                                  base (-> row (* width) (+ col) (* 4))]
                              (doto data
                                (aset base
                                      (aget sd sbase))
                                (aset (+ base 1)
                                      (aget sd (+ sbase 1)))
                                (aset (+ base 2)
                                      (aget sd (+ sbase 2)))
                                (aset (+ base 3)
                                      (aget sd (+ sbase 3)))))))
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

(defn row-display [style target schema imagedata {:keys [subscriber]}]
  (dom/div #js {:style (clj->js style)}
           (om/build row-display-component
                     {:target target :schema schema
                      :imagedata imagedata}
                     {:opts {:subscriber subscriber}})))
