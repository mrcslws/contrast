(ns contrast.components.color-exposer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.components.tracking-area :refer [tracking-area]]))

(defn idwriter [data stalkee]
  (fn [imagedata]
    (let [{c :selected-color} @data]
      (when (and stalkee c)
        (let [[r g b a] c
              w (.-width stalkee)
              h (.-height stalkee)
              d (.-data imagedata)
              sd (.-data stalkee)]
          (dotimes [x w]
            (dotimes [y h]
              (let [base (-> y (* w) (+ x) (* 4))]
                (when (and (identical? (aget sd base) r)
                           (identical? (aget sd (+ base 1)) g)
                           (identical? (aget sd (+ base 2)) b)
                           (identical? (aget sd (+ base 3)) a))
                  (doto d
                    (aset (+ base 2) 255)
                    (aset (+ base 3) 255)))))))))
    imagedata))

(defn color-exposer-component [config owner]
  (reify

    om/IDisplayName
    (display-name [_]
      "color-exposer")

    om/IRenderState
    (render-state [_ {:keys [content]}]
      (let [{:keys [imagedata color-inspect]} config]
        (dom/div #js {:style #js {:display "inline-block"
                                  :verticalAlign "top"
                                  :position "relative"}}
                 (dom/div #js {:style #js {:position "absolute"
                                           :width "100%"
                                           :height "100%"
                                           :zIndex 1}}
                          (when (and imagedata (:selected-color color-inspect))
                            ;; (cnv/canvas config (.-width imagedata)
                            ;;                    (.-height imagedata)
                            ;;                    (cnv/idwriter->painter (idwriter
                            ;;                                            color-inspect
                            ;;                                            imagedata)))
                            (cnv/fading-canvas config
                                               (.-width imagedata)
                                               (.-height imagedata)
                                               (idwriter color-inspect imagedata)
                                               64)
                            ))
                 (apply dom/div #js {:style #js {:position "relative"
                                                 :zIndex 0}}
                        content))))))

(defn color-exposer [config imagedata & content]
  ;; TODO this was failing to re-render when I built a {:color color} value :(
  (om/build color-exposer-component (assoc config
                                      :imagedata imagedata) ;; {:color color}
            {:state {:content content}}))
