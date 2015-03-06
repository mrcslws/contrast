(ns contrast.components.feature-magnet
  (:require [cljs.core.async :refer [<! put! chan alts! take!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [rgb->hexcode trace-rets background-image]]
            [contrast.components.canvas :as cnv :refer [canvas-component]]
            [contrast.components.slider :refer [slider]]
            [contrast.drag :as drag]
            [contrast.easing :as easing]
            [contrast.pixel :as pixel]
            [goog.events :as events]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(defn bezier-feature-magnet-component [bezier-points owner {:keys [x->top x->left]}]
  (reify

    om/IRenderState
    (render-state [_ {:keys [yseek]}]
      (let [{:keys [p1 p2]} bezier-points
            bezier (easing/cubic-bezier-easing (:x p1) (:y p1) (:x p2) (:y p2))
            x (easing/y->x bezier yseek)]
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
