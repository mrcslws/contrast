(ns contrast.pages.index
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [goog.events.KeyCodes :as gkeys]
            [contrast.page-triggers :as page-triggers]
            [contrast.common :refer [trace-rets]]
            [contrast.components.canvas :as cnv]
            [contrast.components.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.components.numvec-editable :refer [numvec-editable]]
            [contrast.components.spectrum-picker :refer [spectrum-picker]]
            [contrast.components.wave-picker :refer [wave-picker-component]]
            [contrast.components.wave-display :refer [wave-display-component]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [contrast.components.chan-handlers :refer [chan-genrender]]
            [contrast.canvas-inspectors :as inspectors :refer [inspected]]
            [cljs.core.async :refer [put! chan mult tap close! <!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; TODO switch away from "radius". "width" or "diameter" are better.

(defonce app-state
  (atom {:hood-open? false

         :single-sinusoidal-gradient {:color-inspect {:selected-color nil}
                                      :row-inspect {:locked {:probed-row 30}}
                                      :graphic {:width 500
                                                :height 256
                                                :transition-radius 250
                                                :spectrum {:left {:color [0 0 0]
                                                                  :position -1}
                                                           :right {:color [255 255 255]
                                                                   :position 1}}}}
         :sweep-grating {:color-inspect {:selected-color nil}
                         :row-inspect {:locked {:probed-row 30}}
                         :graphic {:width 500
                                   :height 256
                                   :contrast 10
                                   :spectrum {:left {:color [136 136 136]
                                                     :position -1}
                                              :right {:color [170 170 170]
                                                      :position 1}}}}

         :harmonic-grating {:color-inspect {:selected-color nil}
                            :graphic {:width 500
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

(defn single-gradient [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "single-gradient")

    om/IRender
    (render [_]
      (let [data (:single-sinusoidal-gradient app)
            gdata (:graphic data)
            {:keys [width height]} gdata]
        (dom/div nil
                 (illusion (inspected #(cnv/canvas gdata
                                                   width height
                                                   (cnv/idwriter->painter
                                                    (trace-rets
                                                     (illusions/single-sinusoidal-gradient-idwriter gdata)
                                                     %)))
                                      data
                                      (inspectors/comp (inspectors/row-display data)
                                                       (inspectors/row-probe data)
                                                       (inspectors/eyedropper-zone data)
                                                       (inspectors/color-exposer data))))
                 (algorithm (dom/div #js {:style #js {:width 280}} ;; TODO temporary hack.
                                     (section (heading "Transition from:")
                                              ;; TODO This isn't wired up to the illusion yet.
                                              (indented (line (om/build color-picker-component
                                                                        {:target (-> gdata :spectrum :left)
                                                                         :schema {:key :color}})
                                                              " <-> "
                                                              (om/build color-picker-component
                                                                        {:target (-> gdata :spectrum :right)
                                                                         :schema {:key :color}}))))
                                     (section (heading "over a distance of:")
                                              (indented (line (:transition-radius gdata) " pixels."
                                                              (slider {:position "absolute"
                                                                       :right 13
                                                                       :top -20
                                                                       :width 180}
                                                                      gdata
                                                                      {:key :transition-radius
                                                                       :min 0
                                                                       :max 250
                                                                       :str-format "%dpx"
                                                                       :interval 1})))))))))))

(defn sweep-grating [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "sweep-grating")

    om/IRender
    (render [_]
      (let [data (:sweep-grating app)
            gdata (:graphic data)
            {:keys [width height]} gdata]
        (dom/div nil
                 (illusion (inspected #(cnv/canvas gdata
                                                   width height
                                                   (cnv/idwriter->painter
                                                    (trace-rets
                                                     (illusions/sweep-grating-idwriter gdata)
                                                     %)))
                                      data
                                      (inspectors/comp (inspectors/row-display data)
                                                       (inspectors/row-probe data)
                                                       (inspectors/eyedropper-zone data)
                                                       (inspectors/color-exposer data))))
                 (algorithm (dom/div #js {:style #js {:width 170}} ;; TODO temporary hack.
                                     (section (heading "Transition from:")
                                              (indented (line (om/build color-picker-component
                                                                        {:target (-> gdata :spectrum :left)
                                                                         :schema {:key :color}})
                                                              " <-> "
                                                              (om/build color-picker-component
                                                                        {:target (-> gdata :spectrum :right)
                                                                         :schema {:key :color}})))))))))))


(defn harmonic-grating [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "harmonic-grating")

    om/IRender
    (render [_]
      (let [data (:harmonic-grating app)
            gdata (:graphic data)
            {:keys [width height]} gdata]
        (dom/div nil
                 (illusion (inspected
                            #(cnv/canvas gdata
                                         width height
                                         (cnv/idwriter->painter
                                          (trace-rets
                                           (illusions/harmonic-grating-idwriter gdata)
                                           %)))
                            data
                            (inspectors/comp (inspectors/eyedropper-zone data)
                                             (inspectors/color-exposer data))))
                 (algorithm (section (heading "For each n ∈ "
                                              (numvec-editable {:width 300 :display "inline"}
                                                               gdata {:key :harmonics}))

                                     (indented (line "Create a " (name (:wave gdata)) " wave "
                                                     (om/build wave-picker-component {:target gdata
                                                                                      :schema {:key :wave}
                                                                                      :period (:period gdata)}))
                                               (line "with amplitude "
                                                     (dom/input #js {:type "text" :value "1 / n"
                                                                     :style #js {:width 30
                                                                                 :textAlign "center"}}))
                                               (line " and period "
                                                     (dom/input #js {:type "text" :value "1 / n"
                                                                     :style #js {:width 30
                                                                                 :textAlign "center"}})
                                                     " * " (:period gdata) " pixels."
                                                     (slider {:position "absolute"
                                                              :right 13
                                                              ;; TODO the slider is really bad.
                                                              ;; This margin is needed for the animation to be seen.
                                                              :top -15
                                                              :width 180
                                                              :display "inline-block"}
                                                             gdata
                                                             {:key :period
                                                              :min 1
                                                              :max 5000
                                                              :str-format "%dpx"
                                                              :interval 1}))))

                            (section (heading "Use the sum of these waves to choose the color:")
                                     (indented (line (spectrum-picker data 360
                                                                      (inspectors/comp (inspectors/eyedropper-zone data)
                                                                                       (inspectors/color-exposer data)))))))
                 ;; (dom/div #js {:style #js {:marginTop 12
                 ;;                           :paddingLeft 14}}
                 ;;          (when-let [[r g b a]
                 ;;                     (:selected-color data)]
                 ;;            (str "Hovered color: rgba(" r "," g "," b "," a ")")))

                 (dom/div #js {:style #js {:marginTop 12}}
                          (om/build wave-display-component (select-keys gdata [:width :wave :harmonics :period]))))))))

