(ns contrast.pages.sandbox
  (:require [com.mrcslws.om-spec :as spec]
            [contrast.components.fixed-table :refer [fixed-table-component]]
            [contrast.hotkeys :as hotkeys]
            [contrast.instrumentation :as instrumentation]
            [contrast.page-triggers :as page-triggers]
            [contrast.state :as state]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn parent-content []
  (-> (om/root-cursor state/app-state)
      :parent-content
      om/ref-cursor))

(defn ref-content []
  (-> (om/root-cursor state/app-state)
      :ref-content
      om/ref-cursor))

(defn content-1 [parent-content owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "content 1")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/em nil
                      "Parent content:")
               (dom/p nil
                      (:text parent-content))
               (dom/br nil)
               (dom/em nil
                      "Ref content:")
               (dom/p nil
                      (-> (om/observe owner (ref-content))
                          :text))))))

(defn child-1 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 1 child")

    om/IRenderState
    (render-state [_ {:keys [content]}]
      (dom/div nil
               content))))

(defn content-strategy-1 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 1 parent")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/strong nil
                           "Strategy 1")
               (dom/br nil)
               (dom/button #js {:onClick #(om/refresh! owner)}
                           "Trigger render")
               (om/build child-1 nil
                         {:state {:content
                                  (om/build content-1
                                            (om/observe owner (parent-content)))}})))))

(defn content-2 [parent-content owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "content 2")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/em nil
                      "Parent content:")
               (dom/p nil
                      (:text parent-content))
               (dom/br nil)
               (dom/em nil
                      "Ref content:")
               (dom/p nil
                      (-> (om/observe owner (ref-content))
                          :text))))))

(defn child-2 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 2 child")

    om/IRenderState
    (render-state [_ {:keys [child-component child-props]}]
      (dom/div nil
               (om/build child-component child-props)))))

(defn content-strategy-2 [_ owner]
    (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 2 parent")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/strong nil
                           "Strategy 2")
               (dom/br nil)
               (dom/button #js {:onClick #(om/refresh! owner)}
                           "Trigger render")
               (om/build child-2 nil
                         {:state {:child-component content-2
                                  :child-props (om/observe owner (parent-content))}})))))

(defn content-3 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "content 3")

    om/IWillMount
    (will-mount [_]
      (go-loop []
        (let [c (<! (om/get-state owner :content-chan))]
          (when (not= c (om/get-state owner :content))
            (om/set-state! owner :content c)))

        (recur)))

    om/IRenderState
    (render-state [_ {:keys [content]}]
      (dom/div nil
               (dom/em nil
                      "Parent content:")
               (dom/p nil
                      content)
               (dom/br nil)
               (dom/em nil
                      "Ref content:")
               (dom/p nil
                      (-> (om/observe owner (ref-content))
                          :text))))))

(defn on-update-3 [owner]

  (put! (om/get-state owner :content-chan)
        (:text (om/observe owner (parent-content)))))

(defn child-3 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 3 child")

    om/IRenderState
    (render-state [_ {:keys [child-component child-props child-m]}]
      (dom/div nil
               (om/build child-component
                         child-props
                         child-m)))))

(defn content-strategy-3 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 3 parent")

    om/IInitState
    (init-state [_]
      {:content-chan (chan)})

    om/IDidMount
    (did-mount [_]
      (on-update-3 owner))

    om/IDidUpdate
    (did-update [_ _ _]
      (on-update-3 owner))

    om/IRenderState
    (render-state [_ {:keys [content-chan]}]
      (dom/div nil
               (dom/strong nil
                           "Strategy 3")
               (dom/br nil)
               (dom/button #js {:onClick #(om/refresh! owner)}
                           "Trigger render")
               (om/build child-3 nil
                         {:state {:child-component content-3
                                  :child-m {:state {:content-chan content-chan}}}})))))

(defn content-4 [content owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "content 4")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/em nil
                      "Parent content:")
               (dom/p nil
                      content)
               (dom/br nil)
               (dom/em nil
                      "Ref content:")
               (dom/p nil
                      (-> (om/observe owner (ref-content))
                          :text))))))

(defn child-4 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 4 child")

    om/IRenderState
    (render-state [_ {:keys [child-component child-props child-m]}]
      (spec/children-in-div owner))))

(defn content-strategy-4 [_ owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "Strategy 4 parent")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/strong nil
                           "Strategy 4")
               (dom/br nil)
               (dom/button #js {:onClick #(om/refresh! owner)}
                           "Trigger render")
               (spec/render {:f child-4
                             :children [{:f content-4
                                         :props (:text (om/observe owner (parent-content)))}]})))))

(defn state-changer [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "State changer")

    om/IRender
    (render [_]
      (dom/div nil
               (dom/div nil
                        (dom/button #js {:onClick #(om/transact! app [:parent-content :text] (fn [s]
                                                                                               (str s "-")))}
                                    "Change parent-content!"))
               (dom/div #js {:style #js {:marginBottom 40}}
                        (dom/button #js {:onClick #(om/transact! app [:ref-content :text] (fn [s] (str s "-")))}
                                    "Change ref-content!"))))))

(def descriptor
  (-> om/pure-methods
      (instrumentation/instrument-methods state/component-data)
      clj->js
      om/specify-state-methods!))

(defn on-code-reload []

  (om/root state-changer state/app-state {:target (js/document.getElementById "editor")})
  (om/root content-strategy-1 nil {:target (js/document.getElementById "strategy-1")
                                   :descriptor descriptor})
  (om/root content-strategy-2 nil {:target (js/document.getElementById "strategy-2")
                                   :descriptor descriptor})
  (om/root content-strategy-3 nil {:target (js/document.getElementById "strategy-3")
                                   :descriptor descriptor})
    (om/root content-strategy-4 nil {:target (js/document.getElementById "strategy-4")
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
  (doseq [id ["editor" "component-stats" "strategy-1" "strategy-2" "strategy-3" "strategy-4"]
          :let [el (js/document.createElement "div")]]
    (.setAttribute el "id" id)
    (js/document.body.appendChild el)))

(defonce initialize-state
  (swap! state/app-state merge
         {:ref-content {:text "|"}
          :parent-content {:text "|"}}))

(defonce render-listen
  (let [reloads (chan)]
    (tap page-triggers/code-reloads reloads)
    (go-loop []
      (<! reloads)
      (on-code-reload)
      (recur))
    :listening))
