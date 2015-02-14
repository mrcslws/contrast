(ns contrast.page-triggers
  (:require [cljs.core.async :refer [put! chan mult]]))

(defonce ^:private reloads-in (chan))
(defonce code-reloads (mult reloads-in))

(defn reload-code []
  (put! reloads-in :reload-code))
