(ns contrast.pixel
  (:refer-clojure :exclude [nth count get]))

(defn base [width x y]
  (-> y (* width) (+ x) (* 4)))

(defn xy->base [imagedata x y]
  (base (.-width imagedata) x y))

(defn get [imagedata x y]
  (let [base (xy->base imagedata x y)
        data (.-data imagedata)]
    [(aget data base)
     (aget data (+ base 1))
     (aget data (+ base 2))
     (aget data (+ base 3))]))
