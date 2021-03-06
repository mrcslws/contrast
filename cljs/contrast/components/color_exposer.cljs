(ns contrast.components.color-exposer
  (:require [com.mrcslws.om-spec :as spec]
            [contrast.components.canvas :as cnv]
            [contrast.state :as state]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn idwriter [color-inspect stalkee]
  (fn [imagedata]
    (let [{c :selected-color} @color-inspect]
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

(defn color-exposer-component [k owner]
  (reify

    om/IDisplayName
    (display-name [_]
      "color-exposer")

    om/IRenderState
    (render-state [_ {:keys [imagedata]}]
      (let [color-inspect (om/observe owner (state/color-inspect k))]
        (dom/div #js {:style #js {:display "inline-block"
                                  :verticalAlign "top"
                                  :position "relative"}}
                 (dom/div #js {:style #js {:position "absolute"
                                           :width "100%"
                                           :height "100%"
                                           :zIndex 1}}
                          (when (and imagedata (:selected-color color-inspect))
                            (cnv/canvas [color-inspect imagedata] (.-width imagedata)
                                        (.-height imagedata)
                                        (cnv/idwriter->painter (idwriter
                                                                color-inspect
                                                                imagedata)))
                            ;; (cnv/fading-canvas [color-inspect imagedata]
                            ;;                  (.-width imagedata)
                            ;;                  (.-height imagedata)
                            ;;                  (idwriter color-inspect imagedata)
                            ;;                  64)
                            )

                          )
                 (dom/div #js {:style #js {:position "relative"
                                           :zIndex 0}}
                          (spec/children-in-div owner)))))))
