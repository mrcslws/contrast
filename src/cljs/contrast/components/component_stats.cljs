(ns contrast.components.component-stats
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn component-stats-component [stats owner]
  (reify
    om/IRender
    (render [_])))
