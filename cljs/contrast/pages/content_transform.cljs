(ns contrast.pages.content-transform
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.components.fixed-table :refer [fixed-table-component]]
            [contrast.hotkeys :as hotkeys]
            [contrast.instrumentation :as instrumentation]
            [contrast.page-triggers :as page-triggers]
            [contrast.state :as state]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(def descriptor
  (-> om/pure-methods
      (instrumentation/instrument-methods state/component-data)
      clj->js
      om/specify-state-methods!))

(defn hello-component [_ owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil "Hello!"))))

(defn bordered [color]
  (fn [text owner]
    (reify
      om/IDisplayName
      (display-name [_]
        (str "bordered " color))

      om/IRenderState
      (render-state [_ {:keys [content child-component]}]
        (dom/div #js {:style #js {:borderWidth 1
                                  :borderStyle "solid"
                                  :borderColor color}}
                 text
                 (spec/children-in-div owner))))))

(def red-border (bordered "red"))
(def blue-border (bordered "blue"))

(defn orchestrator [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "orchestrator")

    om/IInitState
    (init-state [_]
      {:f1 blue-border
       :f2 red-border
       :props1 "|"
       :props2 "|"})

    om/IRenderState
    (render-state [_ {:keys [f1 f2 props1 props2]}]
      (dom/div nil
               (dom/button #js {:onClick #(om/refresh! owner)}
                           "Orchestrator render")
               (dom/button #js {:onClick #(om/update-state!
                                           owner :props1 (fn [s] (str s "+")))}
                           "1++")
               (dom/button #js {:onClick #(om/update-state!
                                           owner :props2 (fn [s] (str s "-")))}
                           "2--")
               (spec/render {:f f1
                             :props props1
                             :children [{:f hello-component}

                                        {:f f2
                                         :props props2
                                         :children [{:f hello-component}]}]})))))


(defn dummy [_ owner]
  (reify
    om/IRender
    (render [_])))

(defn on-code-reload []

  (om/root dummy state/app-state {:target (js/document.getElementById "dummy")})
  (om/root orchestrator nil {:target (js/document.getElementById "experiment")
                             :descriptor descriptor})


  (om/root fixed-table-component state/component-data {:target (js/document.getElementById "component-stats")
                                                       :opts {:extract-table
                                                              instrumentation/aggregate-update-times}})

  (doseq [[m f] {{:modifiers [:ctrl]
                  :char "l"}
                 (fn []
                   (reset! state/component-data {}))}]
    (hotkeys/assoc-global m f)))

(defonce add-divs
  (doseq [id ["dummy" "experiment" "component-stats"]
          :let [el (js/document.createElement "div")]]
    (.setAttribute el "id" id)
    (js/document.body.appendChild el)))

(defonce initialize-state
  (swap! state/app-state merge
         {:foo :bar}))

(defonce render-listen
  (let [reloads (chan)]
    (tap page-triggers/code-reloads reloads)
    (go-loop []
      (<! reloads)
      (on-code-reload)
      (recur))
    :listening))
