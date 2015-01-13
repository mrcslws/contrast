(ns contrast.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.canvas :as cnv]
            [contrast.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.pixel-probe :refer [pixel-probe]]
            [contrast.layeredcanvas :refer [layered-canvas]])
    (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

;; TODO switch away from "radius". "width" or "diameter" are better.
(defonce app-state
  (atom {:single-linear-gradient {:width 600
                                  :height 256
                                  :transition-radius 50
                                  :pixel-probe {}}
         :single-sinusoidal-gradient {:width 600
                                      :height 256
                                      :transition-radius 50
                                      :pixel-probe {}}}))

(defn probed-illusion [illusion]
  (fn [config owner]
    (reify
      om/IInitState
      (init-state [_]
        {:updates (chan)})

      om/IRenderState
      (render-state [_ {:keys [updates]}]
        (let [graphic (om/build illusion
                                (select-keys config [:width :height
                                                     :transition-radius])
                                {:opts {:subscriber updates}})
              probe (om/build pixel-probe
                              (select-keys config [:width :height :pixel-probe
                                                   :transition-radius])
                              {:opts {:updates updates}})]

          (dom/div nil
                   (om/build layered-canvas config
                             {:opts {:width (:width config)
                                     :height (:height config)
                                     :layers [graphic probe]}})
                   (dom/div #js {:style #js {:height 50}}
                            ;; TODO better styling
                            (om/build slider config
                                      {:opts {:data-key :transition-radius
                                              :data-width 280 :data-min 0 :data-max 300
                                              :data-interval 1 :data-format "%dpx"}}))))))))

(defn conjurer [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:slg-updates (chan)
       :ssg-updates (chan)})

    om/IRenderState
    (render-state [_ {:keys [slg-updates ssg-updates]}]
      (dom/div nil
               (om/build (probed-illusion illusions/single-linear-gradient)
                         (:single-linear-gradient app))
               (om/build (probed-illusion illusions/single-sinusoidal-gradient)
                         (:single-linear-gradient app))))))

(defn main []
  (om/root conjurer app-state {:target (.getElementById js/document "app")}))
