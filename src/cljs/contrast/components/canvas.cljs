(ns contrast.components.canvas
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <! pipeline]]
            [contrast.pixel :as pixel])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn clear [ctx]
  (.save ctx)
  (.setTransform ctx 1 0 0 1 0 0)
  (.clearRect ctx 0 0 (.. ctx -canvas -width) (.. ctx -canvas -height))
  (.restore ctx))

(defn fill-rect [ctx x y width height fill]
  (set! (.-fillStyle ctx) fill)
  (.fillRect ctx x y width height))

(defn linear-gradient [ctx x0 y0 x1 y1 & stops]
  (let [g (.createLinearGradient ctx x0 y0 x1 y1)]
    (doseq [[offset color] stops]
      (.addColorStop g offset color))
    g))

(defn layer-paint [data owner fpaint subscriber]
  (let [cnv (om/get-node owner "canvas")
        r (fpaint data cnv)]
    (when subscriber
      (let [imagedata (or r
                          (-> cnv
                              (.getContext "2d")
                              (.getImageData 0 0 (.-width cnv) (.-height cnv))))]
        (put! subscriber imagedata)))))

;; TODO Seriously, should I use state instead of :opts?
(defn canvas [data owner {:keys [fpaint style width height pixel-requests subscriber]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (when pixel-requests
        ;; TODO stop leaking on unmount
        (go-loop []
          (let [response (<! pixel-requests)]
            (put! response (-> (om/get-node owner "canvas")
                               (.getContext "2d")
                               (.getImageData 0 0 width height))))
          (recur))))

    om/IDidMount
    (did-mount [_]
      (layer-paint data owner fpaint subscriber))

    om/IDidUpdate
    (did-update [_ _ _]
      (layer-paint data owner fpaint subscriber))

    om/IRender
    (render [_]
      (dom/canvas #js {:ref "canvas" :width width :height height
                       :style
                       #js {;; Without this, height is added to the containing
                            ;; to make room for descenders.
                            :verticalAlign "top"}}))))
