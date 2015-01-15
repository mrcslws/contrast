(ns contrast.macros)

;; TODO is dotimes equally good?
(defmacro forloop [[init test step] & body]
  `(loop [~@init]
     (when ~test
       ~@body
       (recur ~step))))
