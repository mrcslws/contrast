(ns contrast.pages.shared-content
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [trace-rets wavefn]]
            [contrast.components.canvas :as cnv :refer [canvas-component]]
            [contrast.components.chan-handlers :refer [chan-genrender]]
            [contrast.components.color-exposer :refer [color-exposer-component]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [contrast.components.damped-spectrum-picker :refer [damped-spectrum-picker-component]]
            [contrast.components.easing-picker :refer [easing-picker-component easing-display-component]]
            [contrast.components.eyedropper-zone :refer [eyedropper-zone-spec]]

            [contrast.components.fixed-table :refer [fixed-table-component]]
            [contrast.components.numvec-editable :refer [numvec-editable]]
            [contrast.components.row-display :refer [row-display-component]]
            [contrast.components.row-probe :refer [row-probe-spec]]
            [contrast.components.slider :refer [slider]]
            [contrast.components.spectrum-picker :refer [spectrum-picker-spec color-knob-component]]
            [contrast.components.state-display :refer [state-display-component]]
            [contrast.components.wave-display :refer [wave-display-component]]
            [contrast.components.wave-picker :refer [wave-picker-component]]
            [contrast.hotkeys :as hotkeys]
            [contrast.illusions :as illusions]
            [contrast.instrumentation :as instrumentation]
            [contrast.page-triggers :as page-triggers]
            [contrast.pixel :as pixel]
            [contrast.progress :as progress]
            [contrast.spectrum :as spectrum]
            [contrast.state :as state]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; TODO switch away from "radius". "width" or "diameter" are better.
;; TODO dithering

(defn label-pixels [c]
  (str c " pixel" (when (not= c 1) "s")))

(defn illusion [& els]
  (apply dom/div #js {:style #js {:display "inline-block"}}
         els))

(defn algorithm [& els]
  (dom/div #js {:style #js {:display "inline-block"
                            :marginLeft 24
                            :verticalAlign "top"}}
           (apply
            dom/div
            #js {:style
                 #js {:backgroundColor "#f2f2f2"
                      :background "linear-gradient(#FFFFFF 0%, #f2f2f2 100%)"
                      :border "1px solid #e2e2e2"
                      :paddingTop 8
                      :paddingBottom 8
                      :paddingLeft 14
                      :borderRadius 5
                      :font "12px/1.4 Helvetica, Arial, sans-serif"}}
            els)))

(defn section [& els]
  (apply dom/div #js {:style #js {:marginBottom 12}}
         els))

(defn line [& els]
  (apply dom/div #js {:style #js {:position "relative"
                                  :paddingTop 2}}
         els))

(defn indented [& els]
  (apply dom/div #js {:style #js {:marginLeft 20}}
         els))

(defn heading [& els]
  (apply dom/div #js {:style #js {:fontWeight "bold"
                                  :marginBottom 6}}
         els))

(defn hover-exposer [k imgdata children]
  (->> children
       (assoc {:f color-exposer-component
               :props k
               :m {:state {:imagedata imgdata}}}
         :children)
       vector
       (eyedropper-zone-spec k {:key :selected-color}
                             imgdata)))

(defn spec->row-probed [k imgdata spec]
  (dom/div nil
           (spec/render (row-probe-spec k {:key :probed-row} true [spec]))
           (dom/div
            #js {:style #js {:marginTop 20
                             :position "relative"
                             :left -3
                             :borderLeft "3px solid red"
                             :borderRight "3px solid red"}}
            (chan-genrender
             (fn [channel row-display-imgdata]
               (spec/render
                (hover-exposer k row-display-imgdata
                               [{:f row-display-component
                                 :props k
                                 :m {:state {:stalkee imgdata}
                                     :opts {:subscriber channel}}}])))
             imgdata))))


(defn single-gradient [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "single-gradient")

    om/IRender
    (render [_]
      (let [figure (om/observe owner (state/figure k))
            canary [k figure]
            {:keys [width height]} figure]
        (dom/div
         nil
         (illusion
          (chan-genrender
           (fn [channel imgdata]
             (->> {:f canvas-component
                   :props canary
                   :m {:state {:width width :height height}
                       :opts {:paint
                              (cnv/idwriter->painter
                               (trace-rets
                                (illusions/single-sinusoidal-gradient-idwriter
                                 figure) channel))}}}

                  vector
                  (hover-exposer k imgdata)
                  (spec->row-probed k imgdata)))
           canary))
         (algorithm
          (section
           (heading "Transition from:")
           ;; TODO This isn't wired up to the illusion yet.
           (indented (line (om/build color-picker-component
                                     {:target (-> figure :spectrum :left)
                                      :schema {:key :color}})
                           " <-> "
                           (om/build color-picker-component
                                     {:target (-> figure :spectrum :right)
                                      :schema {:key :color}}))))
          (section
           (heading "over a distance of:")
           ;; hack
           (dom/div #js {:style #js {:width 300
                                     :height 0}})
           (indented (line (label-pixels (get-in figure [:transition
                                                         :radius])) "."
                           (slider {:position "absolute"
                                    :right 13
                                    :top -20
                                    :width 180}
                                   (:transition figure)
                                   {:key :radius
                                    :min 0
                                    :max 250
                                    :str-format "%dpx"
                                    :interval 1}))))))))))

