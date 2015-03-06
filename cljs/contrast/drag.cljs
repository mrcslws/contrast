(ns contrast.drag
  (:require [cljs.core.async :refer [<! put! chan alts!]]
            [goog.events :as events])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [contrast.macros :refer [drain!]]))

(defn event-listen [el type]
  (let [port (chan)
        eventkey (events/listen el type #(put! port %1))]
    [eventkey port]))

(defn watch [mousedown start progress finished]
  (go-loop []
    (when-let [downevt (<! mousedown)]
      (when (= (.-button downevt) 0)
        (put! start :started)

        (let [[kmousemove moves] (event-listen js/window "mousemove")
              [kmouseup ups] (event-listen js/window "mouseup")]
          (loop []
            (let [[evt port] (alts! [moves ups])
                  d [(- (.-clientX evt)
                        (.-clientX downevt))
                     (- (.-clientY evt)
                        (.-clientY downevt))]]
              (if (= port moves)
                (do
                  (put! progress d)
                  (recur))
                (do
                  (put! finished d)
                  (events/unlistenByKey kmousemove)
                  (events/unlistenByKey kmouseup))))))

        ;; In obscure cases (e.g. javascript breakpoints)
        ;; there are stale mousedowns sitting in the queue.
        (drain! mousedown))
      (recur))))
