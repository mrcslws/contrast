(ns contrast.components.fixed-table
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn fixed-table-component [data owner {:keys [extract-table]}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [cols rows]} (extract-table data)]
        (dom/div #js {:style #js {:position "fixed"
                                  :height "30%"
                                  :overflow "auto"
                                  :bottom 0
                                  :left 0
                                  :right 0
                                  :backgroundColor "rgba(0,0,0,0.8)"}}
                 (dom/div #js {:style #js {:font "11px Helvetica, Arial, sans-serif"
                                           :fontWeight "bold"
                                           :color "white"}}
                          (apply dom/table nil
                                 (->> rows
                                      (map (fn [row]
                                             (apply dom/tr nil
                                                    (map (partial dom/td nil)
                                                         ((apply juxt cols) row)))))))))))))
