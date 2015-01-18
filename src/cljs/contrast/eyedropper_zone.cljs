(ns contrast.eyedropper-zone
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.app-state :as state]
            [contrast.canvas :as cnv]
            [contrast.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.row-probe :refer [row-probe]]
            [contrast.dom :as domh]
            [contrast.common :refer [tracking-area]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]
                   [contrast.macros :refer [forloop]]))

(defn set-color! [{:keys [target schema]} color]
  (om/update! target (:key schema) color))

(defn on-move [config owner]
  (fn [content-x content-y]
    (when-let [imagedata (om/get-state owner :imagedata)]
      (let [data (.-data imagedata)
            ;; TODO dedupe
            base (-> content-y
                     (* (.-width imagedata))
                     (+ content-x)
                     (* 4))]
        (set-color! config
                    [(aget data base)
                     (aget data (+ base 1))
                     (aget data (+ base 2))
                     (aget data (+ base 3))])))))

(defn on-exit [config owner]
  (fn [_ _]
    (set-color! config nil)))

(defn eyedropper-zone-component [config owner {:keys [updates]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [updates-out (chan)]
        (tap updates updates-out)
        (go-loop []
          (om/set-state! owner :imagedata (<! updates-out))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [content]}]
      (apply tracking-area nil
             {:on-move (on-move config owner)
              :on-exit (on-exit config owner)
              :determine-width-from-contents? true}
             content))))

(defn eyedropper-zone [style target schema updates & content]
  (dom/div #js {:style (clj->js style)}
           (om/build eyedropper-zone-component {:target target :schema schema}
                     {:state {:content content}
                      :opts {:updates updates}})))
