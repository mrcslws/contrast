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

(defn overlay [[br bg bb ba] [fr fg fb fa]]
  (let [fopacity (/ fa 255)
        bopacity (/ ba 255)
        ftransparency (- 1 fopacity)
        btransparency (- 1 bopacity)
        final-opacity (- 1 (* ftransparency btransparency))]
    [(js/Math.round (+ (* fr fopacity) (* br ftransparency)))
     (js/Math.round (+ (* fg fopacity) (* bg ftransparency)))
     (js/Math.round (+ (* fb fopacity) (* bb ftransparency)))
     (js/Math.round (* final-opacity 255))]))

(defn nth-pixel [pixels n]
  (subvec pixels (* 4 n) (* 4 (inc n))))

(defn pixel-count [pixels]
  (quot (count pixels) 4))

(defn assoc-pixel! [pixels pxi [r g b a]]
  (let [i (* 4 pxi)]
    (assoc! pixels
            i r
            (+ i 1) g
            (+ i 2) b
            (+ i 3) a)))

(defn transparent? [px]
  (zero? (nth px 3)))

(defn overlay-pixel! [foreground victim pxi]
  (let [fpx (nth-pixel foreground pxi)]
    (if (transparent? fpx)
      victim
      (assoc-pixel! victim pxi
                    (overlay (nth-pixel
                              victim pxi)
                             fpx)))))

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

            ;; I could write a core.async-compatible `reduce`.
            ;; Or I could explore channels more and find if there's a way to make
            ;; them do what I want.

            ;; First order of business is perf, though.
            ;; Though I'll clean up stuff that'll remain useful regardless of data structure choice.

            (time
             ;; Use `loop` rather than `reduce` because the reducing function
             ;; consumes a channel.

             (put! pixel-response
                   (loop [pixels (transient [])
                          remaining-children child-requests]
                     (when (not-empty remaining-children)
                       (recur (let [response (chan)]
                                (put! (first remaining-children) [x y w h response])
                                (let [foreground (<! response)]
                                  (if (zero? (count pixels))
                                    (reduce conj! pixels foreground)
                                    (reduce (partial overlay-pixel! foreground)
                                            pixels (range (pixel-count pixels))))))
                              (rest remaining-children)))
                     (persistent! pixels))))
            (println "Finished combining layers"))
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
