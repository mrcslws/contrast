(ns contrast.dom)

(defn offset-from [evt el]
  {:x (- (.-pageX evt)
         (-> el .getBoundingClientRect .-left
             (+ js/document.body.scrollLeft)))
   :y (- (.-pageY evt)
         (-> el .getBoundingClientRect .-top
             (+ js/document.body.scrollTop)))})

(defn offset-from-target [evt]
  (offset-from evt (-> evt .-target)))
