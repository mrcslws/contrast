(ns contrast.components.color-picker
  (:require [cljs.core.async :refer [<! put! chan alts! take!]]
            [contrast.common :refer [rgb->hexcode hexcode->rgb]]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(defn handle-change [evt target schema owner]
  (let [text (.. evt -target -value)]
    (om/set-state! owner :text text)
    (when-let [rgb (hexcode->rgb text)]
      (om/update! target (:key schema) rgb))))

(defn color-picker-component [{:keys [target schema]} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "color-picker")

    om/IInitState
    (init-state [_]
      {:should-focus true
       :text (rgb->hexcode (get target (:key schema)))})

    om/IWillMount
    (will-mount [_]
      (when-let [invokes (om/get-state owner :invokes)]
        (go-loop []
          (when-let [_ (<! invokes)]
            ;; Some browsers aren't ready to put focus yet.
            (om/set-state! owner :should-focus true)
            (recur)))))

    om/IDidUpdate
    (did-update [_ _ _]
      (when (om/get-state owner :should-focus)
        (doto (om/get-node owner "input")
          .focus)
        (om/set-state! owner :should-focus false)))

    om/IRenderState
    (render-state [_ {:keys [blurs text]}]
      (dom/input #js {:type "color"
                      :ref "input"
                      :className "no-wrapper"
                      :onChange #(handle-change % target schema owner)
                      :onLoad #(js/alert "yeah!")
                      :onBlur #(when blurs
                                 (put! blurs :blurred))

                      ;; [enter]
                      :onKeyDown #(when (= 13 (.-keyCode %))
                                    (.blur (om/get-node owner "input")))
                      :value text
                      :spellCheck false
                      :style #js {:width 40
                                  :height 8
                                  :textAlign "center"
                                  :fontSize 9}}))))
