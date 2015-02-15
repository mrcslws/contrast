(ns contrast.page-triggers
  (:require [cljs.core.async :refer [put! chan mult]]))

(defonce ^:private reloads-in (chan))
(defonce code-reloads (mult reloads-in))

;; If `code-reloads` consumers interact with one another, then you can run into
;; issues where one consumer tears down after another handles reload.
;; Handle teardown on `before-code-reload` to avoid these issues.
;; This is not necessary with React components, but may be necessary for, say,
;; keyboard listeners on js/document.
(defonce ^:private before-reload-in (chan))
(defonce before-code-reload (mult before-reload-in))

(defn reload-code []
  (put! before-reload-in :before-reload)
  (put! reloads-in :reload-code))
