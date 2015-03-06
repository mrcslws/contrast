(ns contrast.hotpath
  (:refer-clojure :exclude [comp]))

(defn comp
  ([] identity)
  ([& fs]
     (let [[f & fs] (reverse fs)]
       (loop [f f
              fs fs]
         (if fs
           (let [g (first fs)]
             (recur (fn
                      ([] (g (f)))
                      ([x] (g (f x)))
                      ([x y] (g (f x y)))
                      ([x y z] (g (f x y z)))
                      ([x y z & args]
                         (g (apply f x y z args))))
                    (next fs)))
           f)))))

(defn comp1
  ([] identity)
  ([& fs]
     (let [[f & fs] (reverse fs)]
       (loop [f f
              fs fs]
         (if fs
           (let [g (first fs)]
             (recur (fn [x] (g (f x)))
                    (next fs)))
           f)))))
