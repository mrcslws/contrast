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
            (time (let [imagedata (time (-> (om/get-node owner "canvas")
                                       (.getContext "2d")
                                       (.getImageData x y w h)
                                       .-data ))]
                    (println "Grabbed imagedata")
                    (let [rgbas (array-seq imagedata)
                          ;; Consumers will appreciate it if you give them a
                          ;; fast-lookup data structure
                          pixels (vec (partition 4 rgbas))
                          rows (vec (partition w pixels))]
                      (put! response rows))))
            (println "Gathered a layer's pixels"))
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
            ;; Must go from back to front.

            ;; Here I want to reduce over the bitmaps.
            ;; V1 - Assume nothing is offset.
            ;; The reducing function should grab the pixels for one layer,
            ;; then add it to the background.
            ;; Put a value on channel 1a. Retrieve a value from channel 1b.
            ;; Return the background.
            ;; Put a value on channel 2a. Retrieve a value from channel 2b.
            ;; Return the background.
            ;; ...
            ;; So how do you `reduce` in a channel world?
            ;; I have a vector of channels.
            ;; You could just `doseq` and use an atom.
            ;; Afterward, write the reduce function that you wish you had.

            (let [background (atom nil)]
              (time (doseq [child child-requests
                       :let [response (chan)]]
                 (put! child [x y w h response])

                 (let [foreground (<! response)]
                   (time (swap! background
                                (fn [background]
                                  ;; Consumers will appreciate it if you give them a
                                  ;; fast-lookup data structure
                                  ;; TODO verify that the vecs truly are necessary
                                  (if (not background)
                                    foreground
                                    (vec
                                     (for [y (range (max (count background)
                                                         (count foreground)))]
                                       (vec
                                        (for [x (range (max (count (first background))
                                                            (count (first foreground))))
                                              :let [[fr fg fb fa :as f] (-> foreground (nth y) (nth x))
                                                    [br bg bb ba :as b] (when-not (= fa 255)
                                                                          (-> background (nth y) (nth x)))]]
                                          (cond
                                           (and b f)
                                           (let [fopacity (/ fa 255)
                                                 bopacity (/ ba 255)
                                                 ftransparency (- 1 fopacity)
                                                 btransparency (- 1 bopacity)
                                                 final-transparency (* ftransparency
                                                                       btransparency)
                                                 final-opacity (- 1 final-transparency)
                                                 fweight fopacity
                                                 bweight (- 1 fopacity)]
                                             [(+ (* fr fweight) (* br bweight))
                                              (+ (* fg fweight) (* bg bweight))
                                              (+ (* fb fweight) (* bb bweight))
                                              (* final-opacity 255)])

                                           (or b f)
                                           (or b f)

                                           :else
                                           (throw :wtf))))))))))
                   (println "Swapped background"))))
              (println "Finished calculating pixels")
              (put! pixel-response @background)))
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