(defn bottom-axis [w left-label right-label offset]
  (dom/div #js {:style
                #js {:position "absolute"
                     :bottom (- -20 offset)
                     :width w
                     :height 20
                     :font "10px Helvetica, Arial, sans-serif"
                     :color "#696969"
                     :borderTop "1px solid #696969"}}
           (dom/div #js {:style #js {:position "absolute"
                                     :left 0
                                     :top -4
                                     :width 1
                                     :height 8
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :top 10
                                     :left -3}}
                    left-label)
           (dom/div #js {:style #js {:position "absolute"
                                     :right 0
                                     :top -4
                                     :width 1
                                     :height 8
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :top 10
                                     :textAlign "center"
                                     :width 40
                                     :right -20}}
                    right-label)))

(defn left-axis [h top-label bottom-label offset]
  (dom/div #js {:style
                #js {:position "absolute"
                     :left (- -20 offset)
                     :width 20
                     :height h
                     :font "10px Helvetica, Arial, sans-serif"
                     :color "#696969"
                     :borderRight "1px solid #696969"}}
           (dom/div #js {:style #js {:position "absolute"
                                     :right -4
                                     :top 0
                                     :width 8
                                     :height 1
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :top -3
                                     :right 8}}
                    top-label)
           (dom/div #js {:style #js {:position "absolute"
                                     :right -4
                                     :bottom 0

                                     :width 8
                                     :height 1
                                     :backgroundColor "black"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :right 8
                                     :bottom -5
                                     :textAlign "center"}}
                    bottom-label)))

(defn right-axis [h top-label bottom-label offset top-offset]
  (dom/div #js {:style
                #js {:position "absolute"
                     :right (- -20 offset)
                     :top top-offset
                     :bottom "25%"
                     :width 20
                     :height h
                     :font "10px Helvetica, Arial, sans-serif"
                     :color "blue"
                     :borderLeft "1px solid blue"}}
           (dom/div #js {:style #js {:position "absolute"
                                     :left -4
                                     :top 0
                                     :width 8
                                     :height 1
                                     :backgroundColor "blue"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :top -3
                                     :left 8}}
                    top-label)
           (dom/div #js {:style #js {:position "absolute"
                                     :left -4
                                     :bottom 0

                                     :width 8
                                     :height 1
                                     :backgroundColor "blue"}})
           (dom/div #js {:style #js {:position "absolute"
                                     :left 8
                                     :bottom -5
                                     :textAlign "center"}}
                    bottom-label)))

(defn accelerating-wave-easing-idwriter
  [wave left-period right-period figure-width horizontal-easing]
  (fn [imgdata]
    (let [width (.-width imgdata)
          height (.-height imgdata)
          d (.-data imgdata)
          fig-col->wfnx (illusions/approximate-distances left-period right-period
                                                         figure-width horizontal-easing)
          wfn (partial (get-method wavefn wave) wave 1)
          bottom (quot height 4)
          top (* bottom 3)]
      (dotimes [col width]
        (let [row (-> col
                      (progress/n->p 0 (dec width))
                      (progress/p->int 0 (dec figure-width))
                      fig-col->wfnx
                      wfn
                      (progress/n->p -1 1)
                      (progress/p->int bottom (dec top)))
              base (pixel/base width col row)]
          (doto d
            (aset (+ base 2) 255)
            (aset (+ base 3) 255)))))
    imgdata))

