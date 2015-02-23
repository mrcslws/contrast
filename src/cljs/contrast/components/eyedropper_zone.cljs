(ns contrast.components.eyedropper-zone
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.components.canvas :as cnv]
            [contrast.components.tracking-area :refer [tracking-area-component]]
            [contrast.pixel :as pixel]
            [contrast.state :as state]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn set-color! [color-inspect color schema]
  (om/update! color-inspect (:key schema) color))

(defn eyedropper-zone-component [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "eyedropper-zone")

    om/IInitState
    (init-state [_]
      {:moves (chan)
       :exits (chan)})

    om/IWillMount
    (will-mount [_]
      (let [color-inspect (state/color-inspect k)]
        (go-loop []
          (let [[content-x content-y] (<! (om/get-state owner :moves))
                {:keys [imagedata schema]} (om/get-state owner)]
            (when imagedata
              (set-color! color-inspect (pixel/get imagedata content-x content-y)
                           schema))
            (recur)))

        (go-loop []
          (let [_ (<! (om/get-state owner :exits))
                {:keys [schema]} (om/get-state owner)]
            (set-color! color-inspect nil schema)
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [moves exits]}]
      (spec/render
       {:f tracking-area-component
        :m {:state {:moves moves
                    :exits exits
                    :determine-width-from-contents? true}}
        :children [(spec/children-in-div-spec owner)]}))))

(defn eyedropper-zone-spec [k schema imagedata children]
  {:f eyedropper-zone-component
   :props k
   :m {:state {:schema schema
               :imagedata imagedata}}
   :children children})
