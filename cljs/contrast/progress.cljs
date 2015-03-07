(ns contrast.progress)

(defn opposite [direction]
  (case direction
    :left :right
    :right :left
    :up :down
    :down :up
    (throw (js/Error. (str "Invalid direction " direction)))))

(defn orient-coord
  ([from+ to+]
     (if (= from+ to+)
       identity
       (fn [p] (- 1 p))))
  ([p from+ to+]
     ((orient-coord from+ to+) p)))

;; x+ is the direction in which x increases.
(defn swap-coord-systems
  ([from-x+ from-y+
    to-x+ to-y+]
     (if (or (= from-x+ to-x+)
             (= from-x+ (opposite to-x+)))
       (vector (let [orient (orient-coord from-x+ to-x+)]
                 (fn [xp yp] (orient xp)))
               (let [orient (orient-coord from-y+ to-y+)]
                 (fn [xp yp] (orient yp))))
       (vector (let [orient (orient-coord from-x+ to-y+)]
                 (fn [xp yp] (orient yp)))
               (let [orient (orient-coord from-y+ to-x+)]
                 (fn [xp yp] (orient xp))))))
  ([xp yp
    from-x+ from-y+
    to-x+ to-y+]
     ((apply juxt (swap-coord-systems from-x+ from-y+ to-x+ to-y+))
      xp yp)))

(defn xy->plotxy
  ([x+ y+]
     (swap-coord-systems x+ y+ :right :down))
  ([xp yp x+ y+]
     (swap-coord-systems xp yp x+ y+ :right :down)))

(defn plotxy->xy
  ([x+ y+]
     (swap-coord-systems :right :down x+ y+))
  ([plot-xp plot-yp x+ y+]
     (swap-coord-systems plot-xp plot-yp :right :down x+ y+)))

(defn x->plotx
  ([x+]
     (orient-coord x+ :right))
  ([xp x+]
     ((x->plotx x+) xp)))

(defn y->ploty
  ([y+]
     (orient-coord y+ :down))
  ([yp y+]
     ((y->ploty y+) yp)))

(defn n->p [v vmin vmax]
  (-> v
      (- vmin)
      (/ (- vmax vmin))))

(defn p->n [p vmin vmax]
  (-> p
      (* (- vmax vmin))
      (+ vmin)))

;; Using comp makes this slow.
(defn p->int [p vmin vmax]
  (-> (p->n p vmin vmax)
      js/Math.round))

;; Good for styling elements.
(defn percent [p]
  (-> p
      (* 100)
      (str "%")))
