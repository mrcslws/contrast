(ns contrast.app-state
  (:require [om.core :as om :include-macros true]))

;; TODO switch away from "radius". "width" or "diameter" are better.

(defonce app-state
  (atom {:hood-open? false

         :single-sinusoidal-gradient {:width 600
                                      :height 256
                                      :transition-radius 250
                                      :selected-color nil
                                      :locked {:probed-row 30}}
         :sweep-grating {:width 600
                         :height 256
                         :contrast 10
                         :selected-color nil
                         :locked {:probed-row 30}}

         :harmonic-grating {:width 600
                            :height 256
                            :period 100
                            :selected-color nil
                            :from-color "#888888"
                            :to-color "#AAAAAA"
                            :wave :sine
                            :harmonic-magnitude "1 / n"
                            :harmonics [1 3 5 7 9 11 13
                                        15 17 19 21 23 25
                                        27 29 31 33 35 37 39]}}))
