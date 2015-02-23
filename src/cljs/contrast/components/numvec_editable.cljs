(ns contrast.components.numvec-editable
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]))

(defn numvec->str [v]
  (apply str (interpose " " v)))

(defn str->numvec [s]
  (let [v (->> (string/split s #"[\s,;]+")
               (remove empty?)
               (map js/parseInt)
               vec)]
    (when (every? number? v)
      v)))

(defn handle-change [evt target schema owner]
  (let [t (.. evt -target -value)]
    (om/set-state! owner :text t)
    (when-let [v (str->numvec t)]
      (om/update! target (:key schema) v))))

(defn numvec-editable-component [{:keys [target schema width]} owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "numvec-editable")

    om/IInitState
    (init-state [_]
      {:text (numvec->str (get target (:key schema)))})

    om/IRenderState
    (render-state [_ {:keys [text]}]
      (dom/input #js {:value text
                      :style #js {:width width}
                      :onChange #(handle-change % target schema owner)}))))

(defn numvec-editable [style target schema]
  (dom/div #js {:style (clj->js style)}
           (om/build numvec-editable-component {:target target :schema schema
                                                :width (:width style)})))
