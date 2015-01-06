(ns contrast.slider
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn slider [data owner {:keys [value-key]}]
  (reify
    om/IRender
    (render
      [_]
      (let [n (get data value-key)]
        (dom/div nil
                 (dom/input #js {:type "range" :value n
                                 :min 0 :max 300
                                 :onChange
                                 #(om/update! data value-key
                                              (.. % -target -value))})
                 (dom/div nil n))))))
