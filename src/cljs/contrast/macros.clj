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

;; Implemented as a macro because alts!! isn't available in ClojureScript,
;; so we're forced to use alt! in a (go ...) block, so we need a macro to
;; use the caller's (go ...) block.
;; Returns the final value that was withdrawn from the channel, or nil
;; if the channel was empty.
(defmacro drain! [chn]
  `(loop [v# nil]
     (cljs.core.async.macros/alt!
       ~chn ([vv#] (recur vv#))
       :default v#)))
