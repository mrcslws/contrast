(ns contrast.components.easing-picker
  (:require [cljs.core.async :refer [<! put! chan alts! take!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [background-image]]
            [contrast.components.canvas :as cnv]
            [contrast.components.slider :refer [slider]]
            [contrast.drag :as drag]
            [contrast.easing :as easing]
            [contrast.pixel :as pixel]
            [contrast.progress :as progress]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]

            ;; Until Om packages change
            ;; https://github.com/mrcslws/packages/commit/76369523d3040ae05347b3863ef84dfb3f49b5a0
            [cljsjs.react])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(enable-console-print!)

(defn lines-painter [easing x+ y+]
  (let [[xy->plot-xp xy->plot-yp] (progress/xy->plotxy x+ y+)
        {:keys [p1 p2]} easing
        {x1 :x y1 :y} p1
        {x2 :x y2 :y} p2]
    (fn [cnv]
      (let [ctx (.getContext cnv "2d")
            w (.-width cnv)
            h (.-height cnv)]
        (set! (.-strokeStyle ctx) "black")
        (doto ctx
          (cnv/clear)
          (.setLineDash #js [4 6])
          (.beginPath)
          (.moveTo 0 0)
          (.lineTo (-> (xy->plot-xp x1 y1)
                       (progress/p->int 0 (dec w)))
                   (-> (xy->plot-yp x1 y1)
                       (progress/p->int 0 (dec h))))
          (.stroke)
          (.beginPath)
          (.moveTo (dec w) (dec h))
          (.lineTo (-> (xy->plot-xp x2 y2)
                       (progress/p->int 0 (dec w)))
                   (-> (xy->plot-yp x2 y2)
                       (progress/p->int 0 (dec h))))
          (.stroke))))))

(defn idwriter [easing x+ y+]
  (let [[xy->plot-xp xy->plot-yp] (progress/xy->plotxy x+ y+)]
   (fn write-imagedata! [imgdata]
     (let [width (.-width imgdata)
           height (.-height imgdata)
           d (.-data imgdata)]
       (easing/foreach-xy easing width
                          (fn [easing-x easing-y]
                            (let [col (-> (xy->plot-xp easing-x easing-y)
                                          (progress/p->int 0 (dec width)))
                                  row (-> (xy->plot-yp easing-x easing-y)
                                          (progress/p->int 0 (dec height)))
                                  base (pixel/base width col row)]
                              (doto d
                                (aset (+ base 3) 255)))))
       imgdata))))

(defn point-picker-component [point owner]
  (reify
    om/IInitState
    (init-state [_]
      {:mousedown (chan)
       :normalize-factor 1})

    om/IWillMount
    (will-mount [_]
      (let [mousedown (om/get-state owner :mousedown)
            started (chan)
            progress (chan)
            finished (chan)]
        (drag/watch mousedown started progress finished)

        ;; TODO - boundaries.
        ;; TODO - it's silly that I'm working with deltas.
        ;; My API is making me do extra work. Deltas make sense
        ;; for panning, not for repositioning an object.
        (go-loop []
          (when-let [_ (<! started)]
            (let [{:keys [x+ y+]} (om/get-state owner)
                  {startxp :x startyp :y} @point

                  [plot-xy->xp plot-xy->yp] (progress/plotxy->xy x+ y+)

                  ;; This won't be necessary when I get away from deltas.
                  [xy->plot-xp xy->plot-yp] (progress/xy->plotxy x+ y+)
                  plot-xp-start (xy->plot-xp startxp startyp)
                  plot-yp-start (xy->plot-yp startxp startyp)]
              (loop []
                (alt! progress
                      ([[dxpx dypx]]
                         (let [container (om/get-node owner "container")
                               plot-dxp (progress/n->p dxpx
                                                       0 (.-offsetWidth container))
                               plot-dyp (progress/n->p dypx
                                                       0 (.-offsetHeight container))
                               plot-xp (+ plot-xp-start plot-dxp)
                               plot-yp (+ plot-yp-start plot-dyp)]
                           (om/transact! point
                                         #(assoc %
                                            :x (plot-xy->xp plot-xp plot-yp)
                                            :y (plot-xy->yp plot-xp plot-yp))))
                         (recur))

                      finished
                      :cool!

                      started
                      :hmm?)))
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [mousedown x+ y+]}]
      (dom/div #js {:ref "container"
                    :style #js {:display "inline-block"
                                :height "100%"
                                :width "100%"}}
               (let [[plot-xp plot-yp] (progress/xy->plotxy
                                        (:x point) (:y point)
                                        x+ y+)]
                 (dom/div #js {:onMouseDown (fn [e]
                                              (.persist e)
                                              (.preventDefault e)
                                              (put! mousedown e)
                                              nil)
                               :style #js {:cursor "pointer"
                                           :position "absolute"
                                           :left (progress/percent plot-xp)
                                           :top (progress/percent plot-yp)}}
                          (background-image "images/SliderKnob.png" 13 13 -6 -6)))))))

