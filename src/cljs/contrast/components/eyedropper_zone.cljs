(ns contrast.components.eyedropper-zone
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.pixel :as pixel]))

(defn set-color! [{:keys [target schema]} color]
  (om/update! target (:key schema) color))

(defn on-move [config owner]
  (fn [content-x content-y]
    (when-let [imagedata (:imagedata config)]
      (set-color! config (pixel/get imagedata content-x content-y)))))

(defn on-exit [config owner]
  (fn [_ _]
    (set-color! config nil)))

(defn eyedropper-zone-component [config owner {:keys [updates]}]
  (reify
    om/IRenderState
    (render-state [_ {:keys [content]}]
      (apply tracking-area nil
             {:on-move (on-move config owner)
              :on-exit (on-exit config owner)
              :determine-width-from-contents? true}
             content))))

(defn eyedropper-zone [target schema imagedata & content]
  (om/build eyedropper-zone-component
            {:target target :schema schema :imagedata imagedata}
            {:state {:content content}}))
