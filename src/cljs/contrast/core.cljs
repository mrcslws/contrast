(ns contrast.core
  (:require [clojure.string :as string]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [goog.events.KeyCodes :as gkeys]
            [cljs.core.async :refer [put! chan mult tap close! <!]]
            [contrast.app-state :as state]
            [contrast.components.canvas :as cnv]
            [contrast.components.slider :refer [slider]]
            [contrast.illusions :as illusions]
            [contrast.components.row-probe :refer [row-probe]]
            [contrast.components.row-display :refer [row-display]]
            [contrast.dom :as domh]
            [contrast.common :refer [hexcode->rgb average-color]]
            [contrast.components.color-exposer :refer [color-exposer]]
            [contrast.components.eyedropper-zone :refer [eyedropper-zone]]
            [contrast.components.numvec-editable :refer [numvec-editable]]
            [contrast.components.color-picker :refer [color-picker-component]]
            [contrast.components.wave-picker :refer [wave-picker-component]]
            [contrast.components.wave-display :refer [wave-display-component]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn probed-illusion [illusion]
  (fn [config owner]
    (reify
      om/IInitState
      (init-state [_]
        {:illusion-updates (chan)
         :row-display-updates (chan)})

      om/IWillMount
      (will-mount [_]
        (let [{:keys [illusion-updates row-display-updates]} (om/get-state owner)]
          (go-loop []
            (om/set-state! owner :illusion-imagedata (<! illusion-updates))
            (recur))

          (go-loop []
            (om/set-state! owner :row-display-imagedata (<! row-display-updates))
            (recur))))

      om/IRenderState
      (render-state [_ {:keys [illusion-updates row-display-updates
                               illusion-imagedata row-display-imagedata]}]
        (dom/div nil

                 ;; TODO it's not getting the imagedata until the image updates
                 ;; once
                 (->> (row-display config {:key :probed-row} illusion-imagedata
                                   {:subscriber row-display-updates})
                      (color-exposer config row-display-imagedata)
                      (eyedropper-zone config {:key :selected-color}
                                       row-display-imagedata)
                      (dom/div #js {:style #js {:marginBottom 20
                                                :display "inline-block"
                                                :borderLeft "3px solid red"
                                                :borderRight "3px solid red"}}))

                 (->> (om/build illusion
                                config
                                {:opts {:subscriber illusion-updates}})
                      (color-exposer config illusion-imagedata)
                      (eyedropper-zone config {:key :selected-color}
                                       illusion-imagedata)
                      (row-probe config {:key :probed-row}
                                 {:track-border-only? true})))))))

(defn just-color-probe [illusion]
  (fn [config owner]
    (reify
      om/IInitState
      (init-state [_]
        {:illusion-updates (chan)})

      om/IWillMount
      (will-mount [_]
        (let [{:keys [illusion-updates]} (om/get-state owner)]
          (go-loop []
            (om/set-state! owner :illusion-imagedata (<! illusion-updates))
            (recur))))

      om/IRenderState
      (render-state [_ {:keys [illusion-updates illusion-imagedata]}]
        (dom/div nil
                 (->> (om/build illusion
                                config
                                {:opts {:subscriber illusion-updates}})
                      (color-exposer config illusion-imagedata)
                      (eyedropper-zone config {:key :selected-color}
                                       illusion-imagedata)))))))

(def probed-linear (probed-illusion illusions/single-linear-gradient))
(def probed-sinusoidal (probed-illusion illusions/single-sinusoidal-gradient))
(def probed-grating (probed-illusion illusions/sweep-grating))
(def probed-harmonic-grating (just-color-probe illusions/harmonic-grating))

(defn single-gradient [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:style #js {:marginLeft 40}}

               ;; (om/build probed-linear
               ;;           (:single-linear-gradient app))
               ;; (slider {:width 280 :marginLeft 140}
               ;;         (:single-linear-gradient app)
               ;;         (:transition-radius-schema app))
               (om/build probed-sinusoidal
                         (:single-sinusoidal-gradient app))
               (slider {:width 280 :marginLeft 140}
                       (:single-sinusoidal-gradient app)
                       {:key :transition-radius
                        :min 0
                        :max 300
                        :str-format "%dpx"
                        :interval 1})))))

(defn sweep-grating [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:style #js {:marginLeft 40}}
               (om/build probed-grating
                         (:sweep-grating app))
               (slider {:width 280 :marginLeft 140}
                       (:sweep-grating app)
                       {:key :contrast
                        :min 0
                        :max 128
                        :str-format "%.1f rgb units"
                        :interval 0.5})))))

(defn section [& els]
  (apply dom/div #js {:style #js {:marginBottom 12}}
         els))

(defn line [& els]
  (apply dom/div #js {:style #js {:position "relative"
                                  :paddingTop 2}}
         els))

(defn harmonic-grating [app owner]
  (reify
    om/IRender
    (render [_]
      (let [center-color (str "rgb("
                              (->> [(get-in app [:harmonic-grating :from-color])
                                    (get-in app [:harmonic-grating :to-color])]
                                   (map hexcode->rgb)
                                   (apply average-color)
                                   (map js/Math.round)
                                   (string/join "," ))
                              ")")]
        (dom/div nil
                 (dom/div #js {:style #js {:display "inline-block"
                                           :verticalAlign "top"}}
                          (om/build probed-harmonic-grating
                                    (:harmonic-grating app)))
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
                                            (om/build color-picker-component {:target (:harmonic-grating app)
                                                                              :schema {:key :from-color}})

                                            " and "
                                            (om/build color-picker-component {:target (:harmonic-grating app)
                                                                              :schema {:key :to-color}})
                                            ".")
                                   (section (dom/strong nil "For each n ∈ ")
                                            (numvec-editable {:width 300 :display "inline"}
                                                             (:harmonic-grating app) {:key :harmonics})
                                            (dom/div #js {:style #js {:paddingTop 6
                                                                      :marginLeft 20}}
                                                     (line "Create a " (name (get-in app [:harmonic-grating :wave])) " wave "
                                                           (om/build wave-picker-component {:target (:harmonic-grating app)
                                                                                            :schema {:key :wave}
                                                                                            :period (get-in app [:harmonic-grating :period])}))
                                                     (line "with amplitude "
                                                           (dom/input #js {:type "text" :value "1 / n"
                                                                           :style #js {:width 30
                                                                                       :textAlign "center"}}))
                                                     (line " and period "
                                                           (dom/input #js {:type "text" :value "1 / n"
                                                                           :style #js {:width 30
                                                                                       :textAlign "center"}})
                                                           " * " (get-in app [:harmonic-grating :period]) " pixels."
                                                           (slider {:position "absolute"
                                                                    :right 16
                                                                    :top 6
                                                                    :width 180
                                                                    :display "inline-block"}
                                                                   (:harmonic-grating app)
                                                                   {:key :period
                                                                    :min 1
                                                                    :max 1000
                                                                    :str-format "%dpx"
                                                                    :interval 1}))))

                                   (section (dom/strong nil "Use the sum of these waves to shift the color.")))
                          (dom/div #js {:style #js {:marginTop 12
                                                    :paddingLeft 14}}
                                   (when-let [[r g b a]
                                              (get-in app [:harmonic-grating :selected-color])]
                                     (str "Hovered color: rgba(" r "," g "," b "," a ")"))))
                 (dom/div #js {:style #js {:marginTop 12}}
                          (om/build wave-display-component (:harmonic-grating app)))
                 )))))

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
