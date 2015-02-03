(ns contrast.app-state
  (:require [om.core :as om :include-macros true]))

;; TODO switch away from "radius". "width" or "diameter" are better.

(defonce app-state
  (atom {:hood-open? false

         :single-sinusoidal-gradient {:width 600
                                      :height 256
                                      :transition-radius 250
                                      :selected-color nil
                                      :locked {:probed-row 30}
                                      :spectrum {:knob1-color [136 136 136]
                                                 :knob1-value -1
                                                 :knob2-color [170 170 170]
                                                 :knob2-value 1}}
         :sweep-grating {:width 600
                         :height 256
                         :contrast 10
                         :selected-color nil
                         :locked {:probed-row 30}
                         :spectrum {:knob1-color [136 136 136]
                                    :knob1-value -1
                                    :knob2-color [170 170 170]
                                    :knob2-value 1}}

         :harmonic-grating {:width 600
                            :height 256
                            :period 100
                            :selected-color nil
                            :spectrum {:knob1-color [136 136 136]
                                       :knob1-value -1
                                       :knob2-color [170 170 170]
                                       :knob2-value 1}
                            :wave :sine
                            :harmonic-magnitude "1 / n"
                            :harmonics [1 3 5 7 9 11 13
                                        15 17 19 21 23 25
                                        27 29 31 33 35 37 39]}}))
