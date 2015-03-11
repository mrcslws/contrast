(ns contrast.illusions
  (:require [contrast.common :refer [wavefn harmonic-adder]]
            [contrast.components.canvas :as cnv]
            [contrast.easing :as easing]
            [contrast.pixel :as pixel]
            [contrast.progress :as progress]
            [contrast.spectrum :as spectrum]
            [om.core :as om :include-macros true])
  (:require-macros [contrast.macros :refer [dorange]]))

;; TODO decomplect vertical `progress->color` from horizontal
(defn two-sides-idwriter [config interval-lookup]
  (fn [imagedata]
    (let [transition-radius (get-in config [:transition :radius])
          width (.-width imagedata)
          height (.-height imagedata)
          d (.-data imagedata)
          start 0
          end 255
          middle 127
          leftx 0
          transx (- (/ width 2) transition-radius)
          transw (* 2 transition-radius)
          rightx (+ transx transw)
          lprogress->color (interval-lookup middle start)
          rprogress->color (interval-lookup middle end)]

      (dotimes [row height]
        (let [lcolor (lprogress->color (/ row height))
              rcolor (rprogress->color (/ row height))
              progress->color (interval-lookup lcolor rcolor)]
          (dorange [col leftx transx]
                   (let [base (pixel/base width col row)]
                     (doto d
                       (aset base lcolor)
                       (aset (+ base 1) lcolor)
                       (aset (+ base 2) lcolor)
                       (aset (+ base 3) 255))))
          (dorange [col rightx width]
                   (let [base (pixel/base width col row)]
                     (doto d
                       (aset base rcolor)
                       (aset (+ base 1) rcolor)
                       (aset (+ base 2) rcolor)
                       (aset (+ base 3) 255))))
          (dorange [col transx rightx]
                   (let [c (progress->color (/ (- col transx) transw))
                         base (pixel/base width col row)]
                     (doto d
                       (aset base c)
                       (aset (+ base 1) c)
                       (aset (+ base 2) c)
                       (aset (+ base 3) 255))))))
      imagedata)))

(defn number-line-maker [ptransform]
  (fn [start end]
    (let [l (- end start)]
      (fn [p]
        (-> p
            ptransform
            (* l)
            (+ start)
            js/Math.round)))))

(defn linear->sinusoidal [progress]
  (-> progress
      (* js/Math.PI)
      (- (/ js/Math.PI 2))
      js/Math.sin
      inc
      (/ 2)))

(defn single-sinusoidal-gradient-idwriter [config]
  (two-sides-idwriter config (number-line-maker linear->sinusoidal)))

;; If a body is accelerating from start-period at x of 0 to
;; stop-period at x of (width - 1), get the distance covered at an
;; in-between point. Compose this with other functions
;; (e.g. waveforms) to get an accelerating version of that function.
(defn x->total-distance [start-period end-period duration]
  (let [period-acceleration (/ (- end-period start-period) duration)]
    (if (zero? period-acceleration)
      (fn [x]
        (* x (/ 1 start-period)))
      (fn [x]
        (-> x
            (* period-acceleration)
            (+ start-period)
            js/Math.log
            (- (js/Math.log start-period))
            (* (/ 1 period-acceleration)))))))

(defn sweep-grating-idwriter [config]
  (fn write-sweep-grating! [imgdata]
    (let [{:keys [width height vertical-easing spectrum left-period right-period wave]} config
          wfn (partial (get-method wavefn (:form wave)) (:form wave) 1)
          col->wfnx (x->total-distance (:period left-period)
                                       (:period right-period)
                                       width)

          cached-damp-factors (js/Array. height)
          d (.-data imgdata)

          ;; It's slow to call this once per pixel, so call it once per
          ;; column + this once.
          minr (spectrum/x->r spectrum 0)
          ming (spectrum/x->g spectrum 0)
          minb (spectrum/x->b spectrum 0)]

      ;; Current version: use an easing function for damping. This means one
      ;; side has to be damped to 0% while the other side has to be undamped.
      ;; Arbitrary limitation.
      (easing/foreach-xy vertical-easing height
                         (fn [y d]
                           (aset cached-damp-factors
                                 (js/Math.round (* y height))
                                 d)))

      (dotimes [col width]
        (let [before-damp (-> col col->wfnx wfn)

              maxr (spectrum/x->r spectrum before-damp)
              maxg (spectrum/x->g spectrum before-damp)
              maxb (spectrum/x->b spectrum before-damp)]
          (dotimes [row height]
            (let [damp-factor (aget cached-damp-factors row)
                  base (pixel/base width col row)]
              (doto d
                (aset base (progress/p->int damp-factor minr maxr))
                (aset (+ base 1) (progress/p->int damp-factor ming maxg))
                (aset (+ base 2) (progress/p->int damp-factor minb maxb))
                (aset (+ base 3) 255)))))))
    imgdata))

(defn harmonic-grating-idwriter [config]
  (cnv/solid-vertical-stripe-idwriter (harmonic-adder config)
                                      (:spectrum config)))
