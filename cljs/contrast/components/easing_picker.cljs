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
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(defn x-extractor [xorigin yorigin]
  (if (or (= xorigin :left)
          (= xorigin :right))
    (let [orient (progress/orient xorigin)]
      (fn extract
        [x _]
        (orient x)))
    (let [orient (progress/orient yorigin)]
      (fn extract
        [_ y]
        (orient y)))))

(defn y-extractor [xorigin yorigin]
  (if (or (= yorigin :top)
          (= xorigin :bottom))
    (let [orient (progress/orient yorigin)]
      (fn extract
        [_ y]
        (orient y)))
    (let [orient (progress/orient xorigin)]
      (fn extract
        [x _]
        (orient x)))))

(defn lines-painter [easing xorigin yorigin]
  (let [x-extract (x-extractor xorigin yorigin)
        y-extract (y-extractor xorigin yorigin)

        {:keys [p1 p2]} easing

        x1p (x-extract (:x p1) (:y p1))
        y1p (y-extract (:x p1) (:y p1))
        x2p (x-extract (:x p2) (:y p2))
        y2p (y-extract (:x p2) (:y p2))]
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
          (.lineTo (progress/p->int x1p 0 (dec w))
                   (progress/p->int y1p 0 (dec h)))
          (.stroke)
          (.beginPath)
          (.moveTo (dec w) (dec h))
          (.lineTo (progress/p->int x2p 0 (dec w))
                   (progress/p->int y2p 0 (dec h)))
          (.stroke))))))

(defn idwriter [easing xorigin yorigin]
  (let [{:keys [p1 p2]} easing
        easing-function (easing/cubic-bezier-easing (:x p1)
                                                    (:y p1)
                                                    (:x p2)
                                                    (:y p2))]
    (fn write-imagedata! [imgdata]
      (let [width (.-width imgdata)
            height (.-height imgdata)
            d (.-data imgdata)
            x-extract (x-extractor xorigin yorigin)
            y-extract (y-extractor xorigin yorigin)]
        (easing/foreach-xy easing-function width
                           (fn [easing-x easing-y]
                             (let [col (-> (x-extract easing-x easing-y)
                                           (progress/p->int 0 (dec width)))
                                   row (-> (y-extract easing-x easing-y)
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
      (let [{:keys [mousedown]} (om/get-state owner)
            started (chan)
            progress (chan)
            finished (chan)]
        (drag/watch mousedown started progress finished)

        (go-loop []
          (when-let [_ (<! started)]
            (let [drag-start @point]
              (loop []
                (alt! progress
                      ([[dxp dyp]]
                         (let [container (om/get-node owner "container")]
                           (om/transact! point
                                         #(assoc %
                                            :y (-> dxp
                                                   (* (/ 1 (.-offsetWidth
                                                            container)))
                                                   (+ (:y drag-start)))
                                            :x (-> dyp
                                                   (* (/ 1 (.-offsetHeight
                                                            container)))
                                                   (+ (:x drag-start))))))
                         (recur))

                      finished
                      :cool!

                      started
                      :hmm?)))
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [mousedown]}]
      (dom/div #js {:ref "container"
                    :style #js {:display "inline-block"
                                :height "100%"
                                :width "100%"}}
               (dom/div #js {:onMouseDown (fn [e]
                                            (.persist e)
                                            (.preventDefault e)
                                            (put! mousedown e)
                                            nil)
                             :style #js {:cursor "pointer"
                                         :position "absolute"
                                         :left (progress/percent (:y point))
                                         :top (progress/percent (:x point))}}
                        (background-image "images/SliderKnob.png" 13 13 -6 -6))))))

(defn easing-picker-component [easing owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [xorigin yorigin w h]}]
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
                                  {:state {:xorigin xorigin
                                           :yorigin yorigin}})
                        (om/build point-picker-component (:p2 easing)
                                  {:state {:xorigin xorigin
                                           :yorigin yorigin}}))

               (dom/div #js {:style #js {:position "absolute"
                                         :width "100%"
                                         :height "100%"
                                         :zIndex 1}}
                        (cnv/canvas easing w h
                                    (lines-painter easing xorigin yorigin)))
               (dom/div #js {:style #js {:position "relative"
                                         :zIndex 0}}
                        (cnv/canvas easing w h
                                    (cnv/idwriter->painter
                                     (idwriter easing xorigin yorigin))))))))
