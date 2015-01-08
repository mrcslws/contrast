(ns contrast.canvas)

(defn clear [ctx]
  (.save ctx)
  (.setTransform ctx 1 0 0 1 0 0)
  (.clearRect ctx 0 0 (.. ctx -canvas -width) (.. ctx -canvas -height))
  (.restore ctx))

(defn fill-rect [ctx x y width height fill]
  (set! (.-fillStyle ctx) fill)
  (.fillRect ctx x y width height))

(defn linear-gradient [ctx x0 y0 x1 y1 & stops]
  (let [g (.createLinearGradient ctx x0 y0 x1 y1)]
    (doseq [[offset color] stops]
      (.addColorStop g offset color))
    g))
