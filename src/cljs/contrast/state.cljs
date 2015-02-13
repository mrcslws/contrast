(ns contrast.state
  (:require [om.core :as om :include-macros true]))

(defonce app-state
  (atom {}))

(defn ensure-path! [a ks]
  (when-not (get-in @a ks)
    (swap! a assoc-in ks {})))

(defn get-inspector [inspectork figurek]
  (let [p [:inspectors figurek inspectork]]
    (ensure-path! app-state p)
    (-> (om/root-cursor app-state)
        (get-in p)
        om/ref-cursor)))

(def color-inspect (partial get-inspector :color-inspect))
(def row-inspect (partial get-inspector :row-inspect))

(defn figure [k]
  (-> (om/root-cursor app-state)
      (get-in [:figures k])
      om/ref-cursor))
