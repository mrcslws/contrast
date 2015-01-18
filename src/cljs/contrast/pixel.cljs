(ns contrast.pixel
  (:refer-clojure :exclude [nth count]))

(defprotocol PPixel
  (r [this])
  (g [this])
  (b [this])
  (a [this]))

(defprotocol PPixelCamera
  (pan! [this distance])
  (transport! [this position]))

(defprotocol PMutablePixel
  (overlay! [this foreground])
  (immutable-copy [this]))

(defn transparent? [px]
  (zero? (a px)))

(defn matches? [px1 px2]
  (and (identical? (r px1) (r px2))
       (identical? (g px1) (g px2))
       (identical? (b px1) (b px2))
       (identical? (a px1) (a px2))))

;; For perf experimention.
;; When comparing thousands of pixels to a single pixel,
;; you could imagine it being faster to store the r g b a
;; of the single pixel rather than grab them from the array
;; every time.
(deftype ImmutablePixel [r* g* b* a*]
  PPixel
  (r [_] r*)
  (g [_] g*)
  (b [_] b*)
  (a [_] a*))

;; Useful for converting a `[r g b a]` vector to something that
;; can be compared via `matches?`
(defn immutable-pixel [r g b a]
  (ImmutablePixel. r g b a))

(deftype PixelPointer [arr ^:volatile-mutable base]
  PPixel
  (r [this] (aget arr base))
  (g [this] (aget arr (+ base 1)))
  (b [this] (aget arr (+ base 2)))
  (a [this] (aget arr (+ base 3)))

  PPixelCamera
  (pan! [this distance]
    (set! base (+ base (* distance 4)))
    this)
  (transport! [this position]
    (set! base (* position 4))
    this)

  PMutablePixel
  (overlay! [this foreground]
    (let [fopacity (/ (a foreground) 255)
          bopacity (/ (a this) 255)
          ftransparency (- 1 fopacity)
          btransparency (- 1 bopacity)
          final-opacity (- 1 (* ftransparency btransparency))]
      (doto arr
        (aset base (js/Math.round (+ (* (r foreground) fopacity)
                                         (* (r this) ftransparency))))
        (aset (+ base 1) (js/Math.round (+ (* (g foreground) fopacity)
                                               (* (g this) ftransparency))))
        (aset (+ base 2) (js/Math.round (+ (* (b foreground) fopacity)
                                               (* (b this) ftransparency))))
        (aset (+ base 3) (js/Math.round (* final-opacity 255))))))

  (immutable-copy [this]
    (ImmutablePixel. (r this) (g this) (b this) (a this))))

(defprotocol PMutablePixelArray
  (nth! [this n])
  (xyth! [this x y])
  (pixel-count [this]))

(extend-type js/ImageData
  PMutablePixelArray
  (nth! [this n]
    (PixelPointer. (.-data this) (* n 4)))

  (xyth! [this x y]
    (nth! this (-> y (* (.-width this)) (+ x))))

  (pixel-count [this]
    (quot (.-length (.-data this)) 4)))
