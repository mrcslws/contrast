(ns contrast.pages.sandbox
  (:require [com.mrcslws.om-spec :as spec]
            [contrast.components.easing-picker :refer [easing-picker-component]]
            [contrast.components.fixed-table :refer [fixed-table-component]]
            [contrast.hotkeys :as hotkeys]
            [contrast.instrumentation :as instrumentation]
            [contrast.page-triggers :as page-triggers]
            [contrast.state :as state]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))



(defn channel-renderer [_ owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (go-loop []
        (om/set-state! owner :content (<! (om/get-state owner :channel)))
        (recur)))

    om/IRenderState
    (render-state [_ {:keys [content]}]
      (dom/div nil content))))

(defn state-renderer [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:channel (chan)})

    om/IDidUpdate
    (did-update [_ _ _]
      (put! (om/get-state owner :channel) (:text app)))

    om/IRenderState
    (render-state [_ {:keys [channel]}]
      (om/build channel-renderer nil {:state {:channel channel}}))))

(defn state-changer [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "State changer")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/button #js {:onClick #(om/transact! app :text
                                                        (fn [s]
                                                          (str s "-")))}
                           "Change state")))))

(def descriptor
  (-> om/pure-methods
      (instrumentation/instrument-methods state/component-data)
      clj->js
      om/specify-state-methods!))

(defn on-code-reload []
  (om/root easing-picker-component state/app-state {:target (js/document.getElementById "easing-picker")})

  ;; (om/root state-changer state/app-state {:target (js/document.getElementById "editor")})
  ;; (om/root state-renderer state/app-state {:target (js/document.getElementById "renderer")})

  ;; (om/root fixed-table-component state/component-data {:target (js/document.getElementById "component-stats")
  ;;                                                      :opts {:extract-table
  ;;                                                             instrumentation/aggregate-update-times}})

  (doseq [[m f] {{:modifiers [:ctrl]
                  :char "l"}
                 (fn []
                   (reset! state/component-data {}))}]
    (hotkeys/assoc-global m f)))

(defonce add-divs
  (doseq [id ["easing-picker" "editor" "renderer" "component-stats"]
          :let [el (js/document.createElement "div")]]
    (.setAttribute el "id" id)
    (js/document.body.appendChild el)))

(defonce initialize-state
  (swap! state/app-state merge
         {:x1 0.25
          :y1 0.50
          :x2 0.70
          :y2 0.70}
         ;; {:ref-content {:text "|"}
         ;;  :parent-content {:text "|"}}
         ))

(defonce render-listen
  (let [reloads (chan)]
    (tap page-triggers/code-reloads reloads)
    (go-loop []
      (<! reloads)
      (on-code-reload)
      (recur))
    :listening))
