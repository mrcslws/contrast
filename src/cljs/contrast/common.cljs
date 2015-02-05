(ns contrast.common
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.dom :as domh]))

;; Common components

(defn css-url [url]
  (str "url(" url ")"))

(defn background-image [img w h]
  (dom/div #js {:style #js {:position "absolute"
                            :backgroundSize "100% 100%"
                            :backgroundRepeat "no-repeat"
                            :backgroundImage (css-url img)
                            :width w
                            :height h}}))

(defn wide-background-image [left leftw center right rightw h]
  (let [common {:position "absolute"
                :backgroundSize "100% 100%"
                :backgroundRepeat "no-repeat"
                :height h}]
    (map #(dom/div #js {:style (clj->js (apply assoc common %))})
         [[:left 0
           :backgroundImage (css-url left)
           :width leftw]
          [:left leftw
           :right rightw
           :backgroundImage (css-url center)]
          [:right 0
           :backgroundImage (css-url right)
           :width rightw]])))

(defn hexcode->rgb [s]
  ;; "#FFFFFF" => [255 255 255]
  (let [rgb (map (fn [[start end]]
                   (js/parseInt (subs s start end) 16))
                 [[1 3] [3 5] [5 7]])]
    (when (every? integer? rgb)
      rgb)))

(defn rgb->hexcode [rgb]
  (apply str "#" (map #(-> (str "0" (.toString % 16))
                           (.slice -2))
                      rgb)))

(defmulti wavefn
  (fn [wave col period]
    wave))

(defmethod wavefn :sine [_ col period]
  (-> col
      (* 2 js/Math.PI)
      (/ period)
      js/Math.sin))

(defn cot [x]
  (/ 1 (js/Math.tan x)))

(defmethod wavefn :sawtooth [_ col period]
  (-> col
      (* js/Math.PI)
      (/ period)
      cot
      js/Math.atan
      (* 2)
      (/ js/Math.PI)
      -))

(defmethod wavefn :triangle [_ col period]
  (-> col
      (/ period)
      (mod 1)
      (- 0.5)
      js/Math.abs
      (- 0.25)
      (* 4)))

(defmethod wavefn :square [_ col period]
  (if (pos? (wavefn :sine col period))
    1
    -1))

;; TODO everything about "amplitude" just feels weird.
(defn wavey->ycoord [wavey amplitude height]
  ;; For example, if the height is 10, then a range might be
  ;; between 0 and 9, centered on 4.5.
  (let [c (-> height dec (/ 2))]
    (js/Math.round (-> wavey
                       - ;; Convert to HTML y.
                       (* amplitude)
                       (+ c)))))

(defn spectrum-dictionary [spectrum]
  (let [{:keys [left right]} spectrum
        dpos (- (:position right) (:position left))
        [rs gs bs :as slopes] (map #(/ (- %2 %) dpos)
                                   (:color left) (:color right))
        [rzero gzero bzero] (map (fn [c s]
                                   (- c (* s (:position right))))
                                 (:color right)
                                 slopes)]
    (fn [x]
      ;; TODO - consider js array for perf
      [(-> x (* rs) (+ rzero) js/Math.round)
       (-> x (* gs) (+ gzero) js/Math.round)
       (-> x (* bs) (+ bzero) js/Math.round)])))

(defn progress [start end p]
  (-> p
      (* (- end start))
      (+ start)
      js/Math.round))
