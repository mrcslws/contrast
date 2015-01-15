(ns contrast.dom)

(defn within-element? [evt el]
  (let [rect (.getBoundingClientRect el)
        left (-> rect .-left (+ js/document.body.scrollLeft))
        top (-> rect .-top (+ js/document.body.scrollTop))]
    (and (>= (.-pageX evt) left)
         (< (.-pageX evt) (+ left (.-width rect)))
         (>= (.-pageY evt) top)
         (< (.-pageY evt) (+ top (.-height rect))))))

(defn offset-from [evt el]
  {:x (- (.-pageX evt)
         (-> el .getBoundingClientRect .-left
             (+ js/document.body.scrollLeft)))
   :y (- (.-pageY evt)
         (-> el .getBoundingClientRect .-top
             (+ js/document.body.scrollTop)))})

(defn offset-from-target [evt]
  (offset-from evt (-> evt .-target)))
