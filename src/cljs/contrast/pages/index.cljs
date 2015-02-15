(ns contrast.pages.index
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [contrast.page-triggers :as page-triggers]
            [contrast.common :refer [trace-rets]]
            [contrast.components.canvas :as cnv]
            [contrast.components.slider :refer [slider]]
            [contrast.hotkeys :as hotkeys]
            [contrast.illusions :as illusions]
            [contrast.instrumentation :as instrumentation]
            [contrast.state :as state]
            [contrast.components.fixed-table :refer [fixed-table-component]]
            [contrast.components.numvec-editable :refer [numvec-editable]]
            [contrast.components.spectrum-picker :refer [spectrum-picker]]
            [contrast.components.state-display :refer [state-display-component]]
            [contrast.components.wave-picker :refer [wave-picker-component]]
            [contrast.components.wave-display :refer [wave-display-component]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [contrast.components.chan-handlers :refer [chan-genrender]]
            [contrast.canvas-inspectors :as inspectors :refer [inspected]]
            [cljs.core.async :refer [put! chan mult tap close! <!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; TODO switch away from "radius". "width" or "diameter" are better.

(defn illusion [& els]
  (apply dom/div #js {:style #js {:display "inline-block"}}
         els))

(defn algorithm [& els]
  (dom/div #js {:style #js {:display "inline-block"
                            :marginLeft 24
                            :verticalAlign "top"}}
           (apply dom/div #js {:style
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

(defn single-gradient [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "single-gradient")

    om/IRender
    (render [_]
      (let [figure (om/observe owner (state/figure k))
            {:keys [width height]} figure]
        (dom/div nil
                 (illusion (inspected #(cnv/canvas figure
                                                   width height
                                                   (cnv/idwriter->painter
                                                    (trace-rets
                                                     (illusions/single-sinusoidal-gradient-idwriter figure)
                                                     %)))
                                      [figure k]
                                      (inspectors/comp (inspectors/row-display k)
                                                       (inspectors/row-probe k)
                                                       (inspectors/eyedropper-zone k)
                                                       (inspectors/color-exposer k))))
                 (algorithm (dom/div #js {:style #js {:width 280}} ;; TODO temporary hack.
                                     (section (heading "Transition from:")
                                              ;; TODO This isn't wired up to the illusion yet.
                                              (indented (line (om/build color-picker-component
                                                                        {:target (-> figure :spectrum :left)
                                                                         :schema {:key :color}})
                                                              " <-> "
                                                              (om/build color-picker-component
                                                                        {:target (-> figure :spectrum :right)
                                                                         :schema {:key :color}}))))
                                     (section (heading "over a distance of:")
                                              (indented (line (:transition-radius figure) " pixels."
                                                              (slider {:position "absolute"
                                                                       :right 13
                                                                       :top -20
                                                                       :width 180}
                                                                      figure
                                                                      {:key :transition-radius
                                                                       :min 0
                                                                       :max 250
                                                                       :str-format "%dpx"
                                                                       :interval 1})))))))))))

(defn sweep-grating [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "sweep-grating")

    om/IRender
    (render [_]
      (let [figure (om/observe owner (state/figure k))
            {:keys [width height]} figure]
        (dom/div nil
                 (illusion (inspected #(cnv/canvas figure
                                                   width height
                                                   (cnv/idwriter->painter
                                                    (trace-rets
                                                     (illusions/sweep-grating-idwriter figure)
                                                     %)))
                                      [k figure]
                                      (inspectors/comp (inspectors/row-display k)
                                                       (inspectors/row-probe k)
                                                       (inspectors/eyedropper-zone k)
                                                       (inspectors/color-exposer k))))
                 (algorithm (dom/div #js {:style #js {:width 170}} ;; TODO temporary hack.
                                     (section (heading "Transition from:")
                                              (indented (line (om/build color-picker-component
                                                                        {:target (-> figure :spectrum :left)
                                                                         :schema {:key :color}})
                                                              " <-> "
                                                              (om/build color-picker-component
                                                                        {:target (-> figure :spectrum :right)
                                                                         :schema {:key :color}})))))))))))


(defn harmonic-grating [k owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "harmonic-grating")

    om/IRender
    (render [_]
      (let [figure (om/observe owner (state/figure k))
            {:keys [width height]} figure]
        (dom/div nil
                 (illusion (inspected
                            #(cnv/canvas figure
                                         width height
                                         (cnv/idwriter->painter
                                          (trace-rets
                                           (illusions/harmonic-grating-idwriter figure)
                                           %)))
                            [k figure]
                            (inspectors/comp (inspectors/eyedropper-zone k)
                                             (inspectors/color-exposer k))))
                 (algorithm (section (heading "For each n ∈ "
                                              (numvec-editable {:width 300 :display "inline"}
                                                               figure {:key :harmonics}))

                                     (indented (line "Create a " (name (:wave figure)) " wave "
                                                     (om/build wave-picker-component {:target figure
                                                                                      :schema {:key :wave}
                                                                                      :period (:period figure)}))
                                               (line "with amplitude "
                                                     (dom/input #js {:type "text" :value "1 / n"
                                                                     :style #js {:width 30
                                                                                 :textAlign "center"}}))
                                               (line " and period "
                                                     (dom/input #js {:type "text" :value "1 / n"
                                                                     :style #js {:width 30
                                                                                 :textAlign "center"}})
                                                     " * " (:period figure) " pixels."
                                                     (slider {:position "absolute"
                                                              :right 13
                                                              ;; TODO the slider is really bad.
                                                              ;; This margin is needed for the animation to be seen.
                                                              :top -15
                                                              :width 180
                                                              :display "inline-block"}
                                                             figure
                                                             {:key :period
                                                              :min 1
                                                              :max 5000
                                                              :str-format "%dpx"
                                                              :interval 1}))))

                            (section (heading "Use the sum of these waves to choose the color:")
                                     (indented (line (spectrum-picker figure 360
                                                                      (inspectors/comp (inspectors/eyedropper-zone k)
                                                                                       (inspectors/color-exposer k)))))))
                 ;; (dom/div #js {:style #js {:marginTop 12
                 ;;                           :paddingLeft 14}}
                 ;;          (when-let [[r g b a]
                 ;;                     (:selected-color data)]
                 ;;            (str "Hovered color: rgba(" r "," g "," b "," a ")")))

                 (dom/div #js {:style #js {:marginTop 12}}
                          (om/build wave-display-component (select-keys figure [:width :wave :harmonics :period]))))))))



