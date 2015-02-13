(ns contrast.components.eyedropper-zone
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.components.tracking-area :refer [tracking-area]]
            [contrast.pixel :as pixel]
            [contrast.state :as state]))

(defn set-color! [color-inspect color owner]
  (om/update! color-inspect (:key (om/get-state owner :schema)) color))

(defn on-move [color-inspect owner]
  (fn [content-x content-y]
    (when-let [imagedata (om/get-state owner :imagedata)]
      (set-color! color-inspect (pixel/get imagedata content-x content-y) owner))))

(defn on-exit [color-inspect owner]
  (fn [_ _]
    (set-color! color-inspect nil owner)))

(defn eyedropper-zone-component [k owner {:keys [updates]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "eyedropper-zone")

    om/IRenderState
    (render-state [_ {:keys [content schema imagedata]}]
      (let [color-inspect (state/color-inspect k)]
       (apply tracking-area nil
              {:on-move (on-move color-inspect owner)
               :on-exit (on-exit color-inspect owner)
               :determine-width-from-contents? true}
              content)))))

(defn eyedropper-zone [k schema imagedata & content]
  (om/build eyedropper-zone-component k
            {:state {:content content
                     :schema schema
                     :imagedata imagedata}}))
