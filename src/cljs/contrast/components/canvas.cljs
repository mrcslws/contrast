(ns contrast.components.canvas
  (:require [cljs.core.async :refer [put! chan <!]]
            [contrast.common :refer [progress]]
            [contrast.pixel :as pixel]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn clear [ctx]
  (.save ctx)
  (.setTransform ctx 1 0 0 1 0 0)
  (.clearRect ctx 0 0 (.. ctx -canvas -width) (.. ctx -canvas -height))
  (.restore ctx))

(defn fill-rect [ctx x y width height fill]
  (set! (.-fillStyle ctx) fill)
  (.fillRect ctx x y width height))

(defn canvas-component [_ owner {:keys [paint]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "canvas")

    om/IDidMount
    (did-mount [_]
      (paint (om/get-node owner "canvas")))

    om/IDidUpdate
    (did-update [_ _ _]
      (paint (om/get-node owner "canvas")))

    om/IRenderState
    (render-state [_ {:keys [width height]}]
      (dom/canvas #js {:ref "canvas" :width width :height height
                       :style
                       #js {;; Without this, height is added to the containing
                            ;; to make room for descenders.
                            ;; TODO I hate that this is here.
                            :verticalAlign "top"}}))))

(defn canvas [canary width height paint]
  (om/build canvas-component canary
               {:state {:width width :height height}
                :opts {:paint paint}}))

(defn add-target [owner write-imagedata!]
  (let [{:keys [width height duration]} (om/get-state owner)
        now (js/Date.now)
        new-target [(-> (om/get-node owner "canvas")
                        (.getContext "2d")
                        (.createImageData width height)
                        write-imagedata!)
                    (+ now duration)
                    false]]
    (om/update-state! owner :targets
                      (fn [targets]
                        (-> (mapv
                             (fn [[id prev-finish-time dying?
                                   :as unchanged]]
                               (if dying?
                                 unchanged
                                 (let [since-start (- now
                                                      (- prev-finish-time
                                                         duration))
                                       this-duration (min since-start
                                                          duration)]
                                   [id (+ now this-duration) true])))

                             targets)
                            (conj new-target))))))

(defn on-update [owner]
  (let [{:keys [width height duration]} (om/get-state owner)
        now (js/Date.now)]

    (let [[new-targets opacities in-progress?]
          (reduce (fn [[targets opacities in-progress? :as sofar]
                       [imagedata finish-time dying? :as target]]
                    (let [progress (-> now
                                       (- finish-time)
                                       (/ (om/get-state owner :duration))
                                       inc
                                       (min 1)
                                       (max 0))]
                      (if (and dying?
                               (= progress 1))
                        sofar
                        [(conj targets target)
                         (conj opacities (if dying?
                                           (- 1 progress)
                                           progress))
                         (or in-progress? (not= progress 1))])))
                  [[] [] false]
                  (om/get-state owner :targets))
          ctx (-> (om/get-node owner "canvas")
                  (.getContext "2d"))
          imagedata (.createImageData ctx width height)]
      (om/set-state-nr! owner :targets new-targets)
      (when in-progress?
        (put! (om/get-state owner :queue-render) :go))

      (let [aimgdata (->> new-targets
                          (map first)
                          vec
                          clj->js)
            aops (clj->js opacities)
            c (count opacities)
            finald (.-data imagedata)]
        (dotimes [row height]
          (dotimes [col width]
            (let [base (pixel/base width col row)]
              (loop [rs 0
                     gs 0
                     bs 0
                     i 0
                     ;; `r` `g` `b` use a weighted average, using the opacities
                     ;; as weights.
                     ;; `a` is set to the average opacity, but using only pixels
                     ;; that had a color.
                     os 0
                     oc 0]
                (if (< i c)
                  (let [data (.-data (aget aimgdata i))
                        a (aget data (+ base 3))]
                    (if (zero? a)
                      (recur rs gs bs (inc i) os oc)
                      (let [opacity (aget aops i)
                            r (aget data base)
                            g (aget data (+ base 1))
                            b (aget data (+ base 2))]
                        (recur (+ rs (* r opacity))
                               (+ gs (* g opacity))
                               (+ bs (* b opacity))
                               (inc i)
                               (+ os opacity)
                               (inc oc)))))
                  (when (pos? os)
                    (doto finald
                      (aset base (js/Math.round (/ rs os)))
                      (aset (+ base 1) (js/Math.round (/ gs os)))
                      (aset (+ base 2) (js/Math.round (/ bs os)))
                      (aset (+ base 3)
                            (js/Math.round
                             (-> (/ os oc) (* 255) js/Math.round)))))))))))
      (.putImageData ctx imagedata 0 0))))

(defn fading-canvas-component [canary owner {:keys [write-imagedata!]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "fading-canvas")

    om/IInitState
    (init-state [_]
      {;; [[imagedata finish-time dying?] ...]
       :targets []
       :queue-render (chan)})

    om/IWillMount
    (will-mount [_]
      (go-loop []
        (<! (om/get-state owner :queue-render))
        ;; `om/refresh!` doesn't work when called from `did-update`.
        ;; The current render is in the render-queue, so queueing a render
        ;; does nothing.
        (om/refresh! owner)
        (recur)))

    om/IWillReceiveProps
    (will-receive-props [_ _]
      (add-target owner write-imagedata!))

    om/IDidUpdate
    (did-update [_ _ _]
      (on-update owner))

    om/IDidMount
    (did-mount [this]
      (add-target owner write-imagedata!))

    om/IRenderState
    (render-state [_ {:keys [width height]}]
      (dom/canvas #js {:ref "canvas" :width width :height height
                       :style
                       #js {;; Without this, height is added to the containing
                            ;; to make room for descenders.
                            ;; TODO I hate that this is here.
                            :verticalAlign "top"}}))))

(defn fading-canvas [canary width height write-imagedata! duration]
  (om/build fading-canvas-component canary
            {:state {:width width :height height :duration duration}
             :opts {:write-imagedata! write-imagedata!}}))

(defn idwriter->painter [write-imagedata!]
  (fn [cnv]
    (let [ctx (.getContext cnv "2d")
          id (.createImageData ctx (.-width cnv) (.-height cnv))]
      (.putImageData ctx (write-imagedata! id) 0 0))))

(defn solid-vertical-stripe-idwriter [col->rgb]
  (fn [imagedata]
    (let [width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)]
      (dotimes [col width]
        (let [[r g b] (col->rgb col)]
          (dotimes [row height]
            (let [base (pixel/base width col row)]
              (doto d
                (aset base r)
                (aset (+ base 1) g)
                (aset (+ base 2) b)
                (aset (+ base 3) 255))))))
      imagedata)))

(defn gradient-vertical-stripe-idwriter [col->topcolor col->bottomcolor]
  (fn [imagedata]
    (let [width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)]
      (dotimes [col width]
        (let [[r1 g1 b1] (col->topcolor col)
              [r2 g2 b2] (col->bottomcolor col)]
          (dotimes [row height]
            (let [p (/ row height)
                  base (pixel/base width col row)]
              (doto d
                (aset base (progress r1 r2 p))
                (aset (+ base 1) (progress g1 g2 p))
                (aset (+ base 2) (progress b1 b2 p))
                (aset (+ base 3) 255))))))
      imagedata)))
