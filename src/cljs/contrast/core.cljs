(ns contrast.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [goog.events.KeyCodes :as gkeys]
            [contrast.app-state :as state]
            [contrast.components.canvas :as cnv]
            [contrast.components.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.components.numvec-editable :refer [numvec-editable]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [contrast.components.wave-picker :refer [wave-picker-component]]
            [contrast.components.wave-display :refer [wave-display-component]]
            [contrast.components.chan-handlers :refer [chan-genrender]]
            [contrast.canvas-inspectors :as inspectors :refer [inspected]]))

(defn single-gradient [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "single-gradient")

    om/IRender
    (render [_]
      (let [data (:single-sinusoidal-gradient app)
            {:keys [width height]} data]
        (dom/div #js {:style #js {:marginLeft 40}}
                 (inspected (partial cnv/canvas data width height
                                     (illusions/single-sinusoidal-gradient-painter data))
                            data
                            (inspectors/comp (inspectors/row-display data)
                                             (inspectors/row-probe data)
                                             (inspectors/eyedropper-zone data)
                                             (inspectors/color-exposer data)))
                 (slider {:width 280 :marginLeft 140}
                         data
                         {:key :transition-radius
                          :min 0
                          :max 300
                          :str-format "%dpx"
                          :interval 1}))))))

(defn sweep-grating [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "sweep-grating")

    om/IRender
    (render [_]
      (let [data (:sweep-grating app)
            {:keys [width height]} data]
        (dom/div #js {:style #js {:marginLeft 40}}
                 (inspected (partial cnv/canvas data width height
                                     (illusions/sweep-grating-painter data))
                            data
                            (inspectors/comp (inspectors/row-display data)
                                             (inspectors/row-probe data)
                                             (inspectors/eyedropper-zone data)
                                             (inspectors/color-exposer data)))
                 (slider {:width 280 :marginLeft 140}
                         (:sweep-grating app)
                         {:key :contrast
                          :min 0
                          :max 128
                          :str-format "%.1f rgb units"
                          :interval 0.5}))))))

(defn section [& els]
  (apply dom/div #js {:style #js {:marginBottom 12}}
         els))

(defn line [& els]
  (apply dom/div #js {:style #js {:position "relative"
                                  :paddingTop 2}}
         els))

;; TODO - I'm seeing the harmonic grating get unmounted. Hmm?
;; I think it was when I hovered over the wave picker.

(defn harmonic-grating [app owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "harmonic-grating")

    om/IRender
    (render [_]
      (let [data (:harmonic-grating app)
            {:keys [width height]} data]
        (dom/div nil
                 (dom/div #js {:style #js {:display "inline-block"
                                           :verticalAlign "top"}}
                          (inspected (partial cnv/canvas data width height
                                              (illusions/harmonic-grating-painter data))
                                     data
                                     (inspectors/comp (inspectors/eyedropper-zone data)
                                                      (inspectors/color-exposer data))))
                 (dom/div #js {:style #js {:display "inline-block"
                                           :marginLeft 24}}
                          (dom/div #js {:style #js {:backgroundColor "#f2f2f2"
                                                    :background "linear-gradient(#FFFFFF 0%, #f2f2f2 100%)"
                                                    :border "1px solid #e2e2e2"
                                                    :paddingTop 8
                                                    :paddingBottom 8
                                                    :paddingLeft 14
                                                    :borderRadius 5
                                                    :font "12px/1.4 Helvetica, Arial, sans-serif"}}
                                   (section (dom/strong nil "Start with the midpoint of ")
                                            (om/build color-picker-component {:target (:spectrum data)
                                                                              :schema {:key :knob1-color}})

                                            " and "
                                            (om/build color-picker-component {:target (:spectrum data)
                                                                              :schema {:key :knob2-color}})
                                            ".")
                                   (section (dom/strong nil "For each n ∈ ")
                                            (numvec-editable {:width 300 :display "inline"}
                                                             data {:key :harmonics})
                                            (dom/div #js {:style #js {:paddingTop 6
                                                                      :marginLeft 20}}
                                                     (line "Create a " (name (:wave data)) " wave "
                                                           (om/build wave-picker-component {:target data
                                                                                            :schema {:key :wave}
                                                                                            :period (:period data)}))
                                                     (line "with amplitude "
                                                           (dom/input #js {:type "text" :value "1 / n"
                                                                           :style #js {:width 30
                                                                                       :textAlign "center"}}))
                                                     (line " and period "
                                                           (dom/input #js {:type "text" :value "1 / n"
                                                                           :style #js {:width 30
                                                                                       :textAlign "center"}})
                                                           " * " (:period data) " pixels."
                                                           (slider {:position "absolute"
                                                                    :right 16
                                                                    :top 6
                                                                    :width 180
                                                                    :display "inline-block"}
                                                                   data
                                                                   {:key :period
                                                                    :min 1
                                                                    :max 1000
                                                                    :str-format "%dpx"
                                                                    :interval 1}))))

                                   (section (line (dom/strong nil "Use the sum of these waves to choose the color:"))
                                            (line "(put scale here)")))
                          (dom/div #js {:style #js {:marginTop 12
                                                    :paddingLeft 14}}
                                   (when-let [[r g b a]
                                              (:selected-color data)]
                                     (str "Hovered color: rgba(" r "," g "," b "," a ")"))))
                 (dom/div #js {:style #js {:marginTop 12}}
                          (om/build wave-display-component data)))))))

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

(defn main []
  (om/root single-gradient state/app-state {:target (.getElementById js/document "1-twosides")})
  (om/root sweep-grating state/app-state {:target (.getElementById js/document "2-sweep-grating")})
  (om/root harmonic-grating state/app-state {:target (.getElementById js/document "3-harmonic-grating")})
  (om/root app-state-display state/app-state {:target (.getElementById js/document "app-state")}))
