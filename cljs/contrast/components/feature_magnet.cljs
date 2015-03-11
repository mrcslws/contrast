(ns contrast.components.feature-magnet
  (:require [cljs.core.async :refer [<! put! chan alts! take!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [rgb->hexcode trace-rets background-image]]
            [contrast.components.canvas :as cnv :refer [canvas-component]]
            [contrast.components.slider :refer [slider]]
            [contrast.drag :as drag]
            [contrast.easing :as easing]
            [contrast.pixel :as pixel]
            [contrast.progress :as progress]
            [contrast.spectrum :as spectrum]
            [goog.events :as events]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(defn bezier-feature-magnet-component [easing owner {:keys [x->top x->left]}]
  (reify

    om/IRenderState
    (render-state [_ {:keys [yseek]}]
      (let [x (easing/y->x easing yseek)]
        (dom/div #js {:style #js {:position "absolute"
                                  :top (x->top x)
                                  :left (x->left x)}}
                 (spec/children-in-div owner))))))

(defn bezier-feature-magnet-spec [bezier-points yseek x->top x->left children]
  {:f bezier-feature-magnet-component
   :props bezier-points
   :m {:state {:yseek yseek}
       :opts {:x->top x->top :x->left x->left}}
   :children children})

(defn bezier-spectrum-magnet-content [spectrum owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [color left]}]
      (dom/div
       #js {:style
            #js {:position "absolute"
                 :left left
                 :height 5
                 :width 6
                 :backgroundColor (spectrum/x->cssrgb
                                   spectrum color)}}))))

(defn bezier-spectrum-magnets-component [{:keys [spectrum easing]} owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [h yseeks]}]
      (apply dom/div #js {:style #js {:verticalAlign "top"

                                      :display "inline-block"
                                      :position "relative"}}
             (->> yseeks
                  (map (fn build-magnet
                         [yseek]
                         (spec/render
                          (bezier-feature-magnet-spec
                           easing
                           yseek
                           (fn [p] (-> p
                                       (progress/p->int 0 h)
                                       (- 2)))
                           (constantly 0)
                           [{:f bezier-spectrum-magnet-content
                             :props spectrum
                             :m {:state {:color (* yseek -1)
                                         :left 0}}}
                            {:f bezier-spectrum-magnet-content
                             :props spectrum
                             :m {:state {:color yseek
                                         :left 6}}}])))))))))