(defn sweep-grating [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "sweep-grating")

    om/IRender
    (render [_]
      (let [figure (om/observe owner (state/figure k))
            canary [k figure]
            {:keys [width height]} figure]
        (dom/div nil
                 (illusion
                  (chan-genrender
                   (fn [channel imgdata]
                     (->> {:f canvas-component
                           :props canary
                           :m {:state {:width width :height height}
                               :opts {:paint
                                      (cnv/idwriter->painter
                                       (trace-rets
                                        (illusions/sweep-grating-idwriter
                                         figure) channel))}}}

                          vector
                          (hover-exposer k imgdata)
                          (spec->row-probed k imgdata)))
                   canary))
                 (algorithm

                  (section
                   (heading "Create numbers between -1 and 1 using a wave:")

                   (indented
                    (line "Create a " (name (get-in figure [:wave :form])) " wave.")
                    (line (om/build wave-picker-component {:target (:wave figure)
                                                           :schema {:key :form}}))
                    (line)
                    (line "On the left, use a period of")
                    (indented
                     (line (label-pixels (get-in figure [:left-period :period]))
                           (slider {:width 180
                                    :position "absolute"
                                    :right 13
                                    :top -18}
                                   (:left-period figure)
                                   {:key :period
                                    :min 1
                                    :max 1000
                                    :str-format "%dpx"
                                    :interval 1})))
                    (line)

                    (line "On the right, use a period of"
                          (indented
                           (line (label-pixels (get-in figure [:right-period :period]))
                                 (slider {:width 180
                                          :position "absolute"
                                          :right 13
                                          :top -18}
                                         (:right-period figure)
                                         {:key :period
                                          :min 1
                                          :max 1000
                                          :str-format "%dpx"
                                          :interval 1}))))
                    (line)
                    (let [w 300
                          h 200
                          {:keys [horizontal-easing left-period right-period wave]} figure
                          wave (:form wave)
                          left-period (:period left-period)
                          right-period (:period right-period)]
                      (line
                       (dom/div #js {:style #js {:display "inline-block"
                                                 :position "relative"
                                                 :width w
                                                 :height h
                                                 :marginLeft 25
                                                 :marginRight 10
                                                 :marginBottom 25}}
                                (left-axis h
                                           (label-pixels (get-in figure [:right-period :period]))
                                           (label-pixels (get-in figure [:left-period :period]))
                                           10)
                                (dom/div #js {:style #js {:position "absolute"
                                                          :width "100%"
                                                          :height "100%"
                                                          :zIndex 0}}
                                         (cnv/canvas [wave horizontal-easing left-period right-period]
                                                     w h
                                                     (cnv/idwriter->painter
                                                      (accelerating-wave-easing-idwriter
                                                       wave left-period right-period width horizontal-easing))))
                                (dom/div #js {:style #js {:position "absolute"
                                                          :width "100%"
                                                          :height "100%"
                                                          :zIndex 1}}
                                         (om/build easing-picker-component horizontal-easing
                                                   {:state {:w w
                                                            :h h
                                                            :x+ :right
                                                            :y+ :up}}))
                                (right-axis (quot h 2) "1" "-1" 10 (quot h 4))
                                (bottom-axis w "Left" "Right" 10))))))

                  (section
                   (heading "Translate these numbers into color:")

                   (line "Dampen the numbers according to their height, and then translate to color.")
                   ;; TODO clearly I can do better.
                   (dom/div #js {:style #js {:marginTop 20}})

                   (indented (om/build damped-spectrum-picker-component figure
                                       {:opts {:canvas-spec-transform
                                               (fn [spec imgdata]
                                                 (eyedropper-zone-spec
                                                  k {:key :selected-color} imgdata
                                                  [{:f color-exposer-component
                                                    :props k
                                                    :m {:state {:imagedata imgdata}}
                                                    :children [spec]}]))}})))))))))
(defn simple-grating [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "simple-grating")

    om/IRender
    (render [_]
      (let [figure (om/observe owner (state/figure k))
            canary [k figure]
            {:keys [width height]} figure]
        (dom/div
         nil
         (illusion
          (om/build canvas-component canary
                    {:state {:width width :height height}
                     :opts {:paint (cnv/idwriter->painter
                                    (illusions/harmonic-grating-idwriter
                                     figure))}}))
         (dom/div #js {:style #js {:display "inline-block"
                                   :verticalAlign "top"}}
                  (section
                   (indented
                    (dom/p nil "Spatial frequency: "
                           (-> (get-in figure [:frequency :frequency])
                               (.toFixed 1))
                           " cycles per 100 pixels.")
                    (line
                     (slider {:position "relative"
                              :width 180
                              :top -25}
                             (:frequency figure)
                             {:key :frequency
                              :min 0.1
                              :max 30
                              :str-format "%.1f cycles"
                              :interval 0.1}))))))))))

