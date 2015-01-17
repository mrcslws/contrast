(ns contrast.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.app-state :as state]
            [contrast.canvas :as cnv]
            [contrast.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.pixel-probe :refer [pixel-probe]]
            [contrast.row-probe :refer [row-probe]]
            [contrast.layeredcanvas :refer [layered-canvas]]
            [contrast.dom :as domh])
    (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

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
                   (om/build row-probe config
                             {:init-state {:track-border-only? true}
                              :state {:content
                                      (om/build layered-canvas config
                                                {:opts {:width (:width config)
                                                        :height (:height config)
                                                        :layers [{:built graphic}
                                                                 {:built probe}]}})}})))))))

(def probed-linear (probed-illusion illusions/single-linear-gradient))
(def probed-sinusoidal (probed-illusion illusions/single-sinusoidal-gradient))

(defn conjurer [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build probed-linear
                         (:single-linear-gradient app))
               (slider {:width 280}
                       (:single-linear-gradient app)
                       (:transition-radius-schema app))


               ;; (om/build probed-sinusoidal
               ;;           (:single-sinusoidal-gradient app))

               ;; (dom/div #js {:style #js {:height 50}}
               ;;          ;; TODO better styling
               ;;          (om/build slider (:single-sinusoidal-gradient app)
               ;;                    {:init-state {:data-key :transition-radius
               ;;                                  :data-width 280
               ;;                                  :data-min 0
               ;;                                  :data-max 300
               ;;                                  :data-format "%dpx"
               ;;                                  :data-interval 1}}))
               (dom/p nil
                      "Needs to be fun to look at! That might be the main reason to use a grating.")))))

(defn main []
  (om/root conjurer state/app-state {:target (.getElementById js/document "app")}))