(defonce roots
  (atom
   {"1-twosides" [(fn [] single-gradient) :single-sinusoidal-gradient]
    "2-sweep-grating" [(fn [] sweep-grating) :sweep-grating]
    "3-harmonic-grating" [(fn [] harmonic-grating) :harmonic-grating]}))

(defn workaround-component [_ _]
  (reify
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
                   (page-triggers/reload-code))}]
    (hotkeys/assoc-global m f)))

(defonce renderqueue-workaround
  (let [el (js/document.createElement "div")]
    (.setAttribute el "id" "renderqueue-workaround")
    (js/document.body.appendChild el)))

(defonce initialize-state
  (swap! state/app-state merge
         {:hood-open? false
          :inspectors {:single-sinusoidal-gradient {:color-inspect {:selected-color nil}
                                                    :row-inspect {:locked {:probed-row 30}}}
                       :sweep-grating {:color-inspect {:selected-color nil}
                                       :row-inspect {:locked {:probed-row 30}}}
                       :harmonic-grating {:color-inspect {:selected-color nil}}}
          :figures {:single-sinusoidal-gradient {:width 500
                                                 :height 256
                                                 :transition-radius 250
                                                 :spectrum {:left {:color [0 0 0]
                                                                   :position -1}
                                                            :right {:color [255 255 255]
                                                                    :position 1}}}
                    :sweep-grating {:width 500
                                    :height 256
                                    :contrast 10
                                    :spectrum {:left {:color [136 136 136]
                                                      :position -1}
                                               :right {:color [170 170 170]
                                                       :position 1}}}
                    :harmonic-grating {:width 500
                                       :height 256
                                       :period 100
                                       :spectrum {:left {:color [136 136 136]
                                                         :position -1}
                                                  :right {:color [170 170 170]
                                                          :position 1}}
                                       :wave :sine
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
