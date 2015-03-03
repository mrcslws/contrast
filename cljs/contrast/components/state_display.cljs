(ns contrast.components.state-display
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn state->html [s]
  (if (map? s)
    (dom/div nil
             (dom/div #js {:style #js {:display "inline-block"
                                       :height "100%"
                                       :verticalAlign "top"}}
                      "{")
             (apply dom/div #js {:style #js {:display "inline-block"}}
                    (for [[k v] s]
                      (dom/div nil
                               (dom/div #js {:style #js {:verticalAlign "top"
                                                         :display "inline-block"}}
                                        (pr-str k))
                               (dom/div #js {:style #js {:marginLeft 6
                                                         :display "inline-block"}}
                                        (state->html v)))))
             (dom/div #js {:style #js {:display "inline-block"
                                       :height "100%"
                                       :verticalAlign "bottom"}}
                      "}"))
    (dom/div nil (pr-str s))))

(defn state-display-component [state owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "state-display")

    om/IRender
    (render [_]
      (dom/div #js {:style #js {:position "fixed"
                                :height "30%"
                                :overflow "auto"
                                :bottom 0
                                :left 0
                                :right 0
                                :backgroundColor "rgba(0,0,0,0.8)"}}
               (dom/div #js {:style #js {:font "11px Helvetica, Arial, sans-serif"
                                         :fontWeight "bold"
                                         :color "white"}}
                        (state->html state))))))
