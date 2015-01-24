(ns contrast.components.color-picker
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.components.tracking-area :refer [tracking-area]]))

(defn handle-change [evt target schema owner]
  (om/update! target (:key schema) (.. evt -target -value)))

(defn color-picker-component [{:keys [target schema]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "color"
                      :onChange #(handle-change % target schema owner)
                      :value (get target (:key schema))}))))
