(ns contrast.components.damped-spectrum-picker
  (:require [contrast.components.canvas :as cnv]
            [contrast.components.easing-picker :refer [easing-picker-component easing-display-component lines-painter point-picker-component]]
            [contrast.components.feature-magnet :refer [bezier-spectrum-magnets-component]]
            [contrast.components.spectrum-picker :refer [spectrum-picker-spec color-knob-component]]
            [contrast.easing :as easing]
            [contrast.pixel :as pixel]
            [contrast.progress :as progress]
            [contrast.spectrum :as spectrum]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; A hybrid spectrum + damping picker.
;; Current version: Use an easing function for damping.

(defn left-axis [h top-label bottom-label offset]
  (dom/div #js {:style
                #js {:position "absolute"
                     :left (- -20 offset)
                     :width 20
                     :height h
                     :font "10px Helvetica, Arial, sans-serif"
                     :color "#696969"
                     :borderRight "1px solid #696969"}}
           (dom/div #js {:style #js {:position "absolute"
                                     :right -4
                                     :top 0
                                     :width 8
                                     :height 1
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :top -3
                                     :right 8}}
                    top-label)
           (dom/div #js {:style #js {:position "absolute"
                                     :right -4
                                     :bottom 0

                                     :width 8
                                     :height 1
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :right 8
                                     :bottom -5
                                     :textAlign "center"}}
                    bottom-label)))


(defn bottom-axis [w left-label right-label offset]
  (dom/div #js {:style
                #js {:position "absolute"
                     :bottom (- -20 offset)
                     :width w
                     :height 20
                     :font "10px Helvetica, Arial, sans-serif"
                     :color "#696969"
                     :borderTop "1px solid #696969"}}
           (dom/div #js {:style #js {:position "absolute"
                                     :left 0
                                     :top -4
                                     :width 1
                                     :height 8
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :top 10
                                     :left -3}}
                    left-label)
           (dom/div #js {:style #js {:position "absolute"
                                     :right 0
                                     :top -4
                                     :width 1
                                     :height 8
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :top 10
                                     :textAlign "center"
                                     :width 40
                                     :right -20}}
                    right-label)))

;; Flaws:
;; - lots of gaps
;; - doesn't repaint on spectrum change
;; I should learn from my color-knob experience. Stop relying on easing-picker
;; to display a bezier curve. Display my own, and just use it for the points.
;; What about the lines?
(defn paint-spectrum-underneath [spectrum xoffset totalw]
  (fn [imgdata x y]
    (let [d (.-data imgdata)
          w (.-width imgdata)
          h (.-height imgdata)

          sx (-> (+ x xoffset)
                 (progress/n->p 0 (dec totalw))
                 (progress/p->n -1 1))
          r (spectrum/x->r spectrum sx)
          g (spectrum/x->g spectrum sx)
          b (spectrum/x->b spectrum sx)]
      (loop [i (inc y)]
        (when (< i h)
          (let [base (pixel/base w x i)]
            (doto d
              (aset base r)
              (aset (+ base 1) g)
              (aset (+ base 2) b)
              (aset (+ base 3) 255)))
          (recur (inc i)))))))

(defn idwriter [spectrum easing x+ y+]
  (fn write-damped-spectrum-imagedata! [imagedata]
    (let [width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)]
      ;; First, paint the whole thing with the spectrum, but make it
      ;; transparent.
      (dotimes [col width]
        (let [x (progress/n->p col 0 (dec width))
              r (spectrum/x->r spectrum x)
              g (spectrum/x->g spectrum x)
              b (spectrum/x->b spectrum x)]
          (dotimes [row height]
            (let [base (pixel/base width col row)]
              (doto d
                (aset base r)
                (aset (+ base 1) g)
                (aset (+ base 2) b)
                (aset (+ base 3) 0))))))

      (let [[xy->plot-xp xy->plot-yp] (progress/xy->plotxy x+ y+)]
        (easing/foreach-xy easing width
                           (fn [easing-x easing-y]
                             (let [lcol (-> (xy->plot-xp easing-x easing-y)
                                            (progress/p->int 0 (dec (quot width 2))))
                                   rcol (- (dec width) lcol)
                                   row (-> (xy->plot-yp easing-x easing-y)
                                           (progress/p->int 0 (dec height)))
                                   lbase (pixel/base width lcol row)
                                   rbase (pixel/base width rcol row)]
                               ;; Draw the Bézier curve.
                               (doto d
                                 (aset lbase 0)
                                 (aset (+ lbase 1) 0)
                                 (aset (+ lbase 2) 0)
                                 (aset (+ lbase 3) 255)
                                 (aset rbase 0)
                                 (aset (+ rbase 1) 0)
                                 (aset (+ rbase 2) 0)
                                 (aset (+ rbase 3) 255))

                               ;; TODO - this is a built-in assumption that
                               ;; y+ is :right
                               (loop [i (inc lcol)]
                                 (when (< i rcol)
                                   ;; unhide
                                   (doto d
                                     (aset (-> (pixel/base width i row)
                                               (+ 3))
                                           255))
                                   (recur (inc i)))))))))

    imagedata))

(defn damped-spectrum-picker-component [figure owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [spectrum vertical-easing]} figure
            w 150
            h 200
            totalw (* 2 w)
            x+ :up
            y+ :right]

        (dom/div #js {:style #js {:position "relative"}}
                 (left-axis h "Top" "Bottom" -12)
                 (dom/div #js {:style #js {:display "inline-block"
                                           :position "relative"
                                           :width totalw
                                           :height h
                                           :marginLeft 25
                                           :marginRight 10}}
                          (dom/div #js {:style #js {:position "absolute"
                                                    :width "100%"
                                                    :height "100%"
                                                    :zIndex 0}}
                                   (cnv/canvas [spectrum vertical-easing] totalw h
                                               (cnv/idwriter->painter
                                                (idwriter spectrum vertical-easing
                                                          x+ y+))))
                          (dom/div #js {:style #js {:position "absolute"
                                                    :width "100%"
                                                    :height "100%"
                                                    :zIndex 1}}
                                   (cnv/canvas [vertical-easing] w h
                                               (lines-painter vertical-easing
                                                              x+ y+)))
                          (dom/div #js {:style #js {:position "absolute"
                                                    :width "100%"
                                                    :height "100%"
                                                    :zIndex 2}}
                                   (om/build point-picker-component (:p1 vertical-easing)
                                             {:state {:x+ x+
                                                      :y+ y+
                                                      :w w
                                                      :h h}})
                                   (om/build point-picker-component (:p2 vertical-easing)
                                             {:state {:x+ x+
                                                      :y+ y+
                                                      :w w
                                                      :h h}}))

                          (dom/div #js {:style #js {:position "absolute"
                                                    :top (+ h 5)}}
                                   (om/build color-knob-component (:left spectrum)
                                             {:state {:target-width totalw}})
                                   (om/build color-knob-component (:right spectrum)
                                             {:state {:target-width totalw}})))
                 (om/build bezier-spectrum-magnets-component
                           {:spectrum spectrum
                            :easing (:vertical-easing figure)}
                           {:state {:h h
                                    :yseeks [0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1]}}))))))
