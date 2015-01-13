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


;; Potential improvement:
;; (defonce app-state2
;;   (atom {:entities {:single-linear-gradient {:width 600
;;                                              :height 600
;;                                              :transition-radius 50
;;                                              :imagedata nil
;;                                              :pixel-probe {}}}}))

(defonce app-state
  (atom {:width 600
         :height 600
         :transition-radius 50
         :pixel-probe {}}))

(defn conjurer [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:slg-updates (chan)})

    om/IRenderState
    (render-state [_ {:keys [slg-updates]}]
      (let [graphic (om/build illusions/single-linear-gradient
                              (select-keys app [:width :height
                                                :transition-radius])
                              {:opts {:subscriber slg-updates}})
            probe (om/build pixel-probe
                            (select-keys app [:width :height :pixel-probe
                                              :transition-radius])
                            {:opts {:updates slg-updates}})]
        (dom/div nil
                 (om/build layered-canvas app
                           {:opts {:width (:width app)
                                   :height (:height app)
                                   :layers [graphic probe]}})
                 (om/build slider app
                           {:opts {:data-key :transition-radius

                                   ;; TODO - move to init-state?
                                   :data-width 280 :data-min 0 :data-max 300
                                   :data-interval 1 :data-format "%dpx"}}))))))

(defn main []
  (om/root conjurer app-state {:target (.getElementById js/document "app")}))
