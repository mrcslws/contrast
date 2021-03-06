(ns contrast.components.spectrum-picker
  (:require [cljs.core.async :refer [<! put! chan]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [rgb->hexcode trace-rets]]
            [contrast.components.canvas :as cnv :refer [canvas-component]]
            [contrast.components.chan-handlers :refer [chan-genrender]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [contrast.drag :as drag]
            [contrast.spectrum :as spectrum]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]

            ;; Until Om packages change
            ;; https://github.com/mrcslws/packages/commit/76369523d3040ae05347b3863ef84dfb3f49b5a0
            [cljsjs.react])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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
      {:pointerdown (chan)
       :locked-position nil
       :invokes (chan)
       :blurs (chan)})

    om/IWillMount
    (will-mount [_]
      (let [{:keys [target-width pointerdown]} (om/get-state owner)
            started (chan)
            progress (chan)
            finished (chan)]
        (drag/watch pointerdown started progress finished)

        (go-loop []
          (when-let [_ (<! started)]
            (om/set-state! owner :locked-position (:position @knob))
            (recur)))

        (go-loop []
          (when-let [[dxp _] (drag/delta (<! progress))]
            (om/update! knob :position
                        (-> (+ (om/get-state owner :locked-position)
                               (* 2 (/ dxp target-width)))
                            (min 1)
                            (max -1)))
            (recur)))

        (go-loop []
          (when-let [_ (<! finished)]
            (om/set-state! owner :locked-position nil)
            (recur))))

      (go-loop []
        (when-let [_ (<! (om/get-state owner :blurs))]
          (om/set-state! owner :focused false)
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [target-width pointerdown focused invokes blurs]}]
      (let [knobw (if focused 46 12)]
        (dom/div #js {:style #js {:position "absolute"
                                  :left (knobpos->left
                                         (:position knob) target-width)}}
                 (dom/div #js {:onMouseDown (fn [e]
                                              (.persist e)
                                              (.preventDefault e)
                                              (put! pointerdown e)
                                              nil)
                               :onTouchStart (fn [e]
                                               (.persist e)
                                               (.preventDefault e)
                                               (put! pointerdown (-> e
                                                                   .-touches
                                                                   (aget 0)))
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

    om/IRenderState
    (render-state [_ {:keys [width]}]
      (dom/div #js {:style #js {:position "relative"
                                :width width
                                :marginRight 12
                                :marginBottom 20
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
                                           {:state {:target-width width}})
                                 (om/build color-knob-component (:right
                                                                 spectrum)
                                           {:state {:target-width width}}))
                        (let [idwriter (cnv/solid-vertical-stripe-idwriter
                                        ;; col -> [-1 1]
                                        #(dec (* 2 (/ % width)))
                                        ;; [-1 1] -> color
                                        spectrum)
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
