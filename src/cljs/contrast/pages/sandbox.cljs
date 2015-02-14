(ns contrast.pages.sandbox
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.state :as state]
            [contrast.page-triggers :as page-triggers]
            [cljs.core.async :refer [put! chan mult tap close! <!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn foo-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil))))

(defn bar-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil))))

(defn render []
  (om/root foo-component state/app-state {:target (js/document.getElementById "foo")})
  (om/root bar-component state/app-state {:target (js/document.getElementById "bar")}))

(defonce add-divs
  (doseq [id ["foo" "bar"]
          :let [el (js/document.createElement "div")]]
    (.setAttribute el "id" id)
    (js/document.body.appendChild el)))

(defonce initialize-state
  (swap! state/app-state merge
         {:foo false}))

(defonce render-listen
  (let [renders (chan)]
    (tap page-triggers/renders renders)
    (go-loop []
      (<! renders)
      (render)
      (recur))
    :listening))
