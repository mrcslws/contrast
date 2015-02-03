(ns contrast.components.canvas
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put!]]
            [contrast.pixel :as pixel]
            [contrast.common :refer [progress]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn clear [ctx]
  (.save ctx)
  (.setTransform ctx 1 0 0 1 0 0)
  (.clearRect ctx 0 0 (.. ctx -canvas -width) (.. ctx -canvas -height))
  (.restore ctx))

(defn fill-rect [ctx x y width height fill]
  (set! (.-fillStyle ctx) fill)
  (.fillRect ctx x y width height))

(defn layer-paint [owner fpaint subscriber]
  (let [cnv (om/get-node owner "canvas")
        r (fpaint cnv)]
    (when subscriber
      (let [imagedata (or r
                          (-> cnv
                              (.getContext "2d")
                              (.getImageData 0 0 (.-width cnv) (.-height cnv))))]
        (put! subscriber imagedata)))))

;; TODO Seriously, should I use state instead of :opts?
(defn canvas-component [data owner {:keys [fpaint pixel-requests subscriber]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "canvas")

    om/IWillMount
    (will-mount [_]
      (when pixel-requests
        ;; TODO stop leaking on unmount
        (go-loop []
          (let [response (<! pixel-requests)]
            (put! response (-> (om/get-node owner "canvas")
                               (.getContext "2d")
                               (.getImageData 0 0 (:width data)
                                              (:height data)))))
          (recur))))

    om/IDidMount
    (did-mount [_]
      (layer-paint owner fpaint subscriber))

    om/IDidUpdate
    (did-update [_ _ _]
      (layer-paint owner fpaint subscriber))

    om/IRenderState
    (render-state [_ {:keys [width height]}]
      (dom/canvas #js {:ref "canvas" :width width :height height
                       :style
                       #js {;; Without this, height is added to the containing
                            ;; to make room for descenders.
                            ;; TODO I hate that this is here.
                            :verticalAlign "top"}}))))

(defn canvas
  ([canary width height fpaint]
     (canvas canary width height fpaint nil))
  ([canary width height fpaint subscriber]
     (canvas canary width height fpaint subscriber nil))
  ([canary width height fpaint subscriber pixel-requests]
     (om/build canvas-component canary
               {:state {:width width :height height}
                :opts {:fpaint fpaint
                       :pixel-requests pixel-requests
                       :subscriber subscriber}})))

(defn solid-vertical-stripe-painter [col->rgb]
  (fn [cnv]
    (let [ctx (.getContext cnv "2d")
          width (.-width cnv)
          height (.-height cnv)
          imagedata (.createImageData ctx width height)]
      (clear ctx)
      (dotimes [col width]
        (let [[r g b] (col->rgb col)]
          (dotimes [row height]
            (pixel/write! imagedata col row r g b 255))))
      (.putImageData ctx imagedata 0 0)
      imagedata)))

(defn gradient-vertical-stripe-painter [col->topcolor col->bottomcolor]
  (fn [cnv]
    (let [ctx (.getContext cnv "2d")
          width (.-width cnv)
          height (.-height cnv)
          imagedata (.createImageData ctx width height)]
      (clear ctx)
      (dotimes [col width]
        (let [[r1 g1 b1] (col->topcolor col)
              [r2 g2 b2] (col->bottomcolor col)]
          (dotimes [row height]
            (let [p (/ row height)]
              (pixel/write! imagedata col row
                            (progress r1 r2 p)
                            (progress g1 g2 p)
                            (progress b1 b2 p)
                            255)))))
      (.putImageData ctx imagedata 0 0)
      imagedata)))
