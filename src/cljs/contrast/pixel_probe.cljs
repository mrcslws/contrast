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

(defn paint [data owner]
  (let [ctx (.getContext (om/get-node owner "canvas") "2d")
        y (-> data :pixel-probe :row)
        x (-> data :pixel-probe :col)
        imagedata (om/get-state owner :imagedata)]
    (cnv/clear ctx)
    (when (and x y imagedata)
      (let [w (:width data)
            selected (pixel/xyth! imagedata x y)]
        (let [len (pixel/pixel-count imagedata)]
          (loop [i 0
                 px (pixel/nth! imagedata i)]
            (when (< i len)
              (when (pixel/matches? px selected)
                (cnv/fill-rect ctx (rem i w) (quot i w) 1 1 "blue"))
              (recur (inc i) (pixel/pan! px 1)))))))))

(defn pixel-probe [data owner {:keys [updates]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (go-loop []
        (om/set-state! owner :imagedata (<! updates))))

    om/IDidMount
    (did-mount [_]
      (paint data owner))

    om/IDidUpdate
    (did-update [_ _ _]
      (paint data owner))

    om/IRender
    (render [_]
      (dom/canvas #js {:width (:width data) :height (:height data)
                       :style #js {:position "absolute"
                                   :left 0 :top 0}
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
