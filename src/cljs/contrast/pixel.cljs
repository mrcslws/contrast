(ns contrast.pixel
  (:refer-clojure :exclude [nth count get]))

(defn base [width x y]
  (-> y (* width) (+ x) (* 4)))

(defn xy->base [imagedata x y]
  (base (.-width imagedata) x y))

;; TODO this still isn't great for perf.
;; Gets the width way too often. Shows up in perf traces.
(defn write! [imagedata x y r g b a]
  (let [base (xy->base imagedata x y)]
    (doto (.-data imagedata)
      (aset base r)
      (aset (+ base 1) g)
      (aset (+ base 2) b)
      (aset (+ base 3) a))))

;; Better than using `write!` + `get` because it doesn't construct a vector.
(defn copy! [to tx ty
             from fx fy]
  (let [fbase (xy->base from fx fy)
        fdata (.-data from)]
    (write! to tx ty
            (aget fdata fbase)
            (aget fdata (+ fbase 1))
            (aget fdata (+ fbase 2))
            (aget fdata (+ fbase 3)))))

;; Better than using `get` + `=` because it doesn't construct a vector.
(defn matches? [imagedata x y r g b a]
  (let [base (xy->base imagedata x y)
        data (.-data imagedata)]
    (and (identical? (aget data base) r)
         (identical? (aget data (+ base 1)) g)
         (identical? (aget data (+ base 2)) b)
         (identical? (aget data (+ base 3)) a))))

(defn get [imagedata x y]
  (let [base (xy->base imagedata x y)
        data (.-data imagedata)]
    [(aget data base)
     (aget data (+ base 1))
     (aget data (+ base 2))
     (aget data (+ base 3))]))
