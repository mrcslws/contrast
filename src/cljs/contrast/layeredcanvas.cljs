(ns contrast.layeredcanvas
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.pixel :as pixel])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn layer [data owner {:keys [fpaint style width height pixel-requests]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (when pixel-requests
        (go-loop []
          (let [[x y w h response] (<! pixel-requests)]
            (put! response (-> (om/get-node owner "canvas")
                               (.getContext "2d")
                               (.getImageData x y w h))))
          (recur))))

    om/IDidMount
    (did-mount [_]
      (fpaint data (om/get-node owner "canvas")))

    om/IDidUpdate
    (did-update [_ _ _]
      (fpaint data (om/get-node owner "canvas")))

    om/IRender
    (render [_]
      (dom/canvas #js {:ref "canvas"
                       :width width :height height
                       :style (clj->js style)}))))

(defn overlay-all! [victim foreground]
  (if-not victim
    foreground
    (let [len (pixel/pixel-count victim)
          v (pixel/nth! victim 0)]
      (loop [i 0
             f (pixel/nth! foreground i)]
        (when (< i len)
          (when-not (pixel/transparent? f)
            (pixel/overlay! (pixel/transport! v i) f))
          (recur (inc i) (pixel/pan! f 1))))
      victim)))

(defn layered-canvas [data owner {:keys [layers width height pixel-requests]}]
  (reify
    om/IInitState
    (init-state [_]
      (when pixel-requests
        {:child-requests (take (count layers) (repeatedly #(chan)))}))

    om/IWillMount
    (will-mount [_]
      (when pixel-requests
        (go-loop []
          (let [child-requests (om/get-state owner :child-requests)
                [x y w h pixel-response] (<! pixel-requests)]
            ;; TODO call in order of zIndex

            ;; V1 - Assume nothing is offset.
            ;; Afterward, write the reduce function that you wish you had.

            ;; I could write a core.async-compatible `reduce`.
            ;; Or I could explore channels more and find if there's a way to make
            ;; them do what I want.

             ;; Use `loop` rather than `reduce` because the reducing function
             ;; consumes a channel.
            (put! pixel-response
                  (loop [imagedata nil
                         remaining-children child-requests]
                    (when (not-empty remaining-children)
                      (recur (let [response (chan)]
                               (put! (first remaining-children) [x y w h response])
                               (overlay-all! imagedata (<! response)))
                             (rest remaining-children)))
                    imagedata)))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [child-requests]}]
      (apply dom/div #js {:style #js {:width width :height height
                                      :position "relative"}}
             (for [i (range (count layers))
                   :let [lconfig (nth layers i)]]
               (if (associative? lconfig)
                 (let [{:keys [fpaint fdata left top additional z-index]}
                       lconfig
                       width (or (:width lconfig) width)
                       height (or (:height lconfig) height)
                       left (or left 0)
                       top (or top 0)
                       z-index (or z-index i)]
                   (om/build layer (fdata data)
                             {:opts {:pixel-requests (nth child-requests i)
                                     :fpaint fpaint
                                     :width width :height height
                                     :additional additional
                                     :style {:position "absolute"
                                             :left left :top top
                                             :zIndex i}}}))
                 lconfig))))))
