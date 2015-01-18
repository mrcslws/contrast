(ns contrast.color-exposer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.app-state :as state]
            [contrast.canvas :as cnv]
            [contrast.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.pixel-probe :refer [pixel-probe]]
            [contrast.pixel :as pixel]
            [contrast.row-probe :refer [row-probe]]
            [contrast.layeredcanvas :refer [layered-canvas]]
            [contrast.dom :as domh]
            [contrast.common :refer [tracking-area]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]
                   [contrast.macros :refer [forloop]]))

(defn paint [data owner]
  (fn [data cnv]
    (let [ctx (.getContext cnv "2d")
          spx (apply pixel/immutable-pixel (:selected-color data))
          imagedata (om/get-state owner :imagedata)]
      (cnv/clear ctx)
      (when imagedata
        (let [w (.-width imagedata)]
          (let [len (pixel/pixel-count imagedata)]
            (loop [i 0
                   px (pixel/nth! imagedata i)]
              (when (< i len)
                (when (pixel/matches? px spx)
                  (cnv/fill-rect ctx (rem i w) (quot i w) 1 1 "blue"))
                (recur (inc i) (pixel/pan! px 1))))))))))

(defn color-exposer-component [config owner {:keys [updates]}]
  (reify
    om/IWillMount
    (will-mount [_]
      ;; TODO I've copy-pasted this function in multiple places
      (let [updates-out (chan)]
        (tap updates updates-out)
        (go-loop []
          (om/set-state! owner :imagedata (<! updates-out))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [imagedata content]}]
      (let [[w h] (if imagedata
                    [(.-width imagedata) (.-height imagedata)]
                    [nil nil])]
        (dom/div #js {:style #js {:display "inline-block"
                                  :verticalAlign "top"
                                  :position "relative"}}
                 (dom/div #js {:style #js {:position "absolute"
                                           :width "100%"
                                           :height "100%"
                                           :zIndex 1}}
                          (om/build cnv/canvas config {:opts {:fpaint (paint config owner)
                                                              :width w
                                                              :height h}}))
                 (apply dom/div #js {:style #js {:position "relative"
                                                 :zIndex 0}}
                        content))))))


(defn color-exposer [style config updates & content]
  ;; TODO this was failing to re-render when I built a {:color color} value :(
  (dom/div #js {:style (clj->js style)}
           (om/build color-exposer-component config ;; {:color color}
                     {:state {:content content}
                      :opts {:updates updates}})))
