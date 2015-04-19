(ns contrast.components.easing-picker
  (:require [cljs.core.async :refer [<! put! chan alts! take!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [background-image]]
            [contrast.components.canvas :as cnv]
            [contrast.components.slider :refer [slider]]
            [contrast.dom :as domh]
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

(defn lines-painter [easing x+ y+]
  (let [[xy->plot-xp xy->plot-yp] (progress/xy->plotxy x+ y+)
        {:keys [p1 p2]} easing
        {x1 :x y1 :y} p1
        {x2 :x y2 :y} p2]
    (fn paint-lines [cnv]
      (let [ctx (.getContext cnv "2d")
            w (.-width cnv)
            h (.-height cnv)]
        (set! (.-strokeStyle ctx) "black")
        (doto ctx
          (cnv/clear)
          (.setLineDash #js [4 6])
          (.beginPath)
          (.moveTo (-> (xy->plot-xp 0 0)
                       (progress/p->int 0 (dec w)))
                   (-> (xy->plot-yp 0 0)
                       (progress/p->int 0 (dec h))))
          (.lineTo (-> (xy->plot-xp x1 y1)
                       (progress/p->int 0 (dec w)))
                   (-> (xy->plot-yp x1 y1)
                       (progress/p->int 0 (dec h))))
          (.stroke)
          (.beginPath)
          (.moveTo (-> (xy->plot-xp 1 1)
                       (progress/p->int 0 (dec w)))
                   (-> (xy->plot-yp 1 1)
                       (progress/p->int 0 (dec h))))
          (.lineTo (-> (xy->plot-xp x2 y2)
                       (progress/p->int 0 (dec w)))
                   (-> (xy->plot-yp x2 y2)
                       (progress/p->int 0 (dec h))))
          (.stroke))))))

(defn easing-idwriter [easing x+ y+]
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

        (go-loop []
          (when-let [_ (<! started)]
            (let [{:keys [x+ y+ w h]} (om/get-state owner)
                  [plot-xy->xp plot-xy->yp] (progress/plotxy->xy x+ y+)]
              (loop []
                (alt! progress
                      ([[evt]]
                         (let [topleft (om/get-node owner "topleft")
                               {plot-xpx :x plot-ypx :y} (domh/offset-from
                                                          evt topleft)
                               plot-xp (progress/n->p plot-xpx 0 w)
                               plot-yp (progress/n->p plot-ypx 0 h)]
                           (om/transact! point
                                         #(assoc %
                                            :x (-> (plot-xy->xp plot-xp plot-yp)
                                                   (max 0)
                                                   (min 1))
                                            :y (-> (plot-xy->yp plot-xp plot-yp)
                                                   (max 0)
                                                   (min 1)))))
                         (recur))

                      finished
                      :cool!

                      started
                      :hmm?)))
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [mousedown x+ y+ w h]}]
      (dom/div
       nil
       (dom/div #js {:ref "topleft"
                     :style #js {:position "absolute"
                                 :width 0
                                 :height 0
                                 :top 0
                                 :left 0}})
       (let [[plot-xp plot-yp] (progress/xy->plotxy
                                (:x point) (:y point)
                                x+ y+)]
         (dom/div #js {:onMouseDown (fn [e]
                                      (.persist e)
                                      (.preventDefault e)
                                      (put! mousedown e)
                                      nil)
                       :onTouchStart (fn [e]
                                       (.persist e)
                                       (.preventDefault e)
                                       (put! mousedown (-> e
                                                           .-touches
                                                           (aget 0)))
                                       nil)
                       :style #js {:cursor "pointer"
                                   :position "absolute"
                                   :padding 15
                                   :margin -15
                                   :left (progress/p->int plot-xp 0 (dec w))
                                   :top (progress/p->int plot-yp 0 (dec h))}}
                  (background-image "images/SliderKnob.png" 13 13 8 8)))))))

;; TODO These axes need to stop implying a (0,0).
;; Move them outward.

(defn easing-display-component [easing owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [x+ y+ w h]}]
      (cnv/canvas easing w h
                  (cnv/idwriter->painter
                   (easing-idwriter easing x+ y+))))))

(defn easing-picker-component [easing owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [x+ y+ w h]}]
      (dom/div #js {:style #js {:display "inline-block"
                                :position "relative"
                                :width w
                                :height h}}
               (dom/div #js {:style #js {:position "absolute"
                                         :width "100%"
                                         :height "100%"
                                         :zIndex 2}}
                        (om/build point-picker-component (:p1 easing)
                                  {:state {:x+ x+
                                           :y+ y+
                                           :w w
                                           :h h}})
                        (om/build point-picker-component (:p2 easing)
                                  {:state {:x+ x+
                                           :y+ y+
                                           :w w
                                           :h h}}))

               (dom/div #js {:style #js {:position "absolute"
                                         :width "100%"
                                         :height "100%"
                                         :zIndex 1}}
                        (cnv/canvas easing w h
                                    (lines-painter easing x+ y+)))
               (dom/div #js {:style #js {:position "relative"
                                         :zIndex 0}}
                        (om/build easing-display-component easing
                                  {:state {:x+ x+
                                           :y+ y+
                                           :w w
                                           :h h}}))))))
