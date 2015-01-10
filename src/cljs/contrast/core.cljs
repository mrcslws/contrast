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
;; (defonce app-state
;;   (atom {:entities {:single-linear-gradient {:width 600
;;                                              :height 600
;;                                              :transition-radius 50
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
      {:illusion-pxs (chan)})

    om/IRenderState
    (render-state [_ {:keys [illusion-pxs]}]
      (let [graphic (om/build illusions/single-linear-gradient
                              (select-keys app [:width :height
                                                :transition-radius])
                              {:opts {:pixel-requests illusion-pxs}})
            probe (om/build pixel-probe
                            ;; TODO why isn't it updating when transition-radius changes
                            (select-keys app [:width :height :pixel-probe
                                              :transition-radius])
                            {:opts {:pixel-requests illusion-pxs}})]
        (dom/div nil
                 (om/build layered-canvas app
                           {:opts {:width (:width app)
                                   :height (:height app)
                                   :layers [graphic probe]}})
                 (om/build slider app
                           {:opts {:value-key :transition-radius}}))))))

(defn main []
  (om/root conjurer app-state {:target (.getElementById js/document "app")}))
