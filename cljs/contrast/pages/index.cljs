(ns contrast.pages.index
  (:require [cljs.core.async :refer [put! chan mult tap close! <!]]
            [com.mrcslws.om-spec :as spec]
            [contrast.common :refer [trace-rets]]
            [contrast.components.canvas :as cnv :refer [canvas-component]]
            [contrast.components.chan-handlers :refer [chan-genrender]]
            [contrast.components.color-exposer :refer [color-exposer-component]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [contrast.components.easing-picker :refer [easing-picker-component]]
            [contrast.components.eyedropper-zone :refer [eyedropper-zone-spec]]
            [contrast.components.feature-magnet :refer [bezier-feature-magnet-spec]]
            [contrast.components.fixed-table :refer [fixed-table-component]]
            [contrast.components.numvec-editable :refer [numvec-editable]]
            [contrast.components.row-display :refer [row-display-component]]
            [contrast.components.row-probe :refer [row-probe-spec]]
            [contrast.components.slider :refer [slider]]
            [contrast.components.spectrum-picker :refer [spectrum-picker-spec]]
            [contrast.components.state-display :refer [state-display-component]]
            [contrast.components.wave-display :refer [wave-display-component]]
            [contrast.components.wave-picker :refer [wave-picker-component]]
            [contrast.hotkeys :as hotkeys]
            [contrast.illusions :as illusions]
            [contrast.instrumentation :as instrumentation]
            [contrast.page-triggers :as page-triggers]
            [contrast.spectrum :as spectrum] ;; TODO necessary?
            [contrast.state :as state]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

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
                   (heading "Choose a spectrum.")
                   (indented (line (spectrum-picker-spec
                                    (:spectrum figure) 360
                                    (fn [spec imgdata]
                                      (eyedropper-zone-spec
                                       k {:key :selected-color} imgdata
                                       [{:f color-exposer-component
                                         :props k
                                         :m {:state {:imagedata imgdata}}
                                         :children [spec]}]))))))

                  (section
                   (heading "Configure a wave.")

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

                    (line "In between, ease between these periods linearly.")))

                  (section
                   (heading "Now use them.")
                   (indented
                    (let [w 200
                          h 200]
                      (line
                       (om/build easing-picker-component (:vertical-easing
                                                          figure)
                                 {:state {:w w
                                          :h h

                                          ;; Plot x axis along left side.
                                          :xorigin :top
                                          :yorigin :left}})
                       (apply dom/div #js {:style #js {:verticalAlign "top"

                                                       ;; TODO Mirroring the easing picker
                                                       :marginTop 10

                                                       :marginLeft 30
                                                       :display "inline-block"
                                                       :position "relative"}}
                              (let [s (spectrum/dictionary (:spectrum figure))]
                                (map (fn [yseek]
                                       (spec/render
                                        (bezier-feature-magnet-spec
                                         (:vertical-easing figure)
                                         yseek
                                         (fn [x] (-> x (* h) js/Math.round (- 2)))
                                         (constantly 0)
                                         [(dom/div
                                           #js {:style
                                                #js {:position "absolute"
                                                     :left 0
                                                     :height 5
                                                     :width 6
                                                     :backgroundColor (spectrum/x->cssrgb
                                                                       s (* yseek -1))}})
                                          (dom/div
                                           #js {:style
                                                #js {:position "absolute"
                                                     :left 6
                                                     :height 5
                                                     :width 6
                                                     :backgroundColor (spectrum/x->cssrgb
                                                                       s yseek)}})])))
                                     [0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1])))))))))))))


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
            (line "with amplitude "
                  (dom/input #js {:type "text" :value "1 / n"
                                  :style #js {:width 30
                                              :textAlign "center"}}))
            (line " and period "
                  (dom/input #js {:type "text" :value "1 / n"
                                  :style #js {:width 30
                                              :textAlign "center"}})
                  " * " (get-in figure [:frequency :period]) " pixels."
                  (slider {:position "absolute"
                           :right 13
                           :top -15
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



(defonce roots
  (atom
   {"1-twosides" [(fn [] single-gradient) :single-sinusoidal-gradient]
    "2-sweep-grating" [(fn [] sweep-grating) :sweep-grating]
    "3-harmonic-grating" [(fn [] harmonic-grating) :harmonic-grating]}))

(defn workaround-component [_ _]
  (reify
    om/IDisplayName
    (display-name [_]
      "Om workaround dummy")

    om/IRender
    (render [_])))

(defn install-root [r]
  (let [[element-id [ff v frootm never-instrument?]] r
        el (js/document.getElementById element-id)
        methods (cond-> om/pure-methods
                        (and (not never-instrument?)
                             (:instrument? @state/app-state))
                        (instrumentation/instrument-methods state/component-data))
        descriptor (om/specify-state-methods! (clj->js methods))]
    (om/root (ff) v
             (assoc (when frootm (frootm))
               :target el
               :descriptor descriptor))))

(defn inject-root-element
  ([id ff v]
     (inject-root-element id ff v nil))
  ([id ff v frootm]
     (inject-root-element id ff v frootm false))
  ([id ff v frootm never-instrument?]
     (let [el (js/document.createElement "div")
           r [id [ff v frootm never-instrument?]]]
       (.setAttribute el "id" id)
       (.appendChild js/document.body el)
       (swap! roots conj r)
       (install-root r))))

(defn remove-root-element [id]
  (let [el (js/document.getElementById id)]
    (swap! roots dissoc id)
    (om/detach-root el)
    (-> el .-parentElement (.removeChild el))))

(defn on-code-reload []
  ;; There must be a root on the base app-state. Otherwise Om never specifies
  ;; IRenderQueue onto the atom.
  (let [ordered-roots (into [["renderqueue-workaround" [(fn []
                                                          workaround-component)
                                                        state/app-state]]]
                            @roots)]
    (mapv install-root ordered-roots))

  (doseq [[m f] {{:modifiers [:ctrl]
                  :char "j"}
                 (fn []
                   (if (-> state/app-state
                           (swap! update-in [:hood-open?] not)
                           :hood-open?)
                     (inject-root-element "app-state"
                                          (fn []
                                            state-display-component)
                                          state/app-state)
                     (remove-root-element "app-state")))
                 {:modifiers [:ctrl]
                  :char "k"}
                 (fn []
                   (if (-> state/app-state
                           (swap! update-in [:instrument?] not)
                           :instrument?)
                     (inject-root-element "component-stats"
                                          (fn []
                                            fixed-table-component)
                                          state/component-data
                                          (fn []
                                            {:opts {:extract-table
                                                    instrumentation/aggregate-update-times}})
                                          true)
                     (remove-root-element "component-stats"))
                   (page-triggers/reload-code))
                 {:modifiers [:ctrl]
                  :char "l"}
                 (fn []
                   (reset! state/component-data {}))}]
    (hotkeys/assoc-global m f)))

(defonce renderqueue-workaround
  (let [el (js/document.createElement "div")]
    (.setAttribute el "id" "renderqueue-workaround")
    (js/document.body.appendChild el)))

;; For sliders, editors, etc., it's useful to box its value in a dedicated data
;; structure so that it doesn't have to rerender every time a sibling value
;; changes.
(defonce initialize-state
  (swap! state/app-state merge
         {:hood-open? false
          :inspectors {:single-sinusoidal-gradient {:color-inspect {:selected-color nil}
                                                    :row-inspect {:is-tracking? false
                                                                  :locked {:probed-row 30}}}
                       :sweep-grating {:color-inspect {:selected-color nil}
                                       :row-inspect {:is-tracking? false
                                                     :locked {:probed-row 30}}}
                       :harmonic-grating {:color-inspect {:selected-color nil}}}
          :figures {:single-sinusoidal-gradient {:width 500
                                                 :height 256
                                                 :transition {:radius 250}
                                                 :spectrum {:left {:color [0 0 0]
                                                                   :position -1}
                                                            :right {:color [255 255 255]
                                                                    :position 1}}}
                    :sweep-grating {:width 500
                                    :height 256
                                    :wave {:form :sine}
                                    :left-period {:period 90}
                                    :right-period {:period 1}

                                    :horizontal-easing {:p1 {:x 0.25
                                                             :y 0.50}
                                                        :p2 {:x 0.70
                                                             :y 0.70}}
                                    :vertical-easing {:p1 {:x 0.25
                                                           :y 0.50}
                                                      :p2 {:x 0.70
                                                           :y 0.70}}

                                    :spectrum {:left {:color [136 136 136]
                                                      :position -1}
                                               :right {:color [170 170 170]
                                                       :position 1}}}
                    :harmonic-grating {:width 500
                                       :height 256
                                       :frequency {:period 100}
                                       :spectrum {:left {:color [136 136 136]
                                                         :position -1}
                                                  :right {:color [170 170 170]
                                                          :position 1}}
                                       :wave {:form :sine}
                                       :harmonic-magnitude "1 / n"
                                       :harmonics [1 3 5 7 9 11 13
                                                   15 17 19 21 23 25
                                                   27 29 31 33 35 37 39]}}}))

(defonce code-reload-listen
  (let [reloads (chan)]
    (tap page-triggers/code-reloads reloads)
    (go-loop []
      (<! reloads)
      (on-code-reload)
      (recur))
    :listening))
