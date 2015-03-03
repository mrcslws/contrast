(ns contrast.components.wave-picker
  (:require [contrast.common :refer [wavefn wavey->ycoord]]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn idwriter [wave shift vlinefreq]
  (fn [imagedata]
    (let [width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)
          period (/ width 2)
          amplitude (/ (dec height) 2)
          cshift (* shift period)
          wfn (partial (get-method wavefn wave) wave period)]
      (dotimes [col width]
        (let [row (wavey->ycoord (wfn (+ cshift col))
                                 amplitude height)
              base (pixel/base width col row)]
          (doto d
            (aset (+ base 3) 255))))
      (when (pos? vlinefreq)
        (loop [i 0]
          (let [col (js/Math.round(+ cshift (* i (/ period vlinefreq))))]
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
