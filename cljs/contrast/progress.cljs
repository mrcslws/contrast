(ns contrast.progress)

;; Prepare coordinates for the web browser's coordinate system.
(defn orient
  ([origin]
     (cond (or (= origin :top)
               (= origin :left))
           identity

           (or (= origin :right)
               (= origin :bottom))
           (fn [p] (- 1 p))

           :else
           (throw (js/Error. (str "Invalid origin " origin)))))
  ([p origin]
     ((orient origin) p)))

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
