(ns contrast.components.row-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.app-state :as state]
            [contrast.components.canvas :as cnv]
            [contrast.illusions :as illusions]
            [contrast.dom :as domh])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]
                   [contrast.macros :refer [forloop]]))

(defn paint [owner]
  (fn [{:keys [target schema]} cnv]
    (let [ctx (.getContext cnv "2d")
          sr (get target (:key schema))
          stalkee (om/get-state owner :imagedata)]
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

(defn row-display-component [config owner {:keys [subscriber updates]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [updates-out (chan)]
        (tap updates updates-out)
        (go-loop []
          (om/set-state! owner :imagedata (<! updates-out))
          (recur))))

    om/IRender
    (render [_]
      (dom/div {:onMouseMove #(om/set-state! owner :force-update 1)}
               (om/build cnv/canvas config
                         {:opts {:width 600 :height 50
                                 :fpaint (paint owner)
                                 :subscriber subscriber}})))))

(defn row-display [style target schema {:keys [subscriber updates]}]
  (dom/div #js {:style (clj->js style)}
           (om/build row-display-component
                     {:target target :schema schema}
                     {:opts {:subscriber subscriber
                             :updates updates}})))