(defn harmonic-grating [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "harmonic-grating")

    om/IRender
    (render [_]
      (let [figure (om/observe owner (state/figure k))
            canary [k figure]
            {:keys [width height]} figure]
        (dom/div
         nil
         (illusion
          (chan-genrender
           (fn [channel imgdata]
             (->> {:f canvas-component
                   :props canary
                   :m {:state {:width width :height height}
                       :opts {:paint
                              (cnv/idwriter->painter
                               (trace-rets
                                (illusions/harmonic-grating-idwriter
                                 figure) channel))}}}

                  vector
                  (hover-exposer k imgdata)
                  spec/render))
           canary))
         (algorithm
          (section
           (heading "For each n ∈ "
                    (numvec-editable {:width 300 :display "inline"}
                                     figure {:key :harmonics}))

           (indented
            (line "Create a " (name (get-in figure [:wave :form])) " wave "
                  (om/build wave-picker-component {:target (:wave figure)
                                                   :schema {:key :form}}))
            (line "with amplitude 1/n")
            (line " and period 1/n * " (get-in figure [:frequency :period])
                  " pixels."
                  (slider {:position "absolute"
                           :right 13
                           :top -20
                           :width 180}
                          (:frequency figure)
                          {:key :period
                           :min 1
                           :max 5000
                           :str-format "%dpx"
                           :interval 1}))))

          (section
           (heading "Use the sum of these waves to choose the color:")
           (indented (line (spectrum-picker-spec
                            (:spectrum figure) 360
                            (fn [spec imgdata]
                              (eyedropper-zone-spec
                               k {:key :selected-color} imgdata
                               [{:f color-exposer-component
                                 :props k
                                 :m {:state {:imagedata imgdata}}
                                 :children [spec]}])))))))
         ;; (dom/div #js {:style #js {:marginTop 12
         ;;                           :paddingLeft 14}}
         ;;          (when-let [[r g b a]
         ;;                     (:selected-color data)]
         ;;            (str "Hovered color: rgba(" r "," g "," b "," a ")")))

         (dom/div #js {:style #js {:marginTop 12}}
                  (om/build wave-display-component
                            (select-keys figure
                                         [:width :wave
                                          :harmonics :frequency]))))))))

(defn drag-and-inspect [k owner]
  (reify
    om/IInitState
    (init-state [_]
      {:img (js/document.createElement "img")
       :image-dropped false})

    om/IRenderState
    (render-state [_ {:keys [image-dropped]}]
      (let [figure (om/observe owner (state/figure k))
            {:keys [width height]} figure]
        (dom/div #js {:style #js {:position "relative"
                                  :display "inline-block"
                                  :minWidth 300
                                  :minHeight 300
                                  :border (when-not image-dropped
                                            "1px solid black")}
                      :onDragOver #(.preventDefault %)
                      :onDrop (fn [drop-evt]
                                (.preventDefault drop-evt)
                                (let [files (-> drop-evt .-dataTransfer .-files)
                                      file (aget files 0)
                                      rdr (js/FileReader.)]
                                  (when file
                                    (set! (.-onload rdr)
                                          (fn [load-evt]
                                            (om/set-state! owner :image-dropped true)
                                            (om/update-state! owner :img
                                                              (fn [img]
                                                                (let [src (-> load-evt
                                                                              .-target
                                                                              .-result)]
                                                                  (set! (.-src img) src)
                                                                  (om/transact! figure
                                                                                #(assoc %
                                                                                   :img-src src
                                                                                   :width (.-width img)
                                                                                   :height (.-height img))))
                                                                img))))
                                    (.readAsDataURL rdr file))))}
                 (if image-dropped
                   (let [canary figure]
                     (illusion
                      (chan-genrender
                       (fn [channel imgdata]
                         (->> {:f canvas-component
                               :props canary
                               :m {:state {:width width
                                           :height height}
                                   :opts {:paint
                                          (trace-rets
                                           (fn [cnv]
                                             (let [img (om/get-state owner :img)
                                                   ctx (.getContext cnv "2d")]
                                               (.drawImage ctx img 0 0)
                                               (.getImageData ctx 0 0
                                                              (.-width cnv)
                                                              (.-height cnv))))
                                           channel)}}}

                              vector
                              (hover-exposer k imgdata)
                              (spec->row-probed k imgdata)))
                       canary)))
                   (dom/div #js {:style #js {:position "absolute"
                                             :top "40%"
                                             :width "100%"
                                             :textAlign "center"}}
                            "Drop an image here to inspect it."
                            (dom/br nil)
                            "(Must be a local file)")))))))
