(ns contrast.components.spectrum-picker
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.components.canvas :as cnv]
            [contrast.pixel :as pixel]
            [contrast.components.tracking-area :refer [tracking-area]]))


;; The pieces:
;; - A canvas displaying a linear gradient and "???"s where applicable.
;;   This canvas doesn't know where the gradient is going. It just slides
;;   along it until it runs out of space. Hence the need for "???"s.
;; - A draggable thingy that consists of
;;   > A rectangle with a black border and a fill with a color.
;;     -> Click to display a color picker.
;;   > A triangle that forms a roof on the rectangle and points to a particular
;;     point of the canvas.
;;   What do we call this thingy? It's not a color stop. It's more of a color progress report. A waypoint? A trajectory guide. A trajectory waypoint.
;;   I'm specifying a trajectory using two... grippers. Knobs.
;;   These things could also be used as color stops. They are a color and a position.
;;   I think they are "color knobs".
;; - A color picker experience. Clicking a color knob displays a color input and gives it focus. Clicking the webpage makes it disappear.

;; Words that I like:
;; - trajectory
;; - color knobs
;; - spectrum

(defn spectrum-picker-component [data owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "spectrum-picker")

    om/IRender
    (render [_]
      (dom/div #js {:style #js {:position "relative"}}
               ;; If the labels are divs along a canvas, then it's possible for them to be out of sync.
               ;; The canvas is using its own column numbers, width, and ___ to determine the current x.
               ;; The center is 0, the sides are 0 +- radius.

               ;; Stage 1 -- don't label it.


               ))))
