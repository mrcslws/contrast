(ns contrast.state
  (:require [om.core :as om :include-macros true]))

;; Note: other code currently assumes {} to be the uninitialized value.
(defonce app-state
  (atom {}))

;; Instrumentation lives in a different world.
;; We don't want our components becoming self-referential.
(defonce component-data
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