(defn easing-picker-component [easing owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [x+ y+ w h]}]
      (dom/div #js {:style #js {:display "inline-block"
                                :position "relative"
                                :width w
                                :height h
                                :marginTop 10
                                :marginBottom 20
                                :marginLeft 20
                                :borderBottom "1px solid #696969"
                                :borderLeft "1px solid #696969"}}
               (dom/div #js {:style
                             #js {:position "absolute"
                                  :left -20
                                  :width 20
                                  :height h
                                  :font "10px Helvetica, Arial, sans-serif"
                                  :color "#696969"}}
                        (dom/div #js {:style #js {:position "absolute"
                                                  :right 0
                                                  :top 0
                                                  :width 7
                                                  :height 1
                                                  :backgroundColor "black"}})
                        (dom/div #js {:style #js {:position "absolute"
                                                  :top -3
                                                  :right 10}}
                                 "Top")
                        (dom/div #js {:style #js {:position "absolute"
                                                  :right 0

                                                  ;; adjusted to border
                                                  :bottom -1

                                                  :width 7
                                                  :height 1
                                                  :backgroundColor "black"}})
                        (dom/div #js {:style #js {:position "absolute"
                                                  :right 10
                                                  :bottom -5
                                                  :textAlign "center"}}
                                 "Bottom"))
               (dom/div #js {:style
                             #js {:position "absolute"
                                  :bottom -20
                                  :width w
                                  :height 20
                                  :font "10px Helvetica, Arial, sans-serif"
                                  :color "#696969"}}
                        (dom/div #js {:style #js {:position "absolute"

                                                  ;; adjusted to border
                                                  :left -1

                                                  :top 0
                                                  :width 1
                                                  :height 7
                                                  :backgroundColor "black"}})
                        (dom/div #js {:style #js {:position "absolute"
                                                  :top 10
                                                  :left -3}}
                                 "0")
                        (dom/div #js {:style #js {:position "absolute"
                                                  :right 0
                                                  :top 0
                                                  :width 1
                                                  :height 7
                                                  :backgroundColor "black"}})
                        (dom/div #js {:style #js {:position "absolute"
                                                  :top 10
                                                  :textAlign "center"
                                                  :width 40
                                                  :right -20}}
                                 "wave's value"))
               (dom/div #js {:style #js {:position "absolute"
                                         :width "100%"
                                         :height "100%"
                                         :zIndex 2}}
                        (om/build point-picker-component (:p1 easing)
                                  {:state {:x+ x+
                                           :y+ y+}})
                        (om/build point-picker-component (:p2 easing)
                                  {:state {:x+ x+
                                           :y+ y+}}))

               (dom/div #js {:style #js {:position "absolute"
                                         :width "100%"
                                         :height "100%"
                                         :zIndex 1}}
                        (cnv/canvas easing w h
                                    (lines-painter easing x+ y+)))
               (dom/div #js {:style #js {:position "relative"
                                         :zIndex 0}}
                        (cnv/canvas easing w h
                                    (cnv/idwriter->painter
                                     (idwriter easing x+ y+))))))))
