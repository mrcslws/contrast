(ns contrast.components.chan-handlers
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn chan-gen-component [canary owner {:keys [f]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "chan-gen")

    om/IInitState
    (init-state [_]
      {:ch (chan)})

    om/IRenderState
    (render-state [_ {:keys [ch]}]
      (f ch))))

(defn chan-gen [f canary]
  (om/build chan-gen-component canary
            {:opts {:f f}}))

(defn chan-render-component [_ owner {:keys [f channel]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "chan-render")

    om/IWillMount
    (will-mount [_]
      (go-loop []
        (om/set-state! owner :the-data (<! channel))
        (recur)))

    om/IRenderState
    (render-state [_ {:keys [the-data]}]
      (f the-data))))

(defn chan-render [channel f canary]
  (om/build chan-render-component canary
            {:opts {:f f
                    :channel channel}}))

(defn chan-genrender [f canary]
  (chan-gen (fn [c]
              (chan-render c (fn [v]
                               (f c v))
                           canary))
            canary))
