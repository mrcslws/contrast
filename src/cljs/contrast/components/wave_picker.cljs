(ns contrast.components.wave-picker
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.common :refer [wavefn wavey->ycoord]]))

(defn paint [wave shift vlinefreq]
  (fn [_ cnv]
    (let [ctx (.getContext cnv "2d")
          width (.-width cnv)
          height (.-height cnv)
          imagedata (.createImageData ctx width height)
          period (/ width 2)
          amplitude (/ (dec height) 2)
          cshift (* shift period)]
      (cnv/clear ctx)
      (dotimes [col width]
        (pixel/write! imagedata col (wavey->ycoord (wavefn wave (+ cshift col)
                                                           period)
                                                   amplitude height)
                      0 0 0 255))
      (when (pos? vlinefreq)
        (loop [i 0]
          (let [col (js/Math.round(+ cshift (* i (/ period vlinefreq))))]
            (when (< col width)
              (when (>= col 0)
                (dotimes [row height]
                  (pixel/write! imagedata col row 0 0 0 255)))
              (recur (inc i))))))
      (.putImageData ctx imagedata 0 0))))

(defn wave-picker-component [{:keys [target schema] :as data} owner]
  (reify
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
                                              :marginRight 16}
                                  :onMouseMove #(om/update! target (:key schema) k)
                                  :onClick #(om/set-state! owner :locked k)
                                  :onMouseOut #(om/update! target (:key schema)
                                                           (om/get-state owner :locked))}
                             (om/build cnv/canvas data
                                       {:opts {:width 75
                                               :height 20
                                               :fpaint (paint k shift vlinefreq)}})))
                  [[:sine 0 0]
                   [:sawtooth 0.5 1]
                   [:triangle -0.25 0]
                   [:square 0.25 2]])))))
