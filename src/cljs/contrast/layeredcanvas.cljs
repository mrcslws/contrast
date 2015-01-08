(ns contrast.layeredcanvas
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan mult tap close! <!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

;; Okay, how do these pixel requests work?
;; The `layer` and the `layered-canvas` will handle most of it.
;; The caller will create a requests channel.
;; If the `layered-canvas` is given a `requests`, then it will create one
;; for each layer.
;; The `pixel-probe` will put in a request [x y w h response].
;; The `layered-canvas` will reduce over the layers, putting a similar
;; request into each.
;; Another `layered-canvas` may receive this request.
;; ...
;; The `layer` will receive this request. It grabs the pixels, formats them
;; as expected, and sends them as a response.
;; The `layered-canvas` combines all results, taking z and alpha values
;; into consideration.

(defn layer [data owner {:keys [fpaint style width height pixel-requests]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (when pixel-requests
        (go-loop []
          (let [[x y w h response] (<! pixel-requests)]
            (put! response (-> (om/get-node owner "canvas")
                               (.getContext "2d")
                               (.getImageData x y w h)
                               .-data
                               array-seq)))
          (recur))))

    om/IDidMount
    (did-mount [_]
      (fpaint data (om/get-node owner "canvas")))

    om/IDidUpdate
    (did-update [_ _ _]
      (fpaint data (om/get-node owner "canvas")))

    om/IRender
    (render [_]
      (dom/canvas #js {:ref "canvas"
                       :width width :height height
                       :style (clj->js style)}))))

(defn weighted-average [a wa b wb]
  ;; TODO stop the sneaky rounding
  (let [sum (+ wa wb)]
    (js/Math.round (+ (* a (/ wa sum))
                      (* b (/ wb sum))))))

;; TODO - Expose access to pixels (probably using a channel-based API)
(defn layered-canvas [data owner {:keys [layers width height pixel-requests]}]
  (reify
    om/IInitState
    (init-state [_]
      (when pixel-requests
        {:child-requests (take (count layers) (repeatedly #(chan)))}))

    om/IWillMount
    (will-mount [_]
      (when pixel-requests
        (go-loop []
          (let [child-requests (om/get-state owner :child-requests)
                [x y w h pixel-response] (<! pixel-requests)]
            ;; TODO call in order of zIndex

            ;; V1 - Assume nothing is offset.
            ;; Afterward, write the reduce function that you wish you had.

            ;; This code is terrible. I could write a core.async-compatible `reduce`.
            ;; Or I could explore channels more and find if there's a way to make
            ;; them do what I want.

            ;; First order of business is perf, though.

            (loop [child-index 0
                   pixels nil]
              (if-not (< child-index (count child-requests))
                (when pixels
                  (put! pixel-response (persistent! pixels)))
                (let [child (nth child-requests child-index)
                      response (chan)]
                  (put! child [x y w h response])
                  (let [foreground (<! response)]
                    (recur
                     (inc child-index)
                     (if-not pixels
                       (transient (vec foreground))
                       (loop [pxi 0
                              pixels pixels]
                         (if-not (< pxi (count pixels))
                           pixels
                           (recur
                            (+ pxi 4)
                            (let [fa (nth foreground (+ pxi 3))]
                              (if (= fa 0)
                                pixels
                                (let [ba (nth pixels (+ pxi 3))
                                      fweight fa
                                      bweight (- 255 fa)
                                      fopacity (/ fa 255)
                                      bopacity (/ ba 255)
                                      ftransparency (- 1 fopacity)
                                      btransparency (- 1 bopacity)
                                      final-transparency (* ftransparency
                                                            btransparency)
                                      final-opacity (- 1 final-transparency)]
                                  ;; Consider optimizing the case where the foreground is 255a
                                  (assoc! pixels
                                          pxi
                                          (weighted-average (nth pixels pxi) bweight
                                                            (nth foreground pxi) fweight)
                                          (+ pxi 1)
                                          (weighted-average (nth pixels (+ pxi 1)) bweight
                                                            (nth foreground (+ pxi 1)) fweight)
                                          (+ pxi 2)
                                          (weighted-average (nth pixels (+ pxi 2)) bweight
                                                            (nth foreground (+ pxi 2)) fweight)
                                          (+ pxi 3)
                                          (* final-opacity 255)))))))))))))))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [child-requests]}]
      (apply dom/div #js {:style #js {:width width :height height
                                      :position "relative"}}
             (for [i (range (count layers))
                   :let [lconfig (nth layers i)]]
               (if (associative? lconfig)
                 (let [{:keys [fpaint fdata left top additional z-index]}
                       lconfig
                       width (or (:width lconfig) width)
                       height (or (:height lconfig) height)
                       left (or left 0)
                       top (or top 0)
                       z-index (or z-index i)]
                   (om/build layer (fdata data)
                             {:opts {:pixel-requests (nth child-requests i)
                                     :fpaint fpaint
                                     :width width :height height
                                     :additional additional
                                     :style {:position "absolute"
                                             :left left :top top
                                             :zIndex i}}}))
                 lconfig))))))
