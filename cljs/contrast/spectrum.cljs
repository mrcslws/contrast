(ns contrast.spectrum)

;; Avoid returning rgb values as [r g b]. The hot path is not the best place to
;; construct a bunch of vectors.
(defprotocol PRgbDictionary
  (x->r [this x])
  (x->g [this x])
  (x->b [this x]))

(defn specify-dictionary! [spectrum]
  ;; TODO do some pre verification, or I'll hate myself some day.
  (let [{:keys [left right]} spectrum
        dpos (- (:position right) (:position left))
        [rs gs bs :as slopes] (mapv #(/ (- %2 %) dpos)
                                    (:color left) (:color right))
        [rzero gzero bzero] (mapv (fn [c s]
                                    (- c (* s (:position right))))
                                  (:color right)
                                  slopes)]
    (specify! spectrum
                PRgbDictionary
                (x->r [_ x]
                  (-> x (* rs) (+ rzero) js/Math.round))
                (x->g [_ x]
                  (-> x (* gs) (+ gzero) js/Math.round))
                (x->b [_ x]
                  (-> x (* bs) (+ bzero) js/Math.round)))))

(extend-type default
  PRgbDictionary
  (x->r [this r]
    (specify-dictionary! this)
    (x->r this r))
  (x->g [this g]
    (specify-dictionary! this)
    (x->g this g))
  (x->b [this b]
    (specify-dictionary! this)
    (x->g this b)))

(defn x->cssrgb [spectrum x]
  (str "rgb("
       (x->r spectrum x)
       ","
       (x->g spectrum x)
       ","
       (x->b spectrum x)
       ")"))
