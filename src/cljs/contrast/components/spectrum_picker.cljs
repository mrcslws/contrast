(ns contrast.components.spectrum-picker
  (:require [cljs.core.async :refer [<! put! chan alts! take!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [progress spectrum-dictionary rgb->hexcode trace-rets]]
            [contrast.components.canvas :as cnv :refer [canvas-component]]
            [contrast.components.chan-handlers :refer [chan-genrender]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [goog.events :as events]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]
                   [contrast.macros :refer [drain!]]))

(defn listen [el type]
  (let [port (chan)
        eventkey (events/listen el type #(put! port %1))]
    [eventkey port]))

(defn watch [mousedown start progress finished coalesce-timeout]
  (go-loop []
    (when-let [downevt (<! mousedown)]
      (when (= (.-button downevt) 0)
        (put! start :started)

        (let [[kmousemove moves] (listen js/window "mousemove")
              [kmouseup ups] (listen js/window "mouseup")]
          (loop []
            (let [[evt port] (alts! [moves ups])
                  d [(- (.-clientX evt)
                        (.-clientX downevt))
                     (- (.-clientY evt)
                        (.-clientY downevt))]]
              (if (= port moves)
                (do
                  (put! progress d)
                  (recur))
                (do
                  (put! finished d)
                  (events/unlistenByKey kmousemove)
                  (events/unlistenByKey kmouseup))))))

        ;; In obscure cases (e.g. javascript breakpoints)
        ;; there are stale mousedowns sitting in the queue.
        (drain! mousedown))
      (recur))))

(defn knobpos->left [v w]
  (-> v
      inc
      (/ 2)
      (* w)
      js/Math.round))

(defn color-knob-component [knob owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "color-knob")

    om/IInitState
    (init-state [_]
      {:mousedown (chan)
       :locked-position nil
       :invokes (chan)
       :blurs (chan)})

    om/IWillMount
    (will-mount [_]
      (let [{:keys [target-width mousedown]} (om/get-state owner)
            started (chan)
            progress (chan)
            finished (chan)]
        (watch mousedown started progress finished nil)

        (go-loop []
          (when-let [_ (<! started)]
            (om/set-state! owner :locked-position (:position @knob))
            (when-let [listener (om/get-state owner :listener)]
              (put! listener :started))
            (recur)))

        (go-loop []
          (when-let [[dxp _] (<! progress)]
            (om/update! knob :position
                        (-> (+ (om/get-state owner :locked-position)
                               (* 2 (/ dxp target-width)))
                            (min 1)
                            (max -1)))
            (recur)))

        (go-loop []
          (when-let [_ (<! finished)]
            (om/set-state! owner :locked-position nil)
            (when-let [listener (om/get-state owner :listener)]
              (put! listener :finished))
            (recur))))

      (go-loop []
        (when-let [_ (<! (om/get-state owner :blurs))]
          (om/set-state! owner :focused false)
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [target-width mousedown focused invokes blurs]}]
      (let [knobw (if focused 46 12)]
        (dom/div #js {:style #js {:position "absolute"
                                  :left (knobpos->left
                                         (:position knob) target-width)}}
                 (dom/div #js {:onMouseDown (fn [e]
                                              (.persist e)
                                              (.preventDefault e)
                                              (put! mousedown e)
                                              nil)
                               :onClick (fn [e]
                                          (when-not focused
                                            (om/set-state! owner :focused true)
                                            (put! invokes :invoked)))
                               :style #js {:position "absolute"
                                           :width knobw
                                           :height 18

                                           :transitionProperty "all"
                                           :transitionDuration "0.2s"

                                           :left (-> knobw (/ 2) -)
                                           :cursor "pointer"}}
                          (dom/div #js {:style
                                        #js {:position "absolute"
                                             :height 0
                                             :width 0

                                             :transitionProperty "all"
                                             :transitionDuration "0.2s"
                                             ;; CSS triangle
                                             :borderLeftStyle "solid"
                                             :borderLeftColor "transparent"
                                             :borderRightStyle "solid"
                                             :borderRightColor "transparent"
                                             :borderBottomStyle "solid"
                                             :borderBottomColor "black"

                                             :borderLeftWidth (if focused 23 6)
                                             :borderRightWidth (if focused 23 6)
                                             :borderBottomWidth 10

                                             :top 0}})
                          (dom/div #js {:style
                                        #js {:position "absolute"
                                             :height (if focused 18 4)
                                             :width (if focused knobw 8)
                                             :overflow "hidden"

                                             :transitionProperty "all"
                                             :transitionDuration "0.2s"


                                             :borderRadius 3
                                             :backgroundColor (rgb->hexcode
                                                               (:color knob))
                                             :border (if focused
                                                       0 "2px solid black")
                                             :top 8}}
                                   (dom/div #js {:style #js {:textAlign "center"
                                                             :visibility
                                                             (when-not focused
                                                               "hidden")}}
                                            (om/build color-picker-component
                                                      {:target knob
                                                       :schema {:key :color}}
                                                      {:init-state
                                                       {:invokes invokes
                                                        :blurs blurs}})))))))))

