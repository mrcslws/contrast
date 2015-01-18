(ns contrast.app-state)

;; TODO switch away from "radius". "width" or "diameter" are better.
(defonce app-state
  (atom {:transition-radius-schema {:key :transition-radius
                                    :min 0
                                    :max 300
                                    :str-format "%dpx"
                                    :interval 1}

         :probed-row-schema {:key :probed-row}

         :single-linear-gradient {:width 600
                                  :height 256
                                  :transition-radius 50
                                  :probed-row 150
                                  :selected-color nil
                                  :locked {}}
         :single-sinusoidal-gradient {:width 600
                                      :height 256
                                      :transition-radius 50
                                      :probed-row 130
                                      :selected-color nil
                                      :locked {}}}))
