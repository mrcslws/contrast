(ns contrast.pixel-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.slider :refer [slider]]
            [contrast.canvas :as cnv]
            [contrast.illusions :as illusions]
            [contrast.layeredcanvas :refer [layered-canvas]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn offset-from-target [evt]
  {:x (- (.-pageX evt)
      (-> evt .-target .getBoundingClientRect .-left))
   :y (- (.-pageY evt)
      (-> evt .-target .getBoundingClientRect .-top))})

;; (defn paint-redline [data owner]
;;   (let [row (-> data :pixel-probe :row)
;;         cnv (om/get-node owner "canvas")
;;         ctx (.getContext cnv "2d")]
;;     (cnv/clear ctx)
;;     (when row
;;       (cnv/fill-rect ctx 0 row (.-width cnv) 1 "red"))))

(defn paint [data owner requests]
  (let [row (-> data :pixel-probe :row)
        col (-> data :pixel-probe :col)
        w (:width data)
        h (:height data)
        cnv (om/get-node owner "canvas")
        ctx (.getContext cnv "2d")]
    (when (and row col)
      (let [response (chan)]
        (put! requests [0 0 w h response])
        (go
          (let [pixels (<! response)]
            (cnv/clear ctx)

            ;; (time (let [pixels (time (vec (partition 4 pixelbits)))
            ;;             currenti (+ (* row w) col)
            ;;             currentpx (vec (nth pixels (+ (* row w) col)))]
            ;;         ;; TODO is partition bad for perf?
            ;;         (doseq [i (range (count pixels))
            ;;                 :let [thispx (vec (nth pixels i))]
            ;;                 :when (= currentpx thispx)]
            ;;           (cnv/fill-rect ctx (rem i w) (quot i w) 1 1 "blue"))))

            ;; So slow! A vec of pixels, or a vec of seqs of pixels (i.e. rows).
            ;; Too slow. Is there a way to make my own partition fn / macro?
            ;; (time (let [rows (vec (partition w (partition 4 pixels)))]))
            ;; (println "Trying partition")

            (time (let [currenti (* 4 (+ (* row w) col))
                        currentpx (subvec pixels currenti (+ currenti 4)) ]
                    (doseq [i (range 0 (count pixels) 4)
                            :let [actual-i (quot i 4)]
                            :when (= currentpx (subvec pixels i (+ i 4)))]
                      (cnv/fill-rect ctx (rem actual-i w) (quot actual-i w) 1 1 "blue"))))
            (println "Finished comparing")))))))

(defn pixel-probe [data owner {:keys [pixel-requests]}]
  (reify

    om/IDidMount
    (did-mount [_]
      (paint data owner pixel-requests))

    om/IDidUpdate
    (did-update [_ _ _]
      (paint data owner pixel-requests))

    om/IRender
    (render [_]
      (dom/canvas #js {:width (:width data) :height (:height data)
                       ;; TODO better :zIndex
                       :style #js {:position "absolute"
                                   :left 0 :top 0 :zIndex 10}
                       :ref "canvas"
                       :onMouseLeave #(om/transact!
                                      (:pixel-probe data)
                                      (fn [c]
                                        (assoc c
                                          :row nil
                                          :col nil)))
                       :onMouseMove #(om/transact!
                                      (:pixel-probe data)
                                      (fn [c]
                                        (assoc c
                                          :row (-> % offset-from-target :y)
                                          :col (-> % offset-from-target :x))))}
                  ))))
