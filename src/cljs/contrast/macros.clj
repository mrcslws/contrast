(ns contrast.macros)

(defmacro dorange
  "Like dotimes, but starting from a specified number (rather than 0)"
  [bindings & body]
  (assert (vector? bindings))
  (assert (= 3 (count bindings)))
  (let [i (first bindings)
        m (second bindings)
        n (nth bindings 2)]
    `(let [m# (long ~m)
           n# (long ~n)]
       (loop [~i m#]
         (when (< ~i n#)
           ~@body
           (recur (unchecked-inc ~i)))))))
