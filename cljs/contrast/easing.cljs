(ns contrast.easing)

(defprotocol PBezierDimension
  (t->coord [this t])
  (t->coord-slope [this t]))

(defprotocol PBezier
  (t->x [this t])
  (t->y [this t]))

(defprotocol PEasingBezier
  (x->y [this x])
  (y->x [this y])

  ;; It's simple to implement this on top of x->y, but that will duplicate a
  ;; lot of searching. Give implementors the chance to use a faster approach.
  (foreach-xy [this x-count f]))

;; Terminology: a "Unit Bézier" uses 0 and 1 as its first and last points.
(deftype CubicUnitBezierDimension [a b c]
  PBezierDimension
  (t->coord [this t]
    (-> t
        (* a)
        (+ b)
        (* t)
        (+ c)
        (* t)))
  (t->coord-slope [this t]
    (-> t
        (* 3 a)
        (+ (* 2 b))
        (* t)
        (+ c))))

(defn cubic-unit-bezier-dimension [coord1 coord2]
  ;; Horner's method. All the browsers do this, so it must be right.
  (let [c (* 3 coord1)
        b (- (* 3 (- coord2 coord1))
             c)
        a (- 1 c b)]
    (CubicUnitBezierDimension. a b c)))

(defn x->t [xdim x guess epsilon]
  (let [;; Newton method
        t (loop [t x
                 i 0]
            (when (< i 8)
              (let [error (- (t->coord xdim t)
                             x)]
                (if (< (js/Math.abs error) epsilon)
                  t
                  (let [d (t->coord-slope xdim t)]
                    (when (>= (js/Math.abs d) 1e-6)
                      (recur (- t (/ error d))
                             (inc i))))))))

        ;; Fall back to bisection
        t (if t
            t
            (loop [t 0
                   i 0
                   step 1]
              (assert (< i 30))
              (let [error (- (t->coord xdim t)
                             x)]
                (if (< (js/Math.abs error) epsilon)
                  t
                  (recur (if (pos? error)
                           (- t step)
                           (+ t step))
                         (inc i)
                         (/ step 2))))))]
    t))

(defn specify-easing! [easing]
  ;; TODO do some pre verification, or I'll hate myself some day.
  (let [{:keys [p1 p2]} easing
        xdim (cubic-unit-bezier-dimension (:x p1) (:x p2))
        ydim (cubic-unit-bezier-dimension (:y p1) (:y p2))]
    (specify! easing
              PBezier
              (t->x [_ t]
                (t->coord xdim t))
              (t->y [_ t]
                (t->coord ydim t))

              PEasingBezier
              (x->y [this x]
                (t->y this
                      (x->t xdim x x 1e-7)))
              (y->x [this y]
                (t->x this
                      (x->t ydim y 0.5 1e-7)))
              (foreach-xy [this xcount f]
                (let [step (/ 1 xcount)
                      epsilon (/ step 200)]
                  (loop [i 0
                         guess 0]
                    (when (< i xcount)
                      (let [x (* i step)
                            t (x->t xdim x guess epsilon)]
                        (f x (t->y this t))
                        ;; Use this t to improve the next guess.
                        (recur (inc i)
                               (+ t step))))))))))

(extend-type default
  PBezier
  (t->x [this t]
    (specify-easing! this)
    (t->x this t))
  (t->y [this t]
    (specify-easing! this)
    (t->y this t))

  PEasingBezier
  (x->y [this x]
    (specify-easing! this)
    (x->y this x))
  (y->x [this y]
    (specify-easing! this)
    (y->x this y))
  (foreach-xy [this xcount f]
    (specify-easing! this)
    (foreach-xy this xcount f)))
