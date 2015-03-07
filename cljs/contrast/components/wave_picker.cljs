(ns contrast.components.wave-picker
  (:require [contrast.common :refer [wavefn]]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.progress :as progress]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn idwriter [wave shift vlinefreq]
  (fn write-imagedata! [imagedata]
    (let [width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)
          period (/ width 2)
          cshift (* shift period)

          yp->plot-yp (progress/y->ploty :up)
          wfn (partial (get-method wavefn wave) wave period)
          col->row (fn [col]
                     (-> col
                         (+ cshift)
                         wfn
                         (progress/n->p -1 1)
                         yp->plot-yp
                         (progress/p->int 0 (dec height))))]
      (dotimes [col width]
        (doto d
          (aset (+ (pixel/base width col
                               (col->row col))
                   3)
                255)))
      (when (pos? vlinefreq)
        (loop [i 0]
          (let [col (-> i
                        (* (/ period vlinefreq))
                        (+ cshift)
                        js/Math.round)]
            (when (< col width)
              (when (>= col 0)
                (dotimes [row height]
                  (let [base (pixel/base width col row)]
                    (doto d
                      (aset (+ base 3) 255)))))
              (recur (inc i)))))))
    imagedata))

(defn wave-picker-component [{:keys [target schema] :as data} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "wave-picker")

    om/IInitState
    (init-state [_]
      {:locked (get target (:key schema))})

    om/IRender
    (render [_]
      (apply dom/div nil
             (map (fn [[k shift vlinefreq]]
                    (dom/div #js {:style #js {:display "inline-block"
                                              :padding 1
                                              :border (cond
                                                       (= (om/get-state owner
                                                                        :locked)
                                                          k)
                                                       "1px solid red"

                                                       (= (get target
                                                               (:key schema))
                                                          k)
                                                       "1px dashed red"

                                                       :default
                                                       "1px dashed black")
                                              :marginRight 14}
                                  :onMouseMove #(om/update! target (:key schema) k)
                                  :onClick #(om/set-state! owner :locked k)
                                  :onMouseOut #(om/update! target (:key schema)
                                                           (om/get-state owner :locked))}
                             (cnv/canvas data 75 20
                                         (cnv/idwriter->painter
                                          (idwriter k shift vlinefreq)))))
                  [[:sine 0 0]
                   [:sawtooth 0.5 1]
                   [:triangle -0.25 0]
                   [:square 0.25 2]])))))
