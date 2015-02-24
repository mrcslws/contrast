(ns contrast.illusions
  (:require [contrast.common :refer [wavefn spectrum-dictionary]]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [om.core :as om :include-macros true])
  (:require-macros [contrast.macros :refer [dorange]]))

;; TODO decomplect vertical `progress->color` from horizontal
(defn two-sides-idwriter [config interval-lookup]
  (fn [imagedata]
    (let [{:keys [transition-radius]} config
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
  (cnv/gradient-vertical-stripe-idwriter
   (comp
    (spectrum-dictionary (:spectrum config))
    (fn [_] 0))
   (comp
    (spectrum-dictionary (:spectrum config))
    (fn [x] (wavefn (:wave config) x 1))
    (x->total-distance (:left-period config)
                       (:right-period config)
                       (:width config)))))

(defn sums-of-harmonics [{:keys [harmonics period wave]}]
  (let [harray (clj->js harmonics)
        c (count harmonics)
        wfn (partial (get-method wavefn wave) wave)]
    (fn [x]
      (loop [s 0
             i 0]
        (if-not (< i c)
          s
          (let [h (aget harray i)]
              (recur (-> x
                      (* h)
                      (wfn period)
                      (* (/ 1 h))
                      (+ s))
                  (inc i))))))))

(defn harmonic-grating-idwriter [config]
  (cnv/solid-vertical-stripe-idwriter (comp
                                       (spectrum-dictionary (:spectrum config))
                                       (sums-of-harmonics config))))