(def canvash 30)

(defn spectrum-picker-component [spectrum owner {:keys [canvas-spec-transform]}]
  (reify
    om/IDisplayName
    (display-name [_]
      "spectrum-picker")

    om/IInitState
    (init-state [_]
      {:knob-actions (chan)
       :dragging false})

    om/IWillMount
    (will-mount [_]
      (go-loop []
        (when-let [action (<! (om/get-state owner :knob-actions))]
          (case action
            :started (om/set-state! owner :dragging true)
            :finished (om/set-state! owner :dragging false))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [width knob-actions dragging]}]
      (dom/div #js {:style #js {:position "relative"
                                :width width
                                :marginRight 12
                                :height (+ canvash 30)}}
               (dom/div #js {:style
                             #js {:position "absolute"
                                  :top 0
                                  :width width
                                  :height 20
                                  :font "10px Helvetica, Arial, sans-serif"
                                  :color "#696969"}}
                        (dom/div #js {:style #js {:position "absolute"
                                                  :top 2
                                                  :left -3}}
                                 "-1")
                        (dom/div #js {:style #js {:position "absolute"
                                                  :left 0
                                                  :bottom 0
                                                  :width 1
                                                  :height 7
                                                  :backgroundColor "black"}})
                        (dom/div #js {:style #js {:position "absolute"
                                                  :top 2
                                                  :left (-> width
                                                            (/ 2)
                                                            js/Math.round
                                                            (- 2))}}
                                 "0")
                        (dom/div #js {:style #js {:position "absolute"
                                                  :left (-> width
                                                            (/ 2)
                                                            js/Math.round)
                                                  :bottom 0
                                                  :width 1
                                                  :height 7
                                                  :backgroundColor "black"}})
                        (dom/div #js {:style #js {:position "absolute"
                                                  :top 2
                                                  :right -2}}
                                 "1")
                        (dom/div #js {:style #js {:position "absolute"
                                                  :right 0
                                                  :bottom 0
                                                  :width 1
                                                  :height 7
                                                  :backgroundColor "black"}}))
               (dom/div #js {:style #js {:position "absolute"
                                         :top 20}}
                        (dom/div #js {:style
                                      #js {:position "absolute"
                                           :top canvash}}
                                 (om/build color-knob-component
                                           (:left spectrum)
                                           {:init-state {:listener
                                                         knob-actions}
                                            :state {:target-width width}}))
                        (dom/div #js {:style
                                      #js {:position "absolute"
                                           :top canvash}}
                                 (om/build color-knob-component (:right
                                                                 spectrum)
                                           {:init-state {:listener
                                                         knob-actions}
                                            :state {:target-width width}}))
                        (let [idwriter (cnv/solid-vertical-stripe-idwriter
                                        (comp
                                         ;; [-1 1] -> color
                                         (spectrum-dictionary spectrum)

                                         ;; col -> [-1 1]
                                         #(dec (* 2 (/ % width)))))
                              awaiting-fpaint (fn [p]
                                                {:f canvas-component
                                                 :props spectrum
                                                 :m {:state {:width width
                                                             :height canvash}
                                                     :opts {:paint p}}})]
                          (if canvas-spec-transform
                            (chan-genrender
                             (fn [channel imgdata]
                               (-> (awaiting-fpaint (cnv/idwriter->painter
                                                     (trace-rets idwriter
                                                                 channel)))
                                   (canvas-spec-transform imgdata)
                                   spec/render))
                             spectrum)
                            (-> (awaiting-fpaint (cnv/idwriter->painter
                                                  idwriter))
                                spec/render))))))))

(defn spectrum-picker-spec
  ([spectrum width]
     (spectrum-picker-spec spectrum width nil))
  ([spectrum width canvas-spec-transform]
     (om/build spectrum-picker-component spectrum
               {:init-state {:width width}
                :opts {:canvas-spec-transform canvas-spec-transform}})))
