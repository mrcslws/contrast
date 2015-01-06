(ns contrast.layeredcanvas
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn layer [data owner {:keys [fpaint style width height additional]}]
  (reify
    om/IDidMount
    (did-mount [_]
      (fpaint data (om/get-node owner "canvas")))

    om/IDidUpdate
    (did-update [_ _ _]
      (fpaint data (om/get-node owner "canvas")))

    om/IRender
    (render [_]
      (dom/canvas (clj->js (assoc additional
                             :ref "canvas"
                             :width width :height height
                             :style (clj->js style)))))))

;; TODO - Expose access to pixels (probably using a channel-based API)
(defn layered-canvas [data owner {:keys [layers width height]}]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:style #js {:width width :height height :position "relative"}}
             (for [i (range (count layers))
                   :let [lconfig (nth layers i)]]
               (if (associative? lconfig)
                 (let [{:keys [fpaint fdata left top additional z-index]} lconfig
                       width (or (:width lconfig) width)
                       height (or (:height lconfig) height)
                       left (or left 0)
                       top (or top 0)
                       z-index (or z-index i)]
                   (om/build layer (fdata data)
                             {:opts {:fpaint fpaint
                                     :width width :height height
                                     :additional additional
                                     :style {:position "absolute"
                                             :left left :top top
                                             :zIndex i}}}))
                 lconfig))))))
