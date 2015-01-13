(ns contrast.layeredcanvas
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <! pipeline]]
            [contrast.pixel :as pixel]
            [contrast.canvas :as cnv])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]
                   [contrast.macros :refer [forloop]]))

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

(defn layered-canvas [data owner {:keys [layers width height pixel-requests subscriber]}]
  (reify
    om/IInitState
    (init-state [_]
      (when (or subscriber pixel-requests)
        {:child-requests (take (count layers) (repeatedly #(chan)))
         :child-updates (take (count layers) (repeatedly #(chan)))}))

    om/IWillMount
    (will-mount [_]

      (when (or pixel-requests subscriber)
        (let [combine-these-layers (chan)
              all-layers (vec (map vector
                                   (repeat nil)
                                   (om/get-state owner :child-requests)))]
          (go-loop []
            ;; Use `loop` rather than `reduce` because the reducing function
            ;; consumes a channel.
            (let [[layers response] (<! combine-these-layers)]
              (put! response
                    (loop [imagedata nil
                           remaining layers]
                      (when (not-empty remaining)
                        (let [[absorbee absorbee-requests] (first remaining)
                              absorbee (or absorbee
                                           (let [r (chan)]
                                             (put! absorbee-requests r)
                                             (<! r)))]
                          (recur (overlay-all! imagedata absorbee)
                                 (rest remaining))))
                      imagedata)))
            (recur))

          (when pixel-requests
            (pipeline 1 combine-these-layers
                      (map (fn [response] [all-layers response]))
                      pixel-requests
                      false))

          (when subscriber
            (let [child-updates (om/get-state owner :child-updates)]
              (doseq [i (range (count child-updates))
                      :let [child (nth child-updates i)]]
                (pipeline 1 combine-these-layers
                          (map (fn [imagedata]
                                 [(assoc-in all-layers [i 0] imagedata)
                                  subscriber]))
                          child
                          false)))))))

    om/IRenderState
    (render-state [_ {:keys [child-requests child-updates]}]
      (apply dom/div #js {:style #js {:width width :height height
                                      :position "relative"}}
             (for [i (range (count layers))]
               (let [opts {:pixel-requests (nth child-requests i)
                           :subscriber (nth child-updates i)}
                     layer-or-config (nth layers i)
                     layer (cond
                            (associative? layer-or-config)
                            (om/build cnv/canvas ((:fdata layer-or-config) data)
                                      {:opts (assoc opts
                                               :fpaint (:fpaint layer-or-config)
                                               :width width :height height)})

                            (fn? layer-or-config)
                            (layer-or-config opts)

                            :else
                            layer-or-config)]
                 (dom/div #js {:style #js {:zIndex i :position "absolute"
                                           :left 0 :top 0}}
                          layer)))))))
