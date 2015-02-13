(ns contrast.canvas-inspectors
  (:refer-clojure :exclude [comp])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.row-probe :as row-probe]
            [contrast.components.row-display :as row-display]
            [contrast.components.color-exposer :as color-exposer]
            [contrast.components.eyedropper-zone :as eyedropper-zone]
            [contrast.components.chan-handlers :refer [chan-genrender]]))

;; TODO with ref-cursors I think I can remove the canary.
(defn inspected [awaiting-chan canary inspector]
  (chan-genrender (fn [channel imgdata]
                    (cond-> (awaiting-chan channel)
                            inspector (inspector imgdata)))
                  canary))

(defn compable [f]
  (fn [v]
    (apply f v)))

(defn share-input [f]
  (fn [a b]
    [(f a b) b]))

;; Takes a set of reducing functions
;;  (fn a [r i])
;;  (fn b [r i])
;; and returns the equivalent of
;;  (fn [r i]
;;    (b (a r i) i))
(defn comp [& fns]
  (fn [r i]
    (let [compd (->> fns
                     (map (clojure.core/comp compable share-input))
                     (apply clojure.core/comp))]
      (first (compd [r i])))))

;; I've coerced these to look like reducing functions.
;;   ReactElement, ImageData -> ReactElement
;; But the resemblance is superficial. Really, it's more like
;;   ReactElement, ReactElement internals -> ReactElement
;; so the second arg isn't so much of an input as it is part of the first arg.

(defn color-exposer [k]
  (fn [r imgdata]
    (color-exposer/color-exposer k imgdata r)))

(defn eyedropper-zone [k]
  (fn [r imgdata]
    (eyedropper-zone/eyedropper-zone k {:key :selected-color} imgdata r)))

(defn row-probe [k]
  (fn [r _]
    (row-probe/row-probe k {:key :probed-row} {:track-border-only? true} r)))

(defn row-display [k]
  (fn [r imgdata]
    (dom/div nil
             r
             (dom/div #js {:className "testing"
                           :style #js {:marginTop 20
                                       :display "inline-block"
                                       :position "relative"
                                       :left -3
                                       :borderLeft "3px solid red"
                                       :borderRight "3px solid red"}}
                      (inspected (partial row-display/row-display k imgdata)
                                 [k imgdata]
                                 (comp (eyedropper-zone k)
                                       (color-exposer k)))))))
