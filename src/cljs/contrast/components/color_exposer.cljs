(ns contrast.components.color-exposer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.components.tracking-area :refer [tracking-area]]))

(defn paint [{:keys [imagedata selected-color]} cnv]
  (let [ctx (.getContext cnv "2d")
        [r g b a] selected-color]
    (cnv/clear ctx)
    (when imagedata
      (dotimes [x (.-width imagedata)]
        (dotimes [y (.-height imagedata)]
          (when (pixel/matches? imagedata x y r g b a)
            (cnv/fill-rect ctx x y 1 1 "blue")))))))

(defn color-exposer-component [config owner]
  (reify

    om/IRenderState
    (render-state [_ {:keys [content]}]
      (let [imagedata (:imagedata config)
            [w h] (if imagedata
                    [(.-width imagedata) (.-height imagedata)]
                    [nil nil])]
        (dom/div #js {:style #js {:display "inline-block"
                                  :verticalAlign "top"
                                  :position "relative"}}
                 (dom/div #js {:style #js {:position "absolute"
                                           :width "100%"
                                           :height "100%"
                                           :zIndex 1}}
                          (om/build cnv/canvas config {:opts {:fpaint paint
                                                              :width w
                                                              :height h}}))
                 (apply dom/div #js {:style #js {:position "relative"
                                                 :zIndex 0}}
                        content))))))

(defn color-exposer [config imagedata & content]
  ;; TODO this was failing to re-render when I built a {:color color} value :(
  (om/build color-exposer-component (assoc config
                                      :imagedata imagedata) ;; {:color color}
            {:state {:content content}}))
