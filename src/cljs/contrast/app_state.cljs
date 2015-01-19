(ns contrast.app-state
  (:require [om.core :as om :include-macros true]))

;; TODO switch away from "radius". "width" or "diameter" are better.

(defonce app-state
  (atom {:single-linear-gradient {:width 600
                                  :height 256
                                  :transition-radius 250
                                  :selected-color nil
                                  :locked {:probed-row 30}}
         :single-sinusoidal-gradient {:width 600
                                      :height 256
                                      :transition-radius 250
                                      :selected-color nil
                                      :locked {:probed-row 30}}
         :sweep-grating {:width 600
                         :height 256
                         :contrast 10
                         :selected-color nil
                         :locked {:probed-row 30}}}))
