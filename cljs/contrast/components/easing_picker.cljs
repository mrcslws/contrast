(ns contrast.components.easing-picker
  (:require [cljs.core.async :refer [<! put! chan alts! take!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [progress rgb->hexcode trace-rets]]
            [contrast.components.canvas :as cnv :refer [canvas-component]]
            [contrast.components.slider :refer [slider]]
            [contrast.easing :as easing]
            [contrast.pixel :as pixel]
            [goog.events :as events]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(defn prec [x]
  (.toFixed x 3))

(defn idwriter2 [easing]
  (let [easing-function (easing/cubic-bezier-easing (:x1 easing)
                                                    (:y1 easing)
                                                    (:x2 easing)
                                                    (:y2 easing))]
    (fn [imgdata]
      (let [width (.-width imgdata)
            height (.-height imgdata)
            d (.-data imgdata)]
        (easing/foreach-xy easing-function width
                           (fn [x y]
                             (let [col (js/Math.round (* x width))
                                   row (js/Math.round (* (- 1 y) height))
                                   base (pixel/base width col row)]
                               (doto d
                                 (aset (+ base 2) 255)
                                 (aset (+ base 3) 255)))))
        imgdata))))

(defn easing-picker-component [easing owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               ;; (cnv/canvas easing 200 200
               ;;             (painter1 easing))
               ;; (dom/br nil)
               (dom/div #js {:style #js {:display "inline-block"
                                         :verticalAlign "top"}}
                        "cubic-bezier("
                        (prec (:x1 easing))
                        ","
                        (prec (:y1 easing))
                        ","
                        (prec (:x2 easing))
                        ","
                        (prec (:y2 easing))
                        ")"
                        (dom/div nil
                                 (slider {:width 180}
                                         easing
                                         {:key :x1
                                          :min 0
                                          :max 1
                                          :str-format "%.3f"}))
                        (dom/div nil
                                 (slider {:width 180}
                                         easing
                                         {:key :y1
                                          :min 0
                                          :max 1
                                          :str-format "%.3f"}))
                        (dom/div nil
                                 (slider {:width 180}
                                         easing
                                         {:key :x2
                                          :min 0
                                          :max 1
                                          :str-format "%.3f"}))
                        (dom/div nil
                                 (slider {:width 180}
                                         easing
                                         {:key :y2
                                          :min 0
                                          :max 1
                                          :str-format "%.3f"})))
               (cnv/canvas easing 200 200
                           (cnv/idwriter->painter (idwriter2 easing)))))))
