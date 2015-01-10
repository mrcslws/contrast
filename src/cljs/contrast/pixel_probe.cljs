(ns contrast.pixel-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.slider :refer [slider]]
            [contrast.canvas :as cnv]
            [contrast.illusions :as illusions]
            [contrast.layeredcanvas :refer [layered-canvas]]
            [contrast.pixel :as pixel])
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

(defn paint2 [ctx imagedata x y w h]
  (cnv/clear ctx)
  ;; Comparing is still about 1.5* as slow as combining layers.
  ;; But combining requires reading every pixel of the foreground,
  ;; and some pixels of the background. Comparing should be faster
  ;; than combining.
  (let [currentpx (pixel/xyth! imagedata x y)]
    (time
     (let [len (pixel/pixel-count imagedata)]
       (loop [i 0]
         (when (< i len)
           (when (pixel/matches? (pixel/nth! imagedata i) currentpx)
             (cnv/fill-rect ctx (rem i w) (quot i w) 1 1 "blue"))
           (recur (inc i)))))))
  (println "Finished comparing."))

(defn paint [data owner requests]
  (let [y (-> data :pixel-probe :row)
        x (-> data :pixel-probe :col)
        w (:width data)
        h (:height data)
        ctx (.getContext (om/get-node owner "canvas") "2d")]
    (when (and x y)
      (let [response (chan)]
        (put! requests [0 0 w h response])
        (go
          (paint2 ctx (<! response) x y w h))))))

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
                                          :col (-> % offset-from-target :x))))}))))
