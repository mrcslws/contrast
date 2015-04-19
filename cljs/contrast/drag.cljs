(ns contrast.drag
  (:require [cljs.core.async :refer [<! put! chan alts!]]
            [goog.events :as events])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [contrast.macros :refer [drain!]]))

(defn event-listen [el type prevent-default?]
  (let [port (chan)
        eventkey (events/listen el type (fn [e]
                                          (when prevent-default?
                                            (.preventDefault e))
                                          (put! port e)))]
    [eventkey port]))

(defn watch [pointerdown start progress finished]
  (go-loop []
    (when-let [downevt (<! pointerdown)]
      (when (or (not (number? (.-button downevt)))
                (= (.-button downevt) 0))
        (put! start :started)

        (let [[kmousemove mousemoves] (event-listen js/window "mousemove" false)
              [ktouchmove touchmoves] (event-listen js/window "touchmove" true)
              [kmouseup mouseups] (event-listen js/window "mouseup" false)
              [ktouchup touchends] (event-listen js/window "touchend" false)]
          (loop []
            (let [[evt port] (alts! [mousemoves touchmoves mouseups touchends])]
              (cond (= port mousemoves)
                    (do
                      (put! progress [evt
                                      downevt])
                      (recur))

                    (= port touchmoves)
                    (do
                      (put! progress [(-> evt (aget "event_") .-touches (aget 0))
                                      downevt])
                      (recur))

                    (= port mouseups)
                    (put! finished [evt downevt])

                    (= port touchends)
                    (put! finished [evt downevt]))))
          (events/unlistenByKey kmousemove)
          (events/unlistenByKey kmouseup)
          (events/unlistenByKey ktouchmove)
          (events/unlistenByKey ktouchup))

        ;; In obscure cases (e.g. javascript breakpoints)
        ;; there are stale mousedowns sitting in the queue.
        (drain! pointerdown))
      (recur))))

(defn delta [[evt downevt]]
  [(- (.-clientX evt)
      (.-clientX downevt))
   (- (.-clientY evt)
      (.-clientY downevt))])
