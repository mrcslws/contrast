(ns contrast.spectrum)

;; Avoid returning rgb values as [r g b]. The hot path is not the best place to
;; construct a bunch of vectors.
(defprotocol PRgbDictionary
  (x->r [this x])
  (x->g [this x])
  (x->b [this x]))

(deftype SpectrumDictionary [rzero gzero bzero rs gs bs]
  PRgbDictionary
  (x->r [_ x]
    (-> x (* rs) (+ rzero) js/Math.round))
  (x->g [_ x]
    (-> x (* gs) (+ gzero) js/Math.round))
  (x->b [_ x]
    (-> x (* bs) (+ bzero) js/Math.round)))

(defn dictionary [spectrum]
  (let [{:keys [left right]} spectrum
        dpos (- (:position right) (:position left))
        [rs gs bs :as slopes] (mapv #(/ (- %2 %) dpos)
                                    (:color left) (:color right))
        [rzero gzero bzero] (mapv (fn [c s]
                                    (- c (* s (:position right))))
                                  (:color right)
                                  slopes)]
    (SpectrumDictionary. rzero gzero bzero rs gs bs)))

(defn x->cssrgb [dict x]
  (str "rgb("
       (x->r dict x)
       ","
       (x->g dict x)
       ","
       (x->b dict x)
       ")"))
