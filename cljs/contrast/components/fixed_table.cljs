(ns contrast.components.fixed-table
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn row [tdfn style cells]
  (let [[leftmost & r] cells
        tds (apply vector
                   (->> leftmost
                        (tdfn #js {:style #js
                                   {:textAlign "left"}}))
                   (->> r
                        (map (partial
                              tdfn #js {:style #js
                                        {:textAlign "right"}}))))]
    (apply dom/tr #js {:style (clj->js style)}
           tds)))

(def bottom-border
  {:borderBottom "1px dashed rgba(255,255,255,0.1)"})

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
                 (dom/div #js {:style #js {:fontFamily "Consolas, Liberation Mono, Courier, monospace"
                                           :fontWeight "bold"
                                           :color "white"}}
                          (dom/table #js {:style #js {:width "100%"
                                                      :borderCollapse "collapse"}}
                                     (dom/thead #js {:style #js {:fontSize "9px"}}
                                                (row dom/th bottom-border cols))
                                     (apply dom/tbody #js {:style #js {:fontSize "11px"}}
                                            (conj (->> (drop-last rows)
                                                       (map (partial row dom/td bottom-border))
                                                       vec)
                                                  (row dom/td nil (last rows)))))))))))
