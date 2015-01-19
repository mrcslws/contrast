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
                 (->> (row-display config {:key :probed-row} illusion-imagedata
                                   {:subscriber row-display-updates})
                      (color-exposer config row-display-imagedata)
                      (eyedropper-zone config {:key :selected-color}
                                       row-display-imagedata)
                      (dom/div #js {:style #js {:marginBottom 20}}))

                 (->> (om/build illusion
                                config
                                {:opts {:subscriber illusion-updates}})
                      (color-exposer config illusion-imagedata)
                      (eyedropper-zone config {:key :selected-color}
                                       illusion-imagedata)
                      (row-probe config {:key :probed-row}
                                 {:track-border-only? true})))))))

(def probed-linear (probed-illusion illusions/single-linear-gradient))
(def probed-sinusoidal (probed-illusion illusions/single-sinusoidal-gradient))
(def probed-grating (probed-illusion illusions/grating))

(defn single-gradient [app owner]
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
                       {:key :transition-radius
                        :min 0
                        :max 300
                        :str-format "%dpx"
                        :interval 1})))))

(defn sweep-grating [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:style #js {:marginLeft 40}}
               (om/build probed-grating
                         (:sweep-grating app))
               (slider {:width 280 :marginLeft 140}
                       (:sweep-grating app)
                       {:key :contrast
                        :min 0
                        :max 128
                        :str-format "%.1f rgb units"
                        :interval 0.5})))))

(defn app-state-display [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (pr-str app)))))

(defn main []
  (om/root single-gradient state/app-state {:target (.getElementById js/document "1-twosides")})
  (om/root sweep-grating state/app-state {:target (.getElementById js/document "2-sweep-grating")})
  (om/root app-state-display state/app-state {:target (.getElementById js/document "app-state")}))
