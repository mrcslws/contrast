(ns contrast.hotkeys
  (:require [goog.events :as events]
            [goog.events.KeyCodes :as gkeys]
            [cljs.core.async :refer [chan tap <!]]
            [contrast.page-triggers :as page-triggers])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def global-keydowns (atom {}))
(def global-keydowns-listening? (atom false))

(defn event-modifiers
  "Given a keydown event, return the modifier keys that were being held."
  [e]
  (into #{} (filter identity [(when (.-shiftKey e) :shift)
                              (when (.-altKey e) :alt)
                              (when (.-ctrlKey e) :ctrl)
                              (when (.-metaKey e) :meta)])))

(defn on-keydown-global [e]
  (when-let [f (get @global-keydowns
                    {:modifiers (event-modifiers e)
                     :char (-> e
                               .-keyCode
                               js/String.fromCharCode
                               .toLowerCase)})]
    (f)))

(defn ^:private ensure-listening []
  (when-not @global-keydowns-listening?
    (let [eventkey (events/listen js/document.body "keydown" on-keydown-global)
          before-reload (chan)]
      (reset! global-keydowns-listening? true)
      (tap page-triggers/before-code-reload before-reload)
      (go
        (<! before-reload)

        ;; Design decision: use the already-loaded code to clean up.
        ;; Not the newly-loaded code.
        (events/unlistenByKey eventkey)
        (reset! global-keydowns-listening? false)))))

(defn assoc-global [{:keys [modifiers char]} f]
  (ensure-listening)
  (swap! global-keydowns assoc
         {:modifiers (set modifiers) :char (.toLowerCase char)} f))
