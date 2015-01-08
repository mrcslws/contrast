(ns contrast.pixel-probe
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.slider :refer [slider]]
            [contrast.canvas :as cnv]
            [contrast.illusions :as illusions]
            [contrast.layeredcanvas :refer [layered-canvas]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn offset-from-target [evt]
  {:x (- (.-pageX evt)
      (-> evt .-target .getBoundingClientRect .-left))
   :y (- (.-pageY evt)
      (-> evt .-target .getBoundingClientRect .-top))})

;; (defn paint-redline [data owner]
;;   (let [row (-> data :pixel-probe :row)
;;         cnv (om/get-node owner "canvas")
;;         ctx (.getContext cnv "2d")]
;;     (cnv/clear ctx)
;;     (when row
;;       (cnv/fill-rect ctx 0 row (.-width cnv) 1 "red"))))

(defn paint [data owner requests]
  (let [row (-> data :pixel-probe :row)
        col (-> data :pixel-probe :col)
        cnv (om/get-node owner "canvas")
        ctx (.getContext cnv "2d")]
    (when (and row col)
      (let [response (chan)]
        (put! requests [0 0 (:width data) (:height data) response])
        (go
          (let [rows (<! response)
                currentpx (-> rows
                              (nth row)
                              (nth col))]
            (cnv/clear ctx)
            (time (doseq [y (range (count rows))
                          x (range (count (first rows)))
                          ;; Perf-sensitive.
                          ;; `rows` and its children need to support fast-lookup
                          ;; TODO verify once I solve other issues
                          :let [thispx (-> rows (nth y) (nth x))]]
                    (when (= currentpx thispx)
                      (cnv/fill-rect ctx x y 1 1 "blue"))))
            (println "Compared pixels")))))))

;; (defn probe [x y data requests]
;;   (om/update! (:pixel-probe data) :row row)
;;   (when row
;;     (let [response (chan)]
;;       (put! requests [0 row (:width data) 1 response])
;;       (go
;;         (let [pxs (<! response)]
;;           (doseq [r pxs]
;;             (apply println r)))))))

;; (defn probe [row data requests]
;;   (om/update! (:pixel-probe data) :row row)
;;   (when row
;;     (let [response (chan)]
;;       (put! requests [0 row (:width data) 1 response])
;;       (go
;;         (let [pxs (<! response)]
;;           (doseq [r pxs]
;;             (apply println r)))))))

;; A
;;  conjurer
;; creates an
;;  illusion
;; and layers it with a
;;  pixel-probe
;; The illusion monitors a requests channel if provided.
;; It might have layers of its own, or it might not.

;; Should the pixel-probe really be just another layer?
;; I don't think so. I think it should be handed a canvas.
;; Or not? Think of using a trackpad design.

;; The pixel-probe shouldn't automatically overlay the entire provided
;; canvas. The illusion chooses how it responds to requests, and the
;; pixel-probe chooses what requests to send.
;; So the pixel-probe needs to be:
;; - Positioned by the caller
;; - Given a size by the caller
;; - Given an x and y offset for all requests

(defn pixel-probe [data owner {:keys [pixel-requests]}]
  (reify

    om/IDidMount
    (did-mount [_]
      (paint data owner pixel-requests))

    om/IDidUpdate
    (did-update [_ _ _]
      (paint data owner pixel-requests))

    om/IRender
    (render [_]
      (dom/canvas #js {:width (:width data) :height (:height data)
                       ;; TODO better :zIndex
                       :style #js {:position "absolute"
                                   :left 0 :top 0 :zIndex 10}
                       :ref "canvas"
                       :onMouseLeave #(om/transact!
                                      (:pixel-probe data)
                                      (fn [c]
                                        (assoc c
                                          :row nil
                                          :col nil)))
                       :onMouseMove #(om/transact!
                                      (:pixel-probe data)
                                      (fn [c]
                                        (assoc c
                                          :row (-> % offset-from-target :y)
                                          :col (-> % offset-from-target :x))))}
                  ))))

;; (defn pixel-probe [data owner {:keys [pixel-requests]}]
;;   (reify
;;     om/IInitState
;;     (init-state [_]
;;       {:row nil
;;        :highlighted-pixels []})

;;     om/IDidMount
;;     (did-mount [_]
;;       (paint-redline data owner))

;;     om/IDidUpdate
;;     (did-update [_ _ _]
;;       (paint-redline data owner))

;;     om/IRender
;;     (render [_]
;;       (dom/canvas #js {:width (:width data) :height (:height data)
;;                        ;; TODO better :zIndex
;;                        :style #js {:position "absolute"
;;                                    :left 0 :top 0 :zIndex 10}
;;                        :ref "canvas"
;;                        :onMouseLeave #(probe nil data pixel-requests)
;;                        :onMouseMove #(probe (-> % offset-from-target :y)
;;                                             data pixel-requests)}))))
