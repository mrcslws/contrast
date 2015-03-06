(ns contrast.common
  (:require [cljs.core.async :refer [put!]]
            [contrast.dom :as domh]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

;; Common components

(defn css-url [url]
  (str "url(" url ")"))


(defn background-image
  ([img w h]
     (background-image img w h 0 0))
  ([img w h l t]
     (dom/div #js {:style #js {:position "absolute"
                               :backgroundSize "100% 100%"
                               :backgroundRepeat "no-repeat"
                               :backgroundImage (css-url img)
                               :width w
                               :height h
                               :left l
                               :top t}})))

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
  (fn [wave period col]
    wave))

(defmethod wavefn :sine [_ period col]
  (-> col
      (* 2 js/Math.PI)
      (/ period)
      js/Math.sin))

(defn cot [x]
  (/ 1 (js/Math.tan x)))

(defmethod wavefn :sawtooth [_ period col]
  (-> col
      (* js/Math.PI)
      (/ period)
      cot
      js/Math.atan
      (* 2)
      (/ js/Math.PI)
      -))

(defmethod wavefn :triangle [_ period col]
  (-> col
      (/ period)
      (mod 1)
      (- 0.5)
      js/Math.abs
      (- 0.25)
      (* 4)))

(defmethod wavefn :square [_ period col]
  (if (pos? (wavefn :sine period col))
    1
    -1))

(defn harmonic-adder [{:keys [harmonics frequency wave]}]
  (let [harray (clj->js harmonics)
        c (count harmonics)
        period (:period frequency)
        wfn (partial (get-method wavefn (:form wave)) wave)]
    (fn add
      ([x]
         (add x identity))
      ([x fsummand]
         (loop [s 0
                i 0]
           (if-not (< i c)
             s
             (let [h (aget harray i)
                   y (-> (wfn (/ period h) x)
                         (/ h))]
               (fsummand y)
               (recur (+ y s)
                      (inc i)))))))))

(defn trace-rets [f ch]
  (fn [in]
    (let [r (f in)]
      (when ch
        (put! ch r))
      r)))

(defn display-name [c]
  ((aget c "getDisplayName")))
