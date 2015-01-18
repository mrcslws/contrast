(ns contrast.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.app-state :as state]
            [contrast.components.canvas :as cnv]
            [contrast.components.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.components.row-probe :refer [row-probe]]
            [contrast.components.row-display :refer [row-display]]
            [contrast.dom :as domh]
            [contrast.components.color-exposer :refer [color-exposer]]
            [contrast.components.eyedropper-zone :refer [eyedropper-zone]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn probed-illusion [illusion]
  (fn [config owner]
    (reify
      om/IInitState
      (init-state [_]
        {:illusion-updates (chan)
         :row-display-updates (chan)})

      om/IWillMount
      (will-mount [_]
        (let [{:keys [illusion-updates row-display-updates]} (om/get-state owner)]
          (go-loop []
            (om/set-state! owner :illusion-imagedata (<! illusion-updates))
            (recur))

          (go-loop []
            (om/set-state! owner :row-display-imagedata (<! row-display-updates))
            (recur))))

      om/IRenderState
      (render-state [_ {:keys [illusion-updates row-display-updates
                               illusion-imagedata row-display-imagedata]}]
        (dom/div nil

                 ;; TODO it's not getting the imagedata until the image updates
                 ;; once
                 (->> (row-display nil config {:key :probed-row} illusion-imagedata
                                   {:subscriber row-display-updates})
                      (color-exposer nil config row-display-imagedata)
                      (eyedropper-zone nil config {:key :selected-color}
                                       row-display-imagedata)
                      (dom/div #js {:style #js {:marginBottom 20}}))

                 (->> (om/build illusion
                                (select-keys config [:width :height
                                                     :transition-radius])
                                {:opts {:subscriber illusion-updates}})
                      (color-exposer nil config illusion-imagedata)
                      ;; TODO refer to the actual schema. Oh god this was dumb.
                      (eyedropper-zone nil config {:key :selected-color}
                                       illusion-imagedata)
                      (row-probe nil config {:key :probed-row} {:track-border-only? true})))))))

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
