(ns contrast.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.app-state :as state]
            [contrast.canvas :as cnv]
            [contrast.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.row-probe :refer [row-probe]]
            [contrast.row-display :refer [row-display]]
            [contrast.dom :as domh]
            [contrast.color-exposer :refer [color-exposer]]
            [contrast.eyedropper-zone :refer [eyedropper-zone]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn probed-illusion [illusion]
  (fn [config owner]
    (reify
      om/IInitState
      (init-state [_]
        (let [to-mult (chan)
              ;; TODO terrible
              to-mult2 (chan)]
          {:updates-in to-mult
           :updates (mult to-mult)
           :rd-updates-in to-mult2
           :rd-updates (mult to-mult2)}))

      om/IRenderState
      (render-state [_ {:keys [updates updates-in rd-updates rd-updates-in]}]
        (dom/div nil

                 ;; TODO it's not getting the imagedata until the image updates once

                 (->> (row-display nil config {:key :probed-row}
                                   {:subscriber rd-updates-in
                                    :updates updates})
                      (color-exposer nil config rd-updates)
                      (eyedropper-zone nil config {:key :selected-color} rd-updates)
                      (dom/div #js {:style #js {:marginBottom 20}}))

                 (->> (om/build illusion
                                (select-keys config [:width :height
                                                     :transition-radius])
                                {:opts {:subscriber updates-in}})
                      (color-exposer nil config updates)
                      ;; TODO refer to the actual schema. Oh god this was dumb.
                      (eyedropper-zone nil config {:key :selected-color} updates)
                      (row-probe nil config {:key :probed-row}
                                 {:subscriber rd-updates-in
                                  :updates updates
                                  :track-border-only? true})))))))

(def probed-linear (probed-illusion illusions/single-linear-gradient))
(def probed-sinusoidal (probed-illusion illusions/single-sinusoidal-gradient))

(defn conjurer [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:style #js {:marginLeft 40}}

               ;; (om/build probed-linear
               ;;           (:single-linear-gradient app))
               ;; (slider {:width 280 :marginLeft 140}
               ;;         (:single-linear-gradient app)
               ;;         (:transition-radius-schema app))
               (om/build probed-sinusoidal
                         (:single-sinusoidal-gradient app))
               (slider {:width 280 :marginLeft 140}
                       (:single-sinusoidal-gradient app)
                       (:transition-radius-schema app))

               (dom/p
                nil
                "Needs to be fun to look at! That might be the main reason to use a grating.")

               (dom/p
                nil
                "Put something around the image so that it's clear where the row-probe works.")
               ))))

(defn main []
  (om/root conjurer state/app-state {:target (.getElementById js/document "app")}))