(defn app-state->html [s]
  (if (map? s)
    (dom/div nil
             (dom/div #js {:style #js {:display "inline-block"
                                       :height "100%"
                                       :verticalAlign "top"}}
                      "{")
             (apply dom/div #js {:style #js {:display "inline-block"}}
                    (for [[k v] s]
                      (dom/div nil
                               (dom/div #js {:style #js {:verticalAlign "top"
                                                         :display "inline-block"}}
                                        (pr-str k))
                               (dom/div #js {:style #js {:marginLeft 6
                                                         :display "inline-block"}}
                                        (app-state->html v)))))
             (dom/div #js {:style #js {:display "inline-block"
                                       :height "100%"
                                       :verticalAlign "bottom"}}
                      "}"))
    (dom/div nil (pr-str s))))

;; This shouldn't be included in production unless it's updated to be
;; "pay-per-play", i.e. not running code when the display isn't toggled.

;; It's really not an acceptable component. It modifies the DOM elsewhere.
(defn app-state-display [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "app-state-display")

    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :keypress-eventkey
                     (events/listen js/document.body
                                    "keypress"
                                    (fn [e]
                                      (when (= (.-charCode e) gkeys/QUESTION_MARK)
                                        (-> (js/document.getElementById "hood-config")
                                            .-classList
                                            (cond->
                                             (:hood-open? @app)
                                             (.remove "hood-open")

                                             (not (:hood-open? @app))
                                             (.add "hood-open")))
                                        (om/transact! app :hood-open? not))))))
    om/IWillUnmount
    (will-unmount [_]
      (events/unlistenByKey (om/get-state owner :keypress-eventkey)))

    om/IRender
    (render [_]
      (app-state->html app))))

(defn render []
  (om/root single-gradient app-state {:target (.getElementById js/document "1-twosides")})
  (om/root sweep-grating app-state {:target (.getElementById js/document "2-sweep-grating")})
  (om/root harmonic-grating app-state {:target (.getElementById js/document "3-harmonic-grating")})
  (om/root app-state-display app-state {:target (.getElementById js/document "app-state")}))

(defonce render-listen
  (let [renders (chan)]
    (tap page-triggers/renders renders)
    (go-loop []
      (<! renders)
      (render)
      (recur))
    :listening))
