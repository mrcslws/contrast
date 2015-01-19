(ns contrast.dom)

(defn get-bounding-page-rect [el]
  (let [[left top] (loop [el el
                          x 0
                          y 0]
                     (if el
                       (recur (.-offsetParent el)
                              (-> x
                                  (+ (.-offsetLeft el))
                                  (cond->
                                   (not= el js/document.body)
                                   (- (.-scrollLeft el))))
                              (-> y
                                  (+ (.-offsetTop el))
                                  (cond->
                                   (not= el js/document.body)
                                   (- (.-scrollTop el)))))
                       [x y]))]
    [left top (+ left (.-offsetWidth el)) (+ top (.-offsetHeight el))]))

(defn within-element? [evt el]
  (let [[left top right bottom] (get-bounding-page-rect el)]
    (and (>= (.-pageX evt) left)
         (< (.-pageX evt) right)
         (>= (.-pageY evt) top)
         (< (.-pageY evt) bottom))))

(defn offset-from [evt el]
  (let [[left top right bottom] (get-bounding-page-rect el)]
    {:x (- (.-pageX evt) left)
     :y (- (.-pageY evt) top)}))

(defn offset-from-target [evt]
  (offset-from evt (-> evt .-target)))
